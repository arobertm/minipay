import axios from "axios";

// All requests go through the Next.js proxy to avoid CORS.
// /api/proxy/<path> → server-side → api-minipay.online/<path>
const BASE = "/api/proxy";

export const api = axios.create({ baseURL: BASE });

api.interceptors.request.use((config) => {
  const token =
    typeof window !== "undefined" ? localStorage.getItem("mp_token") : null;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401 && typeof window !== "undefined") {
      localStorage.removeItem("mp_token");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);
