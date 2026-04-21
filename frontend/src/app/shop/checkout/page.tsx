"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { gateway, toCents } from "@/lib/api/gateway";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Loader2, Lock, ShieldCheck, ArrowLeft } from "lucide-react";
import Link from "next/link";

interface CartItem { id: string; name: string; price: number; currency: string; qty: number }

export default function CheckoutPage() {
  const router = useRouter();
  const [cart, setCart] = useState<CartItem[]>([]);
  const [step, setStep] = useState<"form" | "processing" | "done">("form");

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

  const checkoutMut = useMutation({
    mutationFn: () =>
      // Gateway tokenizes PAN internally via vault-svc — send PAN directly
      gateway.authorize({
        pan: cardForm.pan,
        expiryDate: cardForm.expiry,
        cvv: cardForm.cvv,
        amount: toCents(total),          // convert to cents
        currency: "EUR",
        merchantId: "DEMO-SHOP-001",
        orderId: `SHOP-${Date.now()}`,
        description: `Demo Shop — ${cart.length} item(s)`,
      }),
    onSuccess: (data) => {
      localStorage.removeItem("mp_cart");
      router.push(`/shop/receipt?txnId=${data.txnId}&status=${data.status}&amount=${data.amount}&currency=${data.currency}&fraud=${data.fraudScore ?? 0}`);
    },
    onError: () => {
      toast.error("Payment failed — please try again");
      setStep("form");
    },
  });

  const cf = (k: keyof typeof cardForm, v: string) => setCardForm((p) => ({ ...p, [k]: v }));

  return (
    <div className="min-h-screen bg-[#0f1117] text-white">
      <header className="border-b border-white/10 px-6 py-4 flex items-center gap-4">
        <Link href="/shop" className="text-white/40 hover:text-white transition-colors">
          <ArrowLeft size={18} />
        </Link>
        <div>
          <span className="text-xl font-bold text-blue-400">Mini</span>
          <span className="text-xl font-bold">Pay</span>
          <span className="text-white/40 text-sm ml-3">Secure Checkout</span>
        </div>
        <Lock size={16} className="text-green-400 ml-auto" />
      </header>

      <div className="max-w-4xl mx-auto px-6 py-10 grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Order Summary */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 h-fit">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider mb-4">Order Summary</h2>
          <div className="space-y-3">
            {cart.map((item) => (
              <div key={item.id} className="flex items-center justify-between text-sm">
                <span className="text-white/70">{item.name} <span className="text-white/30">×{item.qty}</span></span>
                <span>€{(item.price * item.qty).toFixed(2)}</span>
              </div>
            ))}
          </div>
          <Separator className="my-4 bg-white/10" />
          <div className="flex items-center justify-between font-bold">
            <span>Total</span>
            <span className="text-xl">€{total.toFixed(2)}</span>
          </div>
          <div className="mt-4 flex items-center gap-2 text-xs text-white/30">
            <ShieldCheck size={12} className="text-green-400" />
            Powered by MiniPay Gateway · EMV Tokenized · TLS
          </div>
        </div>

        {/* Payment Form */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider mb-5">Payment Details</h2>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Cardholder Name</Label>
              <Input value={cardForm.name} onChange={(e) => cf("name", e.target.value)} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5">
              <Label>Card Number</Label>
              <Input
                value={cardForm.pan}
                onChange={(e) => cf("pan", e.target.value.replace(/\s/g, ""))}
                placeholder="1234 5678 9012 3456"
                maxLength={16}
                className="bg-white/5 border-white/10 font-mono tracking-widest"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>Expiry</Label>
                <Input value={cardForm.expiry} onChange={(e) => cf("expiry", e.target.value)} placeholder="MM/YY" className="bg-white/5 border-white/10" />
              </div>
              <div className="space-y-1.5">
                <Label>CVV</Label>
                <Input value={cardForm.cvv} onChange={(e) => cf("cvv", e.target.value)} maxLength={4} className="bg-white/5 border-white/10" />
              </div>
            </div>
          </div>

          <div className="mt-2 mb-4 flex items-center gap-2 text-xs text-white/30">
            <Lock size={10} className="text-green-400" />
            Card is tokenized via EMV vault before processing
          </div>

          <Button
            className="w-full bg-blue-600 hover:bg-blue-700 py-5 text-base font-semibold"
            onClick={() => { setStep("processing"); checkoutMut.mutate(); }}
            disabled={checkoutMut.isPending || cart.length === 0}
          >
            {checkoutMut.isPending ? (
              <><Loader2 size={18} className="animate-spin mr-2" />Processing payment…</>
            ) : (
              <>Pay €{total.toFixed(2)}</>
            )}
          </Button>

          <div className="flex items-center justify-center gap-3 mt-4">
            {["VISA", "MC", "AMEX"].map((b) => (
              <Badge key={b} className="bg-white/5 text-white/40 border-white/10 text-xs">{b}</Badge>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
