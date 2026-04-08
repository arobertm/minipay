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
 * DirectoryService — business logic layer over Raft + RocksDB.
 *
 * All writes go through Raft (for replication).
 * Reads can be local (READ from any node) or
 * consistent reads (only from the Leader).
 *
 * Analogy with PingDS REST API:
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
     * Creates a new entry in the directory.
     * Goes through Raft → replicated to all nodes.
     */
    public Entry createEntry(Entry entry) {
        if (entry.getCreateTimestamp() == null) {
            entry.setCreateTimestamp(Instant.now());
        }
        entry.setModifyTimestamp(Instant.now());
        entry.setVersion(1);

        schemaValidator.validate(entry);
        raftClient.submit(DSOperation.put(entry.getDn(), entry));
        log.debug("Entry created: {}", entry.getDn());
        return entry;
    }

    /**
     * Reads an entry by DN.
     * Local read — does not go through Raft (may be slightly stale on replicas).
     */
    public Optional<Entry> getEntry(String dn) {
        try {
            return raftClient.readLocal(dn);
        } catch (RocksDBException e) {
            throw new RuntimeException("Error reading entry: " + dn, e);
        }
    }

    /**
     * Updates an existing entry.
     * Goes through Raft → replicated.
     */
    public Entry updateEntry(String dn, Entry entry) {
        entry.setDn(dn);
        entry.setModifyTimestamp(Instant.now());
        entry.setVersion(entry.getVersion() + 1);

        schemaValidator.validate(entry);
        raftClient.submit(DSOperation.put(dn, entry));
        log.debug("Entry updated: {}", dn);
        return entry;
    }

    /**
     * Deletes an entry.
     * Goes through Raft → deleted on all nodes.
     */
    public void deleteEntry(String dn) {
        raftClient.submit(DSOperation.delete(dn));
        log.debug("Entry deleted: {}", dn);
    }

    /**
     * Searches for entries under a baseDN with a filter.
     * Inspired by the LDAP Search Operation (RFC 4511).
     */
    public List<Entry> search(String baseDn, Map<String, String> filter, int limit) {
        try {
            return raftClient.search(baseDn, filter, limit);
        } catch (RocksDBException e) {
            throw new RuntimeException("Error searching under: " + baseDn, e);
        }
    }

    public boolean exists(String dn) {
        return getEntry(dn).isPresent();
    }
}
