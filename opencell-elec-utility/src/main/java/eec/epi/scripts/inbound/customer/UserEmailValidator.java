package eec.epi.scripts.inbound.customer;

import eec.epi.scripts.inbound.FieldValidator;
import org.meveo.model.crm.Customer;

import java.util.Optional;
import java.util.Map;
import java.util.regex.Pattern;

public class UserEmailValidator implements FieldValidator<Customer> {
    private static final String EMAIL = "email";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[a-zA-Z0-9.-]+$");

    @Override
    public void validateAndApply(Map<String, String> requestData, Customer customer) {
        Optional.ofNullable(requestData.get(EMAIL))
                .filter(value -> EMAIL_PATTERN.matcher(value).matches());
//                .ifPresent(customer::setAddress.);
    }
}

