package eec.epi.scripts;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.model.article.ArticleMappingLine;
import org.meveo.model.billing.*;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.cpq.Product;
import org.meveo.model.rating.EDR;
import org.meveo.security.MeveoUser;
import org.meveo.service.billing.impl.article.ArticleMappingLineService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.meveo.service.cpq.ProductService;
import org.meveo.service.custom.CustomTableService;
import org.meveo.service.script.Script;
import org.meveo.model.crm.Provider;
import org.meveo.service.tax.TaxMappingService;
import org.meveo.service.tax.TaxMappingService.TaxInfo;
import org.meveo.model.article.AccountingArticle;


import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class   RatingScript extends ACommonScript {

    public abstract void execute(RatingContext ratingContext);

    public final TaxMappingService taxMappingService = getServiceInterface(TaxMappingService.class.getSimpleName());
    public final ArticleMappingLineService articleMappingLineService = getServiceInterface(ArticleMappingLineService.class.getSimpleName());
    public final ProductService productService = getServiceInterface(ProductService.class.getSimpleName());
    public final OfferTemplateService offerTemplateService = getServiceInterface(OfferTemplateService.class.getSimpleName());
    public final CustomTableService customTableService = getServiceInterface(CustomTableService.class.getSimpleName());

    public class RatingContext {
        public static final String CT_GRILLE_TARIFAIRE = "ct_grille_tarifaire";
        public static final String VALEUR = "valeur";
        private static final String GET_DATA_FROM_CT_ERROR = "getDataFromCT error : ";
        private static final String CT_GRILLE_TARIFAIRE_IS_EMPTY = "CT_GRILLE_TARIFAIRE is empty :";
        private static final String CF_NATURE_ERROR = "cf_nature ERROR :";
        public static final String MORE_THAN_ONE_VALID_PRICE_IN_THE_TABLE = "More than one valid price in the table";
        private static final String ONE_OF_THIS_PRICES_DOES_NOT_EXIST = "One of this prices does not exist";
        public static final String CF_PUISSANCE_SOUSCRITE = "cf_puissance_souscrite";
        protected static final String CF_NATURE = "cf_nature";
        private final Map<String, Object> methodContext;

        public RatingContext(Map<String, Object> methodContext) {
            this.methodContext = methodContext;
        }

        public Map<String, Object> getMethodContext() {
            return methodContext;
        }

        public WalletOperation getWalletOperation() {
            return (WalletOperation) methodContext.get(Script.CONTEXT_ENTITY);
        }

        public String getScriptCode() {
            return (String) methodContext.get(Script.CONTEXT_ACTION);
        }

        public MeveoUser getCurrentUser() {
            return (MeveoUser) methodContext.get(Script.CONTEXT_CURRENT_USER);
        }

        public Provider getAppProvider() {
            return (Provider) methodContext.get(Script.CONTEXT_APP_PROVIDER);
        }

        protected Product product;
        protected OfferTemplate offerTemplate;
        protected BigDecimal cf_puissance_souscrite;


        private String OFFER;
        private String PROD;
        private String cf_nature = null;
        public BigDecimal reCalculate(BigDecimal price, WalletOperation walletOperation) {
            if (walletOperation.getOperationDate() == null) {
                throw new BusinessApiException("walletOperation.OperationDate is null for subscription code : " + walletOperation.getSubscription().getCode());
            }
            if (walletOperation.getStartDate() == null) {
                throw new BusinessApiException("walletOperation.StartDate is null for subscription code : " + walletOperation.getSubscription().getCode());
            }
            if (walletOperation.getEndDate() == null) {
                throw new BusinessApiException("walletOperation.EndDate is null for subscription code : " + walletOperation.getSubscription().getCode());
            }
            if (price == null) {
                throw new BusinessApiException("price is null for subscription code : " + walletOperation.getSubscription().getCode());
            }
            BigDecimal nbrDaysInMonth = new BigDecimal(
                    nbrDaysInMonth(walletOperation.getOperationDate())
            );
            if (nbrDaysInMonth.equals(BigDecimal.ZERO)) {
                throw new BusinessApiException("Error : nbrDaysInMonth = 0 for subscription code : " + walletOperation.getSubscription().getCode());
            }
            BigDecimal nbrDaysBetweenStartDateAndEndDate = new BigDecimal(
                    nbrDaysBetweenTwoDates(walletOperation.getStartDate(), walletOperation.getEndDate())
            );
            if (nbrDaysBetweenStartDateAndEndDate.equals(BigDecimal.ZERO)) {
                throw new BusinessApiException("Error : nbrDaysBetweenStartDateAndEndDate = 0 for subscription code : " + walletOperation.getSubscription().getCode());
            }
            return
                    price.divide(
                            nbrDaysInMonth, MathContext.DECIMAL128

                    ).multiply(
                            nbrDaysBetweenStartDateAndEndDate
                    );
        }

        public int nbrDaysInMonth(Date operationDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(operationDate);
            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        public int nbrDaysBetweenTwoDates(Date startDate, Date endDate) {
            LocalDate localStartDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate localEndDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long diffDays = ChronoUnit.DAYS.between(localStartDate, localEndDate);
            return Math.toIntExact(diffDays);
        }
        public void calculate(WalletOperation wo, BigDecimal unitPriceWithoutTax,
                              BigDecimal quantity, OperationTypeEnum operationTypeEnum, String offerCode, String productCode) {
            findPOByCodes(offerCode, productCode);
            setTax(wo);
            setArticle(wo);
            wo.setQuantity(quantity);
            //unit
            wo.setUnitAmountWithoutTax(unitPriceWithoutTax);
            wo.setUnitAmountTax((unitPriceWithoutTax.multiply(wo.getTaxPercent()).divide(BigDecimal.valueOf(100))));
            wo.setUnitAmountWithTax(wo.getUnitAmountWithoutTax().add(wo.getUnitAmountTax()));
            //TOTAL
            wo.setAmountWithoutTax((wo.getUnitAmountWithoutTax().multiply(wo.getQuantity())).setScale(0, RoundingMode.HALF_UP));
            wo.setAmountTax((wo.getUnitAmountTax().multiply(wo.getQuantity())));
            wo.setAmountWithTax(wo.getAmountWithoutTax().add(wo.getAmountTax()));
            wo.setType(operationTypeEnum);
        }
        public void calculateTAXCOM(WalletOperation wo, BigDecimal unitPriceWithoutTax,
                              BigDecimal quantity, OperationTypeEnum operationTypeEnum, String offerCode, String productCode) {
            findPOByCodes(offerCode, productCode);
            setTax(wo);
            setArticle(wo);
            wo.setQuantity(quantity.setScale(0, RoundingMode.HALF_UP));
            //unit
            wo.setUnitAmountWithoutTax(unitPriceWithoutTax);
            wo.setUnitAmountTax((unitPriceWithoutTax.multiply(wo.getTaxPercent()).divide(BigDecimal.valueOf(100))));
            wo.setUnitAmountWithTax(wo.getUnitAmountWithoutTax().add(wo.getUnitAmountTax()));
            //TOTAL
            wo.setAmountWithoutTax((wo.getUnitAmountWithoutTax().multiply(wo.getQuantity())).setScale(0, RoundingMode.HALF_UP));
            wo.setAmountTax((wo.getUnitAmountTax().multiply(wo.getQuantity())));
            wo.setAmountWithTax(wo.getAmountWithoutTax().add(wo.getAmountTax()));
            wo.setType(operationTypeEnum);
        }

        public void setTax(WalletOperation wo) {
            TaxInfo taxinfo = findTax(wo);
            if (taxinfo != null) {
                wo.setTaxClass(taxinfo.taxClass);
                wo.setTax(taxinfo.tax);
                wo.setTaxPercent(taxinfo.tax.getPercent());
            }
        }


        public void setArticle(WalletOperation wo) {
            AccountingArticle accountingArticle = findArticle(wo.getChargeInstance().getChargeTemplate());
            wo.setInvoiceSubCategory(accountingArticle.getInvoiceSubCategory());
            wo.setAccountingCode(accountingArticle.getAccountingCode());
        }

        public TaxInfo findTax(WalletOperation wo) {
            return taxMappingService.determineTax(wo);
        }

        public AccountingArticle findArticle(ChargeTemplate chargeTemplate) {
            List<ArticleMappingLine> lines = articleMappingLineService.findByProductAndCharge(product, chargeTemplate, offerTemplate, null, null, null);
            if (lines == null || lines.size() == 0) {
                throw new BusinessApiException("expected 1 article line for wo " + getWalletOperation() + " got " + lines);
            }
            ArticleMappingLine ret = lines.get(0);
            for (ArticleMappingLine line : lines) {
                if (line.getId() > ret.getId()) {
                    ret = line;
                }
            }
            return ret.getAccountingArticle();
        }

        public void findPOByCodes(String offerCode, String productCode) {
            product = productService.findByCode(productCode);
            offerTemplate = offerTemplateService.findByCode(offerCode);
        }

        public Subscription getSubscription(WalletOperation walletOperation) {
            Subscription subscription = walletOperation.getSubscription();
            if (subscription == null)
                throw new BusinessApiException("Subscription error wo : " + walletOperation.getId());
            return subscription;
        }

        public EDR getEdr(WalletOperation walletOperation, String code) {
            EDR edr = walletOperation.getEdr();
            if (edr == null) throw new BusinessApiException("EDR Not Found ERROR  :" + code);
            if (edr.getQuantity() == null) throw new BusinessApiException("EDR Quantity is null  :" + code);
            return edr;
        }

        public OfferTemplate getOfferTemplate(Subscription subscription, String code) {
            OfferTemplate offer = subscription.getOffer();
            if (offer == null || offer.getCode() == null)
                throw new BusinessApiException("Offer ERROR subscription :" + code);
            return offer;
        }

        public Optional<ServiceInstance> getServiceInstance(ServiceInstance prod, List<ServiceInstance> serviceInstances, String code) {
            Optional<ServiceInstance> serviceInstance = serviceInstances.stream().filter(s -> s.getCode().equals(prod.getCode())).findFirst();
            if (serviceInstance.isEmpty() || serviceInstance.get().getCode() == null)
                throw new BusinessApiException("ServiceInstances ERROR subscription :" + code);
            return serviceInstance;
        }

        public ChargeInstance getChargeInstance(WalletOperation walletOperation, String chargeCode) {
            ChargeInstance chargeInstance = walletOperation.getChargeInstance();
            if (chargeInstance == null || !chargeInstance.getCode().equals(chargeCode))
                throw new BusinessApiException("chargeInstance error wo : " + walletOperation.getId());
            return chargeInstance;
        }

        public void setPuissanceSouscrite(Subscription subscription) {
            if (subscription.getCfValue(CF_PUISSANCE_SOUSCRITE) == null)
                throw new BusinessApiException("cf_puissance_souscrite ERROR :" + subscription.getCode());
            setCf_puissance_souscrite(BigDecimal.valueOf((((Number) subscription.getCfValue(CF_PUISSANCE_SOUSCRITE)).doubleValue())));
        }

        public WalletOperation getWalletOperation(RatingContext ratingContext) {
            WalletOperation walletOperation = ratingContext.getWalletOperation();
            if (walletOperation == null) {
                throw new BusinessApiException("don't call this script out of rating job");
            }
            return walletOperation;
        }

        public void validateNature(RatingContext ratingContext, String cfNature, String code) {
            if (cfNature == null || !Objects.equals(cfNature, "HTA") && !Objects.equals(cfNature, "BT")) {
                ratingContext.throwCfNatureError(code);
            }
        }

        public List<Map<String, Object>> getValues(String query, String subCode) {
            List<Map<String, Object>> list = getDataFromCT(CT_GRILLE_TARIFAIRE, Map.of("SQL", query));
            if (list == null || list.isEmpty())
                throwEmptyCTGrilleTarifaireError(subCode);
            return list;
        }

        public BigDecimal getValueFromList(String priceCode, String offre, String nature, Object exploitation, List<Map<String, Object>> list, String subCode) {
            List<Map<String, Object>> filtredList = getValue(priceCode, offre, nature, exploitation, list, subCode);
            if (!filtredList.isEmpty()) return (BigDecimal) filtredList.get(0).get(VALEUR);
            filtredList = getValue(priceCode, offre, nature, list, subCode);
            if (!filtredList.isEmpty()) return (BigDecimal) filtredList.get(0).get(VALEUR);
            filtredList = getValue(priceCode, nature, exploitation, list, subCode);
            if (!filtredList.isEmpty()) return (BigDecimal) filtredList.get(0).get(VALEUR);
            filtredList = getValue(priceCode, nature, list, subCode);
            if (filtredList.isEmpty()) throwPriceNotFoundError(subCode);
            return (BigDecimal) filtredList.get(0).get(VALEUR);
        }

        public List<Map<String, Object>> getValue(String priceCode, String offre, String nature, Object exploitation, List<Map<String, Object>> list, String subCode) {
            List<Map<String, Object>> filteredList = list.stream()
                    .filter(l -> Objects.nonNull(l.get("code")) && l.get("code").equals(priceCode)
                            && Objects.nonNull(l.get("domaine_tension")) && l.get("domaine_tension").equals(nature)
                            && Objects.nonNull(l.get("offre")) && l.get("offre").equals(offre)
                            && Objects.nonNull(l.get("code_exploitation")) && l.get("code_exploitation").equals(exploitation))
                    .collect(Collectors.toList());
            if (filteredList.size() > 1) {
                throwMoreThanOneValidPriceFoundError(subCode);
            }
            return filteredList;
        }

        public List<Map<String, Object>> getValue(String priceCode, String nature, List<Map<String, Object>> list, String subCode) {
            List<Map<String, Object>> filteredList = list.stream()
                    .filter(l -> Objects.nonNull(l.get("code")) && l.get("code").equals(priceCode)
                            && Objects.nonNull(l.get("domaine_tension")) && l.get("domaine_tension").equals(nature)
                            && Objects.isNull(l.get("offre"))
                            && Objects.isNull(l.get("code_exploitation")))
                    .collect(Collectors.toList());
            if (filteredList.size() > 1) {
                throwMoreThanOneValidPriceFoundError(subCode);
            }
            return filteredList;
        }

        public List<Map<String, Object>> getValue(String priceCode, String offre, String nature, List<Map<String, Object>> list, String subCode) {
            List<Map<String, Object>> filteredList = list.stream()
                    .filter(l -> Objects.nonNull(l.get("code")) && l.get("code").equals(priceCode)
                            && Objects.nonNull(l.get("domaine_tension")) && l.get("domaine_tension").equals(nature)
                            && Objects.nonNull(l.get("offre")) && l.get("offre").equals(offre)
                            && Objects.isNull(l.get("code_exploitation")))
                    .collect(Collectors.toList());
            if (filteredList.size() > 1) {
                throwMoreThanOneValidPriceFoundError(subCode);
            }
            return filteredList;
        }

        public List<Map<String, Object>> getValue(String priceCode, String nature, Object exploitation, List<Map<String, Object>> list, String subCode) {
            List<Map<String, Object>> filteredList = list.stream()
                    .filter(l -> Objects.nonNull(l.get("code")) && l.get("code").equals(priceCode)
                            && Objects.nonNull(l.get("domaine_tension")) && l.get("domaine_tension").equals(nature)
                            && Objects.isNull(l.get("offre"))
                            && Objects.nonNull(l.get("code_exploitation")) && l.get("code_exploitation").equals(exploitation))
                    .collect(Collectors.toList());
            if (filteredList.size() > 1) {
                throwMoreThanOneValidPriceFoundError(subCode);
            }
            return filteredList;
        }

        public void applyAccordingToSubscription(Subscription subscription, ServiceInstance prod, String code, RatingContext ratingContext) {
            OfferTemplate offer = ratingContext.getOfferTemplate(subscription, code);
            setOFFER(offer.getCode());
            List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
            if (serviceInstances == null || serviceInstances.isEmpty())
                throw new BusinessApiException("ServiceInstances[] ERROR subscription :" + code);
            Optional<ServiceInstance> serviceInstance = ratingContext.getServiceInstance(prod, serviceInstances, code);
            setPROD(serviceInstance.get().getCode());
            String cf_nature_value = String.valueOf(subscription.getCfValue(CF_NATURE));
            ratingContext.validateNature(ratingContext, cf_nature_value, code);
            setCf_nature(cf_nature_value);
        }

        public List<Map<String, Object>> getDataFromCT(String tableName, Map<String, Object> filters) {
            try {
                return customTableService.list(tableName, new PaginationConfiguration(filters));
            } catch (Exception e) {
                logError(e);
                return Collections.emptyList();
            }
        }

        public void throwCfNatureError(String subscriptionCode) {
            throw new BusinessApiException(CF_NATURE_ERROR + subscriptionCode);
        }

        private void throwEmptyCTGrilleTarifaireError(String subscriptionCode) {
            throw new BusinessApiException(CT_GRILLE_TARIFAIRE_IS_EMPTY + subscriptionCode);
        }

        private void throwMoreThanOneValidPriceFoundError(String subscriptionCode) {
            throw new BusinessApiException(MORE_THAN_ONE_VALID_PRICE_IN_THE_TABLE + subscriptionCode);
        }

        private void throwPriceNotFoundError(String subCode) {
            throw new BusinessApiException(ONE_OF_THIS_PRICES_DOES_NOT_EXIST + subCode);
        }

        private void logError(Exception e) {
            log.error(GET_DATA_FROM_CT_ERROR, e);
        }

        public String getOFFER() {
            return OFFER;
        }

        public void setOFFER(String OFFER) {
            this.OFFER = OFFER;
        }

        public String getPROD() {
            return PROD;
        }

        public void setPROD(String PROD) {
            this.PROD = PROD;
        }

        public String getCf_nature() {
            return cf_nature;
        }

        public void setCf_nature(String cf_nature) {
            this.cf_nature = cf_nature;
        }

        public BigDecimal getCf_puissance_souscrite() {
            return cf_puissance_souscrite;
        }

        public void setCf_puissance_souscrite(BigDecimal cf_puissance_souscrite) {
            this.cf_puissance_souscrite = cf_puissance_souscrite;
        }

    }


    @Override
    public void execute(Map<String, Object> methodContext) throws BusinessException {
        execute(new RatingContext(methodContext));
    }

}
