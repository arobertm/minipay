package ro.minipay.tds.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.tds.model.AuthenticationRequest;
import ro.minipay.tds.model.AuthenticationResult;
import ro.minipay.tds.model.ChallengeSession;
import ro.minipay.tds.service.ThreeDSService;

import java.util.Map;
import java.util.Optional;

/**
 * 3DS2 ACS (Access Control Server) REST endpoints.
 *
 * POST /tds/authenticate            — initiate authentication
 * GET  /tds/challenge/{acsTransId}  — get challenge session
 * POST /tds/challenge/{acsTransId}  — submit OTP
 */
@RestController
@RequestMapping("/tds")
@RequiredArgsConstructor
public class ThreeDSController {

    private final ThreeDSService threeDSService;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResult> authenticate(
            @RequestBody AuthenticationRequest request) {
        AuthenticationResult result = threeDSService.authenticate(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/challenge/{acsTransId}")
    public ResponseEntity<?> getChallenge(@PathVariable String acsTransId) {
        Optional<ChallengeSession> session = threeDSService.getChallenge(acsTransId);
        if (session.isEmpty()) return ResponseEntity.notFound().build();

        ChallengeSession s = session.get();
        // Return masked data (never expose OTP in production!)
        return ResponseEntity.ok(Map.of(
                "acsTransID", s.acsTransID(),
                "status", s.status(),
                "expiresAt", s.expiresAt().toString(),
                // DEMO ONLY — expose OTP so the dissertation tester can complete the flow
                "otp_demo_only", s.otp()
        ));
    }

    @PostMapping("/challenge/{acsTransId}")
    public ResponseEntity<?> submitChallenge(
            @PathVariable String acsTransId,
            @RequestBody Map<String, String> body) {
        String otp = body.get("otp");
        if (otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP required"));
        }
        return threeDSService.verifyChallenge(acsTransId, otp)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
