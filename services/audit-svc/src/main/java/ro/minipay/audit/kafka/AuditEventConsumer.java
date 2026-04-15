package ro.minipay.audit.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.minipay.audit.service.AuditService;

/**
 * Kafka consumer for payment events published by gateway-svc.
 *
 * Topic: payment-events
 * Group: audit-svc-group
 *
 * Each event is appended to the hash chain in AuditService.
 * Processing is idempotent — duplicate events (same txnId) are skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(
        topics = "payment-events",
        groupId = "audit-svc-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(PaymentAuditEvent event) {
        log.info("Received payment event: txnId={} status={}", event.txnId(), event.status());
        try {
            auditService.append(event);
        } catch (Exception e) {
            log.error("Failed to append audit entry for txnId={}: {}", event.txnId(), e.getMessage());
        }
    }
}
