"use client";

import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { Suspense } from "react";
import { CheckCircle2, XCircle, ShoppingBag, LayoutDashboard, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

function ReceiptContent() {
  const sp = useSearchParams();
  const txnId = sp.get("txnId") ?? "—";
  const status = sp.get("status") ?? "UNKNOWN";
  const amount = sp.get("amount") ?? "0";
  const currency = sp.get("currency") ?? "EUR";
  const fraud = parseFloat(sp.get("fraud") ?? "0");

  const ok = status === "AUTHORIZED" || status === "CAPTURED";
  // amount comes from gateway in cents — convert to display
  const displayAmount = (parseFloat(amount) / 100).toFixed(2);

  return (
    <div className="min-h-screen bg-[#0f1117] text-white flex items-center justify-center px-4">
      <div className="max-w-md w-full">
        <div className={`rounded-2xl border p-8 text-center space-y-5 ${ok ? "border-green-500/30 bg-green-500/5" : "border-red-500/30 bg-red-500/5"}`}>
          {ok
            ? <CheckCircle2 size={56} className="mx-auto text-green-400" />
            : <XCircle size={56} className="mx-auto text-red-400" />}

          <div>
            <h1 className={`text-2xl font-bold ${ok ? "text-green-400" : "text-red-400"}`}>
              {ok ? "Payment Successful" : "Payment Failed"}
            </h1>
            <p className="text-white/40 text-sm mt-1">
              {ok ? "Your order has been placed" : "Please try again"}
            </p>
          </div>

          {ok && (
            <div className="rounded-xl border border-white/10 bg-white/5 p-5 space-y-3 text-left">
              <div className="flex justify-between text-sm">
                <span className="text-white/50">Amount</span>
                <span className="font-bold text-lg">{displayAmount} {currency}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-white/50">Status</span>
                <Badge className="bg-green-500/20 text-green-400 border-green-500/30">{status}</Badge>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-white/50">Transaction ID</span>
                <span className="font-mono text-xs text-white/60">{txnId.slice(0, 20)}…</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-white/50">Fraud Score</span>
                <Badge className={fraud < 0.3 ? "bg-green-500/20 text-green-400 border-green-500/30" : fraud < 0.7 ? "bg-yellow-500/20 text-yellow-400" : "bg-red-500/20 text-red-400"}>
                  {(fraud * 100).toFixed(1)}% — {fraud < 0.3 ? "LOW" : fraud < 0.7 ? "MEDIUM" : "HIGH"}
                </Badge>
              </div>
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <Link href="/shop" className="flex-1">
              <Button variant="outline" className="w-full border-white/20 text-white/60 hover:text-white">
                <ArrowLeft size={14} className="mr-2" /> Shop Again
              </Button>
            </Link>
            <Link href="/dashboard" className="flex-1">
              <Button className="w-full bg-blue-600 hover:bg-blue-700">
                <LayoutDashboard size={14} className="mr-2" /> Dashboard
              </Button>
            </Link>
          </div>
        </div>

        <p className="text-center text-xs text-white/20 mt-4 flex items-center justify-center gap-1">
          <ShoppingBag size={10} /> MiniPay Demo Shop — Dissertation Project
        </p>
      </div>
    </div>
  );
}

export default function ReceiptPage() {
  return (
    <Suspense>
      <ReceiptContent />
    </Suspense>
  );
}
