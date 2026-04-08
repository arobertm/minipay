package ro.minipay.auth.shared.minids;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * REST client wrapper for MiniDS Directory Server.
 *
 * Encapsulates all HTTP calls to MiniDS, handling serialization and error management.
 * Implements CRUD operations on directory entries, plus search functionality.
 *
 * Example usage:
 *   Entry user = minidsClient.createEntry(baseDn, entry);
 *   Optional<Entry> found = minidsClient.getEntry(dn);
 *   minidsClient.deleteEntry(dn);
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniDSClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${minids.base-url}")
    private String minidsBaseUrl;

    /**
     * Create a new entry in MiniDS.
     *
     * @param entry the entry to create (must include DN and objectClass)
     * @return the created entry (may include additional metadata)
     */
    public Entry createEntry(Entry entry) {
        String url = minidsBaseUrl + "/minids/v1/entries";
        log.debug("Creating entry: {}", entry.getDn());
        Entry created = restTemplate.postForObject(url, entry, Entry.class);
        if (created == null) {
            throw new RuntimeException("Failed to create entry: " + entry.getDn());
        }
        return created;
    }

    /**
     * Retrieve an entry by DN.
     *
     * @param dn the distinguished name
     * @return Optional containing the entry, or empty if not found
     */
    public Optional<Entry> getEntry(String dn) {
        try {
            String url = minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn);
            log.debug("Fetching entry: {}", dn);
            Entry entry = restTemplate.getForObject(url, Entry.class);
            return Optional.ofNullable(entry);
        } catch (Exception e) {
            log.debug("Entry not found or error: {}", dn, e);
            return Optional.empty();
        }
    }

    /**
     * Update an entry (full replacement).
     *
     * @param dn the distinguished name
     * @param entry the updated entry
     */
    public void putEntry(String dn, Entry entry) {
        String url = minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn);
        log.debug("Updating entry: {}", dn);
        restTemplate.put(url, entry);
    }

    /**
     * Partially update an entry (merge).
     *
     * @param dn the distinguished name
     * @param partialEntry entry with only fields to update
     */
    public void patchEntry(String dn, Entry partialEntry) {
        String url = minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn);
        log.debug("Patching entry: {}", dn);
        restTemplate.exchange(url, HttpMethod.PATCH, new org.springframework.http.HttpEntity<>(partialEntry), Void.class);
    }

    /**
     * Delete an entry.
     *
     * @param dn the distinguished name
     */
    public void deleteEntry(String dn) {
        String url = minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn);
        log.debug("Deleting entry: {}", dn);
        restTemplate.delete(url);
    }

    /**
     * Search for entries under a base DN with a filter.
     *
     * @param baseDn the base DN for search (e.g., "ou=users,dc=minipay,dc=ro")
     * @param filter attribute filters (e.g., Map.of("mail", "user@example.com"))
     * @param limit maximum results
     * @return list of matching entries
     */
    public java.util.List<Entry> search(String baseDn, Map<String, String> filter, int limit) {
        String url = minidsBaseUrl + "/minids/v1/search";
        SearchRequest request = new SearchRequest(baseDn, filter != null ? filter : Collections.emptyMap(), limit);
        log.debug("Searching under {}: {}", baseDn, filter);

        try {
            SearchResponse response = restTemplate.postForObject(url, request, SearchResponse.class);
            return response != null && response.entries != null ? response.entries : Collections.emptyList();
        } catch (Exception e) {
            log.error("Search error", e);
            return Collections.emptyList();
        }
    }

    /**
     * URL-encode a DN for safe inclusion in URL path.
     */
    private String urlEncodeDn(String dn) {
        return dn.replace(",", "%2C").replace("=", "%3D");
    }

    // Simple DTO classes for request/response
    public static class SearchRequest {
        public String baseDn;
        public Map<String, String> filter;
        public int limit;

        public SearchRequest(String baseDn, Map<String, String> filter, int limit) {
            this.baseDn = baseDn;
            this.filter = filter;
            this.limit = limit;
        }
    }

    public static class SearchResponse {
        public java.util.List<Entry> entries;
    }

    /**
     * Minimal Entry class for serialization.
     * Mirrors minids/src/main/java/ro/minipay/minids/model/Entry.java
     */
    public static class Entry {
        public String dn;
        public String objectClass;
        public Map<String, String> attributes;

        public Entry() {}

        public Entry(String dn, String objectClass, Map<String, String> attributes) {
            this.dn = dn;
            this.objectClass = objectClass;
            this.attributes = attributes != null ? attributes : new java.util.HashMap<>();
        }

        public String getDn() { return dn; }
        public void setDn(String dn) { this.dn = dn; }

        public String getObjectClass() { return objectClass; }
        public void setObjectClass(String objectClass) { this.objectClass = objectClass; }

        public Map<String, String> getAttributes() { return attributes; }
        public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }

        public String getAttribute(String key) {
            return attributes != null ? attributes.get(key) : null;
        }

        public void setAttribute(String key, String value) {
            if (attributes == null) {
                attributes = new java.util.HashMap<>();
            }
            attributes.put(key, value);
        }
    }
}
