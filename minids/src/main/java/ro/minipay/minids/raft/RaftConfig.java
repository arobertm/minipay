package ro.minipay.minids.raft;

import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.model.impl.DefaultRaftModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.minipay.minids.config.MiniDSProperties;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RaftConfig {

    private final MiniDSProperties props;

    @Bean
    RaftNode raftNode(MiniDSStateMachine stateMachine, MiniDSTransport transport) {

        List<RaftEndpoint> endpoints = parseEndpoints(props.getRaftPeers());

        MiniDSEndpoint localEndpoint = endpoints.stream()
                .filter(e -> e instanceof MiniDSEndpoint m && m.id().equals(props.getNodeId()))
                .map(e -> (MiniDSEndpoint) e)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "NODE_ID '" + props.getNodeId() + "' not found in RAFT_PEERS: " + props.getRaftPeers()));

        io.microraft.RaftConfig config = io.microraft.RaftConfig.newBuilder()
                .setLeaderHeartbeatPeriodSecs(1)
                .setLeaderHeartbeatTimeoutSecs(5)
                .setCommitCountToTakeSnapshot(1000)
                .build();

        log.info("Starting Raft node: {} with peers: {}", props.getNodeId(), props.getRaftPeers());

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
        return new MiniDSTransport(props.getNodeId(), props.getRaftPort());
    }

    private List<RaftEndpoint> parseEndpoints(String peers) {
        // Format: "minids-0:8301,minids-1:8311,minids-2:8321"
        // Port = direct HTTP port (API port)
        // On Docker/K8s: host = node-id (internal DNS)
        // On localhost:  RAFT_RESOLVE_LOCALHOST=true → host = "localhost"
        String resolveHost = System.getenv().getOrDefault("RAFT_RESOLVE_LOCALHOST",
                             System.getProperty("RAFT_RESOLVE_LOCALHOST", "false"));
        boolean useLocalhost = "true".equalsIgnoreCase(resolveHost);

        return Arrays.stream(peers.split(","))
                .map(peer -> {
                    String[] parts = peer.trim().split(":");
                    String id   = parts[0];
                    String host = useLocalhost ? "localhost" : parts[0];
                    int    port = Integer.parseInt(parts[1]);
                    return (RaftEndpoint) new MiniDSEndpoint(id, host, port);
                })
                .toList();
    }
}
