import { api } from "./axios";

export interface FraudRequest {
  transactionId?: string;
  amount: number;
  currency: string;
  merchantCountry: string;
  cardType: string;
  hour: number;
  dayOfWeek: number;
  isInternational?: boolean;
  previousFraudCount?: number;
}

export interface FraudResponse {
  fraudScore: number;
  decision: "APPROVE" | "REVIEW" | "DECLINE";
  shapValues: Record<string, number>;
  processingTimeMs?: number;
}

export const fraud = {
  score: (body: FraudRequest) =>
    api.post<FraudResponse>("/fraud/score", body).then((r) => r.data),
};
