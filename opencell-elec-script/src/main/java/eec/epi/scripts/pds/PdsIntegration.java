package eec.epi.scripts.pds;


import com.fasterxml.jackson.databind.ObjectMapper;
import eec.epi.scripts.JobScript;
import eec.epi.scripts.ResultIntegration;
import eec.epi.scripts.ValidatorUtils;
import eec.epi.scripts.pds.structure.Pds;
import eec.epi.scripts.pds.structure.enums.StatusPds;

import eec.epi.scripts.releves.structure.enums.Status;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.service.custom.CustomTableService;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;


/**
 * This class handles the integration of Pds data from "ct_pds_temp" to "ct_pds".
 *
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class PdsIntegration extends JobScript {
    protected static final String CT_PDS_TEMP = "ct_pds_temp";
    protected static final String CT_PDS = "ct_pds";
    protected static final String STATUT = "statut";
    protected static final String ID = "id";
    protected static final String DATE_LAST_UPDATE = "date_last_update";
    protected static final String CODE_ERREUR = "code_erreur";
    protected static final String MESSAGE_ERREUR = "message_erreur";
    protected static final String CONTENU = "contenu";
    protected static final String ID_PDS = "id_pds";
    protected List<Map<String, Object>> PdsTemp;
    protected List<Map<String, Object>> pdsList;
    protected SimpleDateFormat sdf;
    CustomTableService customTableService = getServiceInterface(CustomTableService.class);
    PdsValidator pdsValidator = new PdsValidator();

    @Override
    public void execute(JobContext jobContext) {
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        PdsTemp = getFromCT(CT_PDS_TEMP, prepareFilter(STATUT, Status.A_TRAITER.getValue()));
        if (PdsTemp == null || PdsTemp.isEmpty()) {
            jobContext.reportKO("Aucune ligne a traiter dans la table " + CT_PDS_TEMP);
            return;
        }
        pdsList = getFromCT(CT_PDS, prepareFilter("statut_pds", StatusPds.ACTIF.getValue()));
        if (pdsList == null) pdsList = new ArrayList<>();
        traiterEntry(jobContext, PdsTemp);
    }

    /**
     * Prepares a filter map using the provided key-value pairs.
     *
     * @param keyValue Key-value pairs for the filter.
     * @return A map containing the prepared filter.
     */
    public Map<String, Object> prepareFilter(String... keyValue) {
        return Map.of(keyValue[0], keyValue[1]);
    }

    private void traiterEntry(JobContext jobContext, List<Map<String, Object>> PdsList) {
        try {
            for (Map<String, Object> entry : PdsList) {
                Object id = entry.get(ID);
                String contenu = (String) entry.get(CONTENU);
                Pds pds = getPdsInfo(contenu);
                if (pds == null) {
                    jobContext.reportKO("CONTENU : Parse json fail, ID pds dans la table ct_pds_temp : " + id);
                    return;
                }
                ResultIntegration result = verfiyPdsBeforeIntegration(pds);
                if (!result.isResult()) {
                    makeResult(id, result, Status.FAIL);
                    jobContext.reportKO("Intégration " + entry.get("id") + ", " + result.getError());
                    continue;
                }
                Map<String, Object> pdsActif = pdsValidator.getPds(pds, pdsList);
                if (pdsActif != null) {
                    Date fin_validite_pds_Actif = parseDate(String.valueOf(pdsActif.get("fin_validite")));
                    if (fin_validite_pds_Actif != null) {
                        Date new_debut_validite = parseDate(String.valueOf(pds.getDebutValidite()));
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(new_debut_validite);
                        calendar.add(Calendar.DAY_OF_MONTH, -1);
                        new_debut_validite = calendar.getTime();
                        updatePds(pdsActif, new_debut_validite);

                    }
                }
                integratePds(jobContext, entry, id, pds, result);
            }
        } catch (Exception e) {
            throw new BusinessException("integrationPds fail : " + e.getMessage());
        }
    }

    private void updatePds(Map<String, Object> pdsActif, Date fin_validite) {
        pdsActif.put("statut_pds", "INACTIF");
        pdsActif.put("fin_validite", fin_validite);
        customTableService.update("ct_pds", pdsActif);
    }

    private void integratePds(JobContext jobContext, Map<String, Object> entry, Object id, Pds pds, ResultIntegration result) {
        IntgrateAValidPds(pds);
        makeResult(id, result, Status.SUCCESS);
        jobContext.reportOK("Intégration " + entry.get("id") + " succès");
    }

    public Date parseDate(String datestr) {
        try {
            return sdf.parse(datestr);
        } catch (ParseException ignored) {
            return null;
        }
    }

    /**
     * Integrates a valid Pds object into the target custom table "ct_pds".
     *
     * @param pds The valid Pds object to be integrated.
     */
    private void IntgrateAValidPds(Pds pds) {
        try {
            Map<String, Object> pdsDetail = new HashMap<>();
            if (!isNull(pds.getIdPds()))
                pdsDetail.put("id_pds", pds.getIdPds());
            if (!isNull(pds.getNumCompteur()))
                pdsDetail.put("num_compteur", pds.getNumCompteur());
            if (!isNull(pds.getExploitation()))
                pdsDetail.put("exploitation", pds.getExploitation());
            if (!isNull(pds.getBranchement()))
                pdsDetail.put("branchement", pds.getBranchement());
            if (!isNull(pds.getTableau()))
                pdsDetail.put("tableau", pds.getTableau());
            if (!isNull(pds.getAdresse()))
                pdsDetail.put("adresse", pds.getAdresse());
            if (!isNull(pds.getRoues()))
                pdsDetail.put("roues", pds.getRoues());
            if (!isNull(pds.getNbFils()))
                pdsDetail.put("nb_fils", pds.getNbFils());
            if (!isNull(pds.getCoefLecture()))
                pdsDetail.put("coefficient_lecture", pds.getCoefLecture());
            if (!isNull(pds.getPuissanceTransfo()))
                pdsDetail.put("puissance_transformateur", pds.getPuissanceTransfo());
            if (!isNull(pds.isIsTransfoAvantBranchement()))
                pdsDetail.put("is_transfo_avant_branchement", pds.isIsTransfoAvantBranchement() ? "AVANT" : "APRES");
            if (!isNull(pds.isTransfoIntensite()))
                pdsDetail.put("transformateur_intensite", pds.isTransfoIntensite());
            if (!isNull(pds.getGamme()))
                pdsDetail.put("gamme", pds.getGamme());
            if (!isNull(pds.getLongueurConducteur()))
                pdsDetail.put("longueur_conducteur", pds.getLongueurConducteur());
            if (!isNull(pds.getTypeRaccordement()))
                pdsDetail.put("type_raccordement", pds.getTypeRaccordement());
            if (!isNull(pds.isCoefTanPhi()))
                pdsDetail.put("coef_tan_phi", pds.isCoefTanPhi());
            if (!isNull(pds.getClasseCompteur()))
                pdsDetail.put("classe_compteur", pds.getClasseCompteur());
            if (!isNull(pds.getPrimeMensuelTransformation()))
                pdsDetail.put("prime_mensuel_transformation", pds.getPrimeMensuelTransformation());
            if (!isNull(pds.getPrimeMensuelRaccordement()))
                pdsDetail.put("prime_mensuel_raccordement", pds.getPrimeMensuelRaccordement());
            pdsDetail.put("statut_pds", StatusPds.ACTIF.getValue());
            String dateFin = "9999-12-31";
            pdsDetail.put("fin_validite", parseDate(dateFin));
            pdsDetail.put("debut_validite", parseDate(pds.getDebutValidite()));
            customTableService.create(CT_PDS, pdsDetail);
        } catch (Exception e) {
            throw new BusinessException("Fail Create data in table " + CT_PDS + " : " + e.getMessage());
        }
    }


    /**
     * Verifies if the provided Pds object is valid before integration.
     *
     * @param pds The Pds object to be verified.
     * @return A ResultIntegration object indicating the verification result.
     */
    private ResultIntegration verfiyPdsBeforeIntegration(Pds pds) {
        return pdsValidator.verfiyPdsBeforeIntegration(pds, pdsList);
    }

    /**
     * Creates a result entry based on integration status and updates the source custom table "ct_Pds_temp".
     *
     * @param id     The ID of the entry being processed.
     * @param result The integration result.
     * @param status The status of the integration.
     */
    private void makeResult(Object id, ResultIntegration result, Status status) {
        Map<String, Object> values = new HashMap<>();
        values.put(ID, id);
        if (status == Status.FAIL) {
            values.put(MESSAGE_ERREUR, result.getError());
            values.put(CODE_ERREUR, result.getCode());
        }
        values.put(STATUT, status.getValue());
        values.put(DATE_LAST_UPDATE, String.valueOf(new Date()));
        customTableService.update(CT_PDS_TEMP, values);
    }

    /**
     * Retrieves Pds information from a JSON content.
     *
     * @param contenu The JSON content containing Pds information.
     * @return A Pds object if parsing is successful, otherwise null.
     */
    private Pds getPdsInfo(String contenu) {

        try {
            return new ObjectMapper().readValue(contenu, Pds.class);
        } catch (Exception e) {
            return null;
        }
    }

    public class PdsValidator extends ValidatorUtils {

        private static final String CF_CODE_EXPLOITATION = "code_exploitation";
        private static final String TAB_CT_EXPLOITATIONS = "ct_exploitations";
        CustomTableService customTableService = getServiceInterface(CustomTableService.class);

        private ResultIntegration verfiyPdsBeforeIntegration(Pds pds, List<Map<String, Object>> pdsList) {
            if (isNullOrEmpty(pds.getIdPds()))
                return new ResultIntegration("Identifiant PDS erroné", "PDS001");
            if (isNullOrEmpty(pds.getNumCompteur()))
                return new ResultIntegration("Numéro du compteur erroné", "PDS003");
            if (isNullOrEmpty(pds.getDebutValidite()) || !Pattern.matches("\\d{4}-\\d{2}-\\d{2}", pds.getDebutValidite()))
                return new ResultIntegration("Date de début de validité erronée", "PDS004");
            if (isNullOrEmpty(pds.getExploitation()) || checkExploitation(pds))
                return new ResultIntegration("Identifiant d’exploitation erroné", "PDS006");
            if (isNullOrEmpty(pds.getBranchement()))
                return new ResultIntegration("Identifiant de branchement erroné", "PDS007");
            if (isNullOrEmpty(pds.getTableau()))
                return new ResultIntegration("Identifiant de tableau erroné", "PDS008");
            if (isNullOrEmpty(pds.getAdresse()))
                return new ResultIntegration("Adresse erronée", "PDS009");
            if (isNullOrEmpty(pds.getRoues()))
                return new ResultIntegration("Nombre de roues erroné", "PDS011");
            if (isNullOrEmpty(pds.getNbFils()))
                return new ResultIntegration("Nombre de fils erroné", "PDS013");
            if (isNullOrEmpty(pds.getCoefLecture()))
                return new ResultIntegration("Coefficient de lecture erroné", "PDS014");
            if (isNullOrEmpty(pds.getPuissanceTransfo()))
                return new ResultIntegration("Puissance du transformateur erroné", "PDS015");
            if (isNullOrEmpty(pds.isTransfoIntensite()))
                return new ResultIntegration("Transformateur d’intensité erroné", "PDS016");
            if (!formatGamme(pds))
                return new ResultIntegration("Format gamme erronée", "PDS021");

            return new ResultIntegration(true);
        }


        private boolean formatGamme(Pds pds) {
            String pattern = "\\d{3}/\\d{3}";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(pds.getGamme());
            return matcher.matches();
        }


        private Map<String, Object> getPds(Pds pds, List<Map<String, Object>> pdsList) {
            return pdsList.stream().filter(p -> p.get("id_pds").equals(pds.getIdPds())
                    && p.get("num_compteur").equals(pds.getNumCompteur())
                    && p.get("statut_pds").equals(StatusPds.ACTIF.getValue())).findFirst().orElse(null);
        }

        private boolean checkExploitation(Pds pds) {
            HashMap<String, Object> filters = new HashMap<>();
            filters.put(CF_CODE_EXPLOITATION, pds.getExploitation());

            return customTableService.list(TAB_CT_EXPLOITATIONS, new PaginationConfiguration(filters)).isEmpty();

        }


    }


}