import { api } from "./axios";

export interface Account {
  accountId: string;
  iban: string;
  currency: string;
  ownerName: string;
}

export interface Balance {
  amount: number;
  currency: string;
  type: string;
  referenceDate?: string;
}

export interface SepaPaymentRequest {
  debtorIban: string;
  creditorIban: string;
  creditorName: string;
  amount: number;
  currency: string;
  reference: string;
}

export const psd2 = {
  createConsent: (userId: string, ibans: string[]) =>
    api.post<{ consentId: string; status: string }>("/psd2/consents", { userId, ibans }).then((r) => r.data),

  getAccounts: (consentId: string) =>
    api.get<Account[]>(`/psd2/accounts?consentId=${consentId}`).then((r) => r.data),

  getBalances: (accountId: string, consentId: string) =>
    api.get<Balance[]>(`/psd2/accounts/${accountId}/balances?consentId=${consentId}`).then((r) => r.data),

  getTransactions: (accountId: string, consentId: string) =>
    api.get(`/psd2/accounts/${accountId}/transactions?consentId=${consentId}`).then((r) => r.data),

  initiateSepa: (body: SepaPaymentRequest) =>
    api.post<{ paymentId: string; status: string }>("/psd2/payments/sepa-credit-transfer", body).then((r) => r.data),

  getPaymentStatus: (paymentId: string) =>
    api.get<{ paymentId: string; status: string }>(`/psd2/payments/${paymentId}/status`).then((r) => r.data),
};
