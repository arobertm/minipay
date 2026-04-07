package ro.minipay.minids.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Service;
import ro.minipay.minids.model.Entry;
import ro.minipay.minids.raft.DSOperation;
import ro.minipay.minids.raft.RaftClient;
import ro.minipay.minids.schema.SchemaValidator;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DirectoryService — layer de business logic peste Raft + RocksDB.
 *
 * Toate scrierile trec prin Raft (pentru replicare).
 * Citirile pot fi locale (READ de pe orice nod) sau
 * consistent reads (doar de pe Leader).
 *
 * Analogie cu PingDS REST API:
 *   POST   /users/{id}   → createEntry()
 *   GET    /users/{id}   → getEntry()
 *   PUT    /users/{id}   → updateEntry()
 *   DELETE /users/{id}   → deleteEntry()
 *   POST   /search       → search()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final RaftClient raftClient;
    private final SchemaValidator schemaValidator;

    /**
     * Creeaza un entry nou in director.
     * Trece prin Raft → replicat pe toate nodurile.
     */
    public Entry createEntry(Entry entry) {
        if (entry.getCreateTimestamp() == null) {
            entry.setCreateTimestamp(Instant.now());
        }
        entry.setModifyTimestamp(Instant.now());
        entry.setVersion(1);

        schemaValidator.validate(entry);
        raftClient.submit(DSOperation.put(entry.getDn(), entry));
        log.debug("Entry creat: {}", entry.getDn());
        return entry;
    }

    /**
     * Citeste un entry dupa DN.
     * Read local — nu trece prin Raft (poate fi usor stale pe replici).
     */
    public Optional<Entry> getEntry(String dn) {
        try {
            return raftClient.readLocal(dn);
        } catch (RocksDBException e) {
            throw new RuntimeException("Eroare citire entry: " + dn, e);
        }
    }

    /**
     * Actualizeaza un entry existent.
     * Trece prin Raft → replicat.
     */
    public Entry updateEntry(String dn, Entry entry) {
        entry.setDn(dn);
        entry.setModifyTimestamp(Instant.now());
        entry.setVersion(entry.getVersion() + 1);

        schemaValidator.validate(entry);
        raftClient.submit(DSOperation.put(dn, entry));
        log.debug("Entry actualizat: {}", dn);
        return entry;
    }

    /**
     * Sterge un entry.
     * Trece prin Raft → sters pe toate nodurile.
     */
    public void deleteEntry(String dn) {
        raftClient.submit(DSOperation.delete(dn));
        log.debug("Entry sters: {}", dn);
    }

    /**
     * Cauta entry-uri sub un baseDN cu filtru.
     * Inspirat din LDAP Search Operation (RFC 4511).
     */
    public List<Entry> search(String baseDn, Map<String, String> filter, int limit) {
        try {
            return raftClient.search(baseDn, filter, limit);
        } catch (RocksDBException e) {
            throw new RuntimeException("Eroare search sub: " + baseDn, e);
        }
    }

    public boolean exists(String dn) {
        return getEntry(dn).isPresent();
    }
}
