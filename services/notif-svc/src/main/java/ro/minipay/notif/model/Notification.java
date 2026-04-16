package ro.minipay.notif.model;

import java.time.Instant;

/**
 * A notification generated from a payment event.
 *
 * channel  — delivery channel (EMAIL, SMS, PUSH, LOG)
 * status   — SENT (simulated) or FAILED
 */
public record Notification(
    String  txnId,
    String  paymentStatus,
    String  channel,
    String  subject,
    String  message,
    String  notifStatus,
    Instant createdAt
) {}
