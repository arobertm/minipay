package ro.minipay.vault.shared.minids;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST client for MiniDS Directory Server.
 * Vault tokens stored under: ou=vault,dc=minipay,dc=ro
 *
 * Implements automatic leader-retry: if the targeted node is a Raft follower,
 * it parses the leader's address from the error body and retries there.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniDSClientService {

    private static final Pattern LEADER_PATTERN =
        Pattern.compile("Known leader is: MiniDSEndpoint\\[id=\\S+, host=(\\S+), port=(\\d+)\\]");

    private final RestTemplate restTemplate;

    @Value("${minids.base-url}")
    private String minidsBaseUrl;

    /** Known leader URL — updated on Raft redirect, cached for subsequent calls. */
    private volatile String leaderBaseUrl = null;

    public Entry createEntry(Entry entry) {
        return writeWithLeaderRetry(url ->
            restTemplate.postForObject(url + "/minids/v1/entries", entry, Entry.class));
    }

    public Optional<Entry> getEntry(String dn) {
        try {
            // Build URI without double-encoding: encode once via UriComponentsBuilder
            java.net.URI uri = UriComponentsBuilder
                .fromUriString(effectiveUrl() + "/minids/v1/entries/{dn}")
                .build(dn);   // UriComponentsBuilder encodes path variable once
            log.debug("MiniDS getEntry: {}", uri);
            Entry entry = restTemplate.getForObject(uri, Entry.class);
            log.debug("MiniDS getEntry result: dn={} attrs={}", entry != null ? entry.getDn() : "null",
                entry != null ? entry.getAttributes() : "null");
            return Optional.ofNullable(entry);
        } catch (Exception e) {
            log.error("MiniDS getEntry error for dn={}: {} - {}", dn, e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    public void deleteEntry(String dn) {
        writeWithLeaderRetry(url -> {
            java.net.URI uri = UriComponentsBuilder
                .fromUriString(url + "/minids/v1/entries/{dn}")
                .build(dn);
            restTemplate.delete(uri);
            return null;
        });
    }

    public List<Entry> search(String baseDn, Map<String, String> filter, int limit) {
        try {
            SearchResponse response = restTemplate.postForObject(
                effectiveUrl() + "/minids/v1/search",
                new SearchRequest(baseDn, filter != null ? filter : Collections.emptyMap(), limit),
                SearchResponse.class);
            return response != null && response.entries != null ? response.entries : Collections.emptyList();
        } catch (Exception e) {
            log.error("MiniDS search error", e);
            return Collections.emptyList();
        }
    }

    // ─── Leader retry logic ──────────────────────────────────────────────────

    @FunctionalInterface
    private interface MiniDSCall<T> {
        T call(String baseUrl);
    }

    private <T> T writeWithLeaderRetry(MiniDSCall<T> call) {
        String url = effectiveUrl();
        try {
            return call.call(url);
        } catch (HttpServerErrorException e) {
            if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                String body = e.getResponseBodyAsString();
                String leaderUrl = extractLeaderUrl(body);
                if (leaderUrl != null) {
                    log.info("MiniDS follower detected, retrying on leader: {}", leaderUrl);
                    leaderBaseUrl = leaderUrl;
                    return call.call(leaderUrl);
                }
            }
            throw e;
        }
    }

    private String effectiveUrl() {
        return leaderBaseUrl != null ? leaderBaseUrl : minidsBaseUrl;
    }

    private String extractLeaderUrl(String errorBody) {
        if (errorBody == null) return null;
        Matcher m = LEADER_PATTERN.matcher(errorBody);
        if (m.find()) {
            return "http://" + m.group(1) + ":" + m.group(2);
        }
        return null;
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public static class SearchRequest {
        public String baseDn; public Map<String, String> filter; public int limit;
        public SearchRequest(String b, Map<String, String> f, int l) { baseDn=b; filter=f; limit=l; }
    }
    public static class SearchResponse { public List<Entry> entries; }

    /**
     * Client-side Entry — mirrors MiniDS Entry serialization.
     * MiniDS uses @JsonAnyGetter to flatten attributes into the JSON root,
     * so we use @JsonAnySetter to capture them back into the attributes map.
     * Known structural fields (dn, objectClass, createTimestamp, etc.) are
     * mapped explicitly and excluded from the attributes map via @JsonIgnore.
     */
    public static class Entry {
        public String dn;
        public String objectClass;
        private final Map<String, String> attributes = new HashMap<>();

        public Entry() {}
        public Entry(String dn, String objectClass, Map<String, String> attrs) {
            this.dn = dn;
            this.objectClass = objectClass;
            if (attrs != null) this.attributes.putAll(attrs);
        }

        public String getDn() { return dn; }
        public void setDn(String dn) { this.dn = dn; }
        public String getObjectClass() { return objectClass; }
        public void setObjectClass(String v) { this.objectClass = v; }

        @JsonAnyGetter
        public Map<String, String> getAttributes() { return attributes; }

        @JsonAnySetter
        public void setAttribute(String key, Object value) {
            // Skip structural MiniDS metadata fields
            if (key.equals("createTimestamp") || key.equals("modifyTimestamp")
                    || key.equals("version") || key.equals("rdn") || key.equals("parentDn")) {
                return;
            }
            attributes.put(key, value != null ? value.toString() : null);
        }

        @JsonIgnore
        public String getAttribute(String key) { return attributes.get(key); }
    }
}
