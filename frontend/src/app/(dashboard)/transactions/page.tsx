"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { gateway, AuthorizeRequest, PaymentResponse, toCents, fromCents } from "@/lib/api/gateway";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2, CheckCircle2, XCircle, Clock, RefreshCw } from "lucide-react";

const STATUS_STYLE: Record<string, string> = {
  AUTHORIZED: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
  CAPTURED:   "bg-green-500/20 text-green-400 border-green-500/30",
  REFUNDED:   "bg-purple-500/20 text-purple-400 border-purple-500/30",
  DECLINED:   "bg-red-500/20 text-red-400 border-red-500/30",
  BLOCKED:    "bg-red-500/20 text-red-400 border-red-500/30",
  CHALLENGE:  "bg-blue-500/20 text-blue-400 border-blue-500/30",
  FAILED:     "bg-red-500/20 text-red-400 border-red-500/30",
};

function FraudBadge({ score }: { score?: number }) {
  if (score == null) return null;
  const cls = score < 0.3
    ? "bg-green-500/20 text-green-400 border-green-500/30"
    : score < 0.7
    ? "bg-yellow-500/20 text-yellow-400 border-yellow-500/30"
    : "bg-red-500/20 text-red-400 border-red-500/30";
  return <Badge className={cls}>{score.toFixed(3)}</Badge>;
}

export default function TransactionsPage() {
  const [txns, setTxns] = useState<PaymentResponse[]>([]);
  const [form, setForm] = useState({
    pan: "4111111111111111",
    expiryDate: "12/28",
    cvv: "123",
    amount: 150.00,   // display value in EUR
    currency: "EUR",
    merchantId: "DEMO-MERCHANT-001",
    orderId: `ORD-${Date.now()}`,
    description: "Demo payment",
  });

  const authMut = useMutation({
    mutationFn: () => gateway.authorize({
      ...form,
      amount: toCents(form.amount),  // convert to cents
    } as AuthorizeRequest),
    onSuccess: (data) => {
      setTxns((prev) => [data, ...prev]);
      // refresh orderId for next payment
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
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Transactions</h1>
        <p className="text-white/40 text-sm mt-1">Authorize, capture and refund payments</p>
      </div>

      {/* Authorize Form */}
      <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6">
        <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider mb-5">New Payment Authorization</h2>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <div className="space-y-1.5 col-span-2">
            <Label>PAN</Label>
            <Input value={form.pan} onChange={(e) => setForm((f) => ({ ...f, pan: e.target.value }))}
              className="bg-white/5 border-white/10 font-mono" placeholder="4111111111111111" />
          </div>
          <div className="space-y-1.5">
            <Label>Expiry (MM/YY)</Label>
            <Input value={form.expiryDate} onChange={(e) => setForm((f) => ({ ...f, expiryDate: e.target.value }))}
              className="bg-white/5 border-white/10" placeholder="12/28" />
          </div>
          <div className="space-y-1.5">
            <Label>CVV</Label>
            <Input value={form.cvv} onChange={(e) => setForm((f) => ({ ...f, cvv: e.target.value }))}
              className="bg-white/5 border-white/10" placeholder="123" />
          </div>
          <div className="space-y-1.5">
            <Label>Amount (EUR)</Label>
            <Input type="number" step="0.01" value={form.amount}
              onChange={(e) => setForm((f) => ({ ...f, amount: parseFloat(e.target.value) }))}
              className="bg-white/5 border-white/10" />
          </div>
          <div className="space-y-1.5">
            <Label>Currency</Label>
            <Input value={form.currency} onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value }))}
              className="bg-white/5 border-white/10" />
          </div>
          <div className="space-y-1.5">
            <Label>Merchant ID</Label>
            <Input value={form.merchantId} onChange={(e) => setForm((f) => ({ ...f, merchantId: e.target.value }))}
              className="bg-white/5 border-white/10" />
          </div>
          <div className="space-y-1.5">
            <Label>Order ID</Label>
            <Input value={form.orderId} onChange={(e) => setForm((f) => ({ ...f, orderId: e.target.value }))}
              className="bg-white/5 border-white/10 font-mono text-xs" />
          </div>
        </div>
        <Button className="mt-5 bg-blue-600 hover:bg-blue-700" onClick={() => authMut.mutate()} disabled={authMut.isPending}>
          {authMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Authorizing…</> : "Authorize Payment"}
        </Button>
      </div>

      {/* Transactions table */}
      {txns.length > 0 ? (
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] overflow-hidden">
          <div className="px-5 py-4 border-b border-white/10">
            <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Transactions ({txns.length})</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-white/30 text-xs uppercase tracking-wider border-b border-white/5">
                  <th className="text-left px-5 py-3">ID</th>
                  <th className="text-left px-5 py-3">Amount</th>
                  <th className="text-left px-5 py-3">Status</th>
                  <th className="text-left px-5 py-3">Fraud</th>
                  <th className="text-left px-5 py-3">Auth Code</th>
                  <th className="text-left px-5 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {txns.map((t) => (
                  <tr key={t.txnId} className="border-b border-white/5 last:border-0">
                    <td className="px-5 py-3 font-mono text-white/50 text-xs">{t.txnId?.slice(0, 8)}…</td>
                    <td className="px-5 py-3 font-semibold">{fromCents(t.amount)} {t.currency}</td>
                    <td className="px-5 py-3"><Badge className={STATUS_STYLE[t.status] ?? ""}>{t.status}</Badge></td>
                    <td className="px-5 py-3"><FraudBadge score={t.fraudScore} /></td>
                    <td className="px-5 py-3 font-mono text-xs text-white/40">{t.authCode ?? "—"}</td>
                    <td className="px-5 py-3">
                      <div className="flex gap-2">
                        {t.status === "AUTHORIZED" && (
                          <Button size="sm" variant="outline"
                            className="border-green-500/40 text-green-400 hover:bg-green-500/10 h-7 text-xs"
                            onClick={() => captureMut.mutate(t)} disabled={captureMut.isPending}>
                            <CheckCircle2 size={12} className="mr-1" />Capture
                          </Button>
                        )}
                        {t.status === "CAPTURED" && (
                          <Button size="sm" variant="outline"
                            className="border-purple-500/40 text-purple-400 hover:bg-purple-500/10 h-7 text-xs"
                            onClick={() => refundMut.mutate(t)} disabled={refundMut.isPending}>
                            <RefreshCw size={12} className="mr-1" />Refund
                          </Button>
                        )}
                        {(t.status === "REFUNDED" || t.status === "DECLINED" || t.status === "BLOCKED") && (
                          <span className="text-white/20 text-xs flex items-center gap-1">
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
        <div className="rounded-xl border border-dashed border-white/10 p-12 text-center">
          <Clock size={32} className="mx-auto text-white/20 mb-3" />
          <p className="text-white/30 text-sm">No transactions yet. Authorize a payment above.</p>
        </div>
      )}
    </div>
  );
}
