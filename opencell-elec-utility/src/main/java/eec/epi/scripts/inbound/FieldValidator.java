package eec.epi.scripts.inbound;

import java.util.Map;

@FunctionalInterface
public interface FieldValidator<T> {
    void validateAndApply(Map<String, String> requestData, T entity);
}
