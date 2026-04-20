import { api } from "./axios";

export interface SettlementBatch {
  batchId: string;
  settlementDate: string;
  totalAmount: number;
  currency: string;
  recordCount: number;
  status: "PENDING" | "COMPLETED" | "FAILED";
  createdAt: string;
}

export interface SettlementRecord {
  recordId: string;
  batchId: string;
  transactionId: string;
  amount: number;
  currency: string;
  status: string;
  settledAt: string;
}

export const settlements = {
  batches: () =>
    api.get<SettlementBatch[]>("/settlements/batches").then((r) => r.data),

  records: (batchId?: string) =>
    api
      .get<SettlementRecord[]>(`/settlements/records${batchId ? `?batchId=${batchId}` : ""}`)
      .then((r) => r.data),

  reconcile: () =>
    api.post<{ message: string; batchId?: string }>("/settlements/reconcile").then((r) => r.data),
};
