package ro.minipay.settlement.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.minipay.settlement.service.SettlementService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final SettlementService settlementService;

    @KafkaListener(
        topics = "payment-events",
        groupId = "settlement-svc-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(PaymentEvent event) {
        log.debug("[SETTLEMENT-CONSUMER] received txnId={} status={}", event.txnId(), event.status());
        settlementService.ingest(event);
    }
}
