package eec.epi.scripts.valo;

import eec.epi.scripts.RatingScript;
import org.meveo.model.billing.*;
import org.meveo.model.rating.EDR;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class USValoTCHC extends RatingScript {

    protected static final String CH_US_IR_TAXECOM_ENER_HC = "CH_US_IR_TAXECOM_ENER_HC";
    protected static final String PX_EA_HC = "PX_EA_HC";
    protected static final String TAX_COM = "TAX_COM";
    protected static final String CF_CODE_EXPLOITATION = "cf_code_exploitation";
    protected static final String TAUX = "taux";
    protected static final String CF_NATURE = "cf_nature";
    protected static final String BT = "BT";



    protected String CPT_QUERY;
    protected BigDecimal taux;
    protected BigDecimal priceEA;
    protected String subscriptionCode;
    protected Object cf_code_exploitation;


    @Override
    public void execute(RatingContext ratingContext) {
        List<Map<String, Object>> list = initializeContext(ratingContext);
        WalletOperation walletOperation = ratingContext.getWalletOperation(ratingContext);
        Subscription subscription = ratingContext.getSubscription(walletOperation);
        subscriptionCode = subscription.getCode();
        EDR edr = ratingContext.getEdr(walletOperation,subscriptionCode);
        ChargeInstance chargeInstance = ratingContext.getChargeInstance(walletOperation, CH_US_IR_TAXECOM_ENER_HC);
        ServiceInstance productCode = chargeInstance.getServiceInstance();
        ratingContext.applyAccordingToSubscription(subscription, productCode, subscriptionCode,ratingContext);
        cf_code_exploitation = subscription.getCfValue(CF_CODE_EXPLOITATION);
        applyRulesForBT(ratingContext, list, cf_code_exploitation);
        ratingContext.calculateTAXCOM(walletOperation, taux, edr.getQuantity().multiply(priceEA), OperationTypeEnum.DEBIT, ratingContext.getOFFER(), ratingContext.getPROD());
    }
    private void applyRulesForBT(RatingContext ratingContext, List<Map<String, Object>> list, Object cf_code_exploitation) {
        taux = ratingContext.getValueFromList(TAX_COM, ratingContext.getOFFER(), BT, cf_code_exploitation, list,subscriptionCode).divide(new BigDecimal(100), RoundingMode.UNNECESSARY);
        priceEA = ratingContext.getValueFromList(PX_EA_HC, ratingContext.getOFFER(), BT, cf_code_exploitation, list,subscriptionCode);
    }

    private List<Map<String, Object>> initializeContext(RatingContext ratingContext) {
        subscriptionCode = "";
        taux =BigDecimal.ZERO;
        priceEA = BigDecimal.ZERO;
        cf_code_exploitation=null;
        CPT_QUERY = " id in ( select id\n" +
                "             from ct_grille_tarifaire\n" +
                "             where CURRENT_DATE BETWEEN date_debut_validite AND date_fin_validite )\n";
        return ratingContext.getValues(CPT_QUERY,subscriptionCode);
    }
}
