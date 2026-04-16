package ro.minipay.settlement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.minipay.settlement.model.SettlementRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, Long> {

    Optional<SettlementRecord> findByTxnId(String txnId);

    List<SettlementRecord> findByMerchantId(String merchantId);

    List<SettlementRecord> findBySettlementDate(LocalDate date);

    List<SettlementRecord> findByMerchantIdAndSettlementDate(String merchantId, LocalDate date);

    boolean existsByTxnId(String txnId);
}
