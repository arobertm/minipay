"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { gateway, AuthorizeRequest, PaymentResponse, toCents, fromCents } from "@/lib/api/gateway";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2, CheckCircle2, XCircle, Clock, RefreshCw, CreditCard } from "lucide-react";

const STATUS_STYLE: Record<string, string> = {
  AUTHORIZED: "bg-amber-500/20 text-amber-400 border-amber-500/30",
  CAPTURED:   "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  REFUNDED:   "bg-purple-500/20 text-purple-400 border-purple-500/30",
  DECLINED:   "bg-red-500/20 text-red-400 border-red-500/30",
  BLOCKED:    "bg-red-500/20 text-red-400 border-red-500/30",
  CHALLENGE:  "bg-cyan-500/20 text-cyan-400 border-cyan-500/30",
  FAILED:     "bg-red-500/20 text-red-400 border-red-500/30",
};

function FraudBadge({ score }: { score?: number }) {
  if (score == null) return null;
  const cls = score < 0.3
    ? "bg-emerald-500/20 text-emerald-400 border-emerald-500/30"
    : score < 0.7
    ? "bg-amber-500/20 text-amber-400 border-amber-500/30"
    : "bg-red-500/20 text-red-400 border-red-500/30";
  return <Badge className={cls}>{score.toFixed(3)}</Badge>;
}

export default function TransactionsPage() {
  const [txns, setTxns] = useState<PaymentResponse[]>([]);
  const [form, setForm] = useState({
    pan: "4111111111111111",
    expiryDate: "12/28",
    cvv: "123",
    amount: 150.00,
    currency: "EUR",
    merchantId: "DEMO-MERCHANT-001",
    orderId: `ORD-${Date.now()}`,
    description: "Demo payment",
  });

  const authMut = useMutation({
    mutationFn: () => gateway.authorize({ ...form, amount: toCents(form.amount) } as AuthorizeRequest),
    onSuccess: (data) => {
      setTxns((prev) => [data, ...prev]);
      setForm((f) => ({ ...f, orderId: `ORD-${Date.now()}` }));
      toast.success(`Payment ${data.status} — ${data.txnId?.slice(0, 8)}…`);
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Authorization failed";
      toast.error(msg);
    },
  });

  const captureMut = useMutation({
    mutationFn: (t: PaymentResponse) => gateway.capture(t.txnId, t.amount, t.currency),
    onSuccess: (data) => {
      setTxns((prev) => prev.map((t) => (t.txnId === data.txnId ? data : t)));
      toast.success("Payment captured");
    },
    onError: () => toast.error("Capture failed"),
  });

  const refundMut = useMutation({
    mutationFn: (t: PaymentResponse) => gateway.refund(t.txnId, t.amount),
    onSuccess: (data) => {
      setTxns((prev) => prev.map((t) => (t.txnId === data.txnId ? data : t)));
      toast.success("Payment refunded");
    },
    onError: () => toast.error("Refund failed"),
  });

  return (
    <div className="space-y-8 p-6">
      <div className="fadeIn">
        <h1 className="text-4xl font-display font-bold text-foreground">Tranzacții</h1>
        <p className="text-foreground/60 text-base mt-2 font-medium">Autorizare, capturare și rambursare plăți</p>
      </div>

      {/* Authorize Form */}
      <div className="card-premium fadeIn stagger-1">
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 rounded-lg bg-primary/20">
            <CreditCard size={18} className="text-primary" />
          </div>
          <h2 className="text-base font-display font-semibold">Autorizare plată nouă</h2>
        </div>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <div className="space-y-2 col-span-2">
            <Label className="text-foreground/70 font-medium">PAN</Label>
            <Input value={form.pan} onChange={(e) => setForm((f) => ({ ...f, pan: e.target.value }))}
              className="font-mono" placeholder="4111111111111111" />
          </div>
          <div className="space-y-2">
            <Label className="text-foreground/70 font-medium">Expirare (LL/AA)</Label>
            <Input value={form.expiryDate} onChange={(e) => setForm((f) => ({ ...f, expiryDate: e.target.value }))} placeholder="12/28" />
          </div>
          <div className="space-y-2">
            <Label className="text-foreground/70 font-medium">CVV</Label>
            <Input value={form.cvv} onChange={(e) => setForm((f) => ({ ...f, cvv: e.target.value }))} placeholder="123" />
          </div>
          <div className="space-y-2">
            <Label className="text-foreground/70 font-medium">Sumă (RON)</Label>
            <Input type="number" step="0.01" value={form.amount}
              onChange={(e) => setForm((f) => ({ ...f, amount: parseFloat(e.target.value) }))} />
          </div>
          <div className="space-y-2">
            <Label className="text-foreground/70 font-medium">Currency</Label>
            <Input value={form.currency} onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label className="text-foreground/70 font-medium">Merchant ID</Label>
            <Input value={form.merchantId} onChange={(e) => setForm((f) => ({ ...f, merchantId: e.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label className="text-foreground/70 font-medium">Order ID</Label>
            <Input value={form.orderId} onChange={(e) => setForm((f) => ({ ...f, orderId: e.target.value }))} className="font-mono text-xs" />
          </div>
        </div>
        <Button className="mt-6" onClick={() => authMut.mutate()} disabled={authMut.isPending}>
          {authMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Se autorizează…</> : "Autorizează plata"}
        </Button>
      </div>

      {/* Transactions table */}
      {txns.length > 0 ? (
        <div className="card-premium p-0 overflow-hidden fadeIn stagger-2">
          <div className="px-6 py-4 border-b border-border/40 flex items-center justify-between">
            <h2 className="text-base font-display font-semibold">Transactions</h2>
            <span className="text-xs font-medium px-2.5 py-1 rounded-full bg-primary/20 text-primary">{txns.length} total</span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-foreground/40 text-xs font-semibold uppercase tracking-widest border-b border-border/30">
                  <th className="text-left px-6 py-3">ID Tranzacție</th>
                  <th className="text-left px-6 py-3">Sumă</th>
                  <th className="text-left px-6 py-3">Status</th>
                  <th className="text-left px-6 py-3">Fraudă</th>
                  <th className="text-left px-6 py-3">Cod Auth</th>
                  <th className="text-left px-6 py-3">Acțiuni</th>
                </tr>
              </thead>
              <tbody>
                {txns.map((t) => (
                  <tr key={t.txnId} className="border-b border-border/20 last:border-0 hover:bg-foreground/3 transition-colors duration-150">
                    <td className="px-6 py-4 font-mono text-foreground/50 text-xs">{t.txnId?.slice(0, 8)}…</td>
                    <td className="px-6 py-4 font-semibold">{fromCents(t.amount)} {t.currency}</td>
                    <td className="px-6 py-4"><Badge className={STATUS_STYLE[t.status] ?? ""}>{t.status}</Badge></td>
                    <td className="px-6 py-4"><FraudBadge score={t.fraudScore} /></td>
                    <td className="px-6 py-4 font-mono text-xs text-foreground/40">{t.authCode ?? "—"}</td>
                    <td className="px-6 py-4">
                      <div className="flex gap-2">
                        {t.status === "AUTHORIZED" && (
                          <Button size="sm" variant="outline"
                            className="border-emerald-500/40 text-emerald-400 hover:bg-emerald-500/10 text-xs"
                            onClick={() => captureMut.mutate(t)} disabled={captureMut.isPending}>
                            <CheckCircle2 size={12} className="mr-1" />Capturează
                          </Button>
                        )}
                        {t.status === "CAPTURED" && (
                          <Button size="sm" variant="outline"
                            className="border-purple-500/40 text-purple-400 hover:bg-purple-500/10 text-xs"
                            onClick={() => refundMut.mutate(t)} disabled={refundMut.isPending}>
                            <RefreshCw size={12} className="mr-1" />Rambursează
                          </Button>
                        )}
                        {(t.status === "REFUNDED" || t.status === "DECLINED" || t.status === "BLOCKED") && (
                          <span className="text-foreground/30 text-xs flex items-center gap-1">
                            <XCircle size={12} />{t.declineReason ?? t.status}
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="card-premium border-dashed flex flex-col items-center justify-center py-16 fadeIn stagger-2">
          <div className="w-14 h-14 rounded-full bg-foreground/5 flex items-center justify-center mb-4">
            <Clock size={24} className="text-foreground/30" />
          </div>
          <p className="text-foreground/40 text-sm font-medium">Nicio tranzacție încă</p>
          <p className="text-foreground/30 text-xs mt-1">Autorizează o plată de mai sus pentru a începe</p>
        </div>
      )}
    </div>
  );
}
