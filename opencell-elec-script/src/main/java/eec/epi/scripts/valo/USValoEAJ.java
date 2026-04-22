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
/**
 * @author Sarah khabthani sarah.khabthani@iliadeconsulting.com
 */
public class USValoEAJ extends RatingScript {

    protected static final String CT_GRILLE_TARIFAIRE = "ct_grille_tarifaire";

    protected static final String CH_US_BVE_EA_JOUR = "CH_US_BVE_EA_JOUR";
    protected static final String PX_EA_JOUR = "PX_EA_JOUR";
    private static final String BT = "BT";
    private static final String HTA = "HTA";

    protected static final String CF_CODE_EXPLOITATION = "cf_code_exploitation";

    protected Object cf_code_exploitation;
    protected BigDecimal priceEAJ;
    protected String subscriptionCode;
    protected String CPT_QUERY;

    @Override
    public void execute(RatingContext ratingContext) {
        List<Map<String, Object>> list = initializeContext(ratingContext);
        WalletOperation walletOperation = ratingContext.getWalletOperation(ratingContext);
        Subscription subscription = ratingContext.getSubscription(walletOperation);
        subscriptionCode = subscription.getCode();
        EDR edr = ratingContext.getEdr(walletOperation, subscriptionCode);
        ChargeInstance chargeInstance = ratingContext.getChargeInstance(walletOperation, CH_US_BVE_EA_JOUR);
        ServiceInstance productCode = chargeInstance.getServiceInstance();
        ratingContext.applyAccordingToSubscription(subscription, productCode, subscriptionCode,ratingContext);
        cf_code_exploitation = subscription.getCfValue(CF_CODE_EXPLOITATION);
        if (Objects.equals(ratingContext.getCf_nature(), BT)) {
            priceEAJ = ratingContext.getValueFromList(PX_EA_JOUR, ratingContext.getOFFER(), BT, cf_code_exploitation, list, subscriptionCode);
        }
        if (Objects.equals(ratingContext.getCf_nature(), HTA)) {
            priceEAJ = ratingContext.getValueFromList(PX_EA_JOUR, ratingContext.getOFFER(), HTA, cf_code_exploitation, list, subscriptionCode);
        }
        ratingContext.calculate(walletOperation, priceEAJ, edr.getQuantity(), OperationTypeEnum.DEBIT, ratingContext.getOFFER(), ratingContext.getPROD());
    }




    private List<Map<String, Object>> initializeContext(RatingContext ratingContext) {
        subscriptionCode = "";
        priceEAJ = BigDecimal.ZERO;
        CPT_QUERY = " id in ( select id\n" +
                "             from ct_grille_tarifaire\n" +
                "             where CURRENT_DATE BETWEEN date_debut_validite AND date_fin_validite )\n";
        return ratingContext.getValues(CPT_QUERY, subscriptionCode);
    }


}
