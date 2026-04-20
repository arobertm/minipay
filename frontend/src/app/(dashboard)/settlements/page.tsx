"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { settlements, SettlementBatch } from "@/lib/api/settlements";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { RefreshCw, Loader2, CheckCircle2, XCircle, Clock } from "lucide-react";

const BATCH_STYLE: Record<string, string> = {
  COMPLETED: "bg-green-500/20 text-green-400 border-green-500/30",
  PENDING: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
  FAILED: "bg-red-500/20 text-red-400 border-red-500/30",
};

export default function SettlementsPage() {
  const qc = useQueryClient();
  const [selected, setSelected] = useState<SettlementBatch | null>(null);

  const batchQ = useQuery({ queryKey: ["batches"], queryFn: settlements.batches });
  const recordQ = useQuery({
    queryKey: ["records", selected?.batchId],
    queryFn: () => settlements.records(selected?.batchId),
    enabled: !!selected,
  });

  const reconcileMut = useMutation({
    mutationFn: settlements.reconcile,
    onSuccess: (data) => {
      toast.success(data.message ?? "Reconciliation complete");
      qc.invalidateQueries({ queryKey: ["batches"] });
    },
    onError: () => toast.error("Reconciliation failed"),
  });

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">Settlements</h1>
          <p className="text-white/40 text-sm mt-1">Daily batch reconciliation — auto at 01:00 UTC</p>
        </div>
        <Button
          className="bg-blue-600 hover:bg-blue-700"
          onClick={() => reconcileMut.mutate()}
          disabled={reconcileMut.isPending}
        >
          {reconcileMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Reconciling…</> : <><RefreshCw size={16} className="mr-2" />Run Reconciliation</>}
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Batches */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] overflow-hidden">
          <div className="px-5 py-4 border-b border-white/10">
            <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Batches</h2>
          </div>
          {batchQ.isLoading ? (
            <div className="p-5 space-y-3">{[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12 bg-white/5" />)}</div>
          ) : !batchQ.data?.length ? (
            <p className="p-8 text-center text-white/30 text-sm">No batches yet. Run reconciliation to create one.</p>
          ) : (
            <div className="divide-y divide-white/5">
              {batchQ.data.map((b) => (
                <button
                  key={b.batchId}
                  onClick={() => setSelected(b)}
                  className={`w-full text-left px-5 py-4 hover:bg-white/3 transition-colors ${selected?.batchId === b.batchId ? "bg-blue-600/10" : ""}`}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium">{b.settlementDate}</p>
                      <p className="text-xs text-white/40 mt-0.5">{b.recordCount} records · {b.currency}</p>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="font-semibold">{b.totalAmount.toFixed(2)}</span>
                      <Badge className={BATCH_STYLE[b.status]}>{b.status}</Badge>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Records */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] overflow-hidden">
          <div className="px-5 py-4 border-b border-white/10">
            <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">
              Records {selected ? `— ${selected.settlementDate}` : ""}
            </h2>
          </div>
          {!selected ? (
            <div className="flex flex-col items-center justify-center h-48 text-white/20">
              <Clock size={32} className="mb-2" />
              <p className="text-sm">Select a batch to view records</p>
            </div>
          ) : recordQ.isLoading ? (
            <div className="p-5 space-y-3">{[...Array(5)].map((_, i) => <Skeleton key={i} className="h-10 bg-white/5" />)}</div>
          ) : !recordQ.data?.length ? (
            <p className="p-8 text-center text-white/30 text-sm">No records in this batch.</p>
          ) : (
            <div className="divide-y divide-white/5">
              {recordQ.data.map((r) => (
                <div key={r.recordId} className="px-5 py-3 flex items-center justify-between">
                  <p className="font-mono text-xs text-white/50">{r.transactionId?.slice(0, 18)}…</p>
                  <div className="flex items-center gap-2">
                    <span className="text-sm">{r.amount} {r.currency}</span>
                    {r.status === "SETTLED" ? <CheckCircle2 size={14} className="text-green-400" /> : <XCircle size={14} className="text-red-400" />}
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
