package ro.minipay.settlement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.minipay.settlement.model.SettlementBatch;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {

    List<SettlementBatch> findBySettlementDate(LocalDate date);

    List<SettlementBatch> findByMerchantId(String merchantId);

    Optional<SettlementBatch> findByMerchantIdAndSettlementDateAndCurrency(
            String merchantId, LocalDate date, String currency);
}
