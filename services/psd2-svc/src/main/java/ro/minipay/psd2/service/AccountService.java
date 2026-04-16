package ro.minipay.psd2.service;

import org.springframework.stereotype.Service;
import ro.minipay.psd2.model.AccountBalance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Simulated account data for AIS (dissertation demo — no real bank connection).
 * Returns deterministic fake accounts based on accountId seed.
 */
@Service
public class AccountService {

    // Simulated account registry keyed by accountId
    private static final Map<String, AccountBalance> ACCOUNTS = Map.of(
        "ACC-001", new AccountBalance("ACC-001", "RO49AAAA1B31007593840000", "RON", 12450.50, 11200.00, "closingBooked"),
        "ACC-002", new AccountBalance("ACC-002", "RO49AAAA1B31007593840001", "EUR",  3200.00,  3200.00, "closingBooked"),
        "ACC-003", new AccountBalance("ACC-003", "RO49AAAA1B31007593840002", "RON",   875.25,   875.25, "closingBooked"),
        "ACC-004", new AccountBalance("ACC-004", "RO49AAAA1B31007593840003", "USD",  5000.00,  4750.00, "closingBooked"),
        "ACC-005", new AccountBalance("ACC-005", "RO49AAAA1B31007593840004", "EUR", 15000.00, 14500.00, "closingBooked")
    );

    public List<AccountBalance> getAll() {
        return List.copyOf(ACCOUNTS.values());
    }

    public Optional<AccountBalance> getById(String accountId) {
        return Optional.ofNullable(ACCOUNTS.get(accountId));
    }
}
