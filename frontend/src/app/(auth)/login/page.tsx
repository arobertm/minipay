"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getClientToken } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/store/auth.store";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const setToken = useAuthStore((s) => s.setToken);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setLoading(true);
    try {
      const token = await getClientToken();
      setToken(token);
      toast.success("Authenticated successfully");
      router.push("/dashboard");
    } catch {
      toast.error("Authentication failed — check backend connectivity");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#0f1117] flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <span className="text-3xl font-bold text-blue-400">Mini</span>
          <span className="text-3xl font-bold text-white">Pay</span>
          <p className="text-white/40 text-sm mt-2">Payment Gateway Dashboard</p>
        </div>

        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-8">
          <h1 className="text-lg font-semibold mb-6">Sign in</h1>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="clientId">Client ID</Label>
              <Input
                id="clientId"
                defaultValue="minipay-dashboard"
                className="bg-white/5 border-white/10"
                readOnly
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="secret">Client Secret</Label>
              <Input
                id="secret"
                type="password"
                defaultValue="minipay-dashboard-secret"
                className="bg-white/5 border-white/10"
                readOnly
              />
            </div>
            <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700" disabled={loading}>
              {loading ? <><Loader2 size={16} className="animate-spin mr-2" /> Authenticating...</> : "Sign in"}
            </Button>
          </form>
          <p className="text-xs text-white/30 text-center mt-4">
            OAuth2 client_credentials grant via auth-svc
          </p>
        </div>
      </div>
    </div>
  );
}
