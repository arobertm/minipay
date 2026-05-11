package ro.minipay.issuer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ro.minipay.issuer.model.CardAccount;
import ro.minipay.issuer.model.CardStatus;
import ro.minipay.issuer.repository.CardAccountRepository;

import java.util.List;

/**
 * Seeds test card accounts at startup.
 *
 * Test cards (standard industry test PANs):
 *   4111 1111 1111 1111 — VISA, always approved, balance 5000.00 RON
 *   4000 0000 0000 0002 — VISA, always declined (BLOCKED status)
 *   4000 0000 0000 9995 — VISA, insufficient funds (balance 0)
 *   5500 0000 0000 0004 — Mastercard, always approved, balance 5000.00 RON
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final CardAccountRepository cardRepository;

    record Seed(String pan, String holderName, CardStatus status, long balanceInCents) {}

    private static final List<Seed> SEEDS = List.of(
        new Seed("4111111111111111", "Ion Popescu",      CardStatus.ACTIVE,   500000L),
        new Seed("4000000000000002", "Maria Ionescu",    CardStatus.BLOCKED,  500000L),
        new Seed("4000000000009995", "Andrei Gheorghe",  CardStatus.ACTIVE,        0L),
        new Seed("5500000000000004", "Elena Constantin", CardStatus.ACTIVE,   500000L)
    );

    @Override
    public void run(ApplicationArguments args) {
        int upserted = 0;
        for (Seed s : SEEDS) {
            CardAccount card = cardRepository.findByPan(s.pan()).orElseGet(() ->
                CardAccount.builder()
                    .pan(s.pan())
                    .holderName(s.holderName())
                    .expiryDate("12/28")
                    .status(s.status())
                    .dailyLimitInCents(1000000L)
                    .build()
            );
            card.setStatus(s.status());
            card.setBalanceInCents(s.balanceInCents());
            card.setDailySpentInCents(0L);
            card.setLastSpentDate(null);
            cardRepository.save(card);
            upserted++;
        }
        log.info("Test card accounts reset/seeded: {} cards.", upserted);
    }
}
