package eec.epi.scripts.releves;


import com.fasterxml.jackson.databind.ObjectMapper;
import eec.epi.scripts.JobScript;
import eec.epi.scripts.ResultIntegration;
import eec.epi.scripts.pds.structure.enums.StatusPds;
import eec.epi.scripts.releves.structure.DonneesMesure;
import eec.epi.scripts.releves.structure.Releve;
import eec.epi.scripts.releves.structure.enums.Status;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.Subscription;
import org.meveo.model.crm.Customer;
import org.meveo.model.mediation.Access;
import org.meveo.service.crm.impl.CustomerService;
import org.meveo.service.custom.CustomTableService;
import org.meveo.service.medina.impl.AccessService;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


import static java.util.Objects.isNull;


/**
 * This class handles the integration of releves data from "ct_releves_temp" to "ct_releves".
 *
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class RelevesIntegration extends JobScript {
    // Constants for custom table and field names
    protected static final String CT_RELEVES_TEMP = "ct_releves_temp";
    protected static final String CT_RELEVES = "ct_releves";
    protected static final String STATUT = "statut";
    protected static final String ID = "id";
    protected static final String DATE_LAST_UPDATE = "date_last_update";
    protected static final String DATE_TRAITEMENT = "date_traitement";
    protected static final String CODE_ERREUR = "code_erreur";
    protected static final String MESSAGE_ERREUR = "message_erreur";
    protected static final String CONTENU = "contenu";
    protected static final String ID_RELEVE = "id_releve";
    protected static final String NUM_COMPTEUR = "num_compteur";
    protected static final String DATE_RELEVE = "date_releve";
    protected static final String NATURE_RELEVE = "nature_releve";
    protected static final String ORIGINE = "origine";
    protected static final String ID_PDS = "id_pds";
    protected static final String CODE_RELEVE = "code_releve";
    protected static final String CODE_OBSERVATION = "code_observation";
    protected static final String CODE_RFE = "code_rfe";
    protected static final String LOT = "lot";
    protected static final String TOURNEE = "tournee";
    protected static final String EXPLOITATION = "exploitation";
    protected static final String NATURE_MESURE = "nature_mesure";
    protected static final String MESURE = "mesure";
    protected static final String UNITE = "unite";
    protected static final String STATUT_RELEVE = "statut_releve";
    protected static final String POSTE_HOROSAISONNIER = "poste_horosaisonnier";
    protected static final String SQL_RELEVE = "id in(select id from ct_releves where statut_releve in ('VALIDE','CALCULE'))";
    public static final String SQL = "SQL";
    protected List<Map<String, Object>> relevesTemp;
    protected List<Map<String, Object>> releves;
    protected List<Map<String, Object>> pdsList;
    protected ReleveValidator validator;
    protected List<Access> accessList;
    protected String customerName;
    protected SimpleDateFormat sdf;
    CustomTableService customTableService = getServiceInterface(CustomTableService.class);
    AccessService accessService = getServiceInterface(AccessService.class);
    CustomerService customerService = getServiceInterface(CustomerService.class);

    @Override
    public void execute(JobContext jobContext) {
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        customerName = null;
        accessList = accessService.list();
        pdsList = getFromCT("ct_pds", prepareFilter("statut_pds", StatusPds.ACTIF.getValue()));
        validator = new ReleveValidator();
        relevesTemp = getFromCT(CT_RELEVES_TEMP, prepareFilter(STATUT, Status.A_TRAITER.getValue()));
        if (relevesTemp == null || relevesTemp.isEmpty()) {
            jobContext.reportKO("Aucune ligne a traiter dans la table " + CT_RELEVES_TEMP);
            return;
        }
        if (pdsList == null || pdsList.isEmpty()) {
            jobContext.reportKO("Aucune pds actif dans la table ct_pds");
            return;
        }
        releves = getFromCT(CT_RELEVES, prepareFilter(SQL,SQL_RELEVE));
        if (releves == null) releves = new ArrayList<>();
        traiterEntry(jobContext, relevesTemp);
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

    private void traiterEntry(JobContext jobContext, List<Map<String, Object>> releveList) {
        try {
            for (Map<String, Object> entry : releveList) {
                Object id = entry.get(ID);
                Releve releve = getReleveInfo((String) entry.get(CONTENU));
                if (releve == null) {
                    jobContext.reportKO("CONTENU : Parse json fail, ID Releve dans la table ct_releves_temp : " + id);
                    return;
                }
                ResultIntegration result = verfiyReleveBeforeIntegration(releve);
                if (!result.isResult()) {
                    makeResult(id, result, Status.FAIL);
                    String errorRecord = "";
                    if (id != null) errorRecord = "Intégration " + entry.get("id") + " ";
                    jobContext.reportKO(errorRecord + result.getError());
                    continue;
                }
                IntgrateAValidReleve(releve);
                makeResult(id, result, Status.SUCCESS);
                jobContext.reportOK("Intégration " + entry.get("id") + " succès");

            }
        } catch (Exception e) {
            throw new BusinessException("integrationReleves fail : " + e.getMessage());
        }
    }

    /**
     * Integrates a valid Releve object into the target custom table "ct_releves".
     *
     * @param releve The valid Releve object to be integrated.
     */
    private void IntgrateAValidReleve(Releve releve) {
        try {
            boolean hasCodeReleve = !isNull(releve.getCodeReleve());
            boolean hasCodeObservation = !isNull(releve.getCodeObservation());
            boolean hasCodeRfe = !isNull(releve.getCodeRfe());
            boolean hasExploitation = !isNull(releve.getExploitation());
            List<DonneesMesure> donneesMesures = releve.getDonneesMesures();
            for (DonneesMesure donneesMesure : donneesMesures) {
                Map<String, Object> releveDetail = new HashMap<>();
                releveDetail.put(ID_RELEVE, releve.getIdReleve());
                releveDetail.put(NUM_COMPTEUR, releve.getNumCompteur());
                releveDetail.put(DATE_RELEVE, validator.parseDate(releve.getDateReleve()));
                releveDetail.put(NATURE_RELEVE, releve.getNatureReleve());
                releveDetail.put(ORIGINE, releve.getOrigineReleve());
                releveDetail.put(ID_PDS, releve.getIdPds());
                if (hasCodeReleve) releveDetail.put(CODE_RELEVE, releve.getCodeReleve());
                if (hasCodeObservation) releveDetail.put(CODE_OBSERVATION, releve.getCodeObservation());
                if (hasCodeRfe) releveDetail.put(CODE_RFE, releve.getCodeRfe());
                if (!isNull(releve.getLot()))
                    releveDetail.put(LOT, releve.getLot());
                if (!isNull(releve.getTournee()))
                    releveDetail.put(TOURNEE, releve.getTournee());
                if (hasExploitation) releveDetail.put(EXPLOITATION, releve.getExploitation());
                releveDetail.put(POSTE_HOROSAISONNIER, donneesMesure.getPosteHorosaisonnier());
                releveDetail.put(NATURE_MESURE, donneesMesure.getNatureMesure());
                releveDetail.put(MESURE, donneesMesure.getMesure());
                releveDetail.put(UNITE, donneesMesure.getUnite());
                releveDetail.put(STATUT_RELEVE, releve.getNatureReleve().equals("POSE") ? Status.CALCULE.getValue() : Status.VALIDE.getValue());
                releves.add(releveDetail);
                customTableService.create(CT_RELEVES, releveDetail);
            }
        } catch (Exception e) {
            throw new BusinessException("Fail Create data in table " + CT_RELEVES + " : " + e.getMessage());
        }
    }


    /**
     * Verifies if the provided Releve object is valid before integration.
     *
     * @param releve The Releve object to be verified.
     * @return A ResultIntegration object indicating the verification result.
     */
    private ResultIntegration verfiyReleveBeforeIntegration(Releve releve) {

        if (validator.isNotValidIdReleve(releve.getIdReleve()))
            return new ResultIntegration("Identifiant du relevé erroné", "RELEVE001");
        if (validator.isNotValidNumCompteur(releve.getNumCompteur()))
            return new ResultIntegration("Numéro du compteur erroné", "RELEVE002");
        if (validator.isNotValidDateReleve(releve.getDateReleve()))
            return new ResultIntegration("Date de relevé erronée", "RELEVE003");
        if (validator.isNotValidNatureReleve(releve.getNatureReleve()))
            return new ResultIntegration("Nature de relevé erronée", "RELEVE004");
        if (validator.isNotValidOrigineReleve(releve.getOrigineReleve()))
            return new ResultIntegration("Origine de relevé erronée", "RELEVE005");
        if (validator.isNotValidIdPds(releve.getIdPds()))
            return new ResultIntegration("Identifiant PDS erroné", "RELEVE006");
        if (!checkPds(String.valueOf(releve.getIdPds())))
            return new ResultIntegration("Absence de PDS actif", "RELEVE021");
        if (validator.isNotValidDonneesMesures(releve.getDonneesMesures()))
            return new ResultIntegration("Objet donneeMesures vide", "RELEVE019");
        return isNotValidDonneesMesuresElements(releve);
    }


    /**
     * Verifies if the provided Releve.DonneesMesure object is valid before integration.
     *
     * @param releve The Releve object to be verified.
     * @return A ResultIntegration object indicating the verification result.
     */
    private ResultIntegration isNotValidDonneesMesuresElements(Releve releve) {
        List<DonneesMesure> donneesMesures = releve.getDonneesMesures();
        for (DonneesMesure donneesMesure : donneesMesures) {
            if (validator.isNotValidPosteHorosaisonnier(donneesMesure.getPosteHorosaisonnier()))
                return new ResultIntegration("Poste Horosaisonnier erroné", "RELEVE013");
            if (validator.isNotValidNatureMesure(donneesMesure.getNatureMesure()))
                return new ResultIntegration("Nature de la mesure erronée", "RELEVE014");
            if (validator.isNotValidMesure(donneesMesure.getMesure()))
                return new ResultIntegration("Mesure erronée", "RELEVE015");
            if (validator.isNotValidUnite(donneesMesure.getUnite()))
                return new ResultIntegration("Unite erronée", "RELEVE016");
        }
        if (validator.isNotValidDateReleveIndex(releves, releve))
            return new ResultIntegration("Index plus récent déjà existant", "RELEVE017");
        if (validator.isNotValidNatureReleveIndex(releves, releve))
            return new ResultIntegration("Aucun index précédent trouvé. Vérifier index de pose", "RELEVE018");
        return new ResultIntegration(true);
    }

    /**
     * Creates a result entry based on integration status and updates the source custom table "ct_releves_temp".
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
        customTableService.update(CT_RELEVES_TEMP, values);
    }

    /**
     * Retrieves Releve information from a JSON content.
     *
     * @param contenu The JSON content containing Releve information.
     * @return A Releve object if parsing is successful, otherwise null.
     */
    private Releve getReleveInfo(String contenu) {
        try {
            return new ObjectMapper().readValue(contenu, Releve.class);
        } catch (Exception e) {
            return null;
        }
    }


    private boolean checkPds(String id_pds) {
        return pdsList.stream()
                .anyMatch(p -> p.get("id_pds").equals(id_pds));
    }

}