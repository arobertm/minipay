package ro.minipay.notif.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.minipay.notif.service.NotificationService;

/**
 * Kafka consumer for payment events published by gateway-svc.
 *
 * Topic: payment-events
 * Group: notif-svc-group (separate offset from audit-svc-group)
 *
 * Each event triggers a notification (EMAIL / SMS / PUSH) based on payment status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "payment-events",
        groupId = "notif-svc-group"
    )
    public void consume(PaymentEvent event) {
        log.debug("Received payment event: txnId={} status={}", event.txnId(), event.status());
        try {
            notificationService.process(event);
        } catch (Exception e) {
            log.error("Failed to process notification for txnId={}: {}", event.txnId(), e.getMessage());
        }
    }
}
