package ro.minipay.tds.model;

/**
 * 3DS2 authentication request from the merchant/gateway.
 */
public record AuthenticationRequest(
    String  threeDSServerTransID,
    String  acctNumber,          // masked card number (last 4 digits shown)
    Long    purchaseAmount,      // minor units
    String  purchaseCurrency,
    String  merchantId,
    String  merchantName,
    String  browserIP,
    String  deviceChannel,       // "02" = browser, "03" = 3RI
    double  fraudScore           // from fraud-svc
) {}
