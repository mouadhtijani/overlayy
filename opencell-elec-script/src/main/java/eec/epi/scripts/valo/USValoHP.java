package eec.epi.scripts.valo;

import eec.epi.scripts.RatingScript;
import org.meveo.model.billing.*;
import org.meveo.model.rating.EDR;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class USValoHP extends RatingScript {


    protected static final String CT_GRILLE_TARIFAIRE = "ct_grille_tarifaire";

    protected static final String CH_US_IR_ENER_HP = "CH_US_IR_ENER_HP";
    protected static final String PX_EA_HP = "PX_EA_HP";
    private static final String BT = "BT";



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
        ChargeInstance chargeInstance = ratingContext.getChargeInstance(walletOperation, CH_US_IR_ENER_HP);
        ServiceInstance productCode = chargeInstance.getServiceInstance();
        ratingContext.applyAccordingToSubscription(subscription, productCode, subscriptionCode,ratingContext);
        cf_code_exploitation = subscription.getCfValue(CF_CODE_EXPLOITATION);
        priceEAJ = ratingContext.getValueFromList(PX_EA_HP, ratingContext.getOFFER(), BT, cf_code_exploitation, list, subscriptionCode);
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
