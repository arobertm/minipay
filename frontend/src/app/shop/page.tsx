"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ShoppingCart, Plus, Minus, ArrowRight, ShieldCheck, Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const PRODUCTS = [
  {
    id: "p1",
    name: "Sony WH-1000XM5",
    subtitle: "Căști wireless cu anulare activă a zgomotului",
    price: 449,
    originalPrice: 649,
    currency: "RON",
    category: "Audio",
    rating: 4.8,
    reviews: 2847,
    badge: "Cel mai vândut",
    badgeColor: "bg-amber-500 text-white",
    inStock: true,
    image: "https://i.rtings.com/assets/products/Nc33W9lA/sony-wh-1000xm5-wireless/design-medium.jpg?format=auto",
  },
  {
    id: "p2",
    name: "Keychron K8 Pro",
    subtitle: "Tastatură mecanică tenkeyless",
    price: 749,
    originalPrice: null,
    currency: "RON",
    category: "Periferice",
    rating: 4.7,
    reviews: 1243,
    badge: "Nou",
    badgeColor: "bg-primary text-primary-foreground",
    inStock: true,
    image: "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=800&q=90&auto=format&fit=crop",
  },
  {
    id: "p3",
    name: "Anker USB-C Hub 12-în-1",
    subtitle: "4K HDMI · 100W PD · Cititor SD",
    price: 229,
    originalPrice: 299,
    currency: "RON",
    category: "Accesorii",
    rating: 4.6,
    reviews: 3102,
    badge: "Reducere",
    badgeColor: "bg-red-500 text-white",
    inStock: true,
    image: "https://cdn.fstoppers.com/styles/article_medium/s3/media/2022/08/12/anker_555_usb-c_hub_8-in-1_review_2_copy.jpg?itok=Dfz2G9ig",
  },
  {
    id: "p4",
    name: "Nexstand K2 Laptop Stand",
    subtitle: "Suport aluminiu pentru laptop",
    price: 299,
    originalPrice: null,
    currency: "RON",
    category: "Birou",
    rating: 4.9,
    reviews: 892,
    badge: null,
    badgeColor: "",
    inStock: true,
    image: "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&q=90&auto=format&fit=crop",
  },
  {
    id: "p5",
    name: "Logitech Brio 4K",
    subtitle: "Cameră web Ultra HD · HDR · 90fps",
    price: 399,
    originalPrice: 599,
    currency: "RON",
    category: "Video",
    rating: 4.5,
    reviews: 1567,
    badge: "Reducere",
    badgeColor: "bg-red-500 text-white",
    inStock: true,
    image: "https://static0.xdaimages.com/wordpress/wp-content/uploads/wm/2024/03/logitech-mx-brio-8.jpg",
  },
  {
    id: "p6",
    name: "BenQ ScreenBar Halo",
    subtitle: "Lampă LED pentru monitor",
    price: 479,
    originalPrice: null,
    currency: "RON",
    category: "Iluminat",
    rating: 4.7,
    reviews: 445,
    badge: null,
    badgeColor: "",
    inStock: true,
    image: "https://static0.pocketlintimages.com/wordpress/wp-content/uploads/2023/04/benq-screenbar-halo-light.jpg?w=1600&h=1200&fit=crop",
  },
  {
    id: "p7",
    name: "Apple AirPods Pro 2",
    subtitle: "Căști in-ear cu ANC adaptiv",
    price: 999,
    originalPrice: 1249,
    currency: "RON",
    category: "Audio",
    rating: 4.9,
    reviews: 5632,
    badge: "Top picks",
    badgeColor: "bg-purple-500 text-white",
    inStock: true,
    image: "https://images.unsplash.com/photo-1588423771073-b8903fbb85b5?w=800&q=90&auto=format&fit=crop",
  },
  {
    id: "p8",
    name: "Logitech MX Master 3S",
    subtitle: "Mouse wireless ergonomic · 8K DPI",
    price: 449,
    originalPrice: 549,
    currency: "RON",
    category: "Periferice",
    rating: 4.8,
    reviews: 4210,
    badge: "Reducere",
    badgeColor: "bg-red-500 text-white",
    inStock: true,
    image: "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&q=90&auto=format&fit=crop",
  },
  {
    id: "p9",
    name: "Samsung 27\" 4K Monitor",
    subtitle: "IPS · 144Hz · HDR600 · USB-C",
    price: 1749,
    originalPrice: 2149,
    currency: "RON",
    category: "Monitoare",
    rating: 4.6,
    reviews: 978,
    badge: "Reducere",
    badgeColor: "bg-red-500 text-white",
    inStock: true,
    image: "https://img.us.news.samsung.com/us/wp-content/uploads/2022/06/16130948/ViewFinity-S8-3.jpg",
  },
  {
    id: "p10",
    name: "Samsung T7 SSD 1TB",
    subtitle: "SSD extern portabil · 1050 MB/s",
    price: 449,
    originalPrice: 599,
    currency: "RON",
    category: "Stocare",
    rating: 4.8,
    reviews: 7340,
    badge: "Cel mai vândut",
    badgeColor: "bg-amber-500 text-white",
    inStock: true,
    image: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSm1pXRhdSg2yEGMXcTiJmXJYnTpwWey3dXcw&s",
  },
  {
    id: "p11",
    name: "Apple Watch Series 9",
    subtitle: "Smartwatch GPS · Always-On · 45mm",
    price: 2149,
    originalPrice: null,
    currency: "RON",
    category: "Wearables",
    rating: 4.7,
    reviews: 3890,
    badge: "Nou",
    badgeColor: "bg-primary text-primary-foreground",
    inStock: true,
    image: "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=800&q=90&auto=format&fit=crop",
  },
  {
    id: "p12",
    name: "iPhone 15 Pro",
    subtitle: "Titan · A17 Pro · ProRes Video",
    price: 5749,
    originalPrice: null,
    currency: "RON",
    category: "Telefoane",
    rating: 4.9,
    reviews: 12045,
    badge: "Premium",
    badgeColor: "bg-foreground text-background",
    inStock: false,
    image: "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800&q=90&auto=format&fit=crop",
  },
];

const CATEGORIES = ["Toate", "Audio", "Periferice", "Accesorii", "Birou", "Video", "Iluminat", "Monitoare", "Stocare", "Wearables", "Telefoane"];

interface CartItem { id: string; name: string; price: number; currency: string; qty: number }

function StarRating({ rating }: { rating: number }) {
  return (
    <div className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((i) => (
        <Star key={i} size={11}
          className={i <= Math.round(rating) ? "text-amber-400 fill-amber-400" : "text-foreground/20 fill-foreground/10"}
        />
      ))}
    </div>
  );
}

export default function ShopPage() {
  const router = useRouter();
  const [cart, setCart] = useState<CartItem[]>([]);
  const [activeCategory, setActiveCategory] = useState("Toate");

  function addToCart(p: typeof PRODUCTS[0]) {
    if (!p.inStock) return;
    setCart((c) => {
      const existing = c.find((i) => i.id === p.id);
      if (existing) return c.map((i) => i.id === p.id ? { ...i, qty: i.qty + 1 } : i);
      return [...c, { id: p.id, name: p.name, price: p.price, currency: p.currency, qty: 1 }];
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

  const filtered = activeCategory === "Toate"
    ? PRODUCTS
    : PRODUCTS.filter((p) => p.category === activeCategory);

  function goToCheckout() {
    localStorage.setItem("mp_cart", JSON.stringify(cart));
    router.push("/shop/checkout");
  }

  return (
    <div className="min-h-screen text-gray-900" style={{ background: "#f8f9fb" }}>
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-gray-200 bg-white/95" style={{ backdropFilter: "blur(12px)" }}>
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-6">
            <span className="text-xl font-bold text-gray-900">MiniStore</span>
            <nav className="hidden md:flex items-center gap-5 text-sm font-medium text-gray-400">
              <span className="text-gray-900 cursor-default">Produse</span>
              <span className="hover:text-gray-700 cursor-pointer transition-colors">Oferte</span>
              <span className="hover:text-gray-700 cursor-pointer transition-colors">Suport</span>
            </nav>
          </div>

          <div className="flex items-center gap-3">
            <Link href="/dashboard"
              className="text-sm text-gray-400 hover:text-gray-700 transition-colors font-medium hidden sm:block">
              ← Dashboard
            </Link>
            <button
              onClick={cartCount > 0 ? goToCheckout : undefined}
              className="relative p-2.5 rounded-xl border border-gray-200 hover:bg-gray-50 transition-colors"
            >
              <ShoppingCart size={19} className="text-gray-500" />
              {cartCount > 0 && (
                <span className="absolute -top-1.5 -right-1.5 w-5 h-5 bg-primary text-primary-foreground text-[10px] font-bold rounded-full flex items-center justify-center">
                  {cartCount}
                </span>
              )}
            </button>
            {cartCount > 0 && (
              <Button onClick={goToCheckout} className="hidden sm:flex gap-2">
                Comandă · {total.toLocaleString("ro-RO")} lei
                <ArrowRight size={14} />
              </Button>
            )}
          </div>
        </div>
      </header>

      {/* Green gradient bar above banner */}
      <div className="h-1" style={{ background: "linear-gradient(90deg, #10b981 0%, #06b6d4 50%, #10b981 100%)", backgroundSize: "200% 100%", animation: "gradientSlide 4s linear infinite" }} />
      <style>{`@keyframes gradientSlide { 0% { background-position: 0% 0%; } 100% { background-position: 200% 0%; } }`}</style>

      {/* Hero Banner */}
      <div className="relative overflow-hidden" style={{ background: "#0a0f1a" }}>
        {/* Animated green glow — stânga */}
        <div className="absolute -top-32 -left-32 w-[500px] h-[500px] rounded-full pointer-events-none"
          style={{ background: "radial-gradient(circle, rgba(16,185,129,0.18) 0%, transparent 65%)", animation: "heroPulseA 6s ease-in-out infinite" }} />
        {/* Animated cyan glow — dreapta */}
        <div className="absolute -bottom-32 -right-32 w-[500px] h-[500px] rounded-full pointer-events-none"
          style={{ background: "radial-gradient(circle, rgba(6,182,212,0.13) 0%, transparent 65%)", animation: "heroPulseB 8s ease-in-out infinite" }} />
        {/* Glow central subtil */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-40 pointer-events-none"
          style={{ background: "radial-gradient(ellipse, rgba(16,185,129,0.06) 0%, transparent 70%)", animation: "heroPulseA 10s ease-in-out infinite reverse" }} />
        {/* Dot grid */}
        <div className="absolute inset-0 opacity-[0.05] pointer-events-none" style={{
          backgroundImage: "radial-gradient(circle, #fff 1px, transparent 1px)",
          backgroundSize: "28px 28px"
        }} />
        {/* Linie gradient jos */}
        <div className="absolute bottom-0 left-0 right-0 h-px" style={{ background: "linear-gradient(90deg, transparent, rgba(16,185,129,0.4), rgba(6,182,212,0.3), transparent)" }} />

        <style>{`
          @keyframes heroPulseA {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.6; transform: scale(1.12); }
          }
          @keyframes heroPulseB {
            0%, 100% { opacity: 0.8; transform: scale(1.05); }
            50% { opacity: 1; transform: scale(0.92); }
          }
        `}</style>

        <div className="relative z-10 max-w-7xl mx-auto px-6 py-16">
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full border mb-5 bg-emerald-500/10 text-emerald-400 border-emerald-500/20">
            ✦ Bun venit în magazinul nostru
          </span>
          <h1 className="text-6xl font-bold text-white leading-tight tracking-tight">
            Gadgeturi <span style={{ background: "linear-gradient(90deg, #10b981, #06b6d4)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>&</span> Tech
          </h1>
          <p className="text-slate-400 mt-4 text-lg max-w-lg">
            Cele mai bune produse tech — livrare rapidă, prețuri imbatabile
          </p>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-8">
        {/* Categorii */}
        <div className="flex items-center gap-2 flex-wrap mb-8 overflow-x-auto pb-1">
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className={cn(
                "px-4 py-2 rounded-full text-sm font-medium border transition-all duration-200 whitespace-nowrap",
                activeCategory === cat
                  ? "bg-primary text-primary-foreground border-primary shadow-md shadow-primary/20"
                  : "border-gray-200 text-gray-500 hover:text-gray-800 bg-white hover:border-gray-300 shadow-sm"
              )}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* Grid produse */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {filtered.map((p, idx) => {
            const inCart = cart.find((i) => i.id === p.id);
            const discount = p.originalPrice
              ? Math.round((1 - p.price / p.originalPrice) * 100)
              : null;

            return (
              <div
                key={p.id}
                className={cn(
                  "group rounded-2xl border border-gray-200 bg-white overflow-hidden transition-all duration-300 hover:shadow-xl hover:shadow-gray-200 hover:-translate-y-1 flex flex-col fadeIn",
                  !p.inStock && "opacity-60"
                )}
                style={{ animationDelay: `${idx * 50}ms` }}
              >
                {/* Imagine */}
                <div className="relative h-48 overflow-hidden shrink-0">
                  <Image
                    src={p.image}
                    alt={p.name}
                    fill
                    className="object-cover group-hover:scale-105 transition-transform duration-500"
                    unoptimized
                  />
                  <div className="absolute top-2.5 left-2.5 flex gap-1.5">
                    {p.badge && (
                      <span className={cn("text-[11px] font-bold px-2 py-0.5 rounded-full", p.badgeColor)}>
                        {p.badge}
                      </span>
                    )}
                    {discount && (
                      <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-red-500 text-white">
                        -{discount}%
                      </span>
                    )}
                  </div>
                  {!p.inStock && (
                    <div className="absolute inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center">
                      <span className="text-sm font-semibold text-gray-500 bg-white px-4 py-2 rounded-full border border-gray-200 shadow-sm">
                        Stoc epuizat
                      </span>
                    </div>
                  )}
                </div>

                {/* Continut */}
                <div className="p-4 flex flex-col flex-1">
                  <span className="text-[10px] font-semibold text-gray-400 uppercase tracking-widest mb-1">{p.category}</span>
                  <h3 className="font-semibold text-gray-900 text-sm leading-snug">{p.name}</h3>
                  <p className="text-xs text-gray-500 mt-0.5 leading-relaxed">{p.subtitle}</p>

                  {/* Rating */}
                  <div className="flex items-center gap-1.5 mt-2.5">
                    <StarRating rating={p.rating} />
                    <span className="text-[11px] text-gray-400">{p.rating} ({p.reviews.toLocaleString("ro-RO")})</span>
                  </div>

                  {/* Pret */}
                  <div className="flex items-baseline gap-2 mt-3 mb-4">
                    <span className="text-xl font-bold text-gray-900">{p.price.toLocaleString("ro-RO")} lei</span>
                    {p.originalPrice && (
                      <span className="text-xs text-gray-400 line-through">{p.originalPrice.toLocaleString("ro-RO")} lei</span>
                    )}
                  </div>

                  {/* Buton */}
                  <div className="mt-auto">
                    {!p.inStock ? (
                      <button disabled className="w-full py-2 rounded-xl border border-gray-200 text-xs text-gray-400 font-medium cursor-not-allowed">
                        Stoc epuizat
                      </button>
                    ) : !inCart ? (
                      <Button className="w-full gap-2 text-sm" onClick={() => addToCart(p)}>
                        <ShoppingCart size={14} />
                        Adaugă în coș
                      </Button>
                    ) : (
                      <div className="flex items-center justify-between rounded-xl border border-primary/40 bg-primary/10 px-2 py-1.5">
                        <button onClick={() => removeFromCart(p.id)}
                          className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/20 transition-colors">
                          <Minus size={14} />
                        </button>
                        <span className="text-sm font-bold text-gray-900">{inCart.qty} în coș</span>
                        <button onClick={() => addToCart(p)}
                          className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/20 transition-colors">
                          <Plus size={14} />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Footer */}
      <div className="mt-16 border-t border-gray-200 bg-white py-8">
        <div className="max-w-7xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-gray-400">
          <div className="flex items-center gap-2">
            <ShieldCheck size={13} className="text-primary" />
            <span>Plăți securizate prin MiniPay · Tokenizare EMV · AES-256-GCM · Autentificare 3DS2</span>
          </div>
          <span>© 2026 MiniPay · Proiect de Disertație</span>
        </div>
      </div>

      {/* Floating checkout */}
      {cartCount > 0 && (
        <div className="fixed bottom-6 right-6 z-50 fadeIn">
          <Button className="shadow-2xl shadow-primary/30 px-6 py-5 text-base gap-2" onClick={goToCheckout}>
            <ShoppingCart size={17} />
            {cartCount} {cartCount === 1 ? "produs" : "produse"} · {total.toLocaleString("ro-RO")} lei
            <ArrowRight size={15} />
          </Button>
        </div>
      )}
    </div>
  );
}
