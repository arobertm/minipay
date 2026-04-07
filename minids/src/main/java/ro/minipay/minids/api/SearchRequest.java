package ro.minipay.minids.api;

import java.util.Map;

/**
 * Request pentru operatia de Search.
 * Inspirat din LDAP Search Operation (RFC 4511) si PingDS REST API.
 *
 * @param baseDn  radacina cautarii (ex: "ou=users,dc=minipay,dc=ro")
 * @param filter  atribute de filtrat (ex: {"mail": "john@example.com"})
 * @param limit   numarul maxim de rezultate returnate
 */
public record SearchRequest(
        String baseDn,
        Map<String, String> filter,
        int limit
) {
    public SearchRequest {
        if (baseDn == null || baseDn.isBlank()) {
            throw new IllegalArgumentException("baseDn este obligatoriu");
        }
        if (limit <= 0) limit = 100;
        if (filter == null) filter = Map.of();
    }
}
