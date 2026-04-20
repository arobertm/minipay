import { api } from "./axios";

export interface SettlementBatch {
  id: number;
  merchantId: string;
  settlementDate: string;
  netAmount: number;
  capturedAmount: number;
  refundedAmount: number;
  currency: string;
  txnCount: number;
  status: string;
  reconciledAt: string;
}

export interface SettlementRecord {
  id: number;
  txnId: string;
  merchantId: string;
  amount: number;
  currency: string;
  paymentStatus: string;
  settlementDate: string;
  createdAt: string;
}

export interface ReconcileResponse {
  date: string;
  batchesCreated: number;
  batches: SettlementBatch[];
}

export const settlements = {
  batches: () =>
    api.get<SettlementBatch[]>("/settlements/batches").then((r) => r.data),

  records: (date?: string) =>
    api
      .get<SettlementRecord[]>(`/settlements/records${date ? `?date=${date}` : ""}`)
      .then((r) => r.data),

  reconcile: (date?: string) =>
    api.post<ReconcileResponse>(`/settlements/reconcile${date ? `?date=${date}` : ""}`).then((r) => r.data),
};
