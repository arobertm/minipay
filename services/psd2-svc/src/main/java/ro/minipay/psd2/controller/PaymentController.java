package ro.minipay.psd2.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.psd2.model.PaymentInitiation;
import ro.minipay.psd2.service.PaymentInitiationService;

import java.util.Map;

/**
 * PSD2 PIS — Payment Initiation Service.
 *
 * POST /psd2/payments/sepa-credit-transfers                          — initiate payment
 * GET  /psd2/payments/sepa-credit-transfers/{paymentId}              — get status
 * POST /psd2/payments/sepa-credit-transfers/{paymentId}/authorise    — authorise
 * POST /psd2/payments/sepa-credit-transfers/{paymentId}/cancel       — cancel
 */
@RestController
@RequestMapping("/psd2/payments/sepa-credit-transfers")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentInitiationService paymentService;

    @PostMapping
    public ResponseEntity<PaymentInitiation> initiate(@RequestBody Map<String, Object> body) {
        String debtorIban    = (String) body.get("debtorIban");
        String creditorIban  = (String) body.get("creditorIban");
        String creditorName  = (String) body.get("creditorName");
        Number amountNum     = (Number) body.getOrDefault("amount", 0);
        Long amount          = amountNum.longValue();
        String currency      = (String) body.getOrDefault("currency", "RON");
        String remittanceInfo = (String) body.getOrDefault("remittanceInfo", "");

        PaymentInitiation payment = paymentService.initiate(
                debtorIban, creditorIban, creditorName, amount, currency, remittanceInfo);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentInitiation> get(@PathVariable String paymentId) {
        return paymentService.get(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{paymentId}/authorise")
    public ResponseEntity<?> authorise(@PathVariable String paymentId) {
        return paymentService.authorise(paymentId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String paymentId) {
        return paymentService.cancel(paymentId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
