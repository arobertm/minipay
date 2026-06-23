"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/lib/store/auth.store";
import {
  LayoutDashboard,
  CreditCard,
  ShieldAlert,
  Lock,
  Landmark,
  Bell,
  ClipboardList,
  KeyRound,
  Globe,
  Users,
  ShoppingCart,
  LogOut,
  ChevronRight,
} from "lucide-react";

const NAV = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Tranzacții", href: "/transactions", icon: CreditCard },
  { label: "Detecție Fraudă", href: "/fraud", icon: ShieldAlert },
  { label: "3DS2 Challenge", href: "/tds", icon: Lock },
  { type: "divider" as const, label: "Operațiuni" },
  { label: "Settlement-uri", href: "/settlements", icon: Landmark },
  { label: "Notificări", href: "/notifications", icon: Bell },
  { label: "Jurnal Audit", href: "/audit", icon: ClipboardList },
  { type: "divider" as const, label: "Securitate & Integrare" },
  { label: "Token Vault", href: "/vault", icon: KeyRound },
  { label: "PSD2 Open Banking", href: "/psd2", icon: Globe },
  { label: "Utilizatori", href: "/users", icon: Users },
  { type: "divider" as const, label: "Demo" },
  { label: "Demo Shop", href: "/shop", icon: ShoppingCart },
];

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const logout = useAuthStore((s) => s.logout);

  return (
    <aside className="w-72 shrink-0 flex flex-col bg-sidebar border-r border-sidebar-border h-screen sticky top-0 glass">
      {/* Logo Section */}
      <div className="px-6 py-6 border-b border-sidebar-border/40">
        <div className="flex items-baseline gap-1.5">
          <span className="text-2xl font-display font-bold text-sidebar-primary">Mini</span>
          <span className="text-2xl font-display font-bold text-sidebar-foreground">Pay</span>
        </div>
        <p className="text-xs text-sidebar-foreground/50 mt-1 font-medium uppercase tracking-widest">Platformă de Plăți</p>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-6 px-3 space-y-1 scrollbar-hide">
        {NAV.map((item, i) => {
          if (item.type === "divider") {
            return (
              <div key={i} className="pt-3 pb-2 px-3">
                <p className="text-xs font-semibold text-sidebar-foreground/40 uppercase tracking-widest">{item.label}</p>
              </div>
            );
          }
          const Icon = item.icon!;
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href!}
              className={cn(
                "nav-link group relative",
                active
                  ? "nav-link-active"
                  : "text-sidebar-foreground/60 hover:text-sidebar-foreground hover:bg-white/5"
              )}
            >
              <div className="absolute left-0 top-0 bottom-0 w-1 bg-sidebar-primary rounded-r transition-all duration-300 scale-y-0 group-hover:scale-y-100 origin-center" />
              <Icon size={18} className="shrink-0" />
              <span className="flex-1">{item.label}</span>
              {active && <ChevronRight size={16} className="opacity-50" />}
            </Link>
          );
        })}
      </nav>

      {/* Logout Button */}
      <div className="p-4 border-t border-sidebar-border/40 space-y-3">
        <button
          onClick={() => {
            logout();
            router.push("/login");
          }}
          className="btn-ghost w-full justify-center gap-2 text-sm hover:bg-destructive/10 hover:text-destructive border-destructive/20"
        >
          <LogOut size={16} />
          <span>Deconectare</span>
        </button>
      </div>
    </aside>
  );
}
