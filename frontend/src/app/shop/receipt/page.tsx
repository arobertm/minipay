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
  const displayAmount = (parseFloat(amount) / 100).toFixed(2);

  const fraudClass = fraud < 0.3
    ? "bg-emerald-500/20 text-emerald-400 border-emerald-500/30"
    : fraud < 0.7
    ? "bg-amber-500/20 text-amber-400 border-amber-500/30"
    : "bg-red-500/20 text-red-400 border-red-500/30";

  return (
    <div className="min-h-screen flex items-center justify-center px-4 relative overflow-hidden" style={{ background: "#f8f9fb" }}>

      {/* Green ambient glow — only on success */}
      {ok && (
        <>
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-64 pointer-events-none"
            style={{ background: "radial-gradient(ellipse at center top, rgba(16,185,129,0.12) 0%, transparent 70%)" }} />
          <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-96 h-48 pointer-events-none"
            style={{ background: "radial-gradient(ellipse at center bottom, rgba(16,185,129,0.07) 0%, transparent 70%)" }} />
        </>
      )}

      <div className="max-w-md w-full relative z-10">

        {/* Green top accent bar */}
        {ok && (
          <div className="h-1.5 rounded-t-2xl mx-0 mb-0" style={{ background: "linear-gradient(90deg, #10b981, #06b6d4)" }} />
        )}

        <div className={`rounded-2xl bg-white text-center space-y-6 shadow-lg fadeIn ${ok ? "border-2 border-emerald-100 rounded-t-none" : "border-2 border-red-200"}`}
          style={ok ? { padding: "2rem", boxShadow: "0 8px 40px rgba(16,185,129,0.10), 0 2px 8px rgba(0,0,0,0.06)" } : { padding: "2rem" }}>

          {/* Icon with pulse ring */}
          <div className="relative w-20 h-20 mx-auto">
            {ok && (
              <>
                <span className="absolute inset-0 rounded-full animate-ping"
                  style={{ background: "rgba(16,185,129,0.15)", animationDuration: "2s" }} />
                <span className="absolute -inset-2 rounded-full"
                  style={{ background: "rgba(16,185,129,0.06)", border: "1px solid rgba(16,185,129,0.15)" }} />
              </>
            )}
            <div className={`w-20 h-20 rounded-full flex items-center justify-center relative z-10 ${ok ? "bg-emerald-50" : "bg-red-50"}`}>
              {ok
                ? <CheckCircle2 size={40} className="text-emerald-500" />
                : <XCircle size={40} className="text-red-500" />}
            </div>
          </div>

          <div>
            <h1 className={`text-3xl font-display font-bold ${ok ? "text-emerald-600" : "text-red-600"}`}>
              {ok ? "Plată reușită" : "Plată eșuată"}
            </h1>
            <p className="text-gray-400 text-sm mt-2 font-medium">
              {ok ? "Comanda ta a fost confirmată" : "Te rugăm să încerci din nou"}
            </p>
          </div>

          {ok && (
            <div className="rounded-xl border border-emerald-100 p-5 space-y-3 text-left"
              style={{ background: "linear-gradient(135deg, rgba(16,185,129,0.04) 0%, rgba(6,182,212,0.03) 100%)" }}>
              <div className="flex justify-between text-sm items-center">
                <span className="text-gray-500 font-medium">Sumă</span>
                <span className="font-display font-bold text-2xl" style={{ color: "#059669" }}>{displayAmount} {currency}</span>
              </div>
              <div className="h-px bg-emerald-100" />
              <div className="flex justify-between text-sm items-center">
                <span className="text-gray-500 font-medium">Status</span>
                <Badge className="bg-emerald-50 text-emerald-600 border-emerald-200 font-semibold">{status}</Badge>
              </div>
              <div className="flex justify-between text-sm items-center">
                <span className="text-gray-500 font-medium">ID Tranzacție</span>
                <span className="font-mono text-xs text-gray-400">{txnId.slice(0, 20)}…</span>
              </div>
              <div className="flex justify-between text-sm items-center">
                <span className="text-gray-500 font-medium">Scor fraudă</span>
                <Badge className={fraudClass}>
                  {(fraud * 100).toFixed(1)}% — {fraud < 0.3 ? "SCĂZUT" : fraud < 0.7 ? "MEDIU" : "RIDICAT"}
                </Badge>
              </div>
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <Link href="/shop" className="flex-1">
              <Button variant="outline" className="w-full gap-2 border-gray-200 text-gray-600 hover:bg-gray-50">
                <ArrowLeft size={14} /> Cumpără din nou
              </Button>
            </Link>
            <Link href="/dashboard" className="flex-1">
              <Button className="w-full gap-2" style={{ background: "linear-gradient(135deg, #10b981, #059669)" }}>
                <LayoutDashboard size={14} /> Dashboard
              </Button>
            </Link>
          </div>
        </div>

        <p className="text-center text-xs text-gray-300 mt-6 flex items-center justify-center gap-1.5">
          <ShoppingBag size={11} />
          MiniPay Demo Shop · Proiect de Disertație
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
