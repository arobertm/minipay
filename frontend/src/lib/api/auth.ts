import axios from "axios";

const BASE = process.env.NEXT_PUBLIC_API_URL ?? "https://api-minipay.online";
const CLIENT_ID = process.env.NEXT_PUBLIC_AUTH_CLIENT_ID ?? "minipay-dashboard";

export async function getClientToken(): Promise<string> {
  const params = new URLSearchParams({
    grant_type: "client_credentials",
    client_id: CLIENT_ID,
    client_secret: "minipay-dashboard-secret",
    scope: "openid",
  });

  const { data } = await axios.post(`${BASE}/auth/oauth2/token`, params, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
  });

  return data.access_token as string;
}
