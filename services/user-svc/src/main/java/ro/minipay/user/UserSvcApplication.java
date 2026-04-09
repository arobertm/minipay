package ro.minipay.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MiniPay User Service.
 *
 * Manages user accounts, credentials (Argon2id), and RBAC roles.
 * Identity data stored in MiniDS (ou=users,dc=minipay,dc=ro).
 * Exposes REST API for user management and gRPC for internal service calls.
 */
@SpringBootApplication
public class UserSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserSvcApplication.class, args);
    }
}
