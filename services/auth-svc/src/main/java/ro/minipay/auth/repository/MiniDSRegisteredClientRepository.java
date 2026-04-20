package ro.minipay.auth.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RegisteredClientRepository backed by in-memory store (demo clients pre-seeded).
 * Full MiniDS integration can replace the map lookup in the future.
 */
@Slf4j
@Repository
public class MiniDSRegisteredClientRepository implements RegisteredClientRepository {

    private final Map<String, RegisteredClient> byId = new ConcurrentHashMap<>();
    private final Map<String, RegisteredClient> byClientId = new ConcurrentHashMap<>();

    public MiniDSRegisteredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient demoClient = RegisteredClient.withId("demo-client-id")
                .clientId("demo-client")
                .clientSecret(passwordEncoder.encode("demo-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("payments:write")
                .scope("payments:read")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .build();

        RegisteredClient dashboardClient = RegisteredClient.withId("minipay-dashboard-id")
                .clientId("minipay-dashboard")
                .clientSecret(passwordEncoder.encode("minipay-dashboard-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("openid")
                .scope("payments:read")
                .scope("payments:write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(8))
                        .build())
                .build();

        register(demoClient);
        register(dashboardClient);
        log.info("Registered OAuth2 clients: demo-client, minipay-dashboard");
    }

    private void register(RegisteredClient client) {
        byId.put(client.getId(), client);
        byClientId.put(client.getClientId(), client);
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        register(registeredClient);
        log.info("Saved client: {}", registeredClient.getClientId());
    }

    @Override
    public RegisteredClient findById(String id) {
        return byId.get(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return byClientId.get(clientId);
    }
}
