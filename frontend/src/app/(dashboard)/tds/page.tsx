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
import { Loader2, ShieldCheck, ShieldAlert, Lock } from "lucide-react";

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
  Y: "Frictionless — Autentificare reușită",
  C: "Challenge necesar",
  N: "Neautentificat",
  U: "Authentication Could Not Be Performed",
};

const STATUS_COLORS: Record<string, { border: string; bg: string; text: string }> = {
  Y: { border: "border-emerald-500/30", bg: "bg-emerald-500/10", text: "text-emerald-400" },
  C: { border: "border-amber-500/30", bg: "bg-amber-500/10", text: "text-amber-400" },
  N: { border: "border-red-500/30", bg: "bg-red-500/10", text: "text-red-400" },
  U: { border: "border-red-500/30", bg: "bg-red-500/10", text: "text-red-400" },
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
        api.get<ChallengeSession>(`/tds/challenge/${data.acsTransID}`)
          .then((r) => { setChallengeSession(r.data); setOtpOpen(true); })
          .catch(() => setOtpOpen(true));
        toast.info("Autentificare suplimentară necesară — introduceți OTP");
      } else if (data.transStatus === "Y") {
        toast.success("Autentificare aprobată");
      } else {
        toast.error(`Rezultat autentificare: ${data.transStatus}`);
      }
    },
    onError: () => toast.error("Autentificare 3DS eșuată"),
  });

  const challengeMut = useMutation({
    mutationFn: () =>
      api.post<ChallengeResult>(`/tds/challenge/${authResult?.acsTransID}`, { otp }).then((r) => r.data),
    onSuccess: (data) => {
      setFinalResult(data);
      setOtpOpen(false);
      if (data.transStatus === "Y") toast.success("Verificare reușită");
      else toast.error("OTP incorect");
    },
    onError: () => toast.error("Verificare challenge eșuată"),
  });

  const f = (k: keyof typeof form, v: string | number) => setForm((p) => ({ ...p, [k]: v }));

  return (
    <div className="space-y-8 p-6">
      <div className="fadeIn">
        <h1 className="text-4xl font-display font-bold text-foreground">3DS2 Challenge</h1>
        <p className="text-foreground/60 text-base mt-2 font-medium">
          Bazat pe risc — frictionless (fraudScore &lt; 0.7) sau challenge OTP (≥ 0.7)
        </p>
      </div>

      <div className="max-w-xl space-y-5">
        <div className="card-premium space-y-5 fadeIn stagger-1">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-amber-500/20">
              <Lock size={18} className="text-amber-400" />
            </div>
            <h2 className="text-base font-display font-semibold">Cerere de autentificare</h2>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2 col-span-2">
              <Label className="text-foreground/70 font-medium">Număr card (mascat)</Label>
              <Input value={form.acctNumber} onChange={(e) => f("acctNumber", e.target.value)} className="font-mono" />
            </div>
            <div className="space-y-2">
              <Label className="text-foreground/70 font-medium">Sumă (unități minore)</Label>
              <Input type="number" value={form.purchaseAmount} onChange={(e) => f("purchaseAmount", parseInt(e.target.value))} />
            </div>
            <div className="space-y-2">
              <Label className="text-foreground/70 font-medium">Currency</Label>
              <Input value={form.purchaseCurrency} onChange={(e) => f("purchaseCurrency", e.target.value)} />
            </div>
            <div className="space-y-2 col-span-2">
              <Label className="text-foreground/70 font-medium">Fraud Score (0.0 – 1.0)</Label>
              <Input type="number" step="0.01" min="0" max="1" value={form.fraudScore}
                onChange={(e) => f("fraudScore", parseFloat(e.target.value))} />
              <p className="text-xs text-foreground/40">
                ≥ 0.7 triggers OTP challenge · &lt; 0.7 = frictionless
              </p>
            </div>
          </div>
          <Button className="w-full" onClick={() => authMut.mutate()} disabled={authMut.isPending}>
            {authMut.isPending
              ? <><Loader2 size={16} className="animate-spin mr-2" />Se autentifică…</>
              : "Inițiază autentificare 3DS"}
          </Button>
        </div>

        {/* Auth result */}
        {authResult && (() => {
          const colors = STATUS_COLORS[authResult.transStatus] ?? STATUS_COLORS.U;
          return (
            <div className={`rounded-xl border p-5 space-y-4 fadeIn ${colors.border} ${colors.bg}`}>
              <div className="flex items-center gap-3">
                {authResult.transStatus === "Y"
                  ? <ShieldCheck size={22} className="text-emerald-400" />
                  : <ShieldAlert size={22} className={colors.text} />}
                <p className={`font-semibold ${colors.text}`}>
                  {STATUS_LABEL[authResult.transStatus] ?? authResult.transStatus}
                </p>
              </div>
              <div className="rounded-lg bg-foreground/5 p-3 space-y-1.5 text-xs font-mono text-foreground/50">
                <p>acsTransID: {authResult.acsTransID}</p>
                {authResult.authenticationValue && <p>CAVV: {authResult.authenticationValue}</p>}
                {authResult.eci && <p>ECI: {authResult.eci}</p>}
              </div>
            </div>
          );
        })()}

        {/* Final challenge result */}
        {finalResult && (
          <div className={`rounded-xl border p-5 space-y-2 fadeIn ${
            finalResult.transStatus === "Y"
              ? "border-emerald-500/30 bg-emerald-500/10"
              : "border-red-500/30 bg-red-500/10"
          }`}>
            <p className={`font-semibold ${finalResult.transStatus === "Y" ? "text-emerald-400" : "text-red-400"}`}>
              {finalResult.transStatus === "Y" ? "✅ Challenge trecut — autentificat" : "❌ Challenge eșuat"}
            </p>
            {finalResult.authenticationValue && (
              <p className="text-xs text-foreground/40 font-mono">CAVV: {finalResult.authenticationValue}</p>
            )}
            {finalResult.eci && (
              <Badge className="bg-emerald-500/20 text-emerald-400 border-emerald-500/30">ECI: {finalResult.eci}</Badge>
            )}
          </div>
        )}
      </div>

      {/* OTP Dialog */}
      <Dialog open={otpOpen} onOpenChange={setOtpOpen}>
        <DialogContent className="bg-card border-border/40 max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-foreground">
              <ShieldAlert size={18} className="text-amber-400" /> Challenge OTP
            </DialogTitle>
          </DialogHeader>
          <p className="text-sm text-foreground/60">Introduceți parola de unică folosință pentru a finaliza autentificarea.</p>
          {challengeSession?.otp_demo_only && (
            <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-center">
              <p className="text-xs text-amber-400/70 font-semibold mb-2">OTP Demo (vizibil pentru testare)</p>
              <p className="text-3xl font-mono font-bold text-amber-400 tracking-[0.4em]">
                {challengeSession.otp_demo_only}
              </p>
            </div>
          )}
          <Input
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
            placeholder="Introduceți OTP"
            maxLength={8}
            className="text-center text-xl tracking-[0.4em] font-mono"
          />
          <Button className="w-full" onClick={() => challengeMut.mutate()} disabled={challengeMut.isPending || otp.length < 4}>
            {challengeMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Se verifică…</> : "Verifică OTP"}
          </Button>
        </DialogContent>
      </Dialog>
    </div>
  );
}
