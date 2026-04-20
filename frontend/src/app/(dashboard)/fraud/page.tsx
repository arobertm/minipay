"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { fraud, FraudRequest, FraudResponse } from "@/lib/api/fraud";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2, ShieldCheck, ShieldAlert, ShieldX } from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ReferenceLine,
  ResponsiveContainer,
  Cell,
} from "recharts";

function ScoreMeter({ score }: { score: number }) {
  const pct = Math.round(score * 100);
  const color = score < 0.3 ? "#10b981" : score < 0.7 ? "#f59e0b" : "#ef4444";
  const Icon = score < 0.3 ? ShieldCheck : score < 0.7 ? ShieldAlert : ShieldX;
  const label = score < 0.3 ? "LOW RISK" : score < 0.7 ? "MEDIUM RISK" : "HIGH RISK";

  return (
    <div className="flex flex-col items-center gap-3">
      <Icon size={48} style={{ color }} />
      <div className="text-5xl font-bold" style={{ color }}>{pct}<span className="text-2xl">%</span></div>
      <Badge style={{ backgroundColor: `${color}20`, color, borderColor: `${color}40` }}>{label}</Badge>
      <div className="w-full h-2 rounded-full bg-white/10 mt-1">
        <div className="h-full rounded-full transition-all duration-500" style={{ width: `${pct}%`, backgroundColor: color }} />
      </div>
    </div>
  );
}

function ShapChart({ shapValues }: { shapValues: Record<string, number> }) {
  const data = Object.entries(shapValues)
    .map(([name, value]) => ({ name, value: parseFloat(value.toFixed(4)) }))
    .sort((a, b) => Math.abs(b.value) - Math.abs(a.value))
    .slice(0, 10);

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} layout="vertical" margin={{ left: 20, right: 30 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#ffffff08" />
        <XAxis type="number" tick={{ fill: "#ffffff50", fontSize: 11 }} tickLine={false} axisLine={false} />
        <YAxis type="category" dataKey="name" tick={{ fill: "#ffffff80", fontSize: 11 }} width={110} tickLine={false} axisLine={false} />
        <Tooltip
          contentStyle={{ backgroundColor: "#1a1d27", border: "1px solid #ffffff15", borderRadius: 8 }}
          labelStyle={{ color: "#fff" }}
          formatter={(v) => [(v as number).toFixed(4), "SHAP value"]}
        />
        <ReferenceLine x={0} stroke="#ffffff30" />
        <Bar dataKey="value" radius={[0, 4, 4, 0]}>
          {data.map((entry, i) => (
            <Cell key={i} fill={entry.value >= 0 ? "#ef4444" : "#10b981"} fillOpacity={0.8} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

const DECISION_STYLE = {
  APPROVE: "bg-green-500/20 text-green-400 border-green-500/30",
  REVIEW: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
  DECLINE: "bg-red-500/20 text-red-400 border-red-500/30",
};

export default function FraudPage() {
  const [form, setForm] = useState<FraudRequest>({
    amount: 250,
    currency: "EUR",
    merchantCountry: "RO",
    cardType: "VISA",
    hour: 14,
    dayOfWeek: 3,
    isInternational: false,
    previousFraudCount: 0,
  });
  const [result, setResult] = useState<FraudResponse | null>(null);

  const scoreMut = useMutation({
    mutationFn: () => fraud.score(form),
    onSuccess: (data) => {
      setResult(data);
      toast.success(`Fraud scored: ${(data.fraudScore * 100).toFixed(1)}% — ${data.decision}`);
    },
    onError: () => toast.error("Fraud scoring failed"),
  });

  const f = (key: keyof FraudRequest, value: unknown) => setForm((prev) => ({ ...prev, [key]: value }));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Fraud Detection</h1>
        <p className="text-white/40 text-sm mt-1">XGBoost model with SHAP explainability</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Input Form */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Transaction Features</h2>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Amount</Label>
              <Input type="number" value={form.amount} onChange={(e) => f("amount", parseFloat(e.target.value))} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5">
              <Label>Currency</Label>
              <Input value={form.currency} onChange={(e) => f("currency", e.target.value)} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5">
              <Label>Merchant Country</Label>
              <Input value={form.merchantCountry} onChange={(e) => f("merchantCountry", e.target.value)} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5">
              <Label>Card Type</Label>
              <Input value={form.cardType} onChange={(e) => f("cardType", e.target.value)} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5">
              <Label>Hour of Day (0-23)</Label>
              <Input type="number" min={0} max={23} value={form.hour} onChange={(e) => f("hour", parseInt(e.target.value))} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5">
              <Label>Day of Week (1=Mon)</Label>
              <Input type="number" min={1} max={7} value={form.dayOfWeek} onChange={(e) => f("dayOfWeek", parseInt(e.target.value))} className="bg-white/5 border-white/10" />
            </div>
            <div className="space-y-1.5">
              <Label>International?</Label>
              <div className="flex items-center gap-2 h-10">
                <input type="checkbox" checked={form.isInternational} onChange={(e) => f("isInternational", e.target.checked)} className="w-4 h-4" />
                <span className="text-sm text-white/60">Cross-border transaction</span>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>Previous Fraud Count</Label>
              <Input type="number" min={0} value={form.previousFraudCount} onChange={(e) => f("previousFraudCount", parseInt(e.target.value))} className="bg-white/5 border-white/10" />
            </div>
          </div>
          <Button
            className="w-full bg-blue-600 hover:bg-blue-700 mt-2"
            onClick={() => scoreMut.mutate()}
            disabled={scoreMut.isPending}
          >
            {scoreMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Scoring…</> : "Run Fraud Score"}
          </Button>
        </div>

        {/* Result */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-6">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Result</h2>

          {!result ? (
            <div className="flex flex-col items-center justify-center h-48 text-white/20">
              <ShieldAlert size={40} className="mb-3" />
              <p className="text-sm">Run a fraud score to see results</p>
            </div>
          ) : (
            <>
              <ScoreMeter score={result.fraudScore} />
              <div className="flex items-center justify-between border-t border-white/5 pt-4">
                <span className="text-sm text-white/50">Decision</span>
                <Badge className={DECISION_STYLE[result.decision]}>{result.decision}</Badge>
              </div>
              {result.processingTimeMs != null && (
                <div className="flex items-center justify-between">
                  <span className="text-sm text-white/50">Processing time</span>
                  <span className="text-sm text-white/70">{result.processingTimeMs} ms</span>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* SHAP Explanation */}
      {result && Object.keys(result.shapValues).length > 0 && (
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider mb-2">SHAP Feature Contributions</h2>
          <p className="text-xs text-white/30 mb-5">
            Red bars increase fraud risk · Green bars decrease fraud risk · Sorted by absolute impact
          </p>
          <ShapChart shapValues={result.shapValues} />
        </div>
      )}
    </div>
  );
}
