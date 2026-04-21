import { api } from "./axios";

export interface AuthorizeRequest {
  pan: string;
  expiryDate: string;   // MM/YY
  cvv: string;
  amount: number;       // in cents (e.g. 15000 = 150.00 EUR)
  currency: string;     // ISO 4217, 3 chars
  merchantId: string;
  orderId: string;
  description?: string;
}

export interface PaymentResponse {
  txnId: string;
  dpan?: string;
  status: "AUTHORIZED" | "CAPTURED" | "REFUNDED" | "DECLINED" | "BLOCKED" | "CHALLENGE" | "FAILED";
  isoResponseCode?: string;
  amount: number;       // in cents
  currency: string;
  merchantId?: string;
  orderId?: string;
  fraudScore?: number;
  fraudReasons?: string[];
  authCode?: string;
  declineReason?: string;
  processedAt?: string;
}

export const gateway = {
  authorize: (body: AuthorizeRequest) =>
    api.post<PaymentResponse>("/api/v1/payments/authorize", body).then((r) => r.data),

  capture: (txnId: string, amount: number, currency: string) =>
    api.post<PaymentResponse>(`/api/v1/payments/${txnId}/capture`, { amount, currency }).then((r) => r.data),

  refund: (txnId: string, amount: number, reason?: string) =>
    api.post<PaymentResponse>(`/api/v1/payments/${txnId}/refund`, { amount, reason }).then((r) => r.data),

  get: (txnId: string) =>
    api.get<PaymentResponse>(`/api/v1/payments/${txnId}`).then((r) => r.data),
};

/** Convert EUR/RON amount (e.g. 150.00) to cents (15000) */
export function toCents(amount: number): number {
  return Math.round(amount * 100);
}

/** Convert cents (15000) to display amount (150.00) */
export function fromCents(cents: number): string {
  return (cents / 100).toFixed(2);
}
