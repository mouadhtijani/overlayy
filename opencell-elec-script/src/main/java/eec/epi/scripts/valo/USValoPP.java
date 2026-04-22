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
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class USValoPP extends RatingScript {
    protected static final String CT_GRILLE_TARIFAIRE = "ct_grille_tarifaire";
    protected static final String VALEUR = "valeur";
    protected static final String CODE = "code";
    protected static final String DATE_DEBUT_VALIDITE = "date_debut_validite";
    public static final String CF_CODE_EXPLOITATION = "cf_code_exploitation";
    protected static final String DATE_FIN_VALIDITE = "date_fin_validite";
    public static final String CT_EXPLOITATIONS = "ct_exploitations";
    public static final String PX_PRIMEFIXE_HTA = "PX_PRIMEFIXE_HTA";
    public static final String CONSO_QUANTITY = "quantite";
    public static final String CONSO_NATURE = "nature_mesure";
    protected Map<String, Object> filterPrice;
    protected String subscriptionCode;
    protected static final String ID_PDS = "id_pds";

    protected BigDecimal price;
    protected List<Map<String, Object>> exploitation;
    protected Map<String, Object> filterExploitation;
    protected static final String CT_PDS = "ct_pds";
    protected Map<String, Object> filterpds;
    protected List<Map<String, Object>> pdsTable;
    protected String CPT_QUERY;


    CustomTableService customTableService = getServiceInterface(CustomTableService.class.getSimpleName());

    @Override
    public void execute(RatingContext ratingContext) {
        List<Map<String, Object>> list = initializeContext(ratingContext);
        filterExploitation = new HashMap<>();
        WalletOperation walletOperation = ratingContext.getWalletOperation();
        if (walletOperation == null) {
            throw new BusinessApiException("don't call this script out of rating job");
        }
        Subscription subscription = walletOperation.getSubscription();
        if (subscription == null) throw new BusinessApiException("Subscription error wo : "+walletOperation.getId());
        subscriptionCode = subscription.getCode();
        ChargeInstance chargeInstance = walletOperation.getChargeInstance();
        if(chargeInstance==null)throw new BusinessApiException("chargeInstance error wo : "+walletOperation.getId());
        ServiceInstance chargeCode = chargeInstance.getServiceInstance();
        Map<String, BigDecimal> resultList = applyAccordingToSubscription(ratingContext,list,subscription, chargeCode, walletOperation);
        ratingContext.calculate(walletOperation, resultList.get("unitPriceWithoutTax"),resultList.get("quantity"), OperationTypeEnum.DEBIT, ratingContext.getOFFER(), ratingContext.getPROD());
    }
    private List<Map<String, Object>> initializeContext(RatingContext ratingContext) {
        subscriptionCode = "";
        filterExploitation = new HashMap<>();
        CPT_QUERY = " id in ( select id\n" +
                "             from ct_grille_tarifaire\n" +
                "             where CURRENT_DATE BETWEEN date_debut_validite AND date_fin_validite )\n";
        return ratingContext.getValues(CPT_QUERY, subscriptionCode);
    }

    private Map<String, BigDecimal> applyAccordingToSubscription(RatingContext ratingContext, List<Map<String, Object>> list,Subscription subscription, ServiceInstance prod, WalletOperation walletOperation) {
        OfferTemplate offer = subscription.getOffer();
        if (offer == null || offer.getCode() == null)
            throw new BusinessApiException("Offer ERROR subscription :" + subscriptionCode);
        ratingContext.setOFFER(offer.getCode());
        Object cf_code_exploitation = subscription.getCfValue(CF_CODE_EXPLOITATION);
        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        if (serviceInstances == null || serviceInstances.isEmpty())
            throw new BusinessApiException("ServiceInstances[] ERROR subscription :" + subscriptionCode);
        Optional<ServiceInstance> serviceInstance = serviceInstances.stream().filter(s -> s.getCode().equals(prod.getCode())).findFirst();
        if (serviceInstance.isEmpty() || serviceInstance.get().getCode() == null)
            throw new BusinessApiException("ServiceInstances ERROR subscription :" + subscriptionCode);
        ratingContext.setPROD(serviceInstance.get().getCode());
        EDR edr = walletOperation.getEdr();
        if (edr == null) throw new BusinessApiException("EDR Not Found ERROR  :" + subscriptionCode);
        if (edr.getQuantity() == null) throw new BusinessApiException("EDR Quantity is null  :" + subscriptionCode);
        BigDecimal unitPriceWithoutTax = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;
        if (edr.getParameter1().equals("DP")) {
                price =  ratingContext.getValueFromList(PX_PRIMEFIXE_HTA, ratingContext.getOFFER(), "HTA", cf_code_exploitation, list, subscriptionCode);
            unitPriceWithoutTax = price.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
            quantity = edr.getQuantity();
        }

        Map<String, BigDecimal> itemMap = new HashMap<>();
        itemMap.put("quantity", quantity);
        itemMap.put("unitPriceWithoutTax", unitPriceWithoutTax);
        return  itemMap;
    }

    public BigDecimal getPrice(String priceCode) {
        List<Map<String, Object>> list = getDataFromCT(CT_GRILLE_TARIFAIRE, filterPrice);
        if (list == null || list.isEmpty())
            throw new BusinessApiException("CT_GRILLE_TARIFAIRE is empty subscription :" + subscriptionCode);
        List<Map<String, Object>> prices = list.stream().filter(p -> p.get(CODE).equals(priceCode) && isValid(p)).collect(Collectors.toList());
        if (prices.isEmpty())
            throw new BusinessApiException("price " + priceCode + " does not exist subscription :" + subscriptionCode);
        if (prices.size() > 1)
            throw new BusinessApiException("More than one valid price in the table : " + priceCode + " subscription :" + subscriptionCode);
        return (BigDecimal) prices.get(0).get(VALEUR);
    }

    public List<Map<String, Object>> getDataFromCT(String tableName, Map<String, Object> filters) {
        return customTableService.list(tableName, new PaginationConfiguration(filters));
    }


    private boolean isValid(Map<String, Object> p) {
        Date deb = (Date) p.get(DATE_DEBUT_VALIDITE);
        Date end = (Date) p.get(DATE_FIN_VALIDITE);
        if (deb == null || end == null) throw new BusinessApiException("Date error accessUserId :" + subscriptionCode);
        return new Date().before(end) && new Date().after(deb);
    }
}
