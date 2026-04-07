package ro.minipay.minids.raft;

import io.microraft.RaftNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Component;
import ro.minipay.minids.model.Entry;
import ro.minipay.minids.storage.RocksDBStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RaftClient — wrapper peste MicroRaft RaftNode.
 *
 * Scrieri → replicate() → trec prin Raft Leader → commit pe majority
 * Citiri  → locale din RocksDB (pot fi usor stale pe replici)
 *
 * Analogie cu PingDS:
 *   Scrierile merg la orice nod, dar sunt rutate intern la master.
 *   Citirile pot fi servite local (eventual consistency) sau
 *   de pe master (strong consistency).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RaftClient {

    private final RaftNode raftNode;
    private final RocksDBStore store;

    private static final long TIMEOUT_MS = 5000;

    /**
     * Trimite o operatie de scriere prin Raft.
     * Blocheaza pana la COMMIT pe majority (2/3 noduri).
     */
    public void submit(DSOperation operation) {
        try {
            raftNode.replicate(operation)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RaftTimeoutException("Raft timeout pentru " + operation.type() + " pe " + operation.dn());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RaftException("Raft intrerupt", e);
        } catch (ExecutionException e) {
            throw new RaftException("Raft eroare de executie: " + e.getCause().getMessage(), e.getCause());
        }
    }

    /**
     * Citire locala din RocksDB — fara a trece prin Raft.
     * Poate returna date usor in urma fata de Leader (eventual consistency).
     * Acceptabil pentru tokens/sessions care au TTL.
     */
    public Optional<Entry> readLocal(String dn) throws RocksDBException {
        return store.get(dn);
    }

    /**
     * Search local — fara Raft.
     */
    public List<Entry> search(String baseDn, Map<String, String> filter, int limit)
            throws RocksDBException {
        return store.search(baseDn, filter, limit);
    }

    // ─── Exceptii ─────────────────────────────────────────────

    public static class RaftTimeoutException extends RuntimeException {
        public RaftTimeoutException(String message) { super(message); }
    }

    public static class RaftException extends RuntimeException {
        public RaftException(String message, Throwable cause) { super(message, cause); }
    }
}
