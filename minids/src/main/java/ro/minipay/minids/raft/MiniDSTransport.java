package ro.minipay.minids.raft;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.microraft.RaftEndpoint;
import io.microraft.model.message.RaftMessage;
import io.microraft.transport.Transport;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Transport HTTP simplu pentru comunicarea intre noduri Raft.
 *
 * In productie: gRPC sau Netty ar fi mai eficiente.
 * Pentru disertatie: HTTP/JSON e suficient si usor de debugat.
 */
@Slf4j
public class MiniDSTransport implements Transport {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MiniDSTransport(String localNodeId, int localPort) {
        // parametri pastrati pentru compatibilitate cu RaftConfig
    }

    @Override
    public void send(RaftEndpoint target, RaftMessage message) {
        if (!(target instanceof MiniDSEndpoint endpoint)) return;

        executor.submit(() -> {
            try {
                URI uri = URI.create("http://" + endpoint.host() + ":" + endpoint.port() + "/raft/message");
                byte[] body = objectMapper.writeValueAsBytes(message);

                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                }

                int status = conn.getResponseCode();
                if (status != 200) {
                    log.warn("Raft mesaj catre {} returnat status: {}", endpoint.id(), status);
                }
                conn.disconnect();

            } catch (Exception e) {
                log.debug("Nu pot trimite mesaj Raft catre {}: {}", endpoint.id(), e.getMessage());
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
