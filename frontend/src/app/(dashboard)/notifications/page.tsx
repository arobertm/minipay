"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { notifications } from "@/lib/api/notifications";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Bell, Mail, MessageSquare, Smartphone } from "lucide-react";
import { cn } from "@/lib/utils";

const CHANNEL_ICON: Record<string, React.ElementType> = {
  EMAIL: Mail,
  SMS:   MessageSquare,
  PUSH:  Smartphone,
};

const CHANNEL_STYLE: Record<string, string> = {
  EMAIL: "bg-cyan-500/20 text-cyan-400 border-cyan-500/30",
  SMS:   "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  PUSH:  "bg-purple-500/20 text-purple-400 border-purple-500/30",
  LOG:   "bg-foreground/10 text-foreground/50 border-foreground/20",
};

const CHANNEL_ICON_BG: Record<string, string> = {
  EMAIL: "bg-cyan-500/15 text-cyan-400",
  SMS:   "bg-emerald-500/15 text-emerald-400",
  PUSH:  "bg-purple-500/15 text-purple-400",
  LOG:   "bg-foreground/10 text-foreground/50",
};

const STATUS_STYLE: Record<string, string> = {
  SENT:   "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  FAILED: "bg-red-500/20 text-red-400 border-red-500/30",
};

type ChannelFilter = "ALL" | "EMAIL" | "SMS" | "PUSH";

export default function NotificationsPage() {
  const [filter, setFilter] = useState<ChannelFilter>("ALL");

  const listQ = useQuery({
    queryKey: ["notifications"],
    queryFn: notifications.list,
    refetchInterval: 10_000,
  });
  const statsQ = useQuery({ queryKey: ["notif-stats"], queryFn: notifications.stats });

  const filtered = (listQ.data ?? []).filter(
    (n) => filter === "ALL" || n.channel === filter
  );

  return (
    <div className="space-y-8 p-6">
      <div className="fadeIn">
        <h1 className="text-4xl font-display font-bold text-foreground">Notificări</h1>
        <p className="text-foreground/60 text-base mt-2 font-medium">Consumer Kafka · Reîmprospătare automată la 10 secunde</p>
      </div>

      {/* Filter chips */}
      {statsQ.data && (
        <div className="flex gap-3 flex-wrap fadeIn stagger-1">
          {(["ALL", "SMS", "EMAIL", "PUSH"] as ChannelFilter[]).map((ch) => {
            const count = ch === "ALL" ? statsQ.data.total : (statsQ.data.byChannel[ch] ?? 0);
            const Icon = ch === "ALL" ? Bell : CHANNEL_ICON[ch];
            return (
              <button
                key={ch}
                onClick={() => setFilter(ch)}
                className={cn(
                  "flex items-center gap-2 px-4 py-2.5 rounded-lg border text-sm font-medium transition-all duration-200",
                  filter === ch
                    ? "border-primary/50 bg-primary/20 text-primary shadow-lg shadow-primary/10"
                    : "border-border/40 bg-foreground/5 text-foreground/60 hover:text-foreground hover:bg-foreground/10 hover:border-border/60"
                )}
              >
                <Icon size={15} />
                <span>{ch}</span>
                <span className={cn(
                  "font-bold text-xs px-1.5 py-0.5 rounded-md",
                  filter === ch ? "bg-primary/30 text-primary" : "bg-foreground/10 text-foreground/50"
                )}>{count}</span>
              </button>
            );
          })}
        </div>
      )}

      {/* List */}
      <div className="card-premium p-0 overflow-hidden fadeIn stagger-2">
        {listQ.isLoading ? (
          <div className="p-5 space-y-3">
            {[...Array(6)].map((_, i) => <Skeleton key={i} className="h-20 bg-foreground/5 rounded-lg" />)}
          </div>
        ) : !filtered.length ? (
          <div className="flex flex-col items-center justify-center py-16 text-foreground/20">
            <div className="w-14 h-14 rounded-full bg-foreground/5 flex items-center justify-center mb-4">
              <Bell size={24} />
            </div>
            <p className="text-sm font-medium">Nicio notificare încă</p>
            <p className="text-xs mt-1">Procesează o plată pentru a genera notificări</p>
          </div>
        ) : (
          <div className="divide-y divide-border/20">
            {filtered.map((n, idx) => {
              const Icon = CHANNEL_ICON[n.channel] ?? Bell;
              return (
                <div key={`${n.txnId}-${idx}`} className="px-6 py-4 flex items-start gap-4 hover:bg-foreground/3 transition-colors duration-150">
                  <div className={cn("rounded-lg p-2.5 mt-0.5 shrink-0", CHANNEL_ICON_BG[n.channel] ?? "bg-foreground/10 text-foreground/50")}>
                    <Icon size={16} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1.5 flex-wrap">
                      <Badge className={CHANNEL_STYLE[n.channel]}>{n.channel}</Badge>
                      <Badge className={STATUS_STYLE[n.notifStatus] ?? "bg-foreground/10 text-foreground/50"}>
                        {n.notifStatus}
                      </Badge>
                      <Badge className="bg-foreground/5 text-foreground/40 border-border/30 text-xs">
                        {n.paymentStatus}
                      </Badge>
                    </div>
                    {n.subject && (
                      <p className="text-xs text-foreground/60 font-semibold mb-0.5">{n.subject}</p>
                    )}
                    <p className="text-sm text-foreground/80">{n.message}</p>
                    <p className="text-xs text-foreground/30 mt-1.5 font-mono">
                      txn: {n.txnId?.slice(0, 20)}…
                    </p>
                  </div>
                  <span className="text-xs text-foreground/40 shrink-0 mt-1">
                    {n.createdAt ? new Date(n.createdAt).toLocaleTimeString() : ""}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
