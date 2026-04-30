import { api } from "./axios";

export interface ShapDetail {
  feature: string;
  shap_value: number;
  description: string;
}

export interface FraudRequest {
  dpan: string;
  amount: number;        // în cenți
  currency: string;
  merchantId: string;
  ipAddress?: string;
}

export interface FraudResponse {
  score: number;
  decision: "ALLOW" | "CHALLENGE" | "BLOCK";
  reasons: string[];
  shap_details: ShapDetail[];
}

export const fraud = {
  score: (body: FraudRequest) =>
    api.post<FraudResponse>("/fraud/score", body).then((r) => r.data),
};
