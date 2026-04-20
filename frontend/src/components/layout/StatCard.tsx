import { cn } from "@/lib/utils";
import { LucideIcon } from "lucide-react";

interface StatCardProps {
  title: string;
  value: string | number;
  sub?: string;
  icon: LucideIcon;
  color?: "blue" | "green" | "yellow" | "red" | "purple";
}

const colorMap = {
  blue: "text-blue-400 bg-blue-400/10",
  green: "text-green-400 bg-green-400/10",
  yellow: "text-yellow-400 bg-yellow-400/10",
  red: "text-red-400 bg-red-400/10",
  purple: "text-purple-400 bg-purple-400/10",
};

export function StatCard({ title, value, sub, icon: Icon, color = "blue" }: StatCardProps) {
  return (
    <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-5 flex items-start gap-4">
      <div className={cn("rounded-lg p-2.5", colorMap[color])}>
        <Icon size={20} />
      </div>
      <div>
        <p className="text-sm text-white/50">{title}</p>
        <p className="text-2xl font-bold mt-0.5">{value}</p>
        {sub && <p className="text-xs text-white/40 mt-1">{sub}</p>}
      </div>
    </div>
  );
}
