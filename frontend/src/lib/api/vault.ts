import { api } from "./axios";

export interface TokenizeRequest {
  pan: string;
  expiry: string;
  cvv: string;
}

export interface TokenizeResponse {
  dpan: string;
  maskedPan?: string;
  expiresAt?: string;
}

export const vault = {
  tokenize: (body: TokenizeRequest) =>
    api.post<TokenizeResponse>("/vault/tokenize", body).then((r) => r.data),

  detokenize: (dpan: string) =>
    api.post<{ pan: string; maskedPan: string }>(`/vault/detokenize/${dpan}`).then((r) => r.data),

  deleteToken: (dpan: string) =>
    api.delete(`/vault/tokens/${dpan}`).then((r) => r.data),
};
