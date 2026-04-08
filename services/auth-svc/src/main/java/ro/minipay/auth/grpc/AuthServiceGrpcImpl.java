package ro.minipay.auth.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.springboot.grpc.server.service.GrpcService;
import ro.minipay.auth.jwt.DilithiumJwtDecoder;
import ro.minipay.auth.shared.minids.MiniDSClientService;

import java.util.Arrays;
import java.util.Map;

/**
 * gRPC AuthService implementation.
 *
 * Provides token validation (Introspect) and revocation (Revoke) for inter-service communication.
 * Other services call this via gRPC to validate access tokens and refresh tokens.
 *
 * Generated from: proto/auth.proto
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthServiceGrpcImpl extends ro.minipay.auth.grpc.AuthServiceGrpc.AuthServiceImplBase {

    private final DilithiumJwtDecoder jwtDecoder;
    private final MiniDSClientService minidsClient;

    /**
     * Introspect a token to check if it's valid and get its claims.
     *
     * @param request contains the token to validate
     * @param responseObserver completes with the token info or error
     */
    @Override
    public void introspect(ro.minipay.auth.grpc.IntrospectRequest request,
                          StreamObserver<ro.minipay.auth.grpc.IntrospectResponse> responseObserver) {
        String token = request.getToken();
        if (token == null || token.isEmpty()) {
            responseObserver.onError(
                new io.grpc.StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Token must not be empty"))
            );
            return;
        }

        try {
            // Decode JWT
            org.springframework.security.oauth2.jwt.Jwt jwt = jwtDecoder.decode(token);

            // Extract claims
            Map<String, Object> claims = jwt.getClaims();
            boolean active = !jwt.getExpiresAt().isBefore(java.time.Instant.now());
            String subject = jwt.getSubject();
            String clientId = (String) claims.get("client_id");
            Object scopeObj = claims.get("scope");
            long expirationTime = jwt.getExpiresAt().getEpochSecond();
            String jti = (String) claims.get("jti");

            // Parse scopes
            java.util.List<String> scopeList = new java.util.ArrayList<>();
            if (scopeObj instanceof String) {
                scopeList = Arrays.asList(((String) scopeObj).split(" "));
            } else if (scopeObj instanceof java.util.List) {
                scopeList = (java.util.List<String>) scopeObj;
            }

            IntrospectResponse response = IntrospectResponse.newBuilder()
                .setActive(active)
                .setSubject(subject != null ? subject : "")
                .setClientId(clientId != null ? clientId : "")
                .addAllScope(scopeList)
                .setExp(expirationTime)
                .setJti(jti != null ? jti : "")
                .build();

            log.debug("Introspect request for token: {} (active={})", jti, active);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (org.springframework.security.oauth2.jwt.JwtException e) {
            log.warn("Invalid token in Introspect request");
            IntrospectResponse response = IntrospectResponse.newBuilder()
                .setActive(false)
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Introspect error", e);
            responseObserver.onError(
                new io.grpc.StatusRuntimeException(io.grpc.Status.INTERNAL
                    .withDescription("Introspect failed"))
            );
        }
    }

    /**
     * Revoke a token (invalidate it).
     *
     * @param request contains the token to revoke and optional token type hint
     * @param responseObserver completes with success status
     */
    @Override
    public void revoke(ro.minipay.auth.grpc.RevokeRequest request,
                      StreamObserver<ro.minipay.auth.grpc.RevokeResponse> responseObserver) {
        String token = request.getToken();
        String tokenTypeHint = request.getTokenTypeHint();

        if (token == null || token.isEmpty()) {
            responseObserver.onError(
                new io.grpc.StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Token must not be empty"))
            );
            return;
        }

        try {
            // Decode JWT to get JTI
            org.springframework.security.oauth2.jwt.Jwt jwt = jwtDecoder.decode(token);
            String jti = (String) jwt.getClaims().get("jti");

            if (jti == null) {
                throw new RuntimeException("Token missing JTI claim");
            }

            // Delete from MiniDS
            String dn = "coreTokenId=" + jti + ",ou=tokens,dc=minipay,dc=ro";
            minidsClient.deleteEntry(dn);

            log.info("Token revoked: {}", jti);

            RevokeResponse response = RevokeResponse.newBuilder()
                .setSuccess(true)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Revoke error", e);
            RevokeResponse response = RevokeResponse.newBuilder()
                .setSuccess(false)
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}

// Proto-generated classes (placeholders - generated by protoc)
// In real project, these are generated by protoc-jar-maven-plugin in build phase

class AuthServiceGrpc {
    public static abstract class AuthServiceImplBase implements io.grpc.BindableService {
        public void introspect(IntrospectRequest request,
                              io.grpc.stub.StreamObserver<IntrospectResponse> responseObserver) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        public void revoke(RevokeRequest request,
                          io.grpc.stub.StreamObserver<RevokeResponse> responseObserver) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        @Override
        public io.grpc.ServerServiceDefinition bindService() {
            return null; // Implementation details omitted
        }
    }
}

class IntrospectRequest {
    private String token;

    public String getToken() { return token; }
    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
        private String token;
        public Builder setToken(String token) { this.token = token; return this; }
        public IntrospectRequest build() { IntrospectRequest r = new IntrospectRequest(); r.token = token; return r; }
    }
}

class IntrospectResponse {
    private boolean active;
    private String subject;
    private String clientId;
    private java.util.List<String> scopes = new java.util.ArrayList<>();
    private long exp;
    private String jti;

    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
        private boolean active;
        private String subject;
        private String clientId;
        private java.util.List<String> scopes = new java.util.ArrayList<>();
        private long exp;
        private String jti;

        public Builder setActive(boolean active) { this.active = active; return this; }
        public Builder setSubject(String subject) { this.subject = subject; return this; }
        public Builder setClientId(String clientId) { this.clientId = clientId; return this; }
        public Builder addAllScope(java.util.List<String> scopes) { this.scopes.addAll(scopes); return this; }
        public Builder setExp(long exp) { this.exp = exp; return this; }
        public Builder setJti(String jti) { this.jti = jti; return this; }

        public IntrospectResponse build() {
            IntrospectResponse r = new IntrospectResponse();
            r.active = active; r.subject = subject; r.clientId = clientId;
            r.scopes = scopes; r.exp = exp; r.jti = jti;
            return r;
        }
    }
}

class RevokeRequest {
    private String token;
    private String tokenTypeHint;

    public String getToken() { return token; }
    public String getTokenTypeHint() { return tokenTypeHint; }
    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
        private String token;
        private String tokenTypeHint;
        public Builder setToken(String token) { this.token = token; return this; }
        public Builder setTokenTypeHint(String tokenTypeHint) { this.tokenTypeHint = tokenTypeHint; return this; }
        public RevokeRequest build() { RevokeRequest r = new RevokeRequest(); r.token = token; r.tokenTypeHint = tokenTypeHint; return r; }
    }
}

class RevokeResponse {
    private boolean success;

    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
        private boolean success;
        public Builder setSuccess(boolean success) { this.success = success; return this; }
        public RevokeResponse build() { RevokeResponse r = new RevokeResponse(); r.success = success; return r; }
    }
}
