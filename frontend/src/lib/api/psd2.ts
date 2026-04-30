import { api } from "./axios";

export interface AccountBalance {
  accountId: string;
  iban: string;
  currency: string;
  closingBookedBalance: number;
  availableBalance: number;
  balanceType: string;
}

export interface Consent {
  consentId: string;
  status: string;
  permissions: string[];
  validUntil: string;
}

export interface SepaPaymentRequest {
  debtorIban: string;
  creditorIban: string;
  creditorName: string;
  amount: number;
  currency: string;
  reference: string;
}

const consentHeader = (consentId: string) => ({ "Consent-ID": consentId });

export const psd2 = {
  createConsent: (psuId: string, accountIds: string[], permissions: string[], validUntil: string) =>
    api
      .post<Consent>("/psd2/consents", { psuId, accountIds, permissions, validUntil })
      .then((r) => r.data),

  getConsent: (consentId: string) =>
    api.get<Consent>(`/psd2/consents/${consentId}`).then((r) => r.data),

  revokeConsent: (consentId: string) =>
    api.delete<{ consentId: string; status: string }>(`/psd2/consents/${consentId}`).then((r) => r.data),

  getAccounts: (consentId: string) =>
    api
      .get<{ accounts: AccountBalance[] }>("/psd2/accounts", {
        headers: consentHeader(consentId),
      })
      .then((r) => r.data.accounts),

  getBalances: (accountId: string, consentId: string) =>
    api
      .get<{ balances: AccountBalance[] }>(`/psd2/accounts/${accountId}/balances`, {
        headers: consentHeader(consentId),
      })
      .then((r) => r.data.balances),

  getTransactions: (accountId: string, consentId: string) =>
    api
      .get(`/psd2/accounts/${accountId}/transactions`, {
        headers: consentHeader(consentId),
      })
      .then((r) => r.data),

  initiateSepa: (body: SepaPaymentRequest) =>
    api
      .post<{ paymentId: string; status: string }>("/psd2/payments/sepa-credit-transfers", body)
      .then((r) => r.data),
};
