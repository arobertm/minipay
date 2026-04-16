package ro.minipay.settlement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Persists every CAPTURED or REFUNDED payment event for settlement.
 */
@Entity
@Table(name = "settlement_records",
       indexes = {
           @Index(name = "idx_sr_merchant_date", columnList = "merchant_id, settlement_date"),
           @Index(name = "idx_sr_txn", columnList = "txn_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_id", nullable = false, unique = true, length = 64)
    private String txnId;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    /** Amount in minor units (cents). */
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    /** CAPTURED, REFUNDED */
    @Column(nullable = false, length = 16)
    private String paymentStatus;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Column(nullable = false)
    private Instant createdAt;
}
