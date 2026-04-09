package ro.minipay.vault.shared.minids;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST client for MiniDS Directory Server.
 * Vault tokens stored under: ou=vault,dc=minipay,dc=ro
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
        Entry created = restTemplate.postForObject(url, entry, Entry.class);
        if (created == null) throw new RuntimeException("Failed to create entry: " + entry.getDn());
        return created;
    }

    public Optional<Entry> getEntry(String dn) {
        try {
            Entry entry = restTemplate.getForObject(
                minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn), Entry.class);
            return Optional.ofNullable(entry);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void deleteEntry(String dn) {
        restTemplate.delete(minidsBaseUrl + "/minids/v1/entries/" + urlEncodeDn(dn));
    }

    public List<Entry> search(String baseDn, Map<String, String> filter, int limit) {
        try {
            SearchResponse response = restTemplate.postForObject(
                minidsBaseUrl + "/minids/v1/search",
                new SearchRequest(baseDn, filter != null ? filter : Collections.emptyMap(), limit),
                SearchResponse.class);
            return response != null && response.entries != null ? response.entries : Collections.emptyList();
        } catch (Exception e) {
            log.error("MiniDS search error", e);
            return Collections.emptyList();
        }
    }

    private String urlEncodeDn(String dn) {
        return dn.replace(",", "%2C").replace("=", "%3D");
    }

    public static class SearchRequest {
        public String baseDn; public Map<String, String> filter; public int limit;
        public SearchRequest(String b, Map<String, String> f, int l) { baseDn=b; filter=f; limit=l; }
    }
    public static class SearchResponse { public List<Entry> entries; }

    public static class Entry {
        public String dn; public String objectClass; public Map<String, String> attributes;
        public Entry() {}
        public Entry(String dn, String objectClass, Map<String, String> attributes) {
            this.dn = dn; this.objectClass = objectClass;
            this.attributes = attributes != null ? attributes : new java.util.HashMap<>();
        }
        public String getDn() { return dn; }
        public void setDn(String dn) { this.dn = dn; }
        public String getObjectClass() { return objectClass; }
        public void setObjectClass(String v) { this.objectClass = v; }
        public Map<String, String> getAttributes() { return attributes; }
        public void setAttributes(Map<String, String> v) { this.attributes = v; }
        public String getAttribute(String key) { return attributes != null ? attributes.get(key) : null; }
    }
}
