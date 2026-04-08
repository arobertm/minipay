package ro.minipay.minids.raft;

import io.microraft.RaftNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the RaftNode after the Spring context is fully initialized.
 * MicroRaft requires an explicit call to .start() to enter the Raft lifecycle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RaftStartup {

    private final RaftNode raftNode;

    @EventListener(ApplicationReadyEvent.class)
    public void startRaft() {
        log.info("Starting Raft node: {}", raftNode.getLocalEndpoint().getId());
        raftNode.start();
        log.info("Raft node started. Status: {}", raftNode.getStatus());
    }
}
