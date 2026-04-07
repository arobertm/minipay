package ro.minipay.minids.raft;

import io.microraft.statemachine.StateMachine;
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
 * Analogie cu PingDS:
 *   PingDS real → changelog replication (multi-master)
 *   MiniDS      → MicroRaft (single-leader consensus)
 *
 * Flux Raft pentru o scriere:
 *   1. Client → Leader: trimite DSOperation
 *   2. Leader: adauga in log + trimite AppendEntries la replici
 *   3. Majority ACK (2 din 3) → COMMIT
 *   4. runOperation() apelat pe FIECARE nod in aceeasi ordine
 *   5. Fiecare nod aplica in RocksDB propriu
 *   6. Leader raspunde clientului
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniDSStateMachine implements StateMachine {

    private final RocksDBStore store;

    /**
     * Apelat de MicroRaft dupa COMMIT.
     * Garantat: aceeasi ordine pe toate nodurile.
     */
    @Override
    public Object runOperation(long commitIndex, Object operation) {
        if (!(operation instanceof DSOperation op)) {
            throw new IllegalArgumentException("Operatie necunoscuta: " + operation);
        }

        try {
            return switch (op.type()) {
                case PUT    -> executePut(op);
                case DELETE -> executeDelete(op);
                case GET    -> executeGet(op);
                case SEARCH -> executeSearch(op);
            };
        } catch (RocksDBException e) {
            log.error("RocksDB error la commitIndex={}: {}", commitIndex, e.getMessage());
            throw new RuntimeException("Eroare storage la commitIndex=" + commitIndex, e);
        }
    }

    /**
     * Operatie speciala trimisa automat de MicroRaft la inceputul unui nou termen.
     * Folosita pentru a marca tranzitia de leadership in log.
     * Returnam null = nicio operatie necesara la schimbarea termenului.
     */
    @Override
    public Object getNewTermOperation() {
        return null;
    }

    /**
     * Snapshot — salveaza starea pentru noduri noi sau ramase in urma.
     * In PingDS: "full replication" cand un nod nou se alatura.
     */
    @Override
    public void takeSnapshot(long commitIndex, Consumer<Object> snapshotChunkConsumer) {
        log.info("Snapshot la commitIndex={}", commitIndex);
        snapshotChunkConsumer.accept("snapshot-" + commitIndex);
    }

    /**
     * Instaleaza snapshot pe un nod nou.
     */
    @Override
    public void installSnapshot(long commitIndex, List<Object> snapshotChunks) {
        log.info("Install snapshot la commitIndex={}, chunks={}", commitIndex, snapshotChunks.size());
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
