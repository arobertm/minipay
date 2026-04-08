package ro.minipay.minids.api;

import java.util.Map;

/**
 * Request for the Search operation.
 * Inspired by the LDAP Search Operation (RFC 4511) and PingDS REST API.
 *
 * @param baseDn  search root (e.g.: "ou=users,dc=minipay,dc=ro")
 * @param filter  attributes to filter on (e.g.: {"mail": "john@example.com"})
 * @param limit   maximum number of results returned
 */
public record SearchRequest(
        String baseDn,
        Map<String, String> filter,
        int limit
) {
    public SearchRequest {
        if (baseDn == null || baseDn.isBlank()) {
            throw new IllegalArgumentException("baseDn is required");
        }
        if (limit <= 0) limit = 100;
        if (filter == null) filter = Map.of();
    }
}
