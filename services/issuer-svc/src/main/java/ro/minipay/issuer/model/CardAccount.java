package ro.minipay.issuer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Represents a card account in the issuer bank simulator.
 *
 * Stores test cards preloaded at startup.
 * Pan is stored in plain text for demo purposes — in production it would be
 * encrypted (HSM-protected) or stored as a one-way hash for lookup.
 */
@Entity
@Table(name = "card_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full card number (PAN) — plain text for simulator */
    @Column(nullable = false, unique = true, length = 19)
    private String pan;

    /** Cardholder name */
    @Column(nullable = false)
    private String holderName;

    /** Card expiry date MM/YY format */
    @Column(nullable = false, length = 5)
    private String expiryDate;

    /** Card status: ACTIVE, BLOCKED, EXPIRED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    /** Available balance in cents (e.g. 500000 = 5000.00 RON) */
    @Column(nullable = false)
    private Long balanceInCents;

    /** Daily spending limit in cents */
    @Column(nullable = false)
    private Long dailyLimitInCents;

    /** Amount spent today in cents — resets each day */
    @Column(nullable = false)
    private Long dailySpentInCents;

    /** Date of last spending (for daily limit reset logic) */
    @Column
    private LocalDate lastSpentDate;
}
