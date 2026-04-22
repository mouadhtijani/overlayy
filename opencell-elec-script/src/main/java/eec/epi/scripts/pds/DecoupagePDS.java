package eec.epi.scripts.pds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eec.epi.scripts.JobScript;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.service.crm.impl.ProviderService;
import org.meveo.service.custom.CustomTableService;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DecoupagePDS extends JobScript {
    private final ProviderService providerService = (ProviderService) getServiceInterface("ProviderService");

    private final CustomTableService customTableService = (CustomTableService) getServiceInterface(CustomTableService.class.getSimpleName());
    String TAB_INTERFACE_PDS = "ct_pds_interface";
    String TAB_PDS_TEMP = "ct_pds_temp";

    private static final String CF_STATUT = "statut";
    private static final String CF_ID_INTERFACE = "id_pds_interface";
    private static final String CF_ID_PDS = "id_pds";
    private static final String CF_DATE_PDS = "date_pds";
    private static final String CF_DATE_LAST_UPDATE_TEMP = "date_last_update";
    private static final String CF_CONTENU = "contenu";
    private static final String CF_DATE_TRAITEMENT = "date_traitement";


    private static final String CF_NOM_FICHIER = "nom_fichier";

    private static final String DECOUPE = "DECOUPE";
    private static final String A_TRAITER = "A_TRAITER";
    private static final String ID = "id";
    private static final String PDS = "Pds";
    private static final String IdPDS = "idPds";


    protected final static String REP_TRAITE_PARAM = "rep_traite";
    protected final static String REP_TRAITE_DEFAULT = "imports/ne/pds/OK";
    protected final static String FORMAT_PATTERN = "^\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}";


    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date currentDate = new Date();
    String formattedDate = dateFormat.format(currentDate);

    @Override
    public void execute(JobContext context) throws BusinessException {
        String rootPath = ParamBean.getInstance().getChrootDir(providerService.getProvider().getCode());
        String rep_pdss = (String) context.getMethodContext().getOrDefault(REP_TRAITE_PARAM, REP_TRAITE_DEFAULT);
        File inputDir = new File(rootPath, rep_pdss);
        inputDir.mkdirs();

        List<Map<String, Object>> sortedLines = getSortedLinesATraiter();
        if (sortedLines.isEmpty()) {
            log.info("Aucune ligne A_TRAITER ");
            context.reportWARN("Aucune ligne A_TRAITER ");
            return;
        }
        File[] files = inputDir.listFiles();
        if (files == null || files.length == 0) {
            log.info("Aucun fichier dans le répertoire");
            context.reportWARN("Aucun fichier dans le répertoire ");
            return;
        }
        Set<String> fileNames = Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toSet());

        ObjectMapper objectMapper = new ObjectMapper();

        for (Map<String, Object> lines : sortedLines) {
            BigInteger id = (BigInteger) lines.get(ID);
            String nom_fich = (String) lines.get(CF_NOM_FICHIER);
            String dateTrait = (String) lines.get(CF_DATE_TRAITEMENT);
            if (StringUtils.isBlank(nom_fich)) {
                context.reportWARN("Le nom de fichier est manquant ou vide.");
                continue;
            }
            if (!fileNames.contains(nom_fich)) {
                context.reportWARN("Fichier non trouvé: " + nom_fich);
                continue;
            }
            File file = new File(inputDir, nom_fich);
            String jsonContent;
            try {
                jsonContent = new String(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(jsonContent);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            JsonNode pdsArray = jsonNode.get(PDS);
            if (pdsArray.isArray()) {
                for (JsonNode pds : pdsArray) {
                    String idpdsValue = pds.get(IdPDS).asText();
                    String contenu = pds.toString();
                    alimTable(nom_fich, id, idpdsValue, extractFormattedDate(context, nom_fich, nom_fich), dateTrait, contenu, formattedDate);
                    context.reportOK("Découpage fichier " + nom_fich + " avec succès");
                }
            }

            updateCT(id);
        }


    }


    public List<Map<String, Object>> getLinesATraiter() {
        HashMap<String, Object> filters = new HashMap<>();
        filters.put(CF_STATUT, A_TRAITER);
        return customTableService.list(TAB_INTERFACE_PDS, new PaginationConfiguration(filters));
    }

    public List<Map<String, Object>> getSortedLinesATraiter() {
        List<Map<String, Object>> linesATraiter = getLinesATraiter();
        if (linesATraiter.isEmpty()) {
            return Collections.emptyList();
        } else {
            linesATraiter.sort((line1, line2) -> {
                BigInteger id1 = (BigInteger) line1.get(ID);
                BigInteger id2 = (BigInteger) line2.get(ID);
                return id1.compareTo(id2);
            });
        }
        return linesATraiter;
    }


    public String extractFormattedDate(JobContext context, String input, String nomFichier) {
        Pattern pattern = Pattern.compile(FORMAT_PATTERN);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        } else {
            log.info("Le format de la date du fichier " + nomFichier + " n'est pas valide");
            context.reportWARN("Le format de la date du fichier " + nomFichier + " n'est pas valide");
        }
        return null;
    }


    public void alimTable(String nomFichier, BigInteger id, String idpds, String datepds, String dateTrait, String contenu, String dateLastUpdate) {
        Map<String, Object> values = new HashMap<>();
        List<Map<String, Object>> valuesList = new ArrayList<>();
        values.put(CF_ID_INTERFACE, id);
        values.put(CF_ID_PDS, idpds);
        values.put(CF_DATE_PDS, datepds);
        values.put(CF_NOM_FICHIER, nomFichier);
        values.put(CF_STATUT, A_TRAITER);
        values.put(CF_DATE_TRAITEMENT, dateTrait);
        values.put(CF_CONTENU, contenu);
        values.put(CF_DATE_LAST_UPDATE_TEMP, dateLastUpdate);
        valuesList.add(values);
        customTableService.create(TAB_PDS_TEMP, TAB_PDS_TEMP, valuesList);

    }

    public void updateCT(BigInteger id) {
        Map<String, Object> valuesStatut = new HashMap<String, Object>();
        valuesStatut.put(ID, id);
        valuesStatut.put(CF_STATUT, DECOUPE);
        valuesStatut.put(CF_DATE_LAST_UPDATE_TEMP, formattedDate);
        customTableService.update(TAB_INTERFACE_PDS, valuesStatut);
    }


}
