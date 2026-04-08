package ro.minipay.minids.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for MiniDS.
 * Eliminates the "Unknown property 'minids'" warning in the IDE.
 */
@Component
@ConfigurationProperties(prefix = "minids")
public class MiniDSProperties {

    private String nodeId   = "minids-0";
    private int    raftPort = 8300;
    private String raftPeers = "minids-0:8301";
    private String dataDir  = "./data/rocksdb";

    public String getNodeId()      { return nodeId; }
    public int    getRaftPort()    { return raftPort; }
    public String getRaftPeers()   { return raftPeers; }
    public String getDataDir()     { return dataDir; }

    public void setNodeId(String nodeId)       { this.nodeId = nodeId; }
    public void setRaftPort(int raftPort)      { this.raftPort = raftPort; }
    public void setRaftPeers(String raftPeers) { this.raftPeers = raftPeers; }
    public void setDataDir(String dataDir)     { this.dataDir = dataDir; }
}
