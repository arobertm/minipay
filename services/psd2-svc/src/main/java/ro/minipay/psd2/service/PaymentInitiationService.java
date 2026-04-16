package ro.minipay.psd2.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.minipay.psd2.model.PaymentInitiation;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PSD2 Payment Initiation Service (PIS).
 * Simulates SEPA Credit Transfer flow: RCVD → ACTC → ACSC.
 */
@Slf4j
@Service
public class PaymentInitiationService {

    private final Map<String, PaymentInitiation> store = new ConcurrentHashMap<>();

    public PaymentInitiation initiate(String debtorIban, String creditorIban,
                                      String creditorName, Long amount,
                                      String currency, String remittanceInfo) {
        String paymentId = UUID.randomUUID().toString();
        PaymentInitiation payment = new PaymentInitiation(
                paymentId, debtorIban, creditorIban, creditorName,
                amount, currency, remittanceInfo,
                "RCVD",   // Received — awaiting authorisation
                Instant.now(), Instant.now());
        store.put(paymentId, payment);
        log.info("[PSD2-PIS] payment initiated id={} {} {} {} {}",
                paymentId, debtorIban, creditorIban, amount, currency);
        return payment;
    }

    public Optional<PaymentInitiation> get(String paymentId) {
        return Optional.ofNullable(store.get(paymentId));
    }

    /**
     * Authorise a payment — transitions RCVD → ACTC → ACSC (simulated instant settlement).
     */
    public Optional<PaymentInitiation> authorise(String paymentId) {
        PaymentInitiation p = store.get(paymentId);
        if (p == null) return Optional.empty();
        if (!"RCVD".equals(p.status())) return Optional.of(p);

        // Simulate: RCVD → ACTC (accepted, pending settlement)
        PaymentInitiation actc = withStatus(p, "ACTC");
        store.put(paymentId, actc);

        // Simulate instant settlement: ACTC → ACSC
        PaymentInitiation acsc = withStatus(actc, "ACSC");
        store.put(paymentId, acsc);

        log.info("[PSD2-PIS] payment authorised and settled id={}", paymentId);
        return Optional.of(acsc);
    }

    /**
     * Cancel / reject a payment.
     */
    public Optional<PaymentInitiation> cancel(String paymentId) {
        PaymentInitiation p = store.get(paymentId);
        if (p == null) return Optional.empty();
        PaymentInitiation rejected = withStatus(p, "RJCT");
        store.put(paymentId, rejected);
        log.info("[PSD2-PIS] payment cancelled id={}", paymentId);
        return Optional.of(rejected);
    }

    private PaymentInitiation withStatus(PaymentInitiation p, String status) {
        return new PaymentInitiation(p.paymentId(), p.debtorIban(), p.creditorIban(),
                p.creditorName(), p.amount(), p.currency(), p.remittanceInfo(),
                status, p.createdAt(), Instant.now());
    }
}
