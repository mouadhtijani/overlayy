package eec.epi.scripts.conso;

import eec.epi.scripts.JobScript;
import eec.epi.scripts.ResultIntegration;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.mediation.Access;
import org.meveo.model.rating.EDR;
import org.meveo.model.rating.EDRStatusEnum;
import org.meveo.service.billing.impl.EdrService;
import org.meveo.service.custom.CustomTableService;
import org.meveo.service.medina.impl.AccessService;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class represents a script for processing consumption data and creating
 * EDR (Event Data Record) records based on specific business rules and conditions.
 * It extends the JobScript class and is designed to be executed within a job
 * context.
 * The class performs the following tasks:
 * - Initializes required services and data lists.
 * - Validates consumption data.
 * - Creates EDR records when specific conditions are met.
 * - Updates a custom table with the status of processed consumption data.
 * The class uses various constants, services, and enums to achieve its functionality.
 * It also provides utility methods for checking and processing consumption data.
 *
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */

public class ConsoEDR extends JobScript {


    // Constants for custom table names and field names
    protected static final String CT_CONSO = "ct_conso";
    protected static final String STATUT = "statut";

    // ... (list of constants)
    protected static final String CT_PDS = "ct_pds";
    protected static final String STATUT_PDS = "statut_pds";
    protected static final String ORIGINE = "origine";
    protected static final String OPENCELL = "OPENCELL";
    protected static final String NATURE_MESURE = "nature_mesure";
    protected static final String ER = "ER";
    protected static final String EA = "EA";
    protected static final String DP = "DP";
    protected static final String HTA = "HTA";
    protected static final String BT = "BT";
    protected static final String ID_PDS = "id_pds";
    protected static final String DATE_INDEX_PRECEDENT = "date_index_precedent";
    protected static final String DATE_RELEVE = "date_releve";
    protected static final String ORIGIN_RECORD = "originRecord";
    protected static final String POSTE_HOROSAISONNIER = "poste_horosaisonnier";
    protected static final String UNITE = "unite";
    protected static final String QUANTITE = "quantite";
    protected static final String ID = "id";
    protected static final String CF_NATURE = "cf_nature";
    protected static final String MESSAGE_ERREUR = "message_erreur";
    protected static final String CODE_ERREUR = "code_erreur";
    protected static final String AUCUNE_CONSO_DANS_LA_TABLE_CT_CONSO = "Aucune consommation à transcoder dans la table ct_conso";
    protected static final String ACTIF = "ACTIF";
    protected static final String EDR_001 = "EDR001";
    protected static final String EDR_002 = "EDR002";
    protected static final String EDR_003 = "EDR003";
    protected static final String EDR_004 = "EDR004";
    protected static final String ABSENCE_DE_PDS = "Absence de PDS Actif";
    protected static final String ABSENCE_DE_SOUSCRIPTION = "Absence de souscription active";
    protected static final String INCOHERENCE_CONSOMMATION003 = "Incohérence ct_conso.origine ou ct_conso.statut";
    protected static final String INCOHERENCE_CONSOMMATION004 = "Incohérence nature_mesure avec le domaine de tension";
    protected static final String ID_edr = "edr_id";
    protected static final String COSPHI = "COSPHI";
    protected static final String MAJO = "MAJO";
    protected static final String MINO = "MINO";
    // Lists for storing consumption data and other related data
    protected List<Map<String, Object>> consoList;
    protected List<Map<String, Object>> consoListCIMPORTE;
    protected List<Map<String, Object>> consoListCALCULE;
    protected List<Map<String, Object>> PDSList;
    protected List<Access> accessList;


    // Service instances for accessing database and custom tables
    EdrService edrService = getServiceInterface(EdrService.class);
    AccessService accessService = getServiceInterface(AccessService.class);
    CustomTableService customTableService = getServiceInterface(CustomTableService.class);

    /**
     * Executes the main logic of the script within the provided job context.
     *
     * @param jobContext The context in which the script is executed.
     */
    @Override
    public void execute(JobContext jobContext) {
        // Initialize data
        init();
        // Check if consumption data is available
        if (consoList == null || consoList.isEmpty()) {
            jobContext.reportWARN(AUCUNE_CONSO_DANS_LA_TABLE_CT_CONSO);
            return;
        }
        // Process each consumption record
        for (Map<String, Object> conso : consoList) {
            ResultIntegration result = isvalidConso(conso);
            // If the consumption data is not valid : report an error if conso BT with nature = "ER" else set INFO and report warning
            // Else create an EDR and repport OK
            if (!result.isResult()) {
                updateTableConso(conso, ConsoStatut.ERREUR.getValue(), result, null);
                jobContext.reportKO(result.getError() + " pour la conso " + conso.get(ID));
                continue;
            }
            String voltageRange = selectVoltageRange(String.valueOf(conso.get(ID_PDS)));
            if (voltageRange == null) {
                jobContext.reportKO("Subscription.cf_nature is null pour l'Access : " + conso.get(ID_PDS));
                continue;
            }
            result = verifvoltageRangeInCT_CONSO(conso, voltageRange);
            if (!result.isResult()) {
                if (voltageRange.equals(HTA) && (result.getCode().equals(EDR_004) || result.getCode().equals(EDR_003))) {
                    updateTableConso(conso, ConsoStatut.INFO.getValue(), null, null);
                    jobContext.addWARN();
                    continue;
                }
                updateTableConso(conso, ConsoStatut.ERREUR.getValue(), result, null);
                jobContext.reportKO(result.getError() + " pour la conso " + conso.get(ID));
                continue;
            }
            EDR edr = createEDR(conso);
            if (edr == null) continue;
            updateTableConso(conso, ConsoStatut.VALORISE.getValue(), result, edr);
            jobContext.addOK();
        }
        if (jobContext.nbOK() > 0)
            jobContext.addReport("OK : " + jobContext.nbOK() + " consommations transcodées en EDR");
        if (jobContext.nbWarn() > 0)
            jobContext.addReport("WARN : " + jobContext.nbWarn() + " consommations passées au statut informatif");

    }

    /**
     * check for inconsistencies in ct_conso.origin or ct_conso.status
     * check whether there is a nature_measurement inconsistency with the voltage domain
     *
     * @param conso        A map representing the consumption data record.
     * @param voltageRange "BT" or "HTA"
     * @return A ResultIntegration object indicating the validation result.
     */
    private ResultIntegration verifvoltageRangeInCT_CONSO(Map<String, Object> conso, String voltageRange) {
        //check for inconsistencies in ct_conso.origin or ct_conso.status
        if (
                (HTA.equals(voltageRange) && !(conso.get(ORIGINE).equals(OPENCELL) && conso.get(STATUT).equals(ConsoStatut.CALCULE.getValue())))
                        ||
                        (BT.equals(voltageRange) && !(!conso.get(ORIGINE).equals(OPENCELL) && conso.get(STATUT).equals(ConsoStatut.IMPORTE.getValue())))
        ) {
            return new ResultIntegration(INCOHERENCE_CONSOMMATION003, EDR_003);
        }

        //check whether there is a nature_measurement inconsistency with the voltage domain(BT)
        if (voltageRange.equals(BT) && !String.valueOf(conso.get(NATURE_MESURE)).equals(EA)) {
            return new ResultIntegration(INCOHERENCE_CONSOMMATION004, EDR_004);
        }
        //check whether there is a nature_measurement inconsistency with the voltage domain(HTA)
        if (voltageRange.equals(HTA) && !(String.valueOf(conso.get(NATURE_MESURE)).equals(EA) || String.valueOf(conso.get(NATURE_MESURE)).equals(DP) || String.valueOf(conso.get(NATURE_MESURE)).equals(COSPHI))) {
            return new ResultIntegration(INCOHERENCE_CONSOMMATION004, EDR_004);
        }
        return new ResultIntegration(true);
    }

    /**
     * Verify if cf nature have "BT" or "HTA" value
     *
     * @param id_pds The ID_PDS to be checked for selecting subscription .
     * @return "BT" or "HTA"
     */
    private String selectVoltageRange(String id_pds) {
        Subscription subscription = getSubscription(id_pds);
        if (subscription == null || subscription.getCfValue(CF_NATURE) == null) return null;
        if (subscription.getCfValue(CF_NATURE).equals(BT)) return BT;
        if (subscription.getCfValue(CF_NATURE).equals(HTA)) return HTA;
        return null;
    }


    /**
     * Initializes the necessary data and services for the script execution.
     * - Retrieves a list of Access objects.
     * - Retrieves a list of consumption data records from a custom table based on a specified filter. statut = "CALCULE" & "IMPORTE"
     * - Retrieves a list of PDS (Product Delivery System) records from a custom table based on a specified filter. statut_pds = "ACTIF"
     */
    private void init() {
        consoList = new ArrayList<>();
        consoListCALCULE = getFromCT(CT_CONSO, prepareFilter(STATUT, ConsoStatut.CALCULE.getValue()));
        if (consoListCALCULE != null && !consoListCALCULE.isEmpty())
            consoList.addAll(consoListCALCULE);
        consoListCIMPORTE = getFromCT(CT_CONSO, prepareFilter(STATUT, ConsoStatut.IMPORTE.getValue()));
        if (consoListCIMPORTE != null && !consoListCIMPORTE.isEmpty())
            consoList.addAll(consoListCIMPORTE);
        accessList = accessService.list();
        PDSList = getFromCT(CT_PDS, prepareFilter(STATUT_PDS, ACTIF));
    }

    /**
     * Validates the consumption data record to ensure it meets specific criteria.
     * - Checks if the ID_PDS field is present and valid.
     * - Verifies if a subscription exists for the given ID_PDS.
     * - Check if the nature is consistent with the contract
     *
     * @param conso A map representing the consumption data record.
     * @return A ResultIntegration object indicating the validation result.
     */
    private ResultIntegration isvalidConso(Map<String, Object> conso) {
        //PDS linked to consumption does not exist in Opencell or is not active
        if (String.valueOf(conso.get(ID_PDS)) == null || notValidPDS(String.valueOf(conso.get(ID_PDS))))
            return new ResultIntegration(ABSENCE_DE_PDS, EDR_001);
        //Subscription linked to consumption does not exist in Opencell or is not active
        if (getSubscription(String.valueOf(conso.get(ID_PDS))) == null)
            return new ResultIntegration(ABSENCE_DE_SOUSCRIPTION, EDR_002);
        return new ResultIntegration(true);
    }


    /**
     * Checks if a given ID_PDS is not valid by comparing it against a list of PDS records.
     *
     * @param idpds The ID_PDS to be checked for validity.
     * @return True if the ID_PDS is not valid; otherwise, false.
     */
    private boolean notValidPDS(String idpds) {
        if (PDSList == null || PDSList.isEmpty()) return true;
        return PDSList.stream().filter(pds -> idpds.equals(pds.get(ID_PDS))).findFirst().isEmpty();
    }

    private EDR setParamAndQuantity(EDR edr, Map<String, Object> conso) {
        if (conso.get(NATURE_MESURE).equals(DP)) {
            edr.setDateParam1((Date) conso.get(DATE_RELEVE));
            edr.setParameter1(DP);
            return edr;
        }
        if (conso.get(NATURE_MESURE).equals(COSPHI)) {
            edr.setParameter1(COSPHI);
            edr.setDateParam1((Date) conso.get(DATE_RELEVE));
            double cosphiValue = Double.parseDouble(String.valueOf(conso.get(QUANTITE)));
            if (cosphiValue > 0.90) {
                edr.setParameter4(MINO);
                BigDecimal quantity = (new BigDecimal(cosphiValue).subtract(new BigDecimal("0.90")))
                        .divide(new BigDecimal("5"), new MathContext(10, RoundingMode.HALF_UP));
                edr.setQuantity(quantity);
            } else if (cosphiValue < 0.80) {
                edr.setParameter4(MAJO);
                BigDecimal quantity = (new BigDecimal("0.80").subtract(new BigDecimal(cosphiValue)))
                        .divide(new BigDecimal("100"), new MathContext(10, RoundingMode.HALF_UP));
                edr.setQuantity(quantity);
            } else {
                return null;
            }
            return edr;
        }
        edr.setDateParam1((Date) conso.get(DATE_RELEVE));
        edr.setDateParam2((Date) conso.get(DATE_INDEX_PRECEDENT));
        edr.setOriginRecord(String.valueOf(conso.get(ORIGIN_RECORD)));
        edr.setParameter1(String.valueOf(conso.get(NATURE_MESURE)));
        edr.setParameter2(String.valueOf(conso.get(POSTE_HOROSAISONNIER)));
        edr.setParameter3(String.valueOf(conso.get(UNITE)));

        return edr;
    }

    /**
     * Creates an Event Data Record (EDR) based on the consumption data.
     * - Sets various EDR attributes such as status, access code, dates, and parameters.
     *
     * @param conso A map representing the consumption data record.
     */
    private EDR createEDR(Map<String, Object> conso) {
        EDR edr = new EDR();
        edr.setStatus(EDRStatusEnum.OPEN);
        edr.setAccessCode(String.valueOf(conso.get(ID_PDS)));
        edr.setCreated(new Date());
        edr.setEventDate(conso.get(DATE_RELEVE) == null ? new Date() : (Date) conso.get(DATE_RELEVE));
        edr.setQuantity(new BigDecimal(String.valueOf(conso.get(QUANTITE))));
        Subscription subscription = getSubscription(String.valueOf(conso.get(ID_PDS)));
        edr.setSubscription(subscription);
        edr.setTimesTried(0);
        edr.setVersion(1);
        edr = setParamAndQuantity(edr, conso);
        if (edr == null) return null;
        edr.setOriginRecord(String.valueOf(conso.get(ID)));
        edrService.create(edr);
        return edr;
    }

    /**
     * Retrieves a subscription based on the provided ID_PDS.
     *
     * @param id_pds The ID_PDS for which to retrieve the subscription.
     * @return The Subscription object if found; otherwise, null.
     */
    private Subscription getSubscription(String id_pds) {
        Optional<Access> first = accessList.stream().filter(a -> a.getAccessUserId() != null && a.getAccessUserId().equals(id_pds)).findFirst();
        return first.isPresent() && first.get().getSubscription() != null && first.get().getSubscription().getStatus().equals(SubscriptionStatusEnum.ACTIVE) ? first.get().getSubscription() : null;
    }

    /**
     * Updates the custom table containing consumption data records with a new status and, optionally, error details.
     *
     * @param conso  A map representing the consumption data record to be updated.
     * @param statut The new status to set for the record.
     * @param result A ResultIntegration object containing error details (if any).
     * @param edr
     */
    private void updateTableConso(Map<String, Object> conso, String statut, ResultIntegration result, EDR edr) {
        Map<String, Object> values = new HashMap<>();
        values.put(ID, conso.get(ID));
        values.put(STATUT, statut);
        if (edr != null && edr.getId() != null) {
            values.put(ID_edr, edr.getId());
        }
        if (result != null) {
            values.put(MESSAGE_ERREUR, result.getError());
            values.put(CODE_ERREUR, result.getCode());
        }
        customTableService.update(CT_CONSO, values);
    }


    /**
     * Prepares a filter map with key-value pairs for querying a custom table.
     *
     * @param keyValue An array of key-value pairs to be included in the filter.
     * @return A map containing the filter criteria.
     */
    private Map<String, Object> prepareFilter(String... keyValue) {
        return Map.of(keyValue[0], keyValue[1]);
    }

    /**
     * Enumeration representing possible consumption status values.
     */
    private enum ConsoStatut {
        CALCULE("CALCULE"), IMPORTE("IMPORTE"), VALORISE("VALORISE"), ERREUR("ERREUR"), INFO("INFO");

        public String getValue() {
            return value;
        }

        private final String value;

        ConsoStatut(String value) {
            this.value = value;
        }
    }
}
