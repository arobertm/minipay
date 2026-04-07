package ro.minipay.minids.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.minipay.minids.model.Entry;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * RocksDB Storage Engine pentru MiniDS.
 *
 * Analogie cu PingDS:
 *   PingDS real    → Berkeley DB JE (sau RocksDB in versiuni noi)
 *   MiniDS         → RocksDB embedded
 *
 * Column Families = "tabele" separate in RocksDB:
 *   cf_entries     → toate entry-urile (key=DN, value=JSON)
 *   cf_idx_mail    → index dupa email (key=email, value=DN)
 *   cf_idx_uid     → index dupa uid (key=uid, value=DN)
 *   cf_idx_type    → index dupa objectClass (key=type+DN, value=DN)
 *   cf_idx_expiry  → index dupa expiry (pentru CTS cleanup)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RocksDBStore {

    @Value("${minids.data-dir:/data/rocksdb}")
    private String dataDir;

    private final ObjectMapper objectMapper;

    private RocksDB db;
    private DBOptions dbOptions;
    private ColumnFamilyHandle cfEntries;
    private ColumnFamilyHandle cfIdxMail;
    private ColumnFamilyHandle cfIdxUid;
    private ColumnFamilyHandle cfIdxType;
    private ColumnFamilyHandle cfIdxExpiry;

    @PostConstruct
    public void init() throws RocksDBException {
        RocksDB.loadLibrary();

        List<ColumnFamilyDescriptor> cfDescriptors = List.of(
            new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY),
            new ColumnFamilyDescriptor("cf_entries".getBytes()),
            new ColumnFamilyDescriptor("cf_idx_mail".getBytes()),
            new ColumnFamilyDescriptor("cf_idx_uid".getBytes()),
            new ColumnFamilyDescriptor("cf_idx_type".getBytes()),
            new ColumnFamilyDescriptor("cf_idx_expiry".getBytes())
        );

        List<ColumnFamilyHandle> handles = new ArrayList<>();

        dbOptions = new DBOptions()
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true);

        db = RocksDB.open(dbOptions, dataDir, cfDescriptors, handles);

        // handles[0] = default (ignoram)
        cfEntries  = handles.get(1);
        cfIdxMail  = handles.get(2);
        cfIdxUid   = handles.get(3);
        cfIdxType  = handles.get(4);
        cfIdxExpiry = handles.get(5);

        log.info("MiniDS RocksDB pornit la: {}", dataDir);
        initBaseDIT();
    }

    /**
     * Initializeaza arborele DIT de baza (ca PingDS la primul start).
     *
     * dc=minipay,dc=ro
     *   ou=users
     *   ou=tokens
     *   ou=sessions
     *   ou=clients
     *   ou=vault
     */
    private void initBaseDIT() throws RocksDBException {
        String baseDn = "dc=minipay,dc=ro";
        if (get(baseDn) == null) {
            log.info("Initializez DIT de baza...");
            createOU(baseDn, "domain", "dc", "minipay");
            createOU("ou=users,dc=minipay,dc=ro",    "organizationalUnit", "ou", "users");
            createOU("ou=tokens,dc=minipay,dc=ro",   "organizationalUnit", "ou", "tokens");
            createOU("ou=sessions,dc=minipay,dc=ro", "organizationalUnit", "ou", "sessions");
            createOU("ou=clients,dc=minipay,dc=ro",  "organizationalUnit", "ou", "clients");
            createOU("ou=vault,dc=minipay,dc=ro",    "organizationalUnit", "ou", "vault");
            log.info("DIT initializat: {}", baseDn);
        }
    }

    private void createOU(String dn, String objectClass, String attrKey, String attrVal)
            throws RocksDBException {
        Entry ou = new Entry();
        ou.setDn(dn);
        ou.setObjectClass(objectClass);
        ou.setAttribute(attrKey, attrVal);
        ou.setCreateTimestamp(java.time.Instant.now());
        ou.setModifyTimestamp(java.time.Instant.now());
        put(dn, ou);
    }

    // ─── CRUD Operations ──────────────────────────────────────

    public void put(String dn, Entry entry) throws RocksDBException {
        byte[] key   = dn.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] value = serialize(entry);

        try (WriteBatch batch = new WriteBatch();
             WriteOptions writeOpts = new WriteOptions()) {

            batch.put(cfEntries, key, value);
            updateIndexes(batch, entry);
            db.write(writeOpts, batch);
        }
    }

    public Optional<Entry> get(String dn) throws RocksDBException {
        byte[] key   = dn.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] value = db.get(cfEntries, key);
        if (value == null) return Optional.empty();
        return Optional.of(deserialize(value));
    }

    public void delete(String dn) throws RocksDBException {
        Optional<Entry> existing = get(dn);
        if (existing.isEmpty()) return;

        byte[] key = dn.toLowerCase().getBytes(StandardCharsets.UTF_8);
        try (WriteBatch batch = new WriteBatch();
             WriteOptions writeOpts = new WriteOptions()) {

            batch.delete(cfEntries, key);
            removeIndexes(batch, existing.get());
            db.write(writeOpts, batch);
        }
    }

    public boolean exists(String dn) throws RocksDBException {
        byte[] key = dn.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return db.get(cfEntries, key) != null;
    }

    // ─── Search ───────────────────────────────────────────────

    /**
     * Cauta entry-uri sub un baseDN cu un filtru simplu.
     *
     * Inspirat din LDAP Search Operation (RFC 4511).
     * PingDS real indexeaza automat; noi facem scan pe prefix.
     *
     * @param baseDn  ex: "ou=users,dc=minipay,dc=ro"
     * @param filter  ex: Map.of("mail", "john@example.com")
     * @param limit   numarul maxim de rezultate
     */
    public List<Entry> search(String baseDn, Map<String, String> filter, int limit)
            throws RocksDBException {

        List<Entry> results = new ArrayList<>();
        String prefix = baseDn.toLowerCase();

        try (RocksIterator iter = db.newIterator(cfEntries)) {
            iter.seekToFirst();
            while (iter.isValid() && results.size() < limit) {
                String key = new String(iter.key(), StandardCharsets.UTF_8);

                // Filtram doar entry-urile sub baseDN
                if (key.endsWith(prefix)) {
                    Entry entry = deserialize(iter.value());
                    if (matchesFilter(entry, filter)) {
                        results.add(entry);
                    }
                }
                iter.next();
            }
        }
        return results;
    }

    /** Cauta dupa email (foloseste indexul cf_idx_mail) */
    public Optional<Entry> findByMail(String mail) throws RocksDBException {
        byte[] dnBytes = db.get(cfIdxMail, mail.toLowerCase().getBytes(StandardCharsets.UTF_8));
        if (dnBytes == null) return Optional.empty();
        return get(new String(dnBytes, StandardCharsets.UTF_8));
    }

    /** Cauta dupa uid (foloseste indexul cf_idx_uid) */
    public Optional<Entry> findByUid(String uid) throws RocksDBException {
        byte[] dnBytes = db.get(cfIdxUid, uid.toLowerCase().getBytes(StandardCharsets.UTF_8));
        if (dnBytes == null) return Optional.empty();
        return get(new String(dnBytes, StandardCharsets.UTF_8));
    }

    // ─── Indexare ─────────────────────────────────────────────

    private void updateIndexes(WriteBatch batch, Entry entry) throws RocksDBException {
        byte[] dnBytes = entry.getDn().toLowerCase().getBytes(StandardCharsets.UTF_8);

        String mail = entry.getAttribute("mail");
        if (mail != null) {
            batch.put(cfIdxMail, mail.toLowerCase().getBytes(StandardCharsets.UTF_8), dnBytes);
        }

        String uid = entry.getAttribute("uid");
        if (uid != null) {
            batch.put(cfIdxUid, uid.toLowerCase().getBytes(StandardCharsets.UTF_8), dnBytes);
        }

        if (entry.getObjectClass() != null) {
            String typeKey = entry.getObjectClass() + ":" + entry.getDn().toLowerCase();
            batch.put(cfIdxType, typeKey.getBytes(StandardCharsets.UTF_8), dnBytes);
        }

        String expiry = entry.getAttribute("coreTokenExpiry");
        if (expiry != null) {
            String expiryKey = expiry + ":" + entry.getDn().toLowerCase();
            batch.put(cfIdxExpiry, expiryKey.getBytes(StandardCharsets.UTF_8), dnBytes);
        }
    }

    private void removeIndexes(WriteBatch batch, Entry entry) throws RocksDBException {
        String mail = entry.getAttribute("mail");
        if (mail != null) {
            batch.delete(cfIdxMail, mail.toLowerCase().getBytes(StandardCharsets.UTF_8));
        }
        String uid = entry.getAttribute("uid");
        if (uid != null) {
            batch.delete(cfIdxUid, uid.toLowerCase().getBytes(StandardCharsets.UTF_8));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────

    private boolean matchesFilter(Entry entry, Map<String, String> filter) {
        if (filter == null || filter.isEmpty()) return true;
        for (Map.Entry<String, String> f : filter.entrySet()) {
            String val = entry.getAttribute(f.getKey());
            if (val == null || !val.equalsIgnoreCase(f.getValue())) return false;
        }
        return true;
    }

    private byte[] serialize(Entry entry) {
        try {
            return objectMapper.writeValueAsBytes(entry);
        } catch (Exception e) {
            throw new RuntimeException("Serializare entry esuata", e);
        }
    }

    private Entry deserialize(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, Entry.class);
        } catch (Exception e) {
            throw new RuntimeException("Deserializare entry esuata", e);
        }
    }

    @PreDestroy
    public void close() {
        if (db != null) db.close();
        if (dbOptions != null) dbOptions.close();
        log.info("MiniDS RocksDB inchis.");
    }
}
