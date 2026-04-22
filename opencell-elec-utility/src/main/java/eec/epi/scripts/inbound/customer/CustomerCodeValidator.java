package eec.epi.scripts.inbound.customer;

import eec.epi.scripts.inbound.FieldValidator;
import org.meveo.model.crm.Customer;

import java.util.Optional;
import java.util.Map;

public class CustomerCodeValidator implements FieldValidator<Customer> {
    private static final String CUSTOMER_CODE = "customerCode";

    @Override
    public void validateAndApply(Map<String, String> requestData, Customer customer) {
        Optional.ofNullable(requestData.get(CUSTOMER_CODE))
                .filter(value -> !value.trim().isEmpty())
                .ifPresent(customer::setCode);
    }
}