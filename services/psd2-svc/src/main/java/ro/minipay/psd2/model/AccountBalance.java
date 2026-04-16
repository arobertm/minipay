package ro.minipay.psd2.model;

/**
 * Simulated account balance for AIS.
 */
public record AccountBalance(
    String accountId,
    String iban,
    String currency,
    double closingBookedBalance,
    double availableBalance,
    String balanceType   // "closingBooked" | "expected"
) {}
