package ro.minipay.minids.raft;

import io.microraft.RaftEndpoint;

/**
 * Reprezinta un nod din clusterul MiniDS.
 * Implementeaza RaftEndpoint — identitatea unui nod in MicroRaft.
 *
 * @param id    identificatorul unic al nodului (ex: "minids-0")
 * @param host  adresa de retea (ex: "minids-0" sau "localhost")
 * @param port  portul Raft (ex: 8300)
 */
public record MiniDSEndpoint(String id, String host, int port) implements RaftEndpoint {

    @Override
    public Comparable<String> getId() {
        return id;
    }
}
