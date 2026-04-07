package ro.minipay.minids.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.time.Instant;
import java.util.*;

/**
 * Entry — unitatea de baza a MiniDS, inspirata din LDAP Entry.
 *
 * Similar cu un entry LDAP din PingDS:
 *   dn: uid=john,ou=users,dc=minipay,dc=ro
 *   objectClass: minipayUser
 *   uid: john
 *   cn: John Doe
 *   ...
 *
 * Diferenta fata de LDAP clasic: stocam JSON, nu ASN.1 binar.
 * Backend: RocksDB (similar Berkeley DB JE din PingDS real).
 */
@Data
public class Entry {

    /** Distinguished Name — cheia unica in director */
    private String dn;

    /** Tipul entry-ului — determina ce atribute sunt permise */
    private String objectClass;

    /** Atributele entry-ului (schema-flexible, ca LDAP) */
    private Map<String, Object> attributes = new LinkedHashMap<>();

    /** Metadata interna */
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
     * Extrage RDN (Relative Distinguished Name) — primul component din DN.
     * Ex: "uid=john,ou=users,dc=minipay,dc=ro" → "uid=john"
     */
    public String getRdn() {
        if (dn == null) return null;
        int comma = dn.indexOf(',');
        return comma > 0 ? dn.substring(0, comma) : dn;
    }

    /**
     * Extrage parent DN.
     * Ex: "uid=john,ou=users,dc=minipay,dc=ro" → "ou=users,dc=minipay,dc=ro"
     */
    public String getParentDn() {
        if (dn == null) return null;
        int comma = dn.indexOf(',');
        return comma > 0 ? dn.substring(comma + 1) : null;
    }
}
