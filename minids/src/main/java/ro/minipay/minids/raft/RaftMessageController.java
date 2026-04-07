package ro.minipay.minids.raft;

import io.microraft.RaftNode;
import io.microraft.model.message.RaftMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receiver pentru mesajele Raft intre noduri.
 *
 * MiniDSTransport trimite mesaje HTTP catre acest endpoint
 * pe celelalte noduri din cluster.
 *
 * Tipuri de mesaje Raft primite:
 *   VoteRequest      → candidatul cere voturi (leader election)
 *   VoteResponse     → raspuns la cerere de vot
 *   AppendEntries    → liderul trimite log entries la replici
 *   AppendEntriesAck → replica confirma primirea entries
 *   InstallSnapshot  → liderul trimite snapshot la noduri noi
 */
@Slf4j
@RestController
@RequestMapping("/raft")
@RequiredArgsConstructor
public class RaftMessageController {

    private final RaftNode raftNode;

    /**
     * Primeste un mesaj Raft de la alt nod din cluster.
     * MicroRaft il proceseaza intern si actualizeaza starea nodului.
     */
    @PostMapping("/message")
    public ResponseEntity<Void> receiveMessage(@RequestBody RaftMessage message) {
        raftNode.handle(message);
        return ResponseEntity.ok().build();
    }

    /**
     * Health check folosit de MiniDSTransport.isReachable()
     */
    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    /**
     * Status curent al nodului Raft (pentru debugging si monitorizare).
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
