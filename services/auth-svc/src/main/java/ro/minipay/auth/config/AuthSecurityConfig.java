package ro.minipay.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import ro.minipay.auth.jwt.DilithiumJwtDecoder;
import ro.minipay.auth.jwt.DilithiumJwtEncoder;
import ro.minipay.auth.repository.MiniDSAuthorizationConsentRepository;
import ro.minipay.auth.repository.MiniDSRegisteredClientRepository;

/**
 * Spring Authorization Server security configuration.
 *
 * Configures OAuth2/OIDC endpoints and integrates custom Dilithium3 JWT encoding/decoding
 * with MiniDS-backed repositories for clients, tokens, and consents.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final DilithiumJwtEncoder dilithiumJwtEncoder;
    private final DilithiumJwtDecoder dilithiumJwtDecoder;
    private final MiniDSRegisteredClientRepository registeredClientRepository;
    private final MiniDSAuthorizationConsentRepository authorizationConsentRepository;

    /**
     * Authorization server security filter chain.
     * Configures OAuth2 endpoints (/oauth2/token, /oauth2/authorize, etc.) and JWT handling.
     */
    @Bean
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/login")
                )
            )
            .oauth2ResourceServer(resourceServer ->
                resourceServer.jwt(jwt ->
                    jwt.decoder(dilithiumJwtDecoder)
                )
            );

        return http.build();
    }

    /**
     * Default security filter chain for other requests.
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/actuator/**", "/health", "/oauth2/jwks", "/.well-known/**").permitAll()
                    .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Authorization server settings.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://auth-svc:8081")
            .authorizationEndpoint("/oauth2/authorize")
            .tokenEndpoint("/oauth2/token")
            .jwkSetEndpoint("/oauth2/jwks")
            .tokenRevocationEndpoint("/oauth2/revoke")
            .tokenIntrospectionEndpoint("/oauth2/introspect")
            .oidcUserInfoEndpoint("/oauth2/userinfo")
            .oidcLogoutEndpoint("/oauth2/logout")
            .build();
    }

    /**
     * Provide registered client repository bean.
     */
    @Bean
    public org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository registeredClientRepository() {
        return registeredClientRepository;
    }

    /**
     * Provide authorization repository bean.
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            ro.minipay.auth.repository.MiniDSOAuth2AuthorizationRepository repo) {
        return repo;
    }

    /**
     * Provide authorization consent repository bean.
     */
    @Bean
    public org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService authorizationConsentService() {
        return new org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService() {
            @Override
            public void save(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent authorizationConsent) {
                authorizationConsentRepository.save(authorizationConsent);
            }

            @Override
            public void remove(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent authorizationConsent) {
                authorizationConsentRepository.remove(authorizationConsent);
            }

            @Override
            public org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
                return authorizationConsentRepository.findById(registeredClientId, principalName);
            }
        };
    }

    /**
     * Provide JWT encoder bean (Dilithium3).
     */
    @Bean
    public org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder() {
        return dilithiumJwtEncoder;
    }

    /**
     * Provide JWT decoder bean (Dilithium3).
     */
    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        return dilithiumJwtDecoder;
    }
}
