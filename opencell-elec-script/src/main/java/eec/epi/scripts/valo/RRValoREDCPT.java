package eec.epi.scripts.valo;

import eec.epi.scripts.RatingScript;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.model.billing.*;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.service.custom.CustomTableService;

import java.math.BigDecimal;
import java.util.*;
/**
 * @author Sarah khabthani sarah.khabthani@iliadeconsulting.com
 */
public class RRValoREDCPT extends RatingScript {
    private static final String CT_PDS = "ct_pds";
    private static final String MONOPHASE = "Monophase";
    private static final String TRIPHASE = "TRIPHASE";

    private static final String TRANSFORMATEUR_INTENSITE = "transformateur_intensite";
    private static final String BT = "BT";
    private static final String HTA = "HTA";
    private static final String GAMME = "gamme";
    private static final String NB_FILS = "nb_fils";
    private static final String ID_PDS = "id_pds";
    private static final String CT_GRILLE_TARIFAIRE = "ct_grille_tarifaire";
    private static final String VALEUR = "valeur";
    private static final String CODE = "code";
    private static final String STATUT_PDS = "statut_pds";
    private static final String ACTIF = "ACTIF";
    private static final String SQL = "SQL";
    private static final String CF_NATURE = "cf_nature";
    private static final String PRICE_ONE = "price_one";
    private static final String PRICE_TWO = "price_two";
    private static final String GET_DATA_FROM_CT_ERROR = "getDataFromCT error : ";
    private static final String CF_NATURE_ERROR_ID_PDS = "cf_nature ERROR id_pds :";
    private static final String CT_GRILLE_TARIFAIRE_IS_EMPTY_ACCESS_USER_ID = "CT_GRILLE_TARIFAIRE is empty accessUserId :";
    private static final String ONE_OF_THIS_PRICES_DOES_NOT_EXIST = "One of this prices does not exist";
    private static final String PAS_DE_PRIX_QUI_CORRESPOND_AU_NOMBRE_DE_FILS_ID_PDS = "Pas de prix qui correspond au nombre de fils id_pds:";
    private static final String CLASSE_COMPTEUR = "classe_compteur";
    private static final String CLASSE_COMPTEUR_1_OU_0_5 = "Classe compteur != 1 ou 0.5 ";
    public static final String RED_CPT_CL_1 = "RED_CPT_CL1";
    public static final String RED_CPT_CL_05 = "RED_CPT_CL05";
    public static final String MORE_THAN_ONE_VALID_PRICE_IN_THE_TABLE = "More than one valid price in the table";
    private static final String ENT_CPT_MONO = "ENT_CPT_MONO";
    private static final String LOC_CPT_MONO = "LOC_CPT_MONO";
    private static final String ENT_CPT_TI = "ENT_CPT_TI";
    private static final String LOC_CPT_TI = "LOC_CPT_TI";
    private static final String ENT_CPT_TRI_SUP = "ENT_CPT_TRI_SUP";
    private static final String LOC_CPT_TRI_SUP = "LOC_CPT_TRI_SUP";
    private static final String ENT_CPT_TRI_INF = "ENT_CPT_TRI_INF";
    private static final String LOC_CPT_TRI_INF = "LOC_CPT_TRI_INF";
    private static final String G030_000 = "030/000";
    private static final String SUBSCRIPTION_ERROR_MESSAGE = "Subscription error";
    private static final String DON_T_CALL_THIS_SCRIPT_OUT_OF_RATING_JOB = "don't call this script out of rating job";
    private static final String PDS_ERROR_MESSAGE = "PDS Error id_pds :";
    private static final String SERVICE_INSTANCES_ERROR_MESSAGE = "ServiceInstances[] ERROR id_pds :";
    private static final String SERVICE_INSTANCE_ERROR_MESSAGE = "ServiceInstances ERROR id_pds :";
    private static final String OFFER_ERROR_MESSAGE = "Offer ERROR id_pds :";
    private static final String ACCESS_ERROR_MESSAGE = "Access error id_pds :";
    private String CPT_BT_QUERY;
    private String CPT_HTA_QUERY;
    private String OFFER;
    private String PROD;
    private BigDecimal unitPriceWithoutTax;
    private BigDecimal qantity;
    private List<Map<String, Object>> pdsTable;
    private String accessUserId;
    CustomTableService customTableService = getServiceInterface(CustomTableService.class.getSimpleName());


    @Override
    public void execute(RatingContext ratingContext) {
        initializeContext();
        WalletOperation walletOperation = findWalletOperation(ratingContext);
        Subscription subscription = findSubscription(walletOperation);
        ServiceInstance chargeCode = walletOperation.getChargeInstance().getServiceInstance();
        applyAccordingToSubscription(subscription, chargeCode);
        calculateRating(ratingContext, walletOperation);
    }

    private void initializeContext() {
        accessUserId = "";
        qantity = BigDecimal.valueOf(1);
        CPT_BT_QUERY = " id in( select id from ct_grille_tarifaire where CURRENT_DATE BETWEEN date_debut_validite AND date_fin_validite and code in('price_one','price_two'))\n";
        CPT_HTA_QUERY = "id in (select id\n" +
                "             from ct_grille_tarifaire\n" +
                "             where CURRENT_DATE BETWEEN date_debut_validite AND date_fin_validite\n" +
                "             and code = 'RED_CPT_CL1')\n";
    }

    private void applyAccordingToSubscription(Subscription subscription, ServiceInstance prod) {
        findAccesPoint(subscription);
        findOffer(subscription);
        findProduct(subscription, prod);
        findPds();
        Map<String, Object> pds = pdsTable.get(0);
        String cf_nature = String.valueOf(subscription.getCfValue(CF_NATURE));
        validateNature(cf_nature);
        if (Objects.equals(cf_nature, BT)) {
            applyRulesForBT(pds);
        }
        if (Objects.equals(cf_nature, HTA)) {
            applyRulesForHTA(pds);
        }
    }


    private void calculateRating(RatingContext ratingContext, WalletOperation walletOperation) {
        ratingContext.calculate(walletOperation, ratingContext.reCalculate(unitPriceWithoutTax,walletOperation), qantity, OperationTypeEnum.DEBIT, OFFER, PROD);
    }

    private void applyRulesForBT(Map<String, Object> pds) {
        Object transformateur_intensite = pds.get(TRANSFORMATEUR_INTENSITE);
        Object nb_fils = pds.get(NB_FILS);
        List<String> prices = updateQueryBT(pds, nb_fils, transformateur_intensite);
        unitPriceWithoutTax = getPrice(prices, BT);
    }

    private void applyRulesForHTA(Map<String, Object> pds) {
        Object classe_compteur = pds.get(CLASSE_COMPTEUR);
        if (classe_compteur == null || (((Number) classe_compteur).doubleValue() != 0.5 && ((Number) classe_compteur).doubleValue() != 1.0)) {
            throwNoClasseCompteurError();
        }
        if (((Number) classe_compteur).doubleValue() == 0.5) {
            unitPriceWithoutTax = getPrice(List.of(RED_CPT_CL_05), HTA);
        }
        if (((Number) classe_compteur).doubleValue() == 1.0) {
            unitPriceWithoutTax = getPrice(List.of(RED_CPT_CL_1), HTA);
        }
    }

    private List<String> updateQueryBT(Map<String, Object> pds, Object nb_fils, Object transformateur_intensite) {
        if (nb_fils == null || ((Number) nb_fils).intValue() != 4 && ((Number) nb_fils).intValue() != 2) {
            throwNoPriceForFilsError();
        }
        if (transformateur_intensite != null && ((Boolean) transformateur_intensite)) {
            replacePricesInQueryBT(ENT_CPT_TI, LOC_CPT_TI);
            return List.of(ENT_CPT_TI, LOC_CPT_TI);
        } else if (phsage((Number) nb_fils).equals(MONOPHASE)) {
            replacePricesInQueryBT(ENT_CPT_MONO, LOC_CPT_MONO);
            return List.of(ENT_CPT_MONO, LOC_CPT_MONO);
        } else if (phsage((Number) nb_fils).equals(TRIPHASE)) {
            String gamme = pds.get(GAMME) == null || pds.get(GAMME).equals("") ? "0" : String.valueOf(pds.get(GAMME));
            if (isSuppGamme(gamme)) {
                replacePricesInQueryBT(ENT_CPT_TRI_SUP, LOC_CPT_TRI_SUP);
                return List.of(ENT_CPT_TRI_SUP, LOC_CPT_TRI_SUP);
            } else {
                replacePricesInQueryBT(ENT_CPT_TRI_INF, LOC_CPT_TRI_INF);
                return List.of(ENT_CPT_TRI_INF, LOC_CPT_TRI_INF);
            }
        }
        throw new BusinessApiException("error in update query or rerun prices Codes id_pds : " + accessUserId);
    }

    private void replacePricesInQueryBT(String priceENT, String priceLOC) {
        CPT_BT_QUERY = CPT_BT_QUERY.replace(PRICE_ONE, priceENT).replace(PRICE_TWO, priceLOC);
    }

    private boolean isSuppGamme(String gamme) {
        return gamme.compareTo(G030_000) >= 1;
    }

    private String phsage(Number nbFils) {
        return nbFils.intValue() == 2 ? MONOPHASE : TRIPHASE;
    }

    public BigDecimal getPrice(List<String> pricesCode, String cfNature) {
        if (HTA.equals(cfNature) && RED_CPT_CL_05.equals(pricesCode.get(0)))
            CPT_HTA_QUERY = CPT_HTA_QUERY.replace(RED_CPT_CL_1, RED_CPT_CL_05);
        return BT.equals(cfNature) ? getPriceBT(pricesCode) : getPriceHTA();
    }

    private BigDecimal getPriceHTA() {
        List<Map<String, Object>> list = getDataFromCT(CT_GRILLE_TARIFAIRE, Map.of(SQL, CPT_HTA_QUERY));
        if (list == null || list.isEmpty())
            throwEmptyCTGrilleTarifaireError();
        if (list.size() > 1)
            throwMoreThanOneValidPriceFoundError();
        return (BigDecimal) list.get(0).get(VALEUR);
    }

    public BigDecimal getPriceBT(List<String> pricesCode) {
        List<Map<String, Object>> list = getDataFromCT(CT_GRILLE_TARIFAIRE, Map.of(SQL, CPT_BT_QUERY));
        if (list == null || list.isEmpty())
            throwEmptyCTGrilleTarifaireError();
        if (list.size() != 2 || !verifyPrices(pricesCode, list))
            throwPriceNotFoundError();
        return calculateTotalPrice(list);
    }

    private BigDecimal calculateTotalPrice(List<Map<String, Object>> priceList) {
        return priceList.stream()
                .map(priceMap -> (BigDecimal) priceMap.get(VALEUR))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    private boolean verifyPrices(List<String> pricesCode, List<Map<String, Object>> list) {
        return pricesCode.stream()
                .allMatch(code -> list.stream()
                        .map(priceMap -> (String) priceMap.get(CODE))
                        .filter(Objects::nonNull)
                        .anyMatch(code::equals));
    }

    private void validateNature(String cfNature) {
        if (cfNature == null || !Objects.equals(cfNature, HTA) && !Objects.equals(cfNature, BT)) {
            throwCfNatureError();
        }
    }

    public List<Map<String, Object>> getDataFromCT(String tableName, Map<String, Object> filters) {
        try {
            return customTableService.list(tableName, new PaginationConfiguration(filters));
        } catch (Exception e) {
            logError(e);
            return Collections.emptyList();
        }
    }

    private WalletOperation findWalletOperation(RatingContext ratingContext) {
        WalletOperation walletOperation = ratingContext.getWalletOperation();
        if (walletOperation == null) {
            throwOutOfRatingJobError();
        }
        return walletOperation;
    }

    private Subscription findSubscription(WalletOperation walletOperation) {
        Subscription subscription = walletOperation.getSubscription();
        Objects.requireNonNull(subscription, SUBSCRIPTION_ERROR_MESSAGE);
        return subscription;
    }

    private void findPds() {
        pdsTable = getDataFromCT(CT_PDS, Map.of(ID_PDS, accessUserId, STATUT_PDS, ACTIF));
        if (pdsTable == null || pdsTable.size() != 1)
            throwPdsError();
    }

    private void findAccesPoint(Subscription subscription) {
        if (subscription.getAccessPoints() == null || subscription.getAccessPoints().isEmpty() || subscription.getAccessPoints().get(0) == null)
            throwAccessError();
        accessUserId = subscription.getAccessPoints().get(0).getAccessUserId();
    }

    private void findProduct(Subscription subscription, ServiceInstance prod) {
        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        if (serviceInstances == null || serviceInstances.isEmpty())
            throwServiceInstancesError();
        Optional<ServiceInstance> serviceInstance = serviceInstances.stream()
                .filter(s -> Objects.equals(s.getCode(), prod.getCode()))
                .findFirst();
        if (serviceInstance.isPresent()) {
            PROD = serviceInstance.get().getCode();
        } else {
            throwServiceInstanceError();
        }
    }

    private void findOffer(Subscription subscription) {
        OfferTemplate offer = subscription.getOffer();
        if (offer == null || offer.getCode() == null)
            throwOfferError();
        OFFER = offer.getCode();
    }

    private void throwOutOfRatingJobError() {
        throw new BusinessApiException(DON_T_CALL_THIS_SCRIPT_OUT_OF_RATING_JOB);
    }

    private void throwCfNatureError() {
        throw new BusinessApiException(CF_NATURE_ERROR_ID_PDS + accessUserId);
    }

    private void throwPdsError() {
        throw new BusinessApiException(PDS_ERROR_MESSAGE + accessUserId);
    }

    private void throwServiceInstancesError() {
        throw new BusinessApiException(SERVICE_INSTANCES_ERROR_MESSAGE + accessUserId);
    }

    private void throwServiceInstanceError() {
        throw new BusinessApiException(SERVICE_INSTANCE_ERROR_MESSAGE + accessUserId);
    }

    private void throwOfferError() {
        throw new BusinessApiException(OFFER_ERROR_MESSAGE + accessUserId);
    }

    private void throwAccessError() {
        throw new BusinessApiException(ACCESS_ERROR_MESSAGE + accessUserId);
    }

    private void throwEmptyCTGrilleTarifaireError() {
        throw new BusinessApiException(CT_GRILLE_TARIFAIRE_IS_EMPTY_ACCESS_USER_ID + accessUserId);
    }

    private void throwPriceNotFoundError() {
        throw new BusinessApiException(ONE_OF_THIS_PRICES_DOES_NOT_EXIST);
    }

    private void throwMoreThanOneValidPriceFoundError() {
        throw new BusinessApiException(MORE_THAN_ONE_VALID_PRICE_IN_THE_TABLE);
    }

    private void throwNoPriceForFilsError() {
        throw new BusinessApiException(PAS_DE_PRIX_QUI_CORRESPOND_AU_NOMBRE_DE_FILS_ID_PDS + accessUserId);
    }

    private void throwNoClasseCompteurError() {
        throw new BusinessApiException(CLASSE_COMPTEUR_1_OU_0_5 + accessUserId);

    }

    private void logError(Exception e) {
        log.error(GET_DATA_FROM_CT_ERROR, e);
    }

}
