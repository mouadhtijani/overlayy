//package eec.epi.scripts;
//
//import org.meveo.admin.exception.BusinessException;
//import org.meveo.api.exception.BusinessApiException;
//import org.meveo.model.article.AccountingArticle;
//import org.meveo.model.article.ArticleMappingLine;
//import org.meveo.model.billing.WalletOperation;
//import org.meveo.model.crm.Provider;
//import org.meveo.security.MeveoUser;
//import org.meveo.service.billing.impl.article.ArticleMappingLineService;
//import org.meveo.service.script.Script;
//import org.meveo.service.tax.TaxMappingService;
//import org.meveo.service.tax.TaxMappingService.TaxInfo;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//
///**
// * rating script based on a wallet operation application. Usual case is to get
// * the EDR from the wo and call the
// * {@link RatingContext#calculateFromUnitPrice(BigDecimal)}
// *
// *
// */
//@SuppressWarnings("serial")
//public abstract class RatingScript extends ACommonScript {
//
//	public abstract void execute(RatingContext ratingContext);
//
//	private static final BigDecimal BD_100 = BigDecimal.valueOf(100);
//
//	public class RatingContext {
//
//		public final TaxMappingService taxMappingService = (TaxMappingService) getServiceInterface("TaxMappingService");
//
//		private ArticleMappingLineService articleMappingLineService = (ArticleMappingLineService) getServiceInterface(
//				"ArticleMappingLineService");
//
//		public final Map<String, Object> methodContext;
//
//		public Map<String, Object> getMethodContext() {
//			return methodContext;
//		}
//
//		public RatingContext(Map<String, Object> methodContext) {
//			this.methodContext = methodContext;
//		}
//
//		public WalletOperation getWalletOperation() {
//			return (WalletOperation) methodContext.get(Script.CONTEXT_ENTITY);
//		}
//
//		public String getScriptCode() {
//			return (String) methodContext.get(Script.CONTEXT_ACTION);
//		}
//
//		public MeveoUser getCurrentUser() {
//			return (MeveoUser) methodContext.get(Script.CONTEXT_CURRENT_USER);
//		}
//
//		public Provider getAppProvider() {
//			return (Provider) methodContext.get(Script.CONTEXT_APP_PROVIDER);
//		}
//
//		/**
//		 * calculate the WO informations based on the quantity and unit price.
//		 *
//		 * @param unitPriceWithoutTax
//		 */
//		public void calculateFromUnitPrice(BigDecimal unitPriceWithoutTax) {
//			WalletOperation wo = getWalletOperation();
//			wo.setUnitAmountWithoutTax(unitPriceWithoutTax);
//			computeTotalPrice(wo);
//			computeTax(wo);
//			computeArticle();
//			computeTaxedAmounts(wo);
//			computeTaxValues(wo);
//		}
//
//		public void calculateFromUnitAndTotalPrice(BigDecimal unitPriceWithoutTax, BigDecimal totalPriceWithoutTax){
//			WalletOperation wo = getWalletOperation();
//			wo.setUnitAmountWithoutTax(unitPriceWithoutTax);
//			wo.setAmountWithoutTax(totalPriceWithoutTax);
//			computeTax(wo);
//			computeArticle();
//			computeTaxedAmounts(wo);
//			computeTaxValues(wo);
//		}
//
//		public void calculateFromAllPrices(BigDecimal unitPriceWithoutTax, BigDecimal unitPriceWithTax,
//				BigDecimal totalPriceWithoutTax, BigDecimal totalPriceWithTax) {
//			WalletOperation wo = getWalletOperation();
//			wo.setUnitAmountWithoutTax(unitPriceWithoutTax);
//			wo.setUnitAmountWithTax(unitPriceWithTax);
//			wo.setAmountWithoutTax(totalPriceWithoutTax);
//			wo.setAmountWithTax(totalPriceWithTax);
//			computeTax(wo);
//			computeArticle();
//			computeTaxValues(wo);
//		}
//
//		/**
//		 * @return the tax to apply, iff the tax is not already set in the charge.
//		 *         In that case, it returns null.
//		 */
//		public TaxInfo findTax(WalletOperation wo) {
//			return taxMappingService.determineTax(wo);
//		}
//
//		/** set the tax data to those found with {@link #findTax(WalletOperation)} */
//		public void computeTax(WalletOperation wo) {
//			TaxInfo taxinfo = findTax(wo);
//			log.debug("found tax " + (taxinfo == null ? null : taxinfo.tax) + " for wo=" + wo);
//			if (taxinfo != null) {
//				wo.setTaxClass(taxinfo.taxClass);
//				wo.setTax(taxinfo.tax);
//				wo.setTaxPercent(taxinfo.tax.getPercent());
//			} else {
//				// this happens when the tax info are already set and should not be
//				// computed.
//				return;
//			}
//		}
//
//		/**
//		 * guess the description to apply to the wo based on the context. Override
//		 * if needed
//		 */
//		public String findDescription() {
//			return getWalletOperation().getChargeInstance().getChargeTemplate().getDescription();
//		}
//
//		/** set the description to the one found with {@link #findDescription()} */
//		public void setDescription() {
//			getWalletOperation().setDescription(findDescription());
//		}
//
//		public AccountingArticle findArticle() {
//			List<ArticleMappingLine> lines = articleMappingLineService.findByProductAndCharge(null,
//					getWalletOperation().getChargeInstance().getChargeTemplate());
//			if (lines == null || lines.size() == 0) {
//				throw new BusinessApiException("expected 1 article line for wo " + getWalletOperation() + " got " + lines);
//			}
//			ArticleMappingLine ret = lines.get(0);
//			for( ArticleMappingLine line : lines){
//				if(line.getId()>ret.getId()) {
//					ret=line;
//				}
//			}
//			return ret.getAccountingArticle();
//		}
//
//		public void computeArticle() {
//			// article
//			AccountingArticle accountingArticle = findArticle();
//			getWalletOperation().setInvoiceSubCategory(accountingArticle.getInvoiceSubCategory());
//			getWalletOperation().setAccountingCode(accountingArticle.getAccountingCode());
//		}
//
//		/**
//		 * compute total price without tax from unit price without tax, and quantity
//		 *
//		 * @param wo
//		 */
//		public void computeTotalPrice(WalletOperation wo){
//			BigDecimal quantity = wo.getQuantity();
//			BigDecimal totalPriceWithoutTax = quantity.multiply(wo.getUnitAmountWithoutTax());
//			wo.setAmountWithoutTax(totalPriceWithoutTax);
//		}
//
//		/**
//		 * compute amount with tax and unit amount with tax, based on the tax
//		 * percent and the values without tax
//		 *
//		 * @param wo
//		 */
//		public void computeTaxedAmounts(WalletOperation wo) {
//			BigDecimal percent = wo.getTaxPercent();
//			if (percent != null) {
//				BigDecimal unitPriceWithTax = BD_100.add(percent).multiply(wo.getUnitAmountWithoutTax()).divide(BD_100);
//				wo.setUnitAmountWithTax(unitPriceWithTax);
//				BigDecimal totalPriceWithTax = BD_100.add(percent).multiply(wo.getAmountWithoutTax()).divide(BD_100);
//				wo.setAmountWithTax(totalPriceWithTax);
//				computeTaxValues(wo);
//			}
//		}
//
//		/**
//		 * compute tax amounts from unit and total prices with and without tax
//		 *
//		 * @param wo
//		 */
//		public void computeTaxValues(WalletOperation wo) {
//			wo.setUnitAmountTax(wo.getUnitAmountWithTax().subtract(wo.getUnitAmountWithoutTax()));
//			wo.setAmountTax(wo.getAmountWithTax().subtract(wo.getAmountWithoutTax()));
//		}
//	}
//
//	@Override
//	public void execute(Map<String, Object> methodContext) throws BusinessException {
//		execute(new RatingContext(methodContext));
//	}
//
//}
