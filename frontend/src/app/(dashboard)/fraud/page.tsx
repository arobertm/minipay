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
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ReferenceLine, ResponsiveContainer, Cell,
} from "recharts";

function ScoreMeter({ score }: { score: number }) {
  const pct = Math.round(score * 100);
  const color = score < 0.5 ? "#10b981" : score < 0.8 ? "#f59e0b" : "#ef4444";
  const Icon = score < 0.5 ? ShieldCheck : score < 0.8 ? ShieldAlert : ShieldX;
  const label = score < 0.5 ? "LOW RISK" : score < 0.8 ? "MEDIUM RISK" : "HIGH RISK";

  return (
    <div className="flex flex-col items-center gap-4 py-4">
      <div className="w-16 h-16 rounded-full flex items-center justify-center" style={{ backgroundColor: `${color}20` }}>
        <Icon size={32} style={{ color }} />
      </div>
      <div className="text-6xl font-display font-bold" style={{ color }}>
        {pct}<span className="text-3xl">%</span>
      </div>
      <Badge style={{ backgroundColor: `${color}20`, color, borderColor: `${color}40` }} className="text-sm px-3 py-1">
        {label}
      </Badge>
      <div className="w-full h-2.5 rounded-full bg-foreground/10 overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-700"
          style={{ width: `${pct}%`, backgroundColor: color }}
        />
      </div>
    </div>
  );
}

function ShapChart({ shapDetails }: { shapDetails: FraudResponse["shap_details"] }) {
  const data = [...shapDetails]
    .sort((a, b) => Math.abs(b.shap_value) - Math.abs(a.shap_value))
    .slice(0, 10)
    .map((d) => ({ name: d.feature, value: parseFloat(d.shap_value.toFixed(4)), description: d.description }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} layout="vertical" margin={{ left: 20, right: 30 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
        <XAxis type="number" tick={{ fill: "rgba(245,247,250,0.4)", fontSize: 11 }} tickLine={false} axisLine={false} />
        <YAxis type="category" dataKey="name" tick={{ fill: "rgba(245,247,250,0.6)", fontSize: 11 }} width={140} tickLine={false} axisLine={false} />
        <Tooltip
          contentStyle={{ backgroundColor: "#0f1419", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 10 }}
          labelStyle={{ color: "#f5f7fa" }}
          formatter={(v, _name, props) => [`${(v as number).toFixed(4)}`, props.payload?.description ?? "SHAP value"]}
        />
        <ReferenceLine x={0} stroke="rgba(255,255,255,0.2)" />
        <Bar dataKey="value" radius={[0, 4, 4, 0]}>
          {data.map((entry, i) => (
            <Cell key={i} fill={entry.value >= 0 ? "#ef4444" : "#10b981"} fillOpacity={0.85} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

const DECISION_STYLE = {
  ALLOW:     "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  CHALLENGE: "bg-amber-500/20 text-amber-400 border-amber-500/30",
  BLOCK:     "bg-red-500/20 text-red-400 border-red-500/30",
};

export default function FraudPage() {
  const [form, setForm] = useState<FraudRequest>({
    dpan: "4111110000001234",
    amount: 25000,
    currency: "RON",
    merchantId: "merchant-001",
    ipAddress: "192.168.1.100",
  });
  const [result, setResult] = useState<FraudResponse | null>(null);

  const scoreMut = useMutation({
    mutationFn: () => fraud.score(form),
    onSuccess: (data) => {
      setResult(data);
      toast.success(`Scor fraudă: ${(data.score * 100).toFixed(1)}% — ${data.decision}`);
    },
    onError: () => toast.error("Evaluare fraudă eșuată"),
  });

  const f = (key: keyof FraudRequest, value: unknown) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  return (
    <div className="space-y-8 p-6">
      <div className="fadeIn">
        <h1 className="text-4xl font-display font-bold text-foreground">Detecție Fraudă</h1>
        <p className="text-foreground/60 text-base mt-2 font-medium">Model XGBoost · Explicabilitate SHAP (GDPR Art. 22)</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Input Form */}
        <div className="card-premium space-y-5 fadeIn stagger-1">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-destructive/20">
              <ShieldAlert size={18} className="text-destructive" />
            </div>
            <h2 className="text-base font-display font-semibold">Tranzacție de evaluat</h2>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2 space-y-2">
              <Label className="text-foreground/70 font-medium">DPAN</Label>
              <Input value={form.dpan} onChange={(e) => f("dpan", e.target.value)} className="font-mono" />
            </div>
            <div className="space-y-2">
              <Label className="text-foreground/70 font-medium">Amount (cenți)</Label>
              <Input type="number" value={form.amount} onChange={(e) => f("amount", parseInt(e.target.value))} />
            </div>
            <div className="space-y-2">
              <Label className="text-foreground/70 font-medium">Currency</Label>
              <Input value={form.currency} onChange={(e) => f("currency", e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label className="text-foreground/70 font-medium">Merchant ID</Label>
              <Input value={form.merchantId} onChange={(e) => f("merchantId", e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label className="text-foreground/70 font-medium">IP Address</Label>
              <Input value={form.ipAddress} onChange={(e) => f("ipAddress", e.target.value)} />
            </div>
          </div>
          <Button className="w-full" onClick={() => scoreMut.mutate()} disabled={scoreMut.isPending}>
            {scoreMut.isPending ? (
              <><Loader2 size={16} className="animate-spin mr-2" />Se evaluează…</>
            ) : "Rulează evaluare fraudă"}
          </Button>
        </div>

        {/* Result */}
        <div className="card-premium space-y-5 fadeIn stagger-2">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/20">
              <ShieldCheck size={18} className="text-primary" />
            </div>
            <h2 className="text-base font-display font-semibold">Evaluare risc</h2>
          </div>
          {!result ? (
            <div className="flex flex-col items-center justify-center h-52 text-foreground/20">
              <div className="w-16 h-16 rounded-full bg-foreground/5 flex items-center justify-center mb-4">
                <ShieldAlert size={28} />
              </div>
              <p className="text-sm font-medium">Rulează o evaluare pentru a vedea rezultatele</p>
            </div>
          ) : (
            <>
              <ScoreMeter score={result.score} />
              <div className="flex items-center justify-between p-4 rounded-lg bg-foreground/5 border border-border/30">
                <span className="text-sm font-medium text-foreground/60">Decizie</span>
                <Badge className={DECISION_STYLE[result.decision]} style={{ fontSize: "0.85rem" }}>{result.decision}</Badge>
              </div>
              {result.reasons.length > 0 && (
                <div className="space-y-2 pt-2 border-t border-border/30">
                  <span className="text-xs font-semibold text-foreground/40 uppercase tracking-widest">Motive (GDPR Art.22)</span>
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {result.reasons.map((r) => (
                      <span key={r} className="text-xs px-2.5 py-1 rounded-md bg-foreground/5 text-foreground/60 border border-border/30">{r}</span>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* SHAP Explanation */}
      {result && result.shap_details.length > 0 && (
        <div className="card-premium fadeIn">
          <div className="flex items-center gap-3 mb-2">
            <h2 className="text-base font-display font-semibold">Contribuții SHAP per Feature</h2>
          </div>
          <p className="text-xs text-foreground/40 mb-6">
            Barele roșii cresc riscul de fraudă · Barele verzi îl reduc · Sortate după impact absolut
          </p>
          <ShapChart shapDetails={result.shap_details} />
        </div>
      )}
    </div>
  );
}
