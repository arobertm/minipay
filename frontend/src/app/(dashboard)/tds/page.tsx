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

interface TdsAuthResponse {
  status: "FRICTIONLESS" | "CHALLENGE";
  transactionId: string;
  challengeId?: string;
  acsUrl?: string;
}

interface TdsChallengeResponse {
  status: "SUCCESS" | "FAILED";
  cavv?: string;
  eci?: string;
}

export default function TdsPage() {
  const [txnId, setTxnId] = useState("");
  const [email, setEmail] = useState("test@example.com");
  const [authResult, setAuthResult] = useState<TdsAuthResponse | null>(null);
  const [otp, setOtp] = useState("");
  const [verifyResult, setVerifyResult] = useState<TdsChallengeResponse | null>(null);
  const [otpOpen, setOtpOpen] = useState(false);

  const authMut = useMutation({
    mutationFn: () =>
      api.post<TdsAuthResponse>("/tds/authenticate", { transactionId: txnId, cardholderEmail: email }).then((r) => r.data),
    onSuccess: (data) => {
      setAuthResult(data);
      if (data.status === "CHALLENGE") {
        setOtpOpen(true);
        toast.info("Challenge required — enter OTP");
      } else {
        toast.success("Frictionless authentication approved");
      }
    },
    onError: () => toast.error("3DS authentication failed"),
  });

  const verifyMut = useMutation({
    mutationFn: () =>
      api
        .post<TdsChallengeResponse>("/tds/challenge/verify", {
          challengeId: authResult?.challengeId,
          transactionId: txnId,
          otp,
        })
        .then((r) => r.data),
    onSuccess: (data) => {
      setVerifyResult(data);
      setOtpOpen(false);
      if (data.status === "SUCCESS") toast.success("Challenge passed ✅");
      else toast.error("OTP incorrect ❌");
    },
    onError: () => toast.error("Challenge verification failed"),
  });

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">3DS2 Challenge</h1>
        <p className="text-white/40 text-sm mt-1">Risk-based authentication — frictionless for low risk, OTP for high fraud score</p>
      </div>

      <div className="max-w-lg">
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Initiate 3DS Authentication</h2>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>Transaction ID</Label>
              <Input value={txnId} onChange={(e) => setTxnId(e.target.value)} placeholder="UUID from authorize" className="bg-white/5 border-white/10 font-mono text-sm" />
            </div>
            <div className="space-y-1.5">
              <Label>Cardholder Email</Label>
              <Input value={email} onChange={(e) => setEmail(e.target.value)} className="bg-white/5 border-white/10" />
            </div>
          </div>
          <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => authMut.mutate()} disabled={authMut.isPending || !txnId}>
            {authMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Authenticating…</> : "Initiate 3DS"}
          </Button>
        </div>

        {/* Result */}
        {authResult && (
          <div className={`mt-4 rounded-xl border p-5 ${authResult.status === "FRICTIONLESS" ? "border-green-500/30 bg-green-500/10" : "border-yellow-500/30 bg-yellow-500/10"}`}>
            <div className="flex items-center gap-3">
              {authResult.status === "FRICTIONLESS"
                ? <ShieldCheck size={24} className="text-green-400" />
                : <ShieldAlert size={24} className="text-yellow-400" />}
              <div>
                <p className={`font-semibold ${authResult.status === "FRICTIONLESS" ? "text-green-400" : "text-yellow-400"}`}>
                  {authResult.status === "FRICTIONLESS" ? "Frictionless — Approved" : "Challenge Required"}
                </p>
                <p className="text-xs text-white/40 mt-0.5">txnId: {authResult.transactionId?.slice(0, 20)}…</p>
              </div>
            </div>
          </div>
        )}

        {/* Challenge success */}
        {verifyResult?.status === "SUCCESS" && (
          <div className="mt-4 rounded-xl border border-green-500/30 bg-green-500/10 p-5">
            <p className="text-green-400 font-semibold flex items-center gap-2"><ShieldCheck size={18} />Challenge Passed</p>
            {verifyResult.cavv && (
              <p className="text-xs text-white/50 mt-2 font-mono">CAVV: {verifyResult.cavv}</p>
            )}
            {verifyResult.eci && (
              <Badge className="mt-2 bg-green-500/20 text-green-400 border-green-500/30">ECI: {verifyResult.eci}</Badge>
            )}
          </div>
        )}
      </div>

      {/* OTP Dialog */}
      <Dialog open={otpOpen} onOpenChange={setOtpOpen}>
        <DialogContent className="bg-[#1a1d27] border-white/10 text-white max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <ShieldAlert size={18} className="text-yellow-400" /> Enter OTP Challenge
            </DialogTitle>
          </DialogHeader>
          <p className="text-sm text-white/50">A one-time password was sent to <strong>{email}</strong>. Enter it below to complete authentication.</p>
          <Input
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
            placeholder="6-digit OTP"
            maxLength={6}
            className="bg-white/5 border-white/10 text-center text-2xl tracking-[0.5em] font-mono"
          />
          <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => verifyMut.mutate()} disabled={verifyMut.isPending || otp.length < 4}>
            {verifyMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Verifying…</> : "Verify OTP"}
          </Button>
        </DialogContent>
      </Dialog>
    </div>
  );
}
