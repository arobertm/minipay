package ro.minipay.minids.raft;

import io.microraft.RaftEndpoint;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a node in the MiniDS cluster.
 *
 * Implements Serializable because MicroRaft includes RaftEndpoint
 * in Raft messages, and our transport uses Java serialization.
 *
 * @param id    unique identifier (e.g.: "minids-0")
 * @param host  network address (e.g.: "localhost" or "minids-0" on Docker)
 * @param port  HTTP port where RaftMessageController listens (e.g.: 8301)
 */
public record MiniDSEndpoint(String id, String host, int port) implements RaftEndpoint, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Comparable<String> getId() {
        return id;
    }
}
