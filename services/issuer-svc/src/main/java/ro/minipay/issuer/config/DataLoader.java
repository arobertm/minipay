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

    @Override
    public void run(ApplicationArguments args) {
        if (cardRepository.count() > 0) {
            log.info("Test cards already loaded, skipping seed.");
            return;
        }

        List<CardAccount> testCards = List.of(
            CardAccount.builder()
                .pan("4111111111111111")
                .holderName("Ion Popescu")
                .expiryDate("12/28")
                .status(CardStatus.ACTIVE)
                .balanceInCents(500000L)      // 5000.00 RON
                .dailyLimitInCents(1000000L)  // 10000.00 RON/day
                .dailySpentInCents(0L)
                .lastSpentDate(null)
                .build(),

            CardAccount.builder()
                .pan("4000000000000002")
                .holderName("Maria Ionescu")
                .expiryDate("12/28")
                .status(CardStatus.BLOCKED)   // always declined — 05 Do Not Honor
                .balanceInCents(500000L)
                .dailyLimitInCents(1000000L)
                .dailySpentInCents(0L)
                .lastSpentDate(null)
                .build(),

            CardAccount.builder()
                .pan("4000000000009995")
                .holderName("Andrei Gheorghe")
                .expiryDate("12/28")
                .status(CardStatus.ACTIVE)
                .balanceInCents(0L)           // zero balance — 51 Insufficient Funds
                .dailyLimitInCents(1000000L)
                .dailySpentInCents(0L)
                .lastSpentDate(null)
                .build(),

            CardAccount.builder()
                .pan("5500000000000004")
                .holderName("Elena Constantin")
                .expiryDate("12/28")
                .status(CardStatus.ACTIVE)
                .balanceInCents(500000L)      // 5000.00 RON
                .dailyLimitInCents(1000000L)
                .dailySpentInCents(0L)
                .lastSpentDate(null)
                .build()
        );

        cardRepository.saveAll(testCards);
        log.info("Loaded {} test card accounts.", testCards.size());
    }
}
