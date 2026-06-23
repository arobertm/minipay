"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { settlements, SettlementBatch, SettlementRecord } from "@/lib/api/settlements";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { RefreshCw, Loader2, CheckCircle2, XCircle, Clock, Landmark } from "lucide-react";
import { cn } from "@/lib/utils";

const BATCH_STYLE: Record<string, string> = {
  COMPLETED: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  PENDING:   "bg-amber-500/20 text-amber-400 border-amber-500/30",
  FAILED:    "bg-red-500/20 text-red-400 border-red-500/30",
};

export default function SettlementsPage() {
  const qc = useQueryClient();
  const [selected, setSelected] = useState<SettlementBatch | null>(null);

  const batchQ = useQuery({ queryKey: ["batches"], queryFn: settlements.batches });
  const recordQ = useQuery({
    queryKey: ["records", selected?.settlementDate],
    queryFn: () => settlements.records(selected?.settlementDate),
    enabled: !!selected,
  });

  const reconcileMut = useMutation({
    mutationFn: settlements.reconcile,
    onSuccess: (data) => {
      toast.success(`Reconciliere completă — ${data.batchesCreated} lot(uri) pentru ${data.date}`);
      qc.invalidateQueries({ queryKey: ["batches"] });
    },
    onError: () => toast.error("Reconciliere eșuată"),
  });

  return (
    <div className="space-y-8 p-6">
      <div className="flex items-start justify-between fadeIn">
        <div>
          <h1 className="text-4xl font-display font-bold text-foreground">Settlement-uri</h1>
          <p className="text-foreground/60 text-base mt-2 font-medium">Reconciliere automată zilnic la 01:00 UTC</p>
        </div>
        <Button onClick={() => reconcileMut.mutate(undefined)} disabled={reconcileMut.isPending}>
          {reconcileMut.isPending
            ? <><Loader2 size={16} className="animate-spin mr-2" />Se reconciliază…</>
            : <><RefreshCw size={16} className="mr-2" />Rulează reconciliere</>}
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Batches */}
        <div className="card-premium p-0 overflow-hidden fadeIn stagger-1">
          <div className="px-6 py-4 border-b border-border/40 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-amber-500/20">
              <Landmark size={16} className="text-amber-400" />
            </div>
            <h2 className="text-base font-display font-semibold">Batches</h2>
          </div>
          {batchQ.isLoading ? (
            <div className="p-5 space-y-3">
              {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-14 bg-foreground/5 rounded-lg" />)}
            </div>
          ) : !batchQ.data?.length ? (
            <div className="flex flex-col items-center justify-center py-14 text-foreground/20">
              <Clock size={28} className="mb-3" />
              <p className="text-sm font-medium">Niciun batch încă</p>
              <p className="text-xs mt-1">Rulează reconcilierea pentru a crea primul batch</p>
            </div>
          ) : (
            <div className="divide-y divide-border/20">
              {batchQ.data.map((b) => (
                <button
                  key={b.id}
                  onClick={() => setSelected(b)}
                  className={cn(
                    "w-full text-left px-6 py-4 hover:bg-foreground/5 transition-colors duration-150",
                    selected?.id === b.id && "bg-primary/10 border-l-2 border-primary"
                  )}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-semibold text-foreground">{b.settlementDate}</p>
                      <p className="text-xs text-foreground/50 mt-1">{b.txnCount} txns · {b.merchantId} · {b.currency}</p>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="font-semibold text-foreground">${(b.netAmount / 100).toFixed(2)}</span>
                      <Badge className={BATCH_STYLE[b.status] ?? "bg-foreground/10 text-foreground/50"}>{b.status}</Badge>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Records */}
        <div className="card-premium p-0 overflow-hidden fadeIn stagger-2">
          <div className="px-6 py-4 border-b border-border/40">
            <h2 className="text-base font-display font-semibold">
              Înregistrări {selected ? <span className="text-foreground/50 font-normal">— {selected.settlementDate}</span> : ""}
            </h2>
          </div>
          {!selected ? (
            <div className="flex flex-col items-center justify-center py-14 text-foreground/20">
              <Clock size={28} className="mb-3" />
              <p className="text-sm font-medium">Selectează un batch pentru a vedea înregistrările</p>
            </div>
          ) : recordQ.isLoading ? (
            <div className="p-5 space-y-3">
              {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12 bg-foreground/5 rounded-lg" />)}
            </div>
          ) : !recordQ.data?.length ? (
            <p className="p-8 text-center text-foreground/30 text-sm">Nicio înregistrare în acest batch.</p>
          ) : (
            <div className="divide-y divide-border/20">
              {recordQ.data.map((r: SettlementRecord) => (
                <div key={r.id} className="px-6 py-4 flex items-center justify-between hover:bg-foreground/3 transition-colors">
                  <div>
                    <p className="font-mono text-xs text-foreground/50">{r.txnId?.slice(0, 18)}…</p>
                    <p className="text-xs text-foreground/40 mt-0.5">{r.paymentStatus}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-medium">{(r.amount / 100).toFixed(2)} {r.currency}</span>
                    {r.paymentStatus === "CAPTURED"
                      ? <CheckCircle2 size={16} className="text-emerald-400" />
                      : <XCircle size={16} className="text-purple-400" />}
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
