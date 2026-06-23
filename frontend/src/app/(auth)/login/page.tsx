"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getClientToken } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/store/auth.store";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Loader2, Lock } from "lucide-react";

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
      toast.success("Autentificare reușită");
      router.push("/dashboard");
    } catch {
      toast.error("Autentificare eșuată — verifică conectivitatea cu serverul");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-[#0a0f18] to-background flex items-center justify-center px-4 relative overflow-hidden">
      {/* Animated background gradient orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-20 right-40 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl animate-pulse opacity-20" />
        <div className="absolute bottom-40 left-32 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl animate-pulse opacity-20 delay-1000" />
      </div>

      <div className="w-full max-w-md relative z-10">
        {/* Header */}
        <div className="mb-12 text-center fadeIn">

          <h1 className="text-4xl font-display font-bold mb-2">
            <span className="bg-gradient-to-r from-emerald-400 to-cyan-400 bg-clip-text text-transparent">Mini</span>
            <span className="text-foreground">Pay</span>
          </h1>
          <p className="text-foreground/60 text-base font-medium">Platformă de Plăți Enterprise</p>
        </div>

        {/* Login Card */}
        <div className="card-premium glass backdrop-blur-xl border-foreground/20 fadeIn" style={{ animationDelay: "0.1s" }}>
          <div className="mb-8">
            <h2 className="text-2xl font-display font-bold text-foreground">Bun venit</h2>
            <p className="text-foreground/50 text-sm mt-2">Autentifică-te în panoul de control</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Client ID */}
            <div className="space-y-2.5">
              <Label htmlFor="clientId" className="text-sm font-semibold text-foreground">
                Client ID
              </Label>
              <Input
                id="clientId"
                defaultValue="minipay-dashboard"
                className="bg-input/30 border-foreground/20 placeholder:text-foreground/30"
                readOnly
              />
            </div>

            {/* Client Secret */}
            <div className="space-y-2.5">
              <Label htmlFor="secret" className="text-sm font-semibold text-foreground flex items-center gap-2">
                <Lock size={16} className="text-cyan-400" />
                Client Secret
              </Label>
              <Input
                id="secret"
                type="password"
                defaultValue="minipay-dashboard-secret"
                className="bg-input/30 border-foreground/20 placeholder:text-foreground/30"
                readOnly
              />
            </div>

            {/* Sign In Button */}
            <Button
              type="submit"
              className="w-full btn-primary mt-6 h-11 text-base font-semibold shadow-lg shadow-emerald-500/30"
              disabled={loading}
            >
              {loading ? (
                <>
                  <Loader2 size={18} className="animate-spin mr-2" />
                  <span>Se autentifică...</span>
                </>
              ) : (
                <span>Intră în cont</span>
              )}
            </Button>
          </form>

          {/* Footer */}
        </div>

        {/* Bottom info */}
        <div className="mt-8 text-center text-xs text-foreground/40 fadeIn" style={{ animationDelay: "0.2s" }}>
          <p>Securitate enterprise cu criptografie post-cuantică</p>
        </div>
      </div>
    </div>
  );
}
