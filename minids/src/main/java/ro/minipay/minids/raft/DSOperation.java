package ro.minipay.minids.raft;

import ro.minipay.minids.model.Entry;

import java.io.Serializable;
import java.util.Map;

/**
 * Operation sent through Raft to the StateMachine.
 * Must be Serializable — MicroRaft stores it in the log.
 */
public record DSOperation(
        Type type,
        String dn,
        Entry entry,
        Map<String, String> filter,
        int limit
) implements Serializable {

    public enum Type { PUT, DELETE, GET, SEARCH }

    public static DSOperation put(String dn, Entry entry) {
        return new DSOperation(Type.PUT, dn, entry, null, 0);
    }

    public static DSOperation delete(String dn) {
        return new DSOperation(Type.DELETE, dn, null, null, 0);
    }

    public static DSOperation get(String dn) {
        return new DSOperation(Type.GET, dn, null, null, 1);
    }

    public static DSOperation search(String baseDn, Map<String, String> filter, int limit) {
        return new DSOperation(Type.SEARCH, baseDn, null, filter, limit);
    }
}
