package ro.minipay.settlement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Daily aggregate settlement per merchant.
 * Created/updated by the scheduled reconciliation job.
 */
@Entity
@Table(name = "settlement_batches",
       uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_id", "settlement_date", "currency"}),
       indexes = @Index(name = "idx_sb_date", columnList = "settlement_date"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(nullable = false)
    private LocalDate settlementDate;

    /** Net amount in minor units (captures minus refunds). */
    @Column(nullable = false)
    private Long netAmount;

    @Column(nullable = false)
    private Long capturedAmount;

    @Column(nullable = false)
    private Long refundedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer txnCount;

    /** PENDING → SETTLED */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private Instant reconciledAt;
}
