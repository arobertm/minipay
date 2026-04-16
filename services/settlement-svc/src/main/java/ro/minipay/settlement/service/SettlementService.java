package ro.minipay.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.minipay.settlement.kafka.PaymentEvent;
import ro.minipay.settlement.model.SettlementBatch;
import ro.minipay.settlement.model.SettlementRecord;
import ro.minipay.settlement.repository.SettlementBatchRepository;
import ro.minipay.settlement.repository.SettlementRecordRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRecordRepository recordRepo;
    private final SettlementBatchRepository  batchRepo;

    /**
     * Persists a CAPTURED or REFUNDED event as a settlement record.
     * Idempotent — duplicate txnId is silently skipped.
     */
    @Transactional
    public void ingest(PaymentEvent event) {
        if (!"CAPTURED".equals(event.status()) && !"REFUNDED".equals(event.status())) {
            return;
        }
        if (recordRepo.existsByTxnId(event.txnId())) {
            log.debug("[SETTLEMENT] duplicate txnId={}, skipping", event.txnId());
            return;
        }

        LocalDate date = Instant.now().atOffset(ZoneOffset.UTC).toLocalDate();
        SettlementRecord record = SettlementRecord.builder()
                .txnId(event.txnId())
                .merchantId(event.merchantId())
                .amount(event.amount())
                .currency(event.currency())
                .paymentStatus(event.status())
                .settlementDate(date)
                .createdAt(Instant.now())
                .build();

        recordRepo.save(record);
        log.info("[SETTLEMENT] ingested txnId={} merchant={} {} {} {}",
                event.txnId(), event.merchantId(), event.status(),
                event.amount(), event.currency());
    }

    /**
     * Daily reconciliation — runs at 01:00 UTC.
     * Aggregates yesterday's records into settlement batches.
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")
    @Transactional
    public void reconcile() {
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        reconcileDate(yesterday);
    }

    /**
     * Reconcile a specific date (also callable from REST for on-demand runs).
     */
    @Transactional
    public List<SettlementBatch> reconcileDate(LocalDate date) {
        List<SettlementRecord> records = recordRepo.findBySettlementDate(date);
        if (records.isEmpty()) {
            log.info("[SETTLEMENT] reconcile {}: no records", date);
            return List.of();
        }

        // Group by merchantId + currency
        Map<String, Map<String, List<SettlementRecord>>> grouped = records.stream()
                .collect(Collectors.groupingBy(SettlementRecord::getMerchantId,
                         Collectors.groupingBy(SettlementRecord::getCurrency)));

        List<SettlementBatch> batches = grouped.entrySet().stream()
                .flatMap(mEntry -> mEntry.getValue().entrySet().stream()
                        .map(cEntry -> buildOrUpdateBatch(
                                mEntry.getKey(), date, cEntry.getKey(), cEntry.getValue())))
                .toList();

        log.info("[SETTLEMENT] reconcile {}: {} batches for {} merchants",
                date, batches.size(), grouped.size());
        return batches;
    }

    public List<SettlementBatch> getBatchesByDate(LocalDate date) {
        return batchRepo.findBySettlementDate(date);
    }

    public List<SettlementBatch> getBatchesByMerchant(String merchantId) {
        return batchRepo.findByMerchantId(merchantId);
    }

    public List<SettlementRecord> getRecordsByDate(LocalDate date) {
        return recordRepo.findBySettlementDate(date);
    }

    public List<SettlementRecord> getAllRecords() {
        return recordRepo.findAll();
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private SettlementBatch buildOrUpdateBatch(String merchantId, LocalDate date,
                                               String currency, List<SettlementRecord> recs) {
        long captured = recs.stream()
                .filter(r -> "CAPTURED".equals(r.getPaymentStatus()))
                .mapToLong(SettlementRecord::getAmount).sum();
        long refunded = recs.stream()
                .filter(r -> "REFUNDED".equals(r.getPaymentStatus()))
                .mapToLong(SettlementRecord::getAmount).sum();

        SettlementBatch batch = batchRepo
                .findByMerchantIdAndSettlementDateAndCurrency(merchantId, date, currency)
                .orElse(SettlementBatch.builder()
                        .merchantId(merchantId)
                        .settlementDate(date)
                        .currency(currency)
                        .build());

        batch.setCapturedAmount(captured);
        batch.setRefundedAmount(refunded);
        batch.setNetAmount(captured - refunded);
        batch.setTxnCount(recs.size());
        batch.setStatus("SETTLED");
        batch.setReconciledAt(Instant.now());

        return batchRepo.save(batch);
    }
}
