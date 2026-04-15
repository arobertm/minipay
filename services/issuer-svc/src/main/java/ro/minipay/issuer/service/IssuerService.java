package ro.minipay.issuer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.minipay.issuer.dto.AuthorizeRequest;
import ro.minipay.issuer.dto.AuthorizeResponse;
import ro.minipay.issuer.model.CardAccount;
import ro.minipay.issuer.model.CardStatus;
import ro.minipay.issuer.repository.CardAccountRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Core issuer bank authorization logic.
 *
 * Authorization checks (in order):
 *   1. Card exists in our database
 *   2. Card is ACTIVE (not BLOCKED or EXPIRED status)
 *   3. Card expiry date has not passed
 *   4. Sufficient balance
 *   5. Daily spending limit not exceeded
 *
 * On approval: deduct balance + update daily spent tracker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssuerService {

    private static final DateTimeFormatter EXPIRY_FMT = DateTimeFormatter.ofPattern("MM/yy");

    private final CardAccountRepository cardRepository;

    @Transactional
    public AuthorizeResponse authorize(AuthorizeRequest request) {
        String pan = request.pan().replaceAll("\\s+", "");
        log.info("Issuer authorize: txnId={} pan={}****{} amount={} {}",
            request.txnId(),
            pan.substring(0, Math.min(6, pan.length())),
            pan.length() >= 4 ? pan.substring(pan.length() - 4) : "****",
            request.amount(), request.currency());

        // 1 — Card lookup
        CardAccount card = cardRepository.findByPan(pan).orElse(null);
        if (card == null) {
            log.warn("Card not found: pan={}****", pan.substring(0, Math.min(6, pan.length())));
            return new AuthorizeResponse("14", "", "");
        }

        // 2 — Card status check
        if (card.getStatus() == CardStatus.BLOCKED) {
            log.warn("Card BLOCKED: txnId={}", request.txnId());
            return new AuthorizeResponse("05", "", card.getHolderName());
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            log.warn("Card EXPIRED (status): txnId={}", request.txnId());
            return new AuthorizeResponse("54", "", card.getHolderName());
        }

        // 3 — Expiry date check
        if (request.expiryDate() != null && !request.expiryDate().isBlank()) {
            try {
                YearMonth expiry = YearMonth.parse(request.expiryDate(), EXPIRY_FMT);
                // Card is valid through the last day of the expiry month
                if (expiry.isBefore(YearMonth.now())) {
                    log.warn("Card expired by date: txnId={} expiry={}", request.txnId(), request.expiryDate());
                    return new AuthorizeResponse("54", "", card.getHolderName());
                }
            } catch (Exception e) {
                log.debug("Could not parse expiry date '{}', skipping check", request.expiryDate());
            }
        }

        // Reset daily spent if it's a new day
        LocalDate today = LocalDate.now();
        if (!today.equals(card.getLastSpentDate())) {
            card.setDailySpentInCents(0L);
            card.setLastSpentDate(today);
        }

        // 4 — Balance check
        if (card.getBalanceInCents() < request.amount()) {
            log.warn("Insufficient funds: txnId={} balance={} amount={}",
                request.txnId(), card.getBalanceInCents(), request.amount());
            return new AuthorizeResponse("51", "", card.getHolderName());
        }

        // 5 — Daily limit check
        long newDailySpent = card.getDailySpentInCents() + request.amount();
        if (newDailySpent > card.getDailyLimitInCents()) {
            log.warn("Daily limit exceeded: txnId={} dailySpent={} limit={} amount={}",
                request.txnId(), card.getDailySpentInCents(),
                card.getDailyLimitInCents(), request.amount());
            return new AuthorizeResponse("65", "", card.getHolderName());
        }

        // Approved — deduct balance and update daily tracker
        card.setBalanceInCents(card.getBalanceInCents() - request.amount());
        card.setDailySpentInCents(newDailySpent);
        card.setLastSpentDate(today);
        cardRepository.save(card);

        String authCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        log.info("Payment APPROVED: txnId={} authCode={} holder={}",
            request.txnId(), authCode, card.getHolderName());
        return new AuthorizeResponse("00", authCode, card.getHolderName());
    }
}
