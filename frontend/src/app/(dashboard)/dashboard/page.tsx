"use client";

import { useQuery } from "@tanstack/react-query";
import { audit } from "@/lib/api/audit";
import { notifications } from "@/lib/api/notifications";
import { settlements } from "@/lib/api/settlements";
import { StatCard } from "@/components/layout/StatCard";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  CreditCard,
  ShieldAlert,
  Landmark,
  Bell,
  CheckCircle2,
  XCircle,
} from "lucide-react";
import { formatDistanceToNow } from "date-fns";

function fraudBadge(score?: number) {
  if (score == null) return null;
  if (score < 0.3) return <Badge className="bg-green-500/20 text-green-400 border-green-500/30">LOW</Badge>;
  if (score < 0.7) return <Badge className="bg-yellow-500/20 text-yellow-400 border-yellow-500/30">MEDIUM</Badge>;
  return <Badge className="bg-red-500/20 text-red-400 border-red-500/30">HIGH</Badge>;
}

export default function DashboardPage() {
  const auditQ = useQuery({ queryKey: ["audit-entries"], queryFn: () => audit.list(0, 20) });
  const notifQ = useQuery({ queryKey: ["notif-stats"], queryFn: notifications.stats });
  const batchQ = useQuery({ queryKey: ["settle-batches"], queryFn: settlements.batches });

  const totalTxns = auditQ.data?.totalElements ?? 0;
  const pendingBatches = batchQ.data?.filter((b) => b.status === "PENDING").length ?? 0;
  const totalNotifs = notifQ.data?.total ?? 0;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-white/40 text-sm mt-1">MiniPay — live overview</p>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard title="Total Audit Events" value={auditQ.isLoading ? "—" : totalTxns} icon={CreditCard} color="blue" sub="from audit log" />
        <StatCard title="Fraud Events" value="—" icon={ShieldAlert} color="red" sub="score > 0.7" />
        <StatCard title="Pending Settlements" value={batchQ.isLoading ? "—" : pendingBatches} icon={Landmark} color="yellow" sub="batches" />
        <StatCard title="Notifications Sent" value={notifQ.isLoading ? "—" : totalNotifs} icon={Bell} color="purple" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Audit Events */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-5">
          <h2 className="text-sm font-semibold text-white/70 mb-4 uppercase tracking-wider">Recent Audit Events</h2>
          {auditQ.isLoading ? (
            <div className="space-y-3">{[...Array(5)].map((_, i) => <Skeleton key={i} className="h-10 bg-white/5" />)}</div>
          ) : auditQ.data?.content.length === 0 ? (
            <p className="text-white/30 text-sm">No events yet.</p>
          ) : (
            <div className="space-y-2">
              {auditQ.data?.content.slice(0, 8).map((entry) => (
                <div key={entry.id} className="flex items-center justify-between py-2 border-b border-white/5 last:border-0">
                  <div>
                    <p className="text-sm font-mono text-white/80">{entry.action}</p>
                    <p className="text-xs text-white/30">{entry.transactionId?.slice(0, 16)}…</p>
                  </div>
                  <span className="text-xs text-white/30">
                    {entry.timestamp ? formatDistanceToNow(new Date(entry.timestamp), { addSuffix: true }) : "—"}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Settlement Batches */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-5">
          <h2 className="text-sm font-semibold text-white/70 mb-4 uppercase tracking-wider">Settlement Batches</h2>
          {batchQ.isLoading ? (
            <div className="space-y-3">{[...Array(4)].map((_, i) => <Skeleton key={i} className="h-10 bg-white/5" />)}</div>
          ) : !batchQ.data?.length ? (
            <p className="text-white/30 text-sm">No batches yet.</p>
          ) : (
            <div className="space-y-2">
              {batchQ.data.slice(0, 6).map((b) => (
                <div key={b.id} className="flex items-center justify-between py-2 border-b border-white/5 last:border-0">
                  <div>
                    <p className="text-sm text-white/80">{b.settlementDate}</p>
                    <p className="text-xs text-white/30">{b.txnCount} txns · {b.currency}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold">{(b.netAmount / 100).toFixed(2)}</span>
                    {b.status === "SETTLED"
                      ? <CheckCircle2 size={14} className="text-green-400" />
                      : <span className="text-xs text-yellow-400">{b.status}</span>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Notifications by Channel */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-5 lg:col-span-2">
          <h2 className="text-sm font-semibold text-white/70 mb-4 uppercase tracking-wider">Notifications by Channel</h2>
          {notifQ.isLoading ? (
            <Skeleton className="h-20 bg-white/5" />
          ) : notifQ.data ? (
            <div className="flex gap-6">
              {Object.entries(notifQ.data.byChannel).map(([ch, count]) => (
                <div key={ch} className="text-center">
                  <p className="text-2xl font-bold">{count}</p>
                  <p className="text-xs text-white/40 mt-1">{ch}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-white/30 text-sm">No notification stats yet.</p>
          )}
        </div>
      </div>
    </div>
  );
}
