package ro.minipay.minids.raft;

import io.microraft.RaftEndpoint;
import io.microraft.model.message.RaftMessage;
import io.microraft.transport.Transport;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP transport for communication between Raft nodes.
 *
 * Uses Java serialization (octet-stream) instead of JSON
 * because RaftMessage is an abstract type (interface) and
 * Jackson cannot deserialize polymorphically without additional configuration.
 */
@Slf4j
public class MiniDSTransport implements Transport {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MiniDSTransport(String localNodeId, int localPort) {
        // parameters kept for compatibility with RaftConfig
    }

    @Override
    public void send(RaftEndpoint target, RaftMessage message) {
        if (!(target instanceof MiniDSEndpoint endpoint)) return;

        executor.submit(() -> {
            try {
                URI uri = URI.create("http://" + endpoint.host() + ":" + endpoint.port() + "/raft/message");

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                    oos.writeObject(message);
                }
                byte[] body = baos.toByteArray();

                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/octet-stream");
                conn.setDoOutput(true);
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                }

                int status = conn.getResponseCode();
                if (status != 200) {
                    log.warn("Raft message to {} returned status: {}", endpoint.id(), status);
                }
                conn.disconnect();

            } catch (Exception e) {
                log.debug("Cannot send Raft message to {}: {}", endpoint.id(), e.getMessage());
            }
        });
    }

    @Override
    public boolean isReachable(RaftEndpoint endpoint) {
        if (!(endpoint instanceof MiniDSEndpoint e)) return false;
        try {
            URI uri = URI.create("http://" + e.host() + ":" + e.port() + "/raft/health");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            int status = conn.getResponseCode();
            conn.disconnect();
            return status == 200;
        } catch (Exception ex) {
            return false;
        }
    }
}
