package eec.epi.scripts.valo;

import eec.epi.scripts.RatingScript;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.model.billing.*;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.rating.EDR;
import org.meveo.service.custom.CustomTableService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Sarah khabthani sarah.khabthani@iliadeconsulting.com
 */
public class RRValoTCPF extends RatingScript {

    protected static final String CH_RR_BVE_TAXECOM_PF = "CH_RR_BVE_TAXECOM_PF";
    protected static final String PX_PRIMEFIXE_BT = "PX_PRIMEFIXE_BT";
    protected static final String PX_PRIMEFIXE_HTA = "PX_PRIMEFIXE_HTA";
    protected static final String TAX_COM = "TAX_COM";
    protected static final String CF_CODE_EXPLOITATION = "cf_code_exploitation";
    public static final String CF_PUISSANCE_SOUSCRITE = "cf_puissance_souscrite";
    protected static final String TAUX = "taux";
    protected static final String CF_NATURE = "cf_nature";
    protected static final String HTA = "HTA";
    protected static final String BT = "BT";

    protected String CPT_QUERY;
    protected BigDecimal taux;
    protected BigDecimal pricePF;
    protected String subscriptionCode;
    protected Object cf_code_exploitation;


    @Override
    public void execute(RatingContext ratingContext) {
        List<Map<String, Object>> list = initializeContext(ratingContext);
        WalletOperation walletOperation = ratingContext.getWalletOperation(ratingContext);
        Subscription subscription = ratingContext.getSubscription(walletOperation);
        subscriptionCode = subscription.getCode();
        ChargeInstance chargeInstance = ratingContext.getChargeInstance(walletOperation, CH_RR_BVE_TAXECOM_PF);
        ServiceInstance productCode = chargeInstance.getServiceInstance();
        ratingContext.applyAccordingToSubscription(subscription, productCode, subscriptionCode, ratingContext);
        cf_code_exploitation = subscription.getCfValue(CF_CODE_EXPLOITATION);
        ratingContext.setPuissanceSouscrite(subscription);
        if (Objects.equals(ratingContext.getCf_nature(), BT)) {
            applyRulesForBT(ratingContext, list, cf_code_exploitation);
        }
        if (Objects.equals(ratingContext.getCf_nature(), HTA)) {
            applyRulesForHTA(ratingContext, list, cf_code_exploitation);
        }
        ratingContext.calculateTAXCOM(walletOperation, taux, ratingContext.reCalculate(pricePF, walletOperation).multiply(ratingContext.getCf_puissance_souscrite()), OperationTypeEnum.DEBIT, ratingContext.getOFFER(), ratingContext.getPROD());
    }

    private void applyRulesForHTA(RatingContext ratingContext, List<Map<String, Object>> list, Object cf_code_exploitation) {
        taux = ratingContext.getValueFromList(TAX_COM, ratingContext.getOFFER(), HTA, cf_code_exploitation, list, subscriptionCode).divide(new BigDecimal(100), RoundingMode.UNNECESSARY);
        pricePF = ratingContext.getValueFromList(PX_PRIMEFIXE_HTA, ratingContext.getOFFER(), HTA, cf_code_exploitation, list, subscriptionCode);
        pricePF = pricePF.divide(new BigDecimal(12), 0, RoundingMode.HALF_UP);
    }

    private void applyRulesForBT(RatingContext ratingContext, List<Map<String, Object>> list, Object cf_code_exploitation) {
        taux = ratingContext.getValueFromList(TAX_COM, ratingContext.getOFFER(), BT, cf_code_exploitation, list, subscriptionCode).divide(new BigDecimal(100), RoundingMode.UNNECESSARY);
        pricePF = ratingContext.getValueFromList(PX_PRIMEFIXE_BT, ratingContext.getOFFER(), BT, cf_code_exploitation, list, subscriptionCode);
        pricePF = pricePF.divide(new BigDecimal(12), 0, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> initializeContext(RatingContext ratingContext) {
        subscriptionCode = "";
        taux = BigDecimal.ZERO;
        pricePF = BigDecimal.ZERO;
        cf_code_exploitation = null;
        CPT_QUERY = " id in ( select id\n" +
                "             from ct_grille_tarifaire\n" +
                "             where CURRENT_DATE BETWEEN date_debut_validite AND date_fin_validite )\n";
        return ratingContext.getValues(CPT_QUERY, subscriptionCode);
    }
}
