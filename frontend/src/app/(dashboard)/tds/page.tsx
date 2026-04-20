"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { api } from "@/lib/api/axios";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Loader2, ShieldCheck, ShieldAlert } from "lucide-react";

interface AuthResult {
  acsTransID: string;
  threeDSServerTransID: string;
  transStatus: "Y" | "C" | "N" | "U";
  transStatusReason?: string;
  authenticationValue?: string;
  eci?: string;
  challengeURL?: string;
}

interface ChallengeSession {
  acsTransID: string;
  status: string;
  expiresAt: string;
  otp_demo_only: string;
}

interface ChallengeResult {
  acsTransID: string;
  transStatus: string;
  authenticationValue?: string;
  eci?: string;
}

const STATUS_LABEL: Record<string, string> = {
  Y: "Frictionless — Authentication Successful",
  C: "Challenge Required",
  N: "Not Authenticated",
  U: "Authentication Could Not Be Performed",
};

export default function TdsPage() {
  const [form, setForm] = useState({
    acctNumber: "411111******1111",
    purchaseAmount: 15000,
    purchaseCurrency: "EUR",
    merchantId: "DEMO-MERCHANT-001",
    merchantName: "Demo Shop",
    fraudScore: 0.15,
  });

  const [authResult, setAuthResult] = useState<AuthResult | null>(null);
  const [challengeSession, setChallengeSession] = useState<ChallengeSession | null>(null);
  const [otp, setOtp] = useState("");
  const [otpOpen, setOtpOpen] = useState(false);
  const [finalResult, setFinalResult] = useState<ChallengeResult | null>(null);

  const authMut = useMutation({
    mutationFn: () =>
      api.post<AuthResult>("/tds/authenticate", {
        threeDSServerTransID: crypto.randomUUID(),
        acctNumber: form.acctNumber,
        purchaseAmount: form.purchaseAmount,
        purchaseCurrency: form.purchaseCurrency,
        merchantId: form.merchantId,
        merchantName: form.merchantName,
        browserIP: "127.0.0.1",
        deviceChannel: "02",
        fraudScore: form.fraudScore,
      }).then((r) => r.data),
    onSuccess: (data) => {
      setAuthResult(data);
      if (data.transStatus === "C") {
        // Fetch challenge session (contains demo OTP)
        api.get<ChallengeSession>(`/tds/challenge/${data.acsTransID}`)
          .then((r) => { setChallengeSession(r.data); setOtpOpen(true); })
          .catch(() => setOtpOpen(true));
        toast.info("Challenge required — enter OTP");
      } else if (data.transStatus === "Y") {
        toast.success("Frictionless authentication approved");
      } else {
        toast.error(`Authentication result: ${data.transStatus}`);
      }
    },
    onError: () => toast.error("3DS authentication failed"),
  });

  const challengeMut = useMutation({
    mutationFn: () =>
      api.post<ChallengeResult>(`/tds/challenge/${authResult?.acsTransID}`, { otp })
        .then((r) => r.data),
    onSuccess: (data) => {
      setFinalResult(data);
      setOtpOpen(false);
      if (data.transStatus === "Y") toast.success("Challenge passed ✅");
      else toast.error("OTP incorrect ❌");
    },
    onError: () => toast.error("Challenge verification failed"),
  });

  const f = (k: keyof typeof form, v: string | number) => setForm((p) => ({ ...p, [k]: v }));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">3DS2 Challenge</h1>
        <p className="text-white/40 text-sm mt-1">
          Risk-based — frictionless (fraudScore &lt; 0.7) or OTP challenge (≥ 0.7)
        </p>
      </div>

      <div className="max-w-lg space-y-4">
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Authentication Request</h2>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5 col-span-2">
              <Label>Card Number (masked)</Label>
              <Input value={form.acctNumber} onChange={(e) => f("acctNumber", e.target.value)} className="bg-white/5 border-white/10 font-mono" />
            </div>
            <div className="space-y-1.5">
              <Label>Amount (minor units)</Label>
              <Input type="number" value={form.purchaseAmount} onChange={(e) => f("purchaseAmount", parseInt(e.target.value))} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5">
              <Label>Currency</Label>
              <Input value={form.purchaseCurrency} onChange={(e) => f("purchaseCurrency", e.target.value)} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5 col-span-2">
              <Label>Fraud Score (0.0–1.0)</Label>
              <Input type="number" step="0.01" min="0" max="1" value={form.fraudScore}
                onChange={(e) => f("fraudScore", parseFloat(e.target.value))} className="bg-white/5 border-white/10" />
              <p className="text-xs text-white/30">≥ 0.7 triggers OTP challenge · &lt; 0.7 = frictionless</p>
            </div>
          </div>
          <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => authMut.mutate()} disabled={authMut.isPending}>
            {authMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Authenticating…</> : "Initiate 3DS Authentication"}
          </Button>
        </div>

        {/* Auth result */}
        {authResult && (
          <div className={`rounded-xl border p-5 space-y-3 ${
            authResult.transStatus === "Y" ? "border-green-500/30 bg-green-500/10" :
            authResult.transStatus === "C" ? "border-yellow-500/30 bg-yellow-500/10" :
            "border-red-500/30 bg-red-500/10"
          }`}>
            <div className="flex items-center gap-3">
              {authResult.transStatus === "Y"
                ? <ShieldCheck size={22} className="text-green-400" />
                : <ShieldAlert size={22} className={authResult.transStatus === "C" ? "text-yellow-400" : "text-red-400"} />}
              <p className={`font-semibold ${authResult.transStatus === "Y" ? "text-green-400" : authResult.transStatus === "C" ? "text-yellow-400" : "text-red-400"}`}>
                {STATUS_LABEL[authResult.transStatus] ?? authResult.transStatus}
              </p>
            </div>
            <div className="text-xs text-white/40 font-mono space-y-1">
              <p>acsTransID: {authResult.acsTransID}</p>
              {authResult.authenticationValue && <p>CAVV: {authResult.authenticationValue}</p>}
              {authResult.eci && <p>ECI: {authResult.eci}</p>}
            </div>
          </div>
        )}

        {/* Final challenge result */}
        {finalResult && (
          <div className={`rounded-xl border p-5 ${finalResult.transStatus === "Y" ? "border-green-500/30 bg-green-500/10" : "border-red-500/30 bg-red-500/10"}`}>
            <p className={`font-semibold ${finalResult.transStatus === "Y" ? "text-green-400" : "text-red-400"}`}>
              {finalResult.transStatus === "Y" ? "✅ Challenge passed — authenticated" : "❌ Challenge failed"}
            </p>
            {finalResult.authenticationValue && (
              <p className="text-xs text-white/40 font-mono mt-2">CAVV: {finalResult.authenticationValue}</p>
            )}
            {finalResult.eci && <Badge className="mt-2 bg-green-500/20 text-green-400 border-green-500/30">ECI: {finalResult.eci}</Badge>}
          </div>
        )}
      </div>

      {/* OTP Dialog */}
      <Dialog open={otpOpen} onOpenChange={setOtpOpen}>
        <DialogContent className="bg-[#1a1d27] border-white/10 text-white max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <ShieldAlert size={18} className="text-yellow-400" /> OTP Challenge
            </DialogTitle>
          </DialogHeader>
          <p className="text-sm text-white/50">Enter the one-time password to complete authentication.</p>
          {challengeSession?.otp_demo_only && (
            <div className="rounded-lg border border-yellow-500/30 bg-yellow-500/10 p-3 text-center">
              <p className="text-xs text-yellow-400/70 mb-1">Demo OTP (visible for testing)</p>
              <p className="text-2xl font-mono font-bold text-yellow-400 tracking-widest">{challengeSession.otp_demo_only}</p>
            </div>
          )}
          <Input
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
            placeholder="Enter OTP"
            maxLength={8}
            className="bg-white/5 border-white/10 text-center text-xl tracking-[0.4em] font-mono"
          />
          <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => challengeMut.mutate()} disabled={challengeMut.isPending || otp.length < 4}>
            {challengeMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Verifying…</> : "Verify OTP"}
          </Button>
        </DialogContent>
      </Dialog>
    </div>
  );
}
