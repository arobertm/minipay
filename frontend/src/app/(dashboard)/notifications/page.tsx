"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { notifications } from "@/lib/api/notifications";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Bell, Mail, MessageSquare, Smartphone } from "lucide-react";

const CHANNEL_ICON: Record<string, React.ElementType> = {
  EMAIL: Mail,
  SMS:   MessageSquare,
  PUSH:  Smartphone,
};

const CHANNEL_STYLE: Record<string, string> = {
  EMAIL: "bg-blue-500/20 text-blue-400 border-blue-500/30",
  SMS:   "bg-green-500/20 text-green-400 border-green-500/30",
  PUSH:  "bg-purple-500/20 text-purple-400 border-purple-500/30",
  LOG:   "bg-white/10 text-white/50 border-white/10",
};

const STATUS_STYLE: Record<string, string> = {
  SENT:   "bg-green-500/20 text-green-400 border-green-500/30",
  FAILED: "bg-red-500/20 text-red-400 border-red-500/30",
};

type ChannelFilter = "ALL" | "EMAIL" | "SMS" | "PUSH";

export default function NotificationsPage() {
  const [filter, setFilter] = useState<ChannelFilter>("ALL");

  const listQ = useQuery({
    queryKey: ["notifications"],
    queryFn:  notifications.list,
    refetchInterval: 10_000,
  });
  const statsQ = useQuery({ queryKey: ["notif-stats"], queryFn: notifications.stats });

  const filtered = (listQ.data ?? []).filter(
    (n) => filter === "ALL" || n.channel === filter
  );

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Notifications</h1>
        <p className="text-white/40 text-sm mt-1">Kafka consumer · Auto-refresh every 10 seconds</p>
      </div>

      {/* Stats */}
      {statsQ.data && (
        <div className="flex gap-4 flex-wrap">
          {(["ALL", "SMS", "EMAIL", "PUSH"] as ChannelFilter[]).map((ch) => {
            const count = ch === "ALL" ? statsQ.data.total : (statsQ.data.byChannel[ch] ?? 0);
            const Icon = ch === "ALL" ? Bell : CHANNEL_ICON[ch];
            return (
              <button
                key={ch}
                onClick={() => setFilter(ch)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg border text-sm transition-colors ${
                  filter === ch
                    ? "border-blue-500/50 bg-blue-600/20 text-blue-400"
                    : "border-white/10 bg-white/5 text-white/50 hover:text-white hover:bg-white/10"
                }`}
              >
                <Icon size={14} />
                {ch} <span className="font-bold">{count}</span>
              </button>
            );
          })}
        </div>
      )}

      {/* List */}
      <div className="rounded-xl border border-white/10 bg-[#1a1d27] overflow-hidden">
        {listQ.isLoading ? (
          <div className="p-5 space-y-3">
            {[...Array(6)].map((_, i) => <Skeleton key={i} className="h-16 bg-white/5" />)}
          </div>
        ) : !filtered.length ? (
          <div className="flex flex-col items-center justify-center py-16 text-white/20">
            <Bell size={36} className="mb-3" />
            <p className="text-sm">No notifications yet. Run a payment to generate one.</p>
          </div>
        ) : (
          <div className="divide-y divide-white/5">
            {filtered.map((n, idx) => {
              const Icon = CHANNEL_ICON[n.channel] ?? Bell;
              return (
                <div key={`${n.txnId}-${idx}`} className="px-5 py-4 flex items-start gap-4">
                  <div className={`rounded-lg p-2 mt-0.5 ${CHANNEL_STYLE[n.channel] ?? ""}`}>
                    <Icon size={16} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <Badge className={CHANNEL_STYLE[n.channel]}>{n.channel}</Badge>
                      <Badge className={STATUS_STYLE[n.notifStatus] ?? "bg-white/10 text-white/50"}>
                        {n.notifStatus}
                      </Badge>
                      <Badge className="bg-white/5 text-white/40 border-white/10 text-xs">
                        {n.paymentStatus}
                      </Badge>
                    </div>
                    {n.subject && (
                      <p className="text-xs text-white/50 font-medium mb-0.5">{n.subject}</p>
                    )}
                    <p className="text-sm text-white/80">{n.message}</p>
                    <p className="text-xs text-white/30 mt-1 font-mono">
                      txn: {n.txnId?.slice(0, 20)}…
                    </p>
                  </div>
                  <span className="text-xs text-white/30 shrink-0">
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
