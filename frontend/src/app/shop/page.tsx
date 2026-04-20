"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ShoppingCart, Plus, Minus, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

const PRODUCTS = [
  { id: "p1", name: "Wireless Headphones", price: 89.99, currency: "EUR", category: "Electronics" },
  { id: "p2", name: "Mechanical Keyboard", price: 149.00, currency: "EUR", category: "Electronics" },
  { id: "p3", name: "USB-C Hub", price: 45.50, currency: "EUR", category: "Accessories" },
  { id: "p4", name: "Laptop Stand", price: 59.99, currency: "EUR", category: "Accessories" },
  { id: "p5", name: "Webcam HD", price: 79.00, currency: "EUR", category: "Electronics" },
  { id: "p6", name: "LED Desk Lamp", price: 35.00, currency: "EUR", category: "Office" },
];

interface CartItem { id: string; name: string; price: number; currency: string; qty: number }

export default function ShopPage() {
  const router = useRouter();
  const [cart, setCart] = useState<CartItem[]>([]);

  function addToCart(p: typeof PRODUCTS[0]) {
    setCart((c) => {
      const existing = c.find((i) => i.id === p.id);
      if (existing) return c.map((i) => i.id === p.id ? { ...i, qty: i.qty + 1 } : i);
      return [...c, { ...p, qty: 1 }];
    });
  }

  function removeFromCart(id: string) {
    setCart((c) => {
      const existing = c.find((i) => i.id === id);
      if (!existing) return c;
      if (existing.qty === 1) return c.filter((i) => i.id !== id);
      return c.map((i) => i.id === id ? { ...i, qty: i.qty - 1 } : i);
    });
  }

  const total = cart.reduce((sum, i) => sum + i.price * i.qty, 0);
  const cartCount = cart.reduce((sum, i) => sum + i.qty, 0);

  function goToCheckout() {
    localStorage.setItem("mp_cart", JSON.stringify(cart));
    router.push("/shop/checkout");
  }

  return (
    <div className="min-h-screen bg-[#0f1117] text-white">
      {/* Header */}
      <header className="border-b border-white/10 px-6 py-4 flex items-center justify-between">
        <div>
          <span className="text-xl font-bold text-blue-400">Mini</span>
          <span className="text-xl font-bold">Pay</span>
          <span className="text-white/40 text-sm ml-3">Demo Shop</span>
        </div>
        <div className="flex items-center gap-4">
          <Link href="/dashboard" className="text-sm text-white/40 hover:text-white transition-colors">Dashboard →</Link>
          {cartCount > 0 && (
            <Button className="bg-blue-600 hover:bg-blue-700" onClick={goToCheckout}>
              <ShoppingCart size={16} className="mr-2" />
              Checkout ({cartCount}) — €{total.toFixed(2)}
            </Button>
          )}
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold mb-2">Products</h1>
        <p className="text-white/40 text-sm mb-8">Payments powered by <span className="text-blue-400">MiniPay Gateway</span></p>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {PRODUCTS.map((p) => {
            const inCart = cart.find((i) => i.id === p.id);
            return (
              <div key={p.id} className="rounded-xl border border-white/10 bg-[#1a1d27] p-5 flex flex-col">
                <div className="h-28 rounded-lg bg-white/5 mb-4 flex items-center justify-center">
                  <ShoppingCart size={32} className="text-white/20" />
                </div>
                <Badge className="bg-blue-500/10 text-blue-400 border-blue-500/20 self-start mb-2 text-xs">{p.category}</Badge>
                <h3 className="font-medium mb-1">{p.name}</h3>
                <p className="text-xl font-bold text-white mt-auto pt-3">€{p.price.toFixed(2)}</p>
                {!inCart ? (
                  <Button className="mt-3 bg-blue-600 hover:bg-blue-700 w-full" onClick={() => addToCart(p)}>
                    Add to Cart
                  </Button>
                ) : (
                  <div className="mt-3 flex items-center justify-between rounded-lg border border-white/10 bg-white/5 p-1">
                    <button onClick={() => removeFromCart(p.id)} className="p-2 hover:bg-white/10 rounded-md transition-colors">
                      <Minus size={14} />
                    </button>
                    <span className="font-bold">{inCart.qty}</span>
                    <button onClick={() => addToCart(p)} className="p-2 hover:bg-white/10 rounded-md transition-colors">
                      <Plus size={14} />
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {cartCount > 0 && (
          <div className="fixed bottom-6 right-6 z-50">
            <Button className="bg-blue-600 hover:bg-blue-700 shadow-2xl px-6 py-3" onClick={goToCheckout}>
              <ShoppingCart size={18} className="mr-2" />
              Checkout {cartCount} items — €{total.toFixed(2)}
              <ArrowRight size={16} className="ml-2" />
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
