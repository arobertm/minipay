package ro.minipay.auth.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;

/**
 * Minimal RegisteredClientRepository implementation for now.
 * Full MiniDS integration will be implemented later.
 */
@Slf4j
@Repository
public class MiniDSRegisteredClientRepository implements RegisteredClientRepository {

    @Override
    public void save(RegisteredClient registeredClient) {
        log.info("Saving client: {}", registeredClient.getClientId());
        // TODO: Implement MiniDS storage
    }

    @Override
    public RegisteredClient findById(String id) {
        log.debug("Finding client by ID: {}", id);
        // TODO: implement
        return null;
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        log.debug("Finding client by clientId: {}", clientId);
        // TODO: implement
        return null;
    }
}
