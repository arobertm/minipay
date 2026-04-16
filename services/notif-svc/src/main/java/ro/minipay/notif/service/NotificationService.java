package ro.minipay.notif.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.minipay.notif.kafka.PaymentEvent;
import ro.minipay.notif.model.Notification;

import java.time.Instant;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Generates and stores notifications from payment events.
 *
 * In a production system this would send real emails/SMS/push via
 * providers (SendGrid, Twilio, Firebase). For the dissertation demo
 * we simulate delivery and store in-memory (bounded queue, max 500).
 */
@Slf4j
@Service
public class NotificationService {

    private static final int MAX_NOTIFICATIONS = 500;

    // Thread-safe bounded deque — newest entries at the front
    private final Deque<Notification> store = new ConcurrentLinkedDeque<>();

    public void process(PaymentEvent event) {
        Notification notif = buildNotification(event);
        store.addFirst(notif);

        // Evict oldest when over capacity
        while (store.size() > MAX_NOTIFICATIONS) {
            store.pollLast();
        }

        log.info("[NOTIF] {} | txnId={} | {} {}{} | channel={}",
                notif.notifStatus(),
                event.txnId(),
                event.status(),
                formatAmount(event.amount(), event.currency()),
                event.fraudScore() > 0.5 ? " ⚠ fraud=" + event.fraudScore() : "",
                notif.channel());
    }

    public List<Notification> getAll() {
        return Collections.unmodifiableList(store.stream().toList());
    }

    public List<Notification> getByTxnId(String txnId) {
        return store.stream()
                .filter(n -> n.txnId().equals(txnId))
                .toList();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Notification buildNotification(PaymentEvent event) {
        String subject = buildSubject(event);
        String message = buildMessage(event);
        String channel = resolveChannel(event);

        return new Notification(
                event.txnId(),
                event.status(),
                channel,
                subject,
                message,
                "SENT",          // simulated delivery
                Instant.now()
        );
    }

    private String buildSubject(PaymentEvent event) {
        return switch (event.status()) {
            case "AUTHORIZED" -> "Payment authorized — " + formatAmount(event.amount(), event.currency());
            case "CAPTURED"   -> "Payment captured — "   + formatAmount(event.amount(), event.currency());
            case "DECLINED"   -> "Payment declined — "   + formatAmount(event.amount(), event.currency());
            case "REFUNDED"   -> "Refund processed — "   + formatAmount(event.amount(), event.currency());
            case "BLOCKED"    -> "Payment blocked (fraud risk)";
            default           -> "Payment update — " + event.status();
        };
    }

    private String buildMessage(PaymentEvent event) {
        String amount = formatAmount(event.amount(), event.currency());
        return switch (event.status()) {
            case "AUTHORIZED" -> String.format(
                    "Your payment of %s to merchant %s has been authorized. Transaction ID: %s",
                    amount, event.merchantId(), event.txnId());
            case "CAPTURED"   -> String.format(
                    "Your payment of %s has been successfully captured. Transaction ID: %s",
                    amount, event.txnId());
            case "DECLINED"   -> String.format(
                    "Your payment of %s was declined. Please check your card details or contact your bank. Transaction ID: %s",
                    amount, event.txnId());
            case "REFUNDED"   -> String.format(
                    "A refund of %s has been initiated. Funds will appear in 3-5 business days. Transaction ID: %s",
                    amount, event.txnId());
            case "BLOCKED"    -> String.format(
                    "Payment of %s was blocked due to suspected fraud (score=%.2f). Transaction ID: %s. Contact support if this is unexpected.",
                    amount, event.fraudScore(), event.txnId());
            default           -> String.format(
                    "Payment status update: %s for transaction %s (%s)",
                    event.status(), event.txnId(), amount);
        };
    }

    private String resolveChannel(PaymentEvent event) {
        // High fraud score → SMS (urgent), declined → EMAIL, rest → PUSH
        if (event.fraudScore() > 0.7) return "SMS";
        if ("DECLINED".equals(event.status()) || "BLOCKED".equals(event.status())) return "EMAIL";
        return "PUSH";
    }

    private String formatAmount(Long cents, String currency) {
        if (cents == null) return "N/A";
        return String.format("%.2f %s", cents / 100.0, currency);
    }
}
