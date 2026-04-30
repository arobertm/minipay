import { api } from "./axios";

export interface AuditEntry {
  id: number;
  sequenceNumber: number;
  txnId: string;
  status: string;
  amount: number;
  currency: string;
  merchantId: string;
  fraudScore: number;
  eventTimestamp: string;
  prevHash: string;
  entryHash: string;
}

export interface VerifyResult {
  isValid: boolean;
  totalEntries: number;
  message: string;
}

export const audit = {
  list: (page = 0, size = 20) =>
    api
      .get<{ content: AuditEntry[]; totalElements: number }>(
        `/audit/entries?page=${page}&size=${size}`
      )
      .then((r) => r.data),

  getByTxn: (txnId: string) =>
    api.get<AuditEntry>(`/audit/entries/${txnId}`).then((r) => r.data),

  verify: () =>
    api.get<VerifyResult>("/audit/verify").then((r) => r.data),
};
