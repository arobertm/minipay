package ro.minipay.minids.schema;

import org.springframework.stereotype.Component;
import ro.minipay.minids.model.Entry;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates an Entry against the ObjectClass schema.
 * Inspired by schema enforcement in PingDS.
 */
@Component
public class SchemaValidator {

    public void validate(Entry entry) {
        if (entry.getDn() == null || entry.getDn().isBlank()) {
            throw new SchemaViolationException("DN is required");
        }
        if (entry.getObjectClass() == null) {
            throw new SchemaViolationException("objectClass is required");
        }

        ObjectClass oc = ObjectClass.fromName(entry.getObjectClass());

        List<String> missing = new ArrayList<>();
        for (String required : oc.requiredAttributes) {
            if (entry.getAttribute(required) == null) {
                missing.add(required);
            }
        }

        if (!missing.isEmpty()) {
            throw new SchemaViolationException(
                "Missing required attributes for " + oc.name + ": " + missing
            );
        }
    }

    public static class SchemaViolationException extends RuntimeException {
        public SchemaViolationException(String message) {
            super(message);
        }
    }
}
