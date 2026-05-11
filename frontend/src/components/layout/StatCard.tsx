import { cn } from "@/lib/utils";
import { LucideIcon } from "lucide-react";

interface StatCardProps {
  title: string;
  value: string | number;
  sub?: string;
  icon: LucideIcon;
  color?: "emerald" | "cyan" | "indigo" | "destructive" | "amber";
  trend?: { value: number; direction: "up" | "down" };
  className?: string;
}

const colorMap = {
  emerald:     { icon: "text-emerald-400",  bg: "bg-emerald-500/10",  gradient: "linear-gradient(90deg,#10b981,#06b6d4)" },
  cyan:        { icon: "text-cyan-400",     bg: "bg-cyan-500/10",     gradient: "linear-gradient(90deg,#06b6d4,#6366f1)" },
  indigo:      { icon: "text-indigo-400",   bg: "bg-indigo-500/10",   gradient: "linear-gradient(90deg,#6366f1,#8b5cf6)" },
  destructive: { icon: "text-red-400",      bg: "bg-red-500/10",      gradient: "linear-gradient(90deg,#ef4444,#f97316)" },
  amber:       { icon: "text-amber-400",    bg: "bg-amber-500/10",    gradient: "linear-gradient(90deg,#f59e0b,#f97316)" },
};

export function StatCard({ title, value, sub, trend, icon: Icon, color = "emerald", className }: StatCardProps) {
  const c = colorMap[color];

  return (
    <div className={cn(
      "rounded-xl border border-border/40 bg-card overflow-hidden transition-all duration-300 hover:shadow-xl hover:shadow-black/20 hover:-translate-y-0.5 hover:border-border/60",
      className
    )}>
      {/* Gradient top bar */}
      <div className="h-[3px]" style={{ background: c.gradient }} />

      <div className="p-6">
        {/* Icon row */}
        <div className="flex items-center justify-between mb-5">
          <div className={cn("p-2.5 rounded-xl", c.bg)}>
            <Icon size={18} className={c.icon} />
          </div>
          {trend && (
            <span className={cn(
              "text-xs font-semibold px-2 py-1 rounded-lg",
              trend.direction === "up" ? "bg-emerald-500/15 text-emerald-400" : "bg-red-500/15 text-red-400"
            )}>
              {trend.direction === "up" ? "↑" : "↓"} {Math.abs(trend.value)}%
            </span>
          )}
        </div>

        {/* Label */}
        <p className="text-[11px] font-semibold text-foreground/50 uppercase tracking-widest">{title}</p>

        {/* Value */}
        <p className="text-4xl font-display font-bold mt-1.5 leading-none">{value}</p>

        {/* Sub */}
        {sub && <p className="text-xs text-foreground/40 mt-2">{sub}</p>}
      </div>
    </div>
  );
}
