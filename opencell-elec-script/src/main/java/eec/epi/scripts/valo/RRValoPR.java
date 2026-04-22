package eec.epi.scripts.valo;

import eec.epi.scripts.RatingScript;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.model.billing.OperationTypeEnum;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.service.custom.CustomTableService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
/**
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class RRValoPR extends RatingScript {
    protected static final String CT_PDS = "ct_pds";
    protected static final String PX_PRIME_RACC = "PX_PRIME_RACC";
    protected static final String PX_INDICE_BORNE_POSTE = "PX_INDICE_BORNE_POSTE";
    protected static final String TYPE_RACCORDEMENT = "type_raccordement";
    protected static final String PRIME_MENSUEL_RACCORDEMENT = "prime_mensuel_raccordement";

    protected static final String ID_PDS = "id_pds";
    protected static final String CT_GRILLE_TARIFAIRE = "ct_grille_tarifaire";
    protected static final String VALEUR = "valeur";
    protected static final String CODE = "code";
    protected static final String DATE_DEBUT_VALIDITE = "date_debut_validite";
    protected static final String DATE_FIN_VALIDITE = "date_fin_validite";
    protected static final String STATUT_PDS = "statut_pds";
    protected static final String ACTIF = "ACTIF";
    protected  String OFFER;
    protected  String PROD;
    protected List<Map<String, Object>> pdsTable;
    protected Map<String, Object> filterPrice;
    protected Map<String, Object> filterpds;
    protected String accessUserId;
    CustomTableService customTableService = getServiceInterface(CustomTableService.class.getSimpleName());


    @Override
    public void execute(RatingContext ratingContext) {

        BigDecimal unitPriceWithoutTax;
        BigDecimal qantity = BigDecimal.valueOf(1);

        accessUserId = "";
        filterPrice = new HashMap<>();
        filterpds = new HashMap<>();
        WalletOperation walletOperation = ratingContext.getWalletOperation();
        if (walletOperation == null) {
            throw new BusinessApiException("don't call this script out of rating job");
        }
        Subscription subscription = walletOperation.getSubscription();
        if (subscription == null) throw new BusinessApiException("Subscription error");
        ServiceInstance chargeCode = walletOperation.getChargeInstance().getServiceInstance();
        unitPriceWithoutTax = applyAccordingToSubscription(subscription, chargeCode);
        if (unitPriceWithoutTax != null)
            ratingContext.calculate(walletOperation, ratingContext.reCalculate(unitPriceWithoutTax,walletOperation), qantity, OperationTypeEnum.DEBIT, OFFER, PROD);
        else
            ratingContext.calculate(walletOperation, BigDecimal.ZERO, BigDecimal.ZERO, OperationTypeEnum.DEBIT, OFFER, PROD);
    }

    private BigDecimal applyAccordingToSubscription(Subscription subscription, ServiceInstance prod) {
        OfferTemplate offer = subscription.getOffer();
        if (offer == null || offer.getCode() == null)
            throw new BusinessApiException("Offer ERROR accessUserId :" + accessUserId);
        OFFER = offer.getCode();
        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        if (serviceInstances == null || serviceInstances.isEmpty())
            throw new BusinessApiException("ServiceInstances[] ERROR accessUserId :" + accessUserId);
        Optional<ServiceInstance> serviceInstance = serviceInstances.stream().filter(s -> s.getCode().equals(prod.getCode())).findFirst();
        if (serviceInstance.isEmpty() || serviceInstance.get().getCode() == null)
            throw new BusinessApiException("ServiceInstances ERROR accessUserId :" + accessUserId);
        PROD = serviceInstance.get().getCode();

        if (subscription.getAccessPoints() == null || subscription.getAccessPoints().isEmpty() || subscription.getAccessPoints().get(0) == null)
            throw new BusinessApiException("Access error accessUserId :" + accessUserId);
        accessUserId = subscription.getAccessPoints().get(0).getAccessUserId();
        filterpds.put(ID_PDS, accessUserId);
        filterpds.put(STATUT_PDS, ACTIF);
        pdsTable = getDataFromCT(CT_PDS, filterpds);
        if (pdsTable == null || pdsTable.size() != 1)
            throw new BusinessApiException("PDS Error accessUserId :" + accessUserId);
        Map<String, Object> pdsObject = pdsTable.get(0);
        if (!pdsObject.get(TYPE_RACCORDEMENT).equals("B")) {
            return null;
        }
        Object prime_mensuel_raccordement = pdsObject.get(PRIME_MENSUEL_RACCORDEMENT);
        long prime_mensuel_raccordement_val;
        try {
            if (prime_mensuel_raccordement == null)
                throw new BusinessApiException("pds.PRIME_MENSUEL_RACCORDEMENT is null accessUserId :" + accessUserId);
            else
                prime_mensuel_raccordement_val = Long.parseLong(prime_mensuel_raccordement.toString());
        } catch (NumberFormatException e) {
            throw new BusinessApiException("pds.PRIME_MENSUEL_RACCORDEMENT is not a valid number accessUserId :" + accessUserId);
        }

        return (new BigDecimal(prime_mensuel_raccordement_val)
                .multiply(getPrice(PX_INDICE_BORNE_POSTE)));
    }


    public BigDecimal getPrice(String priceCode) {
        List<Map<String, Object>> list = getDataFromCT(CT_GRILLE_TARIFAIRE, filterPrice);
        if (list == null || list.isEmpty())
            throw new BusinessApiException("CT_GRILLE_TARIFAIRE is empty accessUserId :" + accessUserId);
        List<Map<String, Object>> prices = list.stream().filter(p -> p.get(CODE).equals(priceCode) && isValid(p)).collect(Collectors.toList());
        if (prices.isEmpty())
            throw new BusinessApiException("price " + priceCode + " does not exist accessUserId :" + accessUserId);
        if (prices.size() > 1)
            throw new BusinessApiException("More than one valid price in the table : " + priceCode + " accessUserId :" + accessUserId);
        return (BigDecimal) prices.get(0).get(VALEUR);
    }

    public List<Map<String, Object>> getDataFromCT(String tableName, Map<String, Object> filters) {
        return customTableService.list(tableName, new PaginationConfiguration(filters));
    }


    private boolean isValid(Map<String, Object> p) {
        Date deb = (Date) p.get(DATE_DEBUT_VALIDITE);
        Date end = (Date) p.get(DATE_FIN_VALIDITE);
        if (deb == null || end == null) throw new BusinessApiException("Date error accessUserId :" + accessUserId);
        return new Date().before(end) && new Date().after(deb);
    }
}
