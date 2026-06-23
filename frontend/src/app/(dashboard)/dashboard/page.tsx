"use client";

import { useQuery } from "@tanstack/react-query";
import { audit } from "@/lib/api/audit";
import { notifications } from "@/lib/api/notifications";
import { settlements } from "@/lib/api/settlements";
import { StatCard } from "@/components/layout/StatCard";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  LineChart, Line, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from "recharts";
import {
  CreditCard, ShieldAlert, Landmark, Bell, TrendingUp,
} from "lucide-react";
import { format, subDays } from "date-fns";

/* ---------- custom tooltip ---------- */
const ChartTooltip = ({ active, payload, label, unit = "" }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ background: "#0f1419", border: "1px solid #1f2937", borderRadius: 12, padding: "10px 14px", boxShadow: "0 8px 32px rgba(0,0,0,0.5)" }}>
      {label && <p style={{ color: "#6b7280", fontSize: 11, marginBottom: 6 }}>{label}</p>}
      {payload.map((p: any, i: number) => (
        <div key={i} style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13, fontWeight: 600, color: "#f5f7fa" }}>
          <span style={{ width: 8, height: 8, borderRadius: "50%", background: p.color || p.fill, flexShrink: 0 }} />
          {p.value?.toLocaleString("ro-RO")}{unit || (p.unit ?? "")} <span style={{ color: "#6b7280", fontWeight: 400, fontSize: 11 }}>{p.name}</span>
        </div>
      ))}
    </div>
  );
};

const C = { green: "#10b981", cyan: "#06b6d4", amber: "#f59e0b", red: "#ef4444", indigo: "#6366f1", purple: "#8b5cf6" };

const statusColor: Record<string, string> = {
  AUTHORIZED: C.green, CAPTURED: C.cyan, DECLINED: C.red,
  FAILED: C.red, REFUNDED: C.amber, PENDING: C.amber,
};

export default function DashboardPage() {
  const auditQ = useQuery({ queryKey: ["audit-entries"], queryFn: () => audit.list(0, 100) });
  const notifQ = useQuery({ queryKey: ["notif-stats"],   queryFn: notifications.stats });
  const batchQ = useQuery({ queryKey: ["settle-batches"], queryFn: settlements.batches });

  const entries        = auditQ.data?.content ?? [];
  const totalTxns      = auditQ.data?.totalElements ?? 0;
  const pendingBatches = batchQ.data?.filter((b) => b.status === "PENDING").length ?? 0;
  const totalNotifs    = notifQ.data?.total ?? 0;
  const high           = entries.filter((e) => e.fraudScore >= 0.7).length;

  /* --- chart data --- */

  // Line chart: transaction count per day, padded to last 14 days ending today
  const today = new Date();
  const last14Keys = Array.from({ length: 14 }, (_, i) =>
    format(subDays(today, 13 - i), "dd MMM")
  );
  const entryCountByDay = entries.reduce<Record<string, number>>((acc, e) => {
    const day = e.eventTimestamp ? format(new Date(e.eventTimestamp), "dd MMM") : null;
    if (day) acc[day] = (acc[day] ?? 0) + 1;
    return acc;
  }, {});
  const txnByDay = last14Keys.map((day) => ({ day, count: entryCountByDay[day] ?? 0 }));

  // Donut: fraud distribution
  const low    = entries.filter((e) => e.fraudScore < 0.3).length;
  const medium = entries.filter((e) => e.fraudScore >= 0.3 && e.fraudScore < 0.7).length;
  const fraudData = [
    { name: "Scăzut",  value: low,    color: C.green },
    { name: "Mediu",   value: medium, color: C.amber },
    { name: "Ridicat", value: high,   color: C.red },
  ].filter((d) => d.value > 0);
  const fraudTotal = low + medium + high;

  // Pie: transaction status breakdown
  const statusMap = entries.reduce<Record<string, number>>((acc, e) => {
    acc[e.status] = (acc[e.status] ?? 0) + 1;
    return acc;
  }, {});
  const statusData = Object.entries(statusMap).map(([name, value]) => ({
    name, value, color: statusColor[name] ?? C.indigo,
  }));

  // Bars: notifications by channel
  const notifData = Object.entries(notifQ.data?.byChannel ?? {}).map(([channel, count], i) => ({
    channel, count, color: [C.indigo, C.cyan, C.green][i % 3],
  }));

  // Line chart: avg fraud score per day
  const avgFraudByDay = Object.entries(
    entries.reduce<Record<string, { total: number; count: number }>>((acc, e) => {
      const day = e.eventTimestamp ? format(new Date(e.eventTimestamp), "dd MMM") : "?";
      if (!acc[day]) acc[day] = { total: 0, count: 0 };
      acc[day].total += e.fraudScore ?? 0;
      acc[day].count += 1;
      return acc;
    }, {})
  )
    .map(([day, v]) => ({ day, avg: parseFloat(((v.total / v.count) * 100).toFixed(1)) }))
    .slice(-14);

  // High risk entries for alerts feed
  const highRiskEntries = [...entries]
    .filter((e) => (e.fraudScore ?? 0) >= 0.7)
    .sort((a, b) => (b.fraudScore ?? 0) - (a.fraudScore ?? 0))
    .slice(0, 6);


  return (
    <div className="space-y-6 p-6">
      <div className="fadeIn">
        <h1 className="text-4xl font-display font-bold text-foreground">Dashboard</h1>
        <p className="text-foreground/60 text-base mt-1 font-medium">Starea sistemului & overview tranzacții</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {[
          { title: "Evenimente Audit",   value: auditQ.isLoading ? "—" : totalTxns,      icon: CreditCard,  color: "emerald" as const,     sub: "activitate totală sistem" },
          { title: "Risc Ridicat",       value: auditQ.isLoading ? "—" : high,           icon: ShieldAlert, color: "destructive" as const, sub: "detectate de fraud-svc" },
          { title: "Loturi Pendinte",    value: batchQ.isLoading ? "—" : pendingBatches,                              icon: Landmark, color: "amber" as const, sub: "în așteptare reconciliere" },
          { title: "Notificări Livrate", value: notifQ.isLoading ? "—" : notifQ.isError ? "—" : totalNotifs, icon: Bell,     color: "cyan" as const,  sub: "SMS · Email · Push" },
        ].map((s, i) => (
          <div key={s.title} className={`fadeIn stagger-${i + 1}`}>
            <StatCard {...s} />
          </div>
        ))}
      </div>

      {/* Area chart (full width) */}
      <div className="card-premium fadeIn stagger-1">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-base font-display font-semibold text-foreground">Tranzacții</h2>
            <p className="text-xs text-foreground/40 mt-0.5">Ultimele 14 zile · număr tranzacții procesate</p>
          </div>
          <div className="flex items-center gap-4 text-xs text-foreground/50">
            <span className="flex items-center gap-1.5"><span className="w-3 h-0.5 rounded-full inline-block" style={{ background: C.cyan }} />Tranzacții</span>
          </div>
        </div>
        {auditQ.isLoading ? <Skeleton className="h-64 bg-foreground/5 rounded-lg" /> : (
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={txnByDay} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" vertical={false} />
              <XAxis dataKey="day" tick={{ fill: "#4b5563", fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: "#4b5563", fontSize: 11 }} axisLine={false} tickLine={false} width={38} allowDecimals={false} />
              <Tooltip content={<ChartTooltip />} />
              <Line type="monotone" dataKey="count" name="Tranzacții" stroke={C.cyan} strokeWidth={2.5} dot={false} activeDot={{ r: 5, fill: C.cyan, strokeWidth: 0 }} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Fraud donut + Status pie + Notifications bar */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

        {/* Donut fraud */}
        <div className="card-premium fadeIn stagger-1">
          <h2 className="text-base font-display font-semibold text-foreground mb-1">Risc fraudă</h2>
          <p className="text-xs text-foreground/40 mb-5">Distribuție per nivel de risc</p>
          {auditQ.isLoading ? <Skeleton className="h-48 bg-foreground/5 rounded-lg" /> : fraudData.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-foreground/30 text-sm">Nicio tranzacție</div>
          ) : (
            <div className="flex flex-col items-center">
              <div className="relative">
                <ResponsiveContainer width={180} height={180}>
                  <PieChart>
                    <Pie data={fraudData} cx="50%" cy="50%" innerRadius={56} outerRadius={80}
                      paddingAngle={4} dataKey="value" stroke="none" startAngle={90} endAngle={-270}>
                      {fraudData.map((d, i) => <Cell key={i} fill={d.color} />)}
                    </Pie>
                    <Tooltip content={<ChartTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                  <span className="text-2xl font-bold text-foreground">{fraudTotal}</span>
                  <span className="text-[10px] text-foreground/40 font-medium">total</span>
                </div>
              </div>
              <div className="flex flex-col gap-2 w-full mt-2">
                {fraudData.map((d) => (
                  <div key={d.name} className="flex items-center justify-between text-sm px-1">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full" style={{ background: d.color }} />
                      <span className="text-foreground/70">{d.name}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="w-16 h-1.5 rounded-full bg-foreground/10 overflow-hidden">
                        <div className="h-full rounded-full" style={{ width: `${(d.value / fraudTotal) * 100}%`, background: d.color }} />
                      </div>
                      <span className="font-semibold text-foreground/80 w-5 text-right">{d.value}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Status pie */}
        <div className="card-premium fadeIn stagger-2">
          <h2 className="text-base font-display font-semibold text-foreground mb-1">Status Plăți</h2>
          <p className="text-xs text-foreground/40 mb-5">Distribuție pe tip de rezultat</p>
          {auditQ.isLoading ? <Skeleton className="h-48 bg-foreground/5 rounded-lg" /> : statusData.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-foreground/30 text-sm">Nicio tranzacție</div>
          ) : (
            <div className="flex flex-col items-center">
              <ResponsiveContainer width={180} height={180}>
                <PieChart>
                  <Pie data={statusData} cx="50%" cy="50%" outerRadius={80} paddingAngle={3}
                    dataKey="value" stroke="none" startAngle={90} endAngle={-270}>
                    {statusData.map((d, i) => <Cell key={i} fill={d.color} opacity={0.85} />)}
                  </Pie>
                  <Tooltip content={<ChartTooltip />} />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex flex-col gap-2 w-full mt-2">
                {statusData.map((d) => (
                  <div key={d.name} className="flex items-center justify-between text-sm px-1">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-sm" style={{ background: d.color }} />
                      <span className="text-foreground/70 text-xs font-mono">{d.name}</span>
                    </div>
                    <span className="font-semibold text-foreground/80">{d.value}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Notifications bar */}
        <div className="card-premium fadeIn stagger-3">
          <h2 className="text-base font-display font-semibold text-foreground mb-1">Canal Notificări</h2>
          <p className="text-xs text-foreground/40 mb-5">Distribuite pe canal de livrare</p>
          {notifQ.isLoading ? <Skeleton className="h-48 bg-foreground/5 rounded-lg" /> : notifQ.isError ? (
            <div className="h-48 flex items-center justify-center text-foreground/30 text-sm">Serviciu indisponibil</div>
          ) : notifData.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-foreground/30 text-sm">Nicio notificare</div>
          ) : (
            <div className="flex flex-col gap-4 mt-2">
              {notifData.map((d) => (
                <div key={d.channel}>
                  <div className="flex justify-between text-sm mb-1.5">
                    <span className="font-medium text-foreground/70">{d.channel}</span>
                    <span className="font-bold" style={{ color: d.color }}>{d.count}</span>
                  </div>
                  <div className="h-2 rounded-full bg-foreground/10 overflow-hidden">
                    <div className="h-full rounded-full transition-all duration-700"
                      style={{ width: `${(d.count / (notifQ.data?.total || 1)) * 100}%`, background: d.color }} />
                  </div>
                </div>
              ))}
              <div className="mt-2 pt-3 border-t border-border/30 flex justify-between text-sm">
                <span className="text-foreground/50">Total trimise</span>
                <span className="font-bold text-foreground">{notifQ.data?.total ?? 0}</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Fraud score trend + High risk alerts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* Fraud score trend */}
        <div className="card-premium fadeIn stagger-1">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-base font-display font-semibold text-foreground">Evoluție scor fraudă</h2>
              <p className="text-xs text-foreground/40 mt-0.5">Scor mediu zilnic · ultimele 14 zile</p>
            </div>
            <TrendingUp size={16} className="text-foreground/30" />
          </div>
          {auditQ.isLoading ? <Skeleton className="h-48 bg-foreground/5 rounded-lg" /> : avgFraudByDay.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-foreground/30 text-sm">Nicio tranzacție</div>
          ) : (
            <ResponsiveContainer width="100%" height={195}>
              <LineChart data={avgFraudByDay} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" vertical={false} />
                <XAxis dataKey="day" tick={{ fill: "#4b5563", fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: "#4b5563", fontSize: 11 }} axisLine={false} tickLine={false} width={38} domain={[0, 100]} tickFormatter={(v) => `${v}%`} />
                <Tooltip content={<ChartTooltip unit="%" />} />
                <Line type="monotone" dataKey="avg" name="Scor mediu" stroke={C.amber} strokeWidth={2.5}
                  dot={{ fill: C.amber, strokeWidth: 0, r: 4 }}
                  activeDot={{ r: 6, fill: C.amber, strokeWidth: 0 }} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* High risk fraud alerts */}
        <div className="card-premium fadeIn stagger-2">
          <div className="flex items-center justify-between mb-5">
            <div>
              <h2 className="text-base font-display font-semibold text-foreground">Alerte Fraudă</h2>
              <p className="text-xs text-foreground/40 mt-0.5">Tranzacții cu scor ≥ 70%</p>
            </div>
            {highRiskEntries.length > 0 && (
              <Badge className="bg-red-500/20 text-red-400 border-red-500/30 text-xs">{highRiskEntries.length} alerte</Badge>
            )}
          </div>
          {auditQ.isLoading ? (
            <div className="space-y-3">{[...Array(4)].map((_, i) => <Skeleton key={i} className="h-10 bg-foreground/5 rounded-lg" />)}</div>
          ) : highRiskEntries.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-40 text-foreground/30">
              <ShieldAlert size={28} className="mb-3 text-foreground/20" />
              <p className="text-sm">Nicio alertă de fraudă</p>
            </div>
          ) : (
            <div className="space-y-2">
              {highRiskEntries.map((entry, idx) => (
                <div key={entry.id} className="flex items-center justify-between p-3 rounded-lg bg-red-500/5 hover:bg-red-500/10 transition-colors border border-red-500/15 fadeIn" style={{ animationDelay: `${(idx + 1) * 80}ms` }}>
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-2 h-2 rounded-full shrink-0 bg-red-400" />
                    <div className="min-w-0">
                      <p className="text-sm font-mono text-foreground/80">{entry.txnId?.slice(0, 16)}…</p>
                      <p className="text-xs text-foreground/40">{entry.eventTimestamp ? new Date(entry.eventTimestamp).toLocaleString("ro-RO") : "—"}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 shrink-0 ml-3">
                    <span className="text-xs font-semibold text-foreground/60">{(entry.amount / 100).toLocaleString("ro-RO")} {entry.currency}</span>
                    <span className="text-sm font-bold text-red-400">{(entry.fraudScore * 100).toFixed(0)}%</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

    </div>
  );
}
