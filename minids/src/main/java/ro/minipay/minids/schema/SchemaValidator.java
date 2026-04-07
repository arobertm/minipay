package ro.minipay.minids.schema;

import org.springframework.stereotype.Component;
import ro.minipay.minids.model.Entry;

import java.util.ArrayList;
import java.util.List;

/**
 * Valideaza un Entry conform schemei ObjectClass.
 * Inspirat din schema enforcement-ul din PingDS.
 */
@Component
public class SchemaValidator {

    public void validate(Entry entry) {
        if (entry.getDn() == null || entry.getDn().isBlank()) {
            throw new SchemaViolationException("DN este obligatoriu");
        }
        if (entry.getObjectClass() == null) {
            throw new SchemaViolationException("objectClass este obligatoriu");
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
                "Atribute obligatorii lipsa pentru " + oc.name + ": " + missing
            );
        }
    }

    public static class SchemaViolationException extends RuntimeException {
        public SchemaViolationException(String message) {
            super(message);
        }
    }
}
