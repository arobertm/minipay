package ro.minipay.minids.raft;

import io.microraft.RaftNode;
import io.microraft.model.message.RaftMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * Receiver for Raft messages between nodes.
 *
 * MiniDSTransport sends HTTP messages to this endpoint
 * on the other nodes in the cluster.
 *
 * Types of Raft messages received:
 *   VoteRequest      → candidate requests votes (leader election)
 *   VoteResponse     → response to a vote request
 *   AppendEntries    → leader sends log entries to replicas
 *   AppendEntriesAck → replica confirms receipt of entries
 *   InstallSnapshot  → leader sends snapshot to new nodes
 */
@Slf4j
@RestController
@RequestMapping("/raft")
@RequiredArgsConstructor
public class RaftMessageController {

    private final RaftNode raftNode;

    /**
     * Receives a Raft message from another node in the cluster (binary Java serialization).
     * RaftMessage is abstract — it cannot be deserialized directly from JSON.
     */
    @PostMapping(value = "/message", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> receiveMessage(@RequestBody byte[] body) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(body))) {
            RaftMessage message = (RaftMessage) ois.readObject();
            raftNode.handle(message);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Raft message deserialization error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Health check used by MiniDSTransport.isReachable()
     */
    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    /**
     * Current status of the Raft node (for debugging and monitoring).
     */
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(java.util.Map.of(
                "nodeId", raftNode.getLocalEndpoint().getId(),
                "term",   raftNode.getTerm(),
                "status", raftNode.getStatus().name()
        ));
    }
}
