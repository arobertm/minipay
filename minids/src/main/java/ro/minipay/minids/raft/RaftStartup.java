package ro.minipay.minids.raft;

import io.microraft.RaftNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Porneste RaftNode dupa ce Spring context e complet initializat.
 * MicroRaft necesita apel explicit la .start() pentru a intra in Raft lifecycle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RaftStartup {

    private final RaftNode raftNode;

    @EventListener(ApplicationReadyEvent.class)
    public void startRaft() {
        log.info("Pornesc Raft node: {}", raftNode.getLocalEndpoint().getId());
        raftNode.start();
        log.info("Raft node pornit. Status: {}", raftNode.getStatus());
    }
}
