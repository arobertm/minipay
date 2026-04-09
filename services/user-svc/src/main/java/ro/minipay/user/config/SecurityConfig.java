package ro.minipay.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for user-svc.
 *
 * - POST /users (registration) is public
 * - All other endpoints require a valid RS256 JWT issued by auth-svc
 * - DELETE /users/** additionally requires ADMIN role (checked via @PreAuthorize)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );

        return http.build();
    }

    /**
     * Argon2id password encoder.
     *
     * Parameters per OWASP recommendation (2023):
     *   memory=19456 KB, iterations=2, parallelism=1
     * Produces hashes resistant to GPU/ASIC brute-force attacks.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        // saltLength=16, hashLength=32, parallelism=1, memory=19456 KB, iterations=2
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }
}
