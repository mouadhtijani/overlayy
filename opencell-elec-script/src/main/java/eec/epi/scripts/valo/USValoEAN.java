package eec.epi.scripts.valo;

import eec.epi.scripts.RatingScript;
import org.meveo.model.billing.*;
import org.meveo.model.rating.EDR;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
/**
 * @author Sarah khabthani sarah.khabthani@iliadeconsulting.com
 */
public class USValoEAN extends RatingScript {
    protected static final String CT_GRILLE_TARIFAIRE = "ct_grille_tarifaire";

    protected static final String CH_US_BVE_EA_NUIT = "CH_US_BVE_EA_NUIT";
    protected static final String PX_EA_NUIT = "PX_EA_NUIT";
    private static final String BT = "BT";
    private static final String HTA = "HTA";

    public static final String CF_NATURE = "cf_nature";
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
        ChargeInstance chargeInstance = ratingContext.getChargeInstance(walletOperation, CH_US_BVE_EA_NUIT);
        ServiceInstance productCode = chargeInstance.getServiceInstance();
        ratingContext.applyAccordingToSubscription(subscription, productCode, subscriptionCode,ratingContext);
        cf_code_exploitation = subscription.getCfValue(CF_CODE_EXPLOITATION);
        if (Objects.equals(ratingContext.getCf_nature(), BT)) {
            priceEAJ = ratingContext.getValueFromList(PX_EA_NUIT, ratingContext.getOFFER(), BT, cf_code_exploitation, list, subscriptionCode);
        }
        if (Objects.equals(ratingContext.getCf_nature(), HTA)) {
            priceEAJ = ratingContext.getValueFromList(PX_EA_NUIT, ratingContext.getOFFER(), HTA, cf_code_exploitation, list, subscriptionCode);
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
