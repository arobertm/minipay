package ro.minipay.session.shared.minids;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST client wrapper for MiniDS Directory Server.
 * Sessions are stored under: ou=sessions,dc=minipay,dc=ro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniDSClientService {

    private final RestTemplate restTemplate;

    @Value("${minids.base-url}")
    private String minidsBaseUrl;

    public Entry createEntry(Entry entry) {
        String url = minidsBaseUrl + "/minids/v1/entries";
        log.debug("Creating entry: {}", entry.getDn());
        Entry created = restTemplate.postForObject(url, entry, Entry.class);
        if (created == null) {
            throw new RuntimeException("Failed to create entry: " + entry.getDn());
        }
        return created;
    }

    public Optional<Entry> getEntry(String dn) {
        try {
            String url = minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn);
            log.debug("Fetching entry: {}", dn);
            Entry entry = restTemplate.getForObject(url, Entry.class);
            return Optional.ofNullable(entry);
        } catch (Exception e) {
            log.debug("Entry not found: {}", dn);
            return Optional.empty();
        }
    }

    public void patchEntry(String dn, Entry partialEntry) {
        String url = minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn);
        log.debug("Patching entry: {}", dn);
        restTemplate.exchange(url, HttpMethod.PATCH,
            new org.springframework.http.HttpEntity<>(partialEntry), Void.class);
    }

    public void deleteEntry(String dn) {
        String url = minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn);
        log.debug("Deleting entry: {}", dn);
        restTemplate.delete(url);
    }

    public List<Entry> search(String baseDn, Map<String, String> filter, int limit) {
        String url = minidsBaseUrl + "/minids/v1/search";
        SearchRequest request = new SearchRequest(baseDn, filter != null ? filter : Collections.emptyMap(), limit);
        log.debug("Searching under {}: {}", baseDn, filter);
        try {
            SearchResponse response = restTemplate.postForObject(url, request, SearchResponse.class);
            return response != null && response.entries != null ? response.entries : Collections.emptyList();
        } catch (Exception e) {
            log.error("MiniDS search error", e);
            return Collections.emptyList();
        }
    }

    private String urlEncodeDn(String dn) {
        return dn.replace(",", "%2C").replace("=", "%3D");
    }

    // --- DTOs ---

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
        public List<Entry> entries;
    }

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
    }
}
