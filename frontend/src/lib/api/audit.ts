import { api } from "./axios";

export interface AuditEntry {
  id: string;
  transactionId: string;
  action: string;
  actorId?: string;
  details?: string;
  merkleHash?: string;
  timestamp: string;
}

export interface VerifyResult {
  valid: boolean;
  merkleRoot?: string;
  totalEntries?: number;
  message?: string;
}

export const audit = {
  list: (page = 0, size = 20) =>
    api.get<{ content: AuditEntry[]; totalElements: number }>(`/audit/entries?page=${page}&size=${size}`).then((r) => r.data),

  getByTxn: (txnId: string) =>
    api.get<AuditEntry[]>(`/audit/entries/${txnId}`).then((r) => r.data),

  verify: () =>
    api.get<VerifyResult>("/audit/verify").then((r) => r.data),
};
