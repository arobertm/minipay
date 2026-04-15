package ro.minipay.gateway.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes payment events to Kafka topic "payment-events".
 *
 * Fail-safe: if Kafka is unavailable, the error is logged but the payment
 * flow is NOT blocked. Audit is asynchronous and non-critical to the
 * payment authorization path.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentAuditEvent> kafkaTemplate;

    public void publish(PaymentAuditEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.txnId(), event);
            log.debug("Published payment event: txnId={} status={}", event.txnId(), event.status());
        } catch (Exception e) {
            // Non-critical — audit must not block payments
            log.warn("Failed to publish audit event for txnId={}: {}", event.txnId(), e.getMessage());
        }
    }
}
