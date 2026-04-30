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
  Atom,
} from "lucide-react";

const NAV = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Transactions", href: "/transactions", icon: CreditCard },
  { label: "Fraud Detection", href: "/fraud", icon: ShieldAlert },
  { label: "3DS2 Challenge", href: "/tds", icon: Lock },
  { type: "divider" as const },
  { label: "Settlements", href: "/settlements", icon: Landmark },
  { label: "Notifications", href: "/notifications", icon: Bell },
  { label: "Audit Log", href: "/audit", icon: ClipboardList },
  { type: "divider" as const },
  { label: "Token Vault", href: "/vault", icon: KeyRound },
  { label: "PSD2 Open Banking", href: "/psd2", icon: Globe },
  { label: "Post-Quantum Crypto", href: "/pqc", icon: Atom },
  { label: "Users", href: "/users", icon: Users },
  { type: "divider" as const },
  { label: "Demo Shop", href: "/shop", icon: ShoppingCart },
];

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const logout = useAuthStore((s) => s.logout);

  return (
    <aside className="w-60 shrink-0 flex flex-col border-r border-white/10 bg-[#13151f] h-screen sticky top-0">
      <div className="px-5 py-5 border-b border-white/10">
        <span className="text-lg font-bold tracking-tight text-blue-400">Mini</span>
        <span className="text-lg font-bold tracking-tight text-white">Pay</span>
        <p className="text-[10px] text-white/40 mt-0.5 uppercase tracking-widest">Dashboard</p>
      </div>

      <nav className="flex-1 overflow-y-auto py-3 px-2 space-y-0.5">
        {NAV.map((item, i) => {
          if (item.type === "divider") {
            return <div key={i} className="my-2 border-t border-white/5" />;
          }
          const Icon = item.icon!;
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href!}
              className={cn(
                "flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors",
                active
                  ? "bg-blue-600/20 text-blue-400 font-medium"
                  : "text-white/60 hover:text-white hover:bg-white/5"
              )}
            >
              <Icon size={16} />
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="px-2 py-3 border-t border-white/10">
        <button
          onClick={() => { logout(); router.push("/login"); }}
          className="flex items-center gap-3 px-3 py-2 rounded-md text-sm text-white/60 hover:text-white hover:bg-white/5 w-full transition-colors"
        >
          <LogOut size={16} />
          Logout
        </button>
      </div>
    </aside>
  );
}
