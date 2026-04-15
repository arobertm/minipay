package ro.minipay.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
/**
 * Spring Authorization Server security configuration.
 *
 * Configures OAuth2/OIDC endpoints with RS256 JWT signing.
 * Identity data stored in MiniDS (directory server with Raft consensus).
 *
 * NOTE:Repository implementations (RegisteredClientRepository, OAuth2AuthorizationRepository)
 * require Spring OAuth2 Authorization Server 1.3+. Currently using simplified configuration.
 * Full implementation will be completed when repositories are available.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final JwtDecoder jwtDecoder;

    /**
     * Authorization server security filter chain.
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
                resourceServer.jwt(jwt -> jwt.decoder(jwtDecoder))
            );

        return http.build();
    }

    /**
     * Default security filter chain.
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers(
                        "/actuator/**", "/health",
                        "/oauth2/jwks", "/oauth2/jwks-rs256", "/.well-known/**",
                        "/auth/token/pqc", "/auth/token/pqc/verify",
                        "/oauth2/server-metadata-pqc"
                    ).permitAll()
                    .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/auth/token/pqc", "/auth/token/pqc/verify"
                )
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
            .jwkSetEndpoint("/oauth2/jwks-rs256")
            .tokenRevocationEndpoint("/oauth2/revoke")
            .tokenIntrospectionEndpoint("/oauth2/introspect")
            .oidcUserInfoEndpoint("/oauth2/userinfo")
            .oidcLogoutEndpoint("/oauth2/logout")
            .build();
    }
}

