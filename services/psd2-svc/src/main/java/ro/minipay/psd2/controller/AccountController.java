package ro.minipay.psd2.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.psd2.model.AccountBalance;
import ro.minipay.psd2.service.AccountService;
import ro.minipay.psd2.service.ConsentService;

import java.util.List;
import java.util.Map;

/**
 * PSD2 AIS — Account information endpoints.
 * All require a valid consent-id header with the appropriate permission.
 *
 * GET /psd2/accounts                          — list all accounts
 * GET /psd2/accounts/{accountId}/balances     — account balance
 * GET /psd2/accounts/{accountId}/transactions — simulated transaction list
 */
@RestController
@RequestMapping("/psd2/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService   accountService;
    private final ConsentService   consentService;

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "Consent-ID", required = false) String consentId) {
        if (!consentService.isAuthorized(consentId, "ReadAccountList")) {
            return ResponseEntity.status(401).body(Map.of("error", "CONSENT_INVALID",
                    "message", "Valid consent with ReadAccountList permission required"));
        }
        List<AccountBalance> accounts = accountService.getAll();
        return ResponseEntity.ok(Map.of("accounts", accounts));
    }

    @GetMapping("/{accountId}/balances")
    public ResponseEntity<?> balances(
            @PathVariable String accountId,
            @RequestHeader(value = "Consent-ID", required = false) String consentId) {
        if (!consentService.isAuthorized(consentId, "ReadBalances")) {
            return ResponseEntity.status(401).body(Map.of("error", "CONSENT_INVALID",
                    "message", "Valid consent with ReadBalances permission required"));
        }
        return accountService.getById(accountId)
                .<ResponseEntity<?>>map(bal -> ResponseEntity.ok(Map.of("balances", List.of(bal))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<?> transactions(
            @PathVariable String accountId,
            @RequestHeader(value = "Consent-ID", required = false) String consentId) {
        if (!consentService.isAuthorized(consentId, "ReadTransactions")) {
            return ResponseEntity.status(401).body(Map.of("error", "CONSENT_INVALID",
                    "message", "Valid consent with ReadTransactions permission required"));
        }
        // Simulated transaction list — in a real system this would query the core banking ledger
        List<Map<String, Object>> transactions = List.of(
            Map.of("txnId", "TXN-001", "amount", -150.00, "currency", "RON",
                   "creditorName", "Mega Image SRL", "bookingDate", "2024-01-15",
                   "remittanceInfo", "groceries"),
            Map.of("txnId", "TXN-002", "amount", 5000.00, "currency", "RON",
                   "debtorName", "Employer SA", "bookingDate", "2024-01-10",
                   "remittanceInfo", "salary January"),
            Map.of("txnId", "TXN-003", "amount", -89.99, "currency", "RON",
                   "creditorName", "Netflix Romania", "bookingDate", "2024-01-05",
                   "remittanceInfo", "subscription")
        );
        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "transactions", Map.of("booked", transactions)
        ));
    }
}
