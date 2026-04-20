import { api } from "./axios";

export interface AuthorizeRequest {
  pan?: string;
  dpan?: string;
  expiry: string;
  cvv: string;
  amount: number;
  currency: string;
  merchantId: string;
  merchantName: string;
  merchantCountry: string;
  merchantCategory: string;
}

export interface PaymentResponse {
  transactionId: string;
  status: "AUTHORIZED" | "CAPTURED" | "REFUNDED" | "DECLINED" | "FAILED";
  amount: number;
  currency: string;
  fraudScore?: number;
  authCode?: string;
  message?: string;
  createdAt?: string;
}

export const gateway = {
  authorize: (body: AuthorizeRequest) =>
    api.post<PaymentResponse>("/api/v1/payments/authorize", body).then((r) => r.data),

  capture: (txnId: string) =>
    api.post<PaymentResponse>(`/api/v1/payments/${txnId}/capture`).then((r) => r.data),

  refund: (txnId: string) =>
    api.post<PaymentResponse>(`/api/v1/payments/${txnId}/refund`).then((r) => r.data),

  get: (txnId: string) =>
    api.get<PaymentResponse>(`/api/v1/payments/${txnId}`).then((r) => r.data),
};
