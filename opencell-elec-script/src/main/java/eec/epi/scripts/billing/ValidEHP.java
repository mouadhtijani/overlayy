package eec.epi.scripts.billing;

import org.meveo.api.exception.MissingParameterException;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceLine;
import org.meveo.service.script.Script;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ValidEHP extends Script {
    private static final long serialVersionUID = -3842955629518523594L;

    public ValidEHP() {
    }

    public void execute(Map<String, Object> context) {
        Invoice invoice = (Invoice) context.get("CONTEXT_ENTITY");
        if (invoice == null) {
            throw new MissingParameterException("CONTEXT_ENTITY");
        } else {
            Map<Object, Object> contextMap = new HashMap<>();
            contextMap.put("invoice", invoice);
            for (InvoiceLine invoiceLine : invoice.getInvoiceLines()) {
                if (invoiceLine.getAccountingArticle().getCode().equals("AA_IR_ENER_HP") && invoiceLine.getQuantity().compareTo(new BigDecimal(100)) >= 0) {
                    context.put("InvoiceValidation.STATUS", true);
                    return;
                } else {
                    context.put("InvoiceValidation.STATUS", false);
                }
            }
        }
    }
}