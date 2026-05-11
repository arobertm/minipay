"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { gateway, toCents } from "@/lib/api/gateway";
import { api } from "@/lib/api/axios";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Loader2, Lock, ShieldCheck, ArrowLeft, CreditCard, RotateCcw } from "lucide-react";
import Link from "next/link";

interface CartItem { id: string; name: string; price: number; currency: string; qty: number }

export default function CheckoutPage() {
  const router = useRouter();
  const [cart, setCart] = useState<CartItem[]>([]);

  const [cardForm, setCardForm] = useState({
    pan: "4111111111111111",
    expiry: "12/28",
    cvv: "123",
    name: "Ion Popescu",
  });

  useEffect(() => {
    const raw = localStorage.getItem("mp_cart");
    if (raw) setCart(JSON.parse(raw));
  }, []);

  const total = cart.reduce((sum, i) => sum + i.price * i.qty, 0);

  const resetMut = useMutation({
    mutationFn: () => api.post("/issuer/admin/reset-cards").then((r) => r.data),
    onSuccess: () => toast.success("Test card balances reset to 5000 RON"),
    onError: () => toast.error("Reset failed — check backend logs"),
  });

  const checkoutMut = useMutation({
    mutationFn: () =>
      gateway.authorize({
        pan: cardForm.pan,
        expiryDate: cardForm.expiry,
        cvv: cardForm.cvv,
        amount: toCents(total),
        currency: "RON",
        merchantId: "DEMO-SHOP-001",
        orderId: `SHOP-${Date.now()}`,
        description: `Demo Shop — ${cart.length} item(s)`,
      }),
    onSuccess: (data) => {
      localStorage.removeItem("mp_cart");
      router.push(`/shop/receipt?txnId=${data.txnId}&status=${data.status}&amount=${data.amount}&currency=${data.currency}&fraud=${data.fraudScore ?? 0}`);
    },
    onError: () => toast.error("Payment failed — please try again"),
  });

  const cf = (k: keyof typeof cardForm, v: string) => setCardForm((p) => ({ ...p, [k]: v }));

  return (
    <div className="min-h-screen text-gray-900" style={{ background: "#f8f9fb" }}>
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-gray-200 bg-white/95 px-6 py-4 flex items-center gap-4" style={{ backdropFilter: "blur(12px)" }}>
        <Link href="/shop" className="p-2 rounded-lg hover:bg-gray-100 transition-colors text-gray-400 hover:text-gray-700">
          <ArrowLeft size={18} />
        </Link>
        <div>
          <span className="text-xl font-display font-bold text-primary">Mini</span>
          <span className="text-xl font-display font-bold text-gray-900">Pay</span>
          <span className="text-gray-400 text-sm ml-3 font-medium">Checkout securizat</span>
        </div>
        <div className="ml-auto flex items-center gap-2 text-xs text-emerald-600 font-medium">
          <Lock size={14} />
          <span>Securizat · TLS</span>
        </div>
      </header>

      <div className="max-w-4xl mx-auto px-6 py-10 grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Order Summary */}
        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm h-fit fadeIn stagger-1">
          <h2 className="text-base font-display font-semibold mb-5 text-gray-900">Sumar comandă</h2>
          <div className="space-y-3">
            {cart.map((item) => (
              <div key={item.id} className="flex items-center justify-between text-sm">
                <span className="text-gray-600">
                  {item.name}
                  <span className="text-gray-400 ml-1">×{item.qty}</span>
                </span>
                <span className="font-semibold text-gray-900">{(item.price * item.qty).toLocaleString("ro-RO")} lei</span>
              </div>
            ))}
          </div>
          <Separator className="my-5 bg-gray-100" />
          <div className="flex items-center justify-between font-bold">
            <span className="text-gray-500">Total</span>
            <span className="text-2xl font-display text-gray-900">{total.toLocaleString("ro-RO")} lei</span>
          </div>
          <div className="mt-5 p-3 rounded-lg bg-emerald-50 border border-emerald-200 flex items-center gap-2 text-xs text-emerald-700 font-medium">
            <ShieldCheck size={14} />
            MiniPay Gateway · EMV Tokenizat · Criptat TLS
          </div>
        </div>

        {/* Payment Form */}
        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm fadeIn stagger-2">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 rounded-lg bg-emerald-50">
              <CreditCard size={18} className="text-primary" />
            </div>
            <h2 className="text-base font-display font-semibold text-gray-900">Date de plată</h2>
          </div>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label className="text-gray-600 font-medium text-sm">Titular card</Label>
              <Input value={cardForm.name} onChange={(e) => cf("name", e.target.value)}
                className="border-gray-200 bg-white text-gray-900 focus-visible:ring-primary/30" />
            </div>
            <div className="space-y-2">
              <Label className="text-gray-600 font-medium text-sm">Număr card</Label>
              <Input
                value={cardForm.pan}
                onChange={(e) => cf("pan", e.target.value.replace(/\s/g, ""))}
                placeholder="1234 5678 9012 3456"
                maxLength={16}
                className="font-mono tracking-widest border-gray-200 bg-white text-gray-900 focus-visible:ring-primary/30"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label className="text-gray-600 font-medium text-sm">Expirare</Label>
                <Input value={cardForm.expiry} onChange={(e) => cf("expiry", e.target.value)} placeholder="MM/AA"
                  className="border-gray-200 bg-white text-gray-900 focus-visible:ring-primary/30" />
              </div>
              <div className="space-y-2">
                <Label className="text-gray-600 font-medium text-sm">CVV</Label>
                <Input value={cardForm.cvv} onChange={(e) => cf("cvv", e.target.value)} maxLength={4}
                  className="border-gray-200 bg-white text-gray-900 focus-visible:ring-primary/30" />
              </div>
            </div>
          </div>

          <div className="mt-3 mb-5 flex items-center gap-2 text-xs text-gray-400">
            <Lock size={11} className="text-primary" />
            Cardul este tokenizat prin EMV vault înainte de procesare
          </div>

          <Button
            className="w-full h-12 text-base font-semibold shadow-lg shadow-primary/20"
            onClick={() => checkoutMut.mutate()}
            disabled={checkoutMut.isPending || cart.length === 0}
          >
            {checkoutMut.isPending ? (
              <><Loader2 size={18} className="animate-spin mr-2" />Se procesează…</>
            ) : (
              <>Plătește {total.toLocaleString("ro-RO")} lei</>
            )}
          </Button>

          <div className="flex items-center justify-center gap-3 mt-5">
            {["VISA", "MC", "AMEX"].map((b) => (
              <Badge key={b} className="bg-gray-100 text-gray-400 border-gray-200 text-xs font-semibold">{b}</Badge>
            ))}
          </div>

          <div className="mt-6 pt-5 border-t border-dashed border-gray-200">
            <p className="text-[10px] text-gray-400 font-mono mb-2 uppercase tracking-wider">Dev Tools</p>
            <Button
              variant="outline"
              size="sm"
              className="w-full text-xs text-gray-400 border-gray-200 hover:border-amber-300 hover:text-amber-600 gap-2"
              onClick={() => resetMut.mutate()}
              disabled={resetMut.isPending}
            >
              {resetMut.isPending
                ? <><Loader2 size={12} className="animate-spin" />Se resetează…</>
                : <><RotateCcw size={12} />Reset solduri carduri test (→ 5000 RON)</>}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
