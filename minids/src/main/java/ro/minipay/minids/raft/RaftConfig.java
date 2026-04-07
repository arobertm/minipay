package ro.minipay.minids.raft;

import io.microraft.RaftNode;
import io.microraft.model.impl.DefaultRaftModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.microraft.RaftEndpoint;

import java.util.Arrays;
import java.util.List;

/**
 * Configuratie Spring pentru MicroRaft.
 *
 * Fiecare nod MiniDS porneste cu:
 *   - NODE_ID propriu (minids-0, minids-1, minids-2)
 *   - lista peer-urilor din cluster
 *   - StateMachine propriu (aplica operatiile in RocksDB local)
 */
@Slf4j
@Configuration
public class RaftConfig {

    @Value("${minids.node-id}")
    private String nodeId;

    @Value("${minids.raft-peers}")
    private String raftPeers;

    @Value("${minids.raft-port:8300}")
    private int raftPort;

    @Bean
    RaftNode raftNode(MiniDSStateMachine stateMachine, MiniDSTransport transport) {

        List<RaftEndpoint> endpoints = parseEndpoints(raftPeers);

        MiniDSEndpoint localEndpoint = endpoints.stream()
                .filter(e -> e instanceof MiniDSEndpoint m && m.id().equals(nodeId))
                .map(e -> (MiniDSEndpoint) e)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "NODE_ID '" + nodeId + "' nu se gaseste in RAFT_PEERS: " + raftPeers));

        io.microraft.RaftConfig config = io.microraft.RaftConfig.newBuilder()
                .setLeaderHeartbeatPeriodSecs(1)
                .setLeaderHeartbeatTimeoutSecs(5)
                .setCommitCountToTakeSnapshot(1000)
                .build();

        log.info("Pornesc nod Raft: {} din cluster: {}", nodeId, raftPeers);

        return RaftNode.newBuilder()
                .setGroupId("minids-cluster")
                .setLocalEndpoint(localEndpoint)
                .setInitialGroupMembers(endpoints)
                .setConfig(config)
                .setStateMachine(stateMachine)
                .setTransport(transport)
                .setModelFactory(new DefaultRaftModelFactory())
                .build();
    }

    @Bean
    MiniDSTransport miniDSTransport() {
        return new MiniDSTransport(nodeId, raftPort);
    }

    private List<RaftEndpoint> parseEndpoints(String peers) {
        // Format: "minids-0:8300,minids-1:8310,minids-2:8320"
        return Arrays.stream(peers.split(","))
                .map(peer -> {
                    String[] parts = peer.trim().split(":");
                    return (RaftEndpoint) new MiniDSEndpoint(parts[0], parts[0], Integer.parseInt(parts[1]));
                })
                .toList();
    }
}
