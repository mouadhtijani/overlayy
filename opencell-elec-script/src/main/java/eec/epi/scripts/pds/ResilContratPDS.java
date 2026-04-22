package eec.epi.scripts.pds;

import eec.epi.scripts.JobScript;
import eec.epi.scripts.pds.structure.enums.StatusPds;
import eec.epi.scripts.releves.structure.enums.Status;
import liquibase.repackaged.net.sf.jsqlparser.statement.update.Update;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.mediation.Access;
import org.meveo.service.crm.impl.CustomerService;
import org.meveo.service.custom.CustomTableService;
import org.meveo.service.medina.impl.AccessService;

import java.util.*;

/**
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class ResilContratPDS extends JobScript {
    private static final String CT_PDS = "ct_pds";
    private static final String STATUT_PDS = "statut_pds";
    private static final String AUCUN_ACCESS_POINT_DANS_LA_DB = "Aucun Access Point dans la db";
    private static final String AUCUN_PDS_DANS_LA_TABLE_CT_PDS = "Aucun Pds dans la table ct_pds";
    private static final String ID_PDS = "id_pds";
    private static final String ID = "id";
    private static final String DATE_RESILIATION_CONTRAT = "date_resiliation_contrat";
    CustomTableService customTableService = getServiceInterface(CustomTableService.class);
    AccessService accessService = getServiceInterface(AccessService.class);

    protected List<Access> accessList;
    protected List<Map<String, Object>> pdsList;

    @Override
    public void execute(JobContext jobContext) {
        accessList = accessService.list();
        if (accessList == null || accessList.isEmpty()) {
            jobContext.reportWARN(AUCUN_ACCESS_POINT_DANS_LA_DB);
            return;
        }
        pdsList = getFromCT(CT_PDS, prepareFilter(STATUT_PDS, StatusPds.ACTIF.getValue()));
        if (pdsList == null || pdsList.isEmpty()) {
            jobContext.reportWARN(AUCUN_PDS_DANS_LA_TABLE_CT_PDS);
            return;
        }
        traitEntry(jobContext);

    }


    private Subscription getSubscription(String id_pds) {
        Optional<Access> first = accessList.stream().filter(a -> a.getAccessUserId() != null && a.getAccessUserId().equals(id_pds)).findFirst();
        return first.isPresent() && first.get().getSubscription() != null ? first.get().getSubscription() : null;
    }

    private Map<String, Object> prepareFilter(String... keyValue) {
        return Map.of(keyValue[0], keyValue[1]);
    }

    private void traitEntry(JobContext jobContext) {
        try {
            for (Map<String, Object> pds : pdsList) {
                Map<String, Object> values = new HashMap<>();
                Number id = (Number) pds.get(ID);
                values.put(ID, id);
                String idPds = String.valueOf(pds.get(ID_PDS));
                Subscription subscription = getSubscription(idPds);
                if (subscription == null) {
                    jobContext.reportKO("No Subscription with accesspoint.code = " + idPds);
                    continue;
                }
                if (subscription.getStatus() == SubscriptionStatusEnum.ACTIVE) continue;
                Date SubscriptionTerminationDate = subscription.getTerminationDate();
                if (SubscriptionTerminationDate == null) continue;
                values.put(DATE_RESILIATION_CONTRAT, SubscriptionTerminationDate);
                customTableService.update(CT_PDS, values);
                jobContext.reportOK("Alimentation de la date de résiliation des contrats liés aux PDS : " + idPds);
            }
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }

    }
}



