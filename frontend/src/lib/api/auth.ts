import axios from "axios";

const CLIENT_ID = process.env.NEXT_PUBLIC_AUTH_CLIENT_ID ?? "minipay-dashboard";

export async function getClientToken(): Promise<string> {
  const params = new URLSearchParams({
    grant_type: "client_credentials",
    client_id: CLIENT_ID,
    client_secret: "minipay-dashboard-secret",
    scope: "openid",
  });

  // Goes through Next.js proxy → no CORS
  const { data } = await axios.post("/api/proxy/auth/oauth2/token", params, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
  });

  return data.access_token as string;
}
