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
 * RaftClient — wrapper over MicroRaft RaftNode.
 *
 * Writes → replicate() → go through Raft Leader → commit on majority
 * Reads  → local from RocksDB (may be slightly stale on replicas)
 *
 * Analogy with PingDS:
 *   Writes go to any node, but are routed internally to the master.
 *   Reads can be served locally (eventual consistency) or
 *   from the master (strong consistency).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RaftClient {

    private final RaftNode raftNode;
    private final RocksDBStore store;

    private static final long TIMEOUT_MS = 5000;

    /**
     * Submits a write operation through Raft.
     * Blocks until COMMIT on majority (2/3 nodes).
     */
    public void submit(DSOperation operation) {
        try {
            raftNode.replicate(operation)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RaftTimeoutException("Raft timeout for " + operation.type() + " on " + operation.dn());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RaftException("Raft interrupted", e);
        } catch (ExecutionException e) {
            throw new RaftException("Raft execution error: " + e.getCause().getMessage(), e.getCause());
        }
    }

    /**
     * Local read from RocksDB — without going through Raft.
     * May return data slightly behind the Leader (eventual consistency).
     * Acceptable for tokens/sessions that have a TTL.
     */
    public Optional<Entry> readLocal(String dn) throws RocksDBException {
        return store.get(dn);
    }

    /**
     * Local search — without Raft.
     */
    public List<Entry> search(String baseDn, Map<String, String> filter, int limit)
            throws RocksDBException {
        return store.search(baseDn, filter, limit);
    }

    // ─── Exceptions ───────────────────────────────────────────

    public static class RaftTimeoutException extends RuntimeException {
        public RaftTimeoutException(String message) { super(message); }
    }

    public static class RaftException extends RuntimeException {
        public RaftException(String message, Throwable cause) { super(message, cause); }
    }
}
