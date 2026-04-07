package ro.minipay.minids.raft;

import io.microraft.RaftEndpoint;

/**
 * Reprezinta un nod din clusterul MiniDS.
 *
 * @param id       identificatorul unic (ex: "minids-0")
 * @param host     adresa de retea (ex: "localhost" sau "minids-0" pe Docker)
 * @param port     portul Raft — folosit de MicroRaft intern (ex: 8300)
 * @param httpPort portul HTTP — unde RaftMessageController asculta (ex: 8301)
 *
 * Conventie: httpPort = raftPort + 1
 *   minids-0: Raft=8300, HTTP=8301
 *   minids-1: Raft=8310, HTTP=8311
 *   minids-2: Raft=8320, HTTP=8321
 */
public record MiniDSEndpoint(String id, String host, int port, int httpPort)
        implements RaftEndpoint {

    /** Constructor scurt — calculeaza httpPort = raftPort + 1 */
    public MiniDSEndpoint(String id, String host, int raftPort) {
        this(id, host, raftPort, raftPort + 1);
    }

    @Override
    public Comparable<String> getId() {
        return id;
    }
}
