package eec.epi.scripts.releves;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eec.epi.scripts.JobScript;
import org.meveo.commons.utils.ParamBean;
import org.meveo.service.crm.impl.ProviderService;
import org.meveo.service.custom.CustomTableService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportReleves extends JobScript {
    private final ProviderService providerService = (ProviderService) getServiceInterface("ProviderService");
    private final CustomTableService customTableService = (CustomTableService) getServiceInterface("CustomTableService");

    //declaration des rep + declaration du format du nom de fichier
    protected final static String REP_RELEVES_PARAM = "rep_releves";
    protected final static String REP_RELEVES_DEFAULT = "imports/ne/releves/input";

    protected final static String REP_TRAITE_PARAM = "rep_traite";
    protected final static String REP_TRAITE_DEFAULT = "imports/ne/releves/OK";

    protected final static String REP_ERREUR_PARAM = "rep_erreur";
    protected final static String REP_ERREUR_DEFAULT = "imports/ne/releves/erreur";

    protected final static String REP_SCHEMA_PARAM = "rep_schema";
    protected final static String REP_SCHEMA_DEFAULT = "schemas";


    protected final static String FORMAT_FICHIER_PARAM = "format_fichier";
    protected final static String FORMAT_FICHIER_DEFAULT = "\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}releves.json";
    protected final static String FORMAT_PATTERN = "\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}";

    private static final String FIELD_NOM_FICHIER = "nom_fichier";
    private final static String SCHEMANAME = "schemaReleves.json";

    private static final String FIELD_STATUT = "statut";
    private static final String A_TRAITER = "A_TRAITER";
    private static final String EN_ERREUR = "EN_ERREUR";

    private static final String FIELD_DATE_TRAITEMENT = "date_traitement";
    private static final String FIELD_MESSAGE_ERREUR = "message_erreur";
    private static final String CT_RELEVE_INTERFACE = "ct_releves_interface";
    private String errorInJson;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Date currentDate = new Date();

    String formattedDate = dateFormat.format(currentDate);

    @Override
    public void execute(JobContext jobContext) {
        errorInJson = "";
        String rootPath = ParamBean.getInstance().getChrootDir(providerService.getProvider().getCode());
        String format_fichier = (String) jobContext.getMethodContext().getOrDefault(FORMAT_FICHIER_PARAM, FORMAT_FICHIER_DEFAULT);


        String rep_releves = (String) jobContext.getMethodContext().getOrDefault(REP_RELEVES_PARAM, REP_RELEVES_DEFAULT);
        File inputDir = new File(rootPath, rep_releves);
        inputDir.mkdirs();

        String rep_traite = (String) jobContext.getMethodContext().getOrDefault(REP_TRAITE_PARAM, REP_TRAITE_DEFAULT);
        File traitDir = new File(rootPath, rep_traite);
        traitDir.mkdirs();

        String rep_erreur = (String) jobContext.getMethodContext().getOrDefault(REP_ERREUR_PARAM, REP_ERREUR_DEFAULT);
        File erreurDir = new File(rootPath, rep_erreur);
        erreurDir.mkdirs();

        String rep_schema = (String) jobContext.getMethodContext().getOrDefault(REP_SCHEMA_PARAM, REP_SCHEMA_DEFAULT);
        File schemaDir = new File(rootPath, rep_schema);
        schemaDir.mkdirs();
        String schemaData = null;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(rep_schema + "/" + SCHEMANAME);
            schemaData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            jobContext.reportKO("Schéma de validation n'existe pas");
            return;
        }
        List<File> FilesFound = listFichier(inputDir);
        if (!FilesFound.isEmpty()) {
            for (File file : FilesFound) {
                if (!verfiName(file, format_fichier)) {
                    errorInJson = "nom invalide";
                    handleInvalidFileName(file, formattedDate, jobContext);
                    file.renameTo(new File(erreurDir, file.getName()));
                } else {
                    //String schemaFilePath = "./opencelldata/default/imports/ne/releves/schema/schema.json";
                    try {
                        //String schema = new String(Files.readAllBytes(Paths.get(schemaFilePath)));
                        String jsonData = new String(Files.readAllBytes(file.toPath()));
                        if (!validateJsonData(schemaData, jsonData, jobContext, file, erreurDir, traitDir)) {
                            handleInvalidStructure(file, formattedDate, jobContext);
                            file.renameTo(new File(erreurDir, file.getName()));
                            continue;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    List<String> filenames = new ArrayList<>();
                    filenames.add(file.getName());
                    listFichierSortedByDate(inputDir, filenames);
                    alimTable(file, formattedDate);
                    jobContext.reportOK("chargement " + file.getName() + " succès");
                    file.renameTo(new File(traitDir, file.getName()));
                }
            }
        } else {
            jobContext.reportWARN("Aucun fichier A_TRAITER");
            log.info("Aucun fichier A_TRAITER");
        }


    }

    /**
     * @param fReleves
     * @param format_fichier
     * @return true if names matches
     */


    protected boolean verfiName(File fReleves, String format_fichier) {
        return fReleves.getName().matches(format_fichier);
    }

    /**
     * @param inputDir
     * @return list of files
     */
    protected List<File> listFichier(File inputDir) {
        File[] allFiles = inputDir.listFiles();
        if (allFiles == null) {
            return Collections.emptyList();
        }
        return Stream.of(allFiles)
                .filter(file -> file.isFile())
                .sorted().collect(Collectors.toList());
    }

    /**
     * @param inputDir
     * @param filenames
     * @return list of files sorted by date
     */
    protected List<String> listFichierSortedByDate(File inputDir, List<String> filenames) {
        List<String> sortedFiles = new ArrayList<>();
        Pattern pattern = Pattern.compile(FORMAT_PATTERN);
        if (listFichier(inputDir).isEmpty()) {
            return Collections.emptyList();
        } else {
            Collections.sort(filenames, (filename1, filename2) -> {
                Matcher matcher1 = pattern.matcher(filename1);
                Matcher matcher2 = pattern.matcher(filename2);

                if (matcher1.find() && matcher2.find()) {
                    String dateStr1 = matcher1.group();
                    String dateStr2 = matcher2.group();

                    try {
                        Date date1 = dateFormat.parse(dateStr1);
                        Date date2 = dateFormat.parse(dateStr2);
                        return date1.compareTo(date2);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                return 0;
            });

            sortedFiles.addAll(filenames);


        }
        log.info("trier avec succes" + sortedFiles);
        return sortedFiles;

    }

    /**
     * @param file
     * @param date_trait
     */
    protected void handleInvalidFile(File file, String date_trait, JobContext jobContext) {
        String errorMessage = "chargement fichier " + file.getName() + errorInJson;
        List<Map<String, Object>> valuesList = new ArrayList<>();
        Map<String, Object> values = new HashMap<>();
        values.put(FIELD_MESSAGE_ERREUR, errorMessage);
        values.put(FIELD_STATUT, EN_ERREUR);
        values.put(FIELD_NOM_FICHIER, file.getName());
        values.put(FIELD_DATE_TRAITEMENT, date_trait);
        valuesList.add(values);
        jobContext.reportKO(errorMessage);
        customTableService.create(CT_RELEVE_INTERFACE, CT_RELEVE_INTERFACE, valuesList);
    }

    protected void handleInvalidFileName(File file, String date_trait, JobContext jobContext) {
        handleInvalidFile(file, date_trait, jobContext);
    }

    protected void handleInvalidStructure(File file, String date_trait, JobContext jobContext) {
        handleInvalidFile(file, date_trait, jobContext);
    }

    /**
     * @param fichier_releves
     * @param date_trait
     */
    protected void alimTable(File fichier_releves, String date_trait) {
        Map<String, Object> values = new HashMap<>();
        List<Map<String, Object>> valuesList = new ArrayList<>();
        values.put(FIELD_STATUT, A_TRAITER);
        values.put(FIELD_NOM_FICHIER, fichier_releves.getName());
        values.put(FIELD_DATE_TRAITEMENT, date_trait);
        valuesList.add(values);
        customTableService.create(CT_RELEVE_INTERFACE, CT_RELEVE_INTERFACE, valuesList);

    }

    //verif json structure
    private String extractFieldName(String errorMessage) {
        // Extract the field name from the error message
        String[] parts = errorMessage.split("\\[\"");
        if (parts.length > 1) {
            return parts[1].split("\"\\]")[0];
        }
        return null;
    }

    private boolean validateJsonData(String schema, String jsonData, JobContext jobContext, File file, File erreurDir, File traitDir) {
        try {
            JsonNode jsonNode = JsonLoader.fromString(jsonData);
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
            JsonSchema jsonSchema = schemaFactory.getJsonSchema(JsonLoader.fromString(schema));
            ProcessingReport report = jsonSchema.validate(jsonNode);
            if (!report.isSuccess()) {
                handleJsonValidationError(jsonData, report, jobContext);
                return false;
            }
        } catch (IOException | ProcessingException e) {
            errorInJson = " structure JSON erronée.";
            log.error("structure JSON erronée : " + e.getMessage());
            file.renameTo(new File(erreurDir, file.getName()));
            return false;
        }
        return true;
    }

    private String extractFieldNameType(String errorMessage) {
        String instanceKey = "instance: {\"pointer\":\"";
        int instanceIndex = errorMessage.indexOf(instanceKey);
        if (instanceIndex != -1) {
            int start = instanceIndex + instanceKey.length();
            int end = errorMessage.indexOf("/Releve/0/", start);
            if (end != -1) {
                int fieldNameStart = end + "/Releve/0/".length();
                int fieldNameEnd = errorMessage.indexOf("\"", fieldNameStart);
                if (fieldNameEnd != -1) {
                    String fieldName = errorMessage.substring(fieldNameStart, fieldNameEnd);
                    if (fieldName.startsWith("/")) {
                        fieldName = fieldName.substring(1);
                    }
                    String[] pathSegments = fieldName.split("/");
                    if (pathSegments.length > 0) {
                        return pathSegments[pathSegments.length - 1];
                    }
                }
            }
        }
        return null;
    }

    private void handleJsonValidationError(String jsonData, ProcessingReport report, JobContext jobContext) {
        String typeErrorMessage = report.toString();
        if (typeErrorMessage.contains("does not match any allowed primitive type")) {
            String fieldName = extractFieldNameType(typeErrorMessage);
            errorInJson = "Erreur dans le type du champ " + fieldName;
            return;
        }
        if (typeErrorMessage.contains("object instance has properties which are not allowed by the schema")) {
            errorInJson = "structure JSON erronée";
            return;
        }
        JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonObject().getAsJsonArray("Releve");
        if(jsonArray==null){
            errorInJson=" : Balise Releve";
            return;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject block = jsonArray.get(i).getAsJsonObject();
            if (!block.has("idreleve") || block.get("idreleve").isJsonNull() || block.get("idreleve").isJsonPrimitive() && !block.get("idreleve").getAsJsonPrimitive().isNumber()) {
                errorInJson = "bloc Releve " + (i + 1) + " : idreleve";
                return;
            }
            String idReleve = String.valueOf(block.get("idreleve"));
            for (ProcessingMessage message : report) {
                if (message.getLogLevel() == LogLevel.ERROR) {
                    String errorMessage = message.getMessage();
                    if (errorMessage.startsWith("object has missing required properties ([")) {
                        String fieldName = extractFieldName(errorMessage);
                        if (fieldName != null) {
                            if(fieldName.contains(",")){
                                fieldName= fieldName.substring(fieldName.lastIndexOf(",")+1);
                            }
                            String field = message.asJson().get("instance").get("pointer").asText();
                            if (field.startsWith("/Releve/" + i)) {
                                fieldName = fieldName.replaceAll("\"", "");
                                errorInJson= "Erreur dans le bloc Releve " + (i + 1) + " (idreleve = " + idReleve + ") : " + fieldName;
                                return;
                            }
                        }
                    }
                }
            }
        }

    }

}
