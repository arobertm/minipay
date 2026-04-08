package ro.minipay.minids.raft;

import io.microraft.statemachine.StateMachine;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Component;
import ro.minipay.minids.storage.RocksDBStore;

import java.util.List;
import java.util.function.Consumer;

/**
 * MiniDS Raft State Machine.
 *
 * Analogy with PingDS:
 *   PingDS real → changelog replication (multi-master)
 *   MiniDS      → MicroRaft (single-leader consensus)
 *
 * Raft flow for a write:
 *   1. Client → Leader: sends DSOperation
 *   2. Leader: appends to log + sends AppendEntries to replicas
 *   3. Majority ACK (2 of 3) → COMMIT
 *   4. runOperation() called on EVERY node in the same order
 *   5. Each node applies to its own RocksDB
 *   6. Leader responds to the client
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniDSStateMachine implements StateMachine {

    private final RocksDBStore store;

    /**
     * Called by MicroRaft after COMMIT.
     * Guaranteed: same order on all nodes.
     */
    @Override
    public Object runOperation(long commitIndex, Object operation) {
        if (!(operation instanceof DSOperation op)) {
            throw new IllegalArgumentException("Unknown operation: " + operation);
        }

        try {
            return switch (op.type()) {
                case PUT    -> executePut(op);
                case DELETE -> executeDelete(op);
                case GET    -> executeGet(op);
                case SEARCH -> executeSearch(op);
            };
        } catch (RocksDBException e) {
            log.error("RocksDB error at commitIndex={}: {}", commitIndex, e.getMessage());
            throw new RuntimeException("Storage error at commitIndex=" + commitIndex, e);
        }
    }

    /**
     * Special operation automatically sent by MicroRaft at the beginning of a new term.
     * Used to mark the leadership transition in the log.
     * Returns null = no operation needed on term change.
     */
    @Override
    public Object getNewTermOperation() {
        return null;
    }

    /**
     * Snapshot — saves state for new or lagging nodes.
     * In PingDS: "full replication" when a new node joins.
     */
    @Override
    public void takeSnapshot(long commitIndex, Consumer<Object> snapshotChunkConsumer) {
        log.info("Snapshot at commitIndex={}", commitIndex);
        snapshotChunkConsumer.accept("snapshot-" + commitIndex);
    }

    /**
     * Installs a snapshot on a new node.
     */
    @Override
    public void installSnapshot(long commitIndex, List<Object> snapshotChunks) {
        log.info("Installing snapshot at commitIndex={}, chunks={}", commitIndex, snapshotChunks.size());
    }

    // ─── Execute helpers ──────────────────────────────────────

    private Object executePut(DSOperation op) throws RocksDBException {
        store.put(op.dn(), op.entry());
        log.debug("PUT committed: {}", op.dn());
        return "OK";
    }

    private Object executeDelete(DSOperation op) throws RocksDBException {
        store.delete(op.dn());
        log.debug("DELETE committed: {}", op.dn());
        return "OK";
    }

    private Object executeGet(DSOperation op) throws RocksDBException {
        return store.get(op.dn()).orElse(null);
    }

    private Object executeSearch(DSOperation op) throws RocksDBException {
        return store.search(op.dn(), op.filter(), op.limit());
    }
}
