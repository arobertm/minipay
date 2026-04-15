package ro.minipay.network.dto;

/**
 * Authorization response returned to gateway-svc.
 */
public record AuthorizeResponse(
    String responseCode,
    String authCode
) {
    public static AuthorizeResponse systemError() {
        return new AuthorizeResponse("96", ""); // 96 = System Malfunction
    }
}
