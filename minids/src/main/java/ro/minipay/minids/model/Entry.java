package ro.minipay.minids.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;

/**
 * Entry — the basic unit of MiniDS, inspired by the LDAP Entry.
 *
 * Similar to an LDAP entry in PingDS:
 *   dn: uid=john,ou=users,dc=minipay,dc=ro
 *   objectClass: minipayUser
 *   uid: john
 *   cn: John Doe
 *   ...
 *
 * Difference from classic LDAP: we store JSON, not binary ASN.1.
 * Backend: RocksDB (similar to Berkeley DB JE in real PingDS).
 */
@Data
public class Entry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Distinguished Name — the unique key in the directory */
    private String dn;

    /** The type of the entry — determines which attributes are allowed */
    private String objectClass;

    /** Entry attributes (schema-flexible, like LDAP) */
    private Map<String, Object> attributes = new LinkedHashMap<>();

    /** Internal metadata */
    private Instant createTimestamp;
    private Instant modifyTimestamp;
    private long version = 1;

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public String getAttribute(String key) {
        Object val = attributes.get(key);
        return val != null ? val.toString() : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getAttributeList(String key) {
        Object val = attributes.get(key);
        if (val instanceof List) return (List<String>) val;
        if (val != null) return List.of(val.toString());
        return Collections.emptyList();
    }

    /**
     * Extracts the RDN (Relative Distinguished Name) — the first component of the DN.
     * E.g.: "uid=john,ou=users,dc=minipay,dc=ro" → "uid=john"
     */
    public String getRdn() {
        if (dn == null) return null;
        int comma = dn.indexOf(',');
        return comma > 0 ? dn.substring(0, comma) : dn;
    }

    /**
     * Extracts the parent DN.
     * E.g.: "uid=john,ou=users,dc=minipay,dc=ro" → "ou=users,dc=minipay,dc=ro"
     */
    public String getParentDn() {
        if (dn == null) return null;
        int comma = dn.indexOf(',');
        return comma > 0 ? dn.substring(comma + 1) : null;
    }
}
