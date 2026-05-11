"use client";

import { useQuery, useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { audit } from "@/lib/api/audit";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { CheckCircle2, XCircle, ShieldCheck, Loader2, ClipboardList } from "lucide-react";

const STATUS_STYLE: Record<string, string> = {
  AUTHORIZED: "bg-cyan-500/20 text-cyan-300 border-cyan-500/30",
  CAPTURED:   "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
  REFUNDED:   "bg-purple-500/20 text-purple-300 border-purple-500/30",
  DECLINED:   "bg-red-500/20 text-red-300 border-red-500/30",
  BLOCKED:    "bg-red-600/20 text-red-400 border-red-600/30",
  CHALLENGE:  "bg-amber-500/20 text-amber-300 border-amber-500/30",
};

export default function AuditPage() {
  const listQ = useQuery({ queryKey: ["audit-list"], queryFn: () => audit.list(0, 50) });

  const verifyMut = useMutation({
    mutationFn: audit.verify,
    onSuccess: (data) => {
      if (data.isValid) toast.success("Merkle integrity verified ✅");
      else toast.error("Merkle integrity FAILED ❌");
    },
    onError: () => toast.error("Verify request failed"),
  });

  return (
    <div className="space-y-8 p-6">
      <div className="flex items-start justify-between fadeIn">
        <div>
          <h1 className="text-4xl font-display font-bold text-foreground">Jurnal Audit</h1>
          <p className="text-foreground/60 text-base mt-2 font-medium">Lanț hash imutabil · PCI DSS Cerința 10</p>
        </div>
        <Button onClick={() => verifyMut.mutate()} disabled={verifyMut.isPending} variant="outline"
          className="border-primary/40 text-primary hover:bg-primary/10">
          {verifyMut.isPending ? (
            <><Loader2 size={16} className="animate-spin mr-2" />Se verifică…</>
          ) : (
            <><ShieldCheck size={16} className="mr-2" />Verifică integritate Merkle</>
          )}
        </Button>
      </div>

      {verifyMut.data && (
        <Alert className={verifyMut.data.isValid
          ? "border-emerald-500/40 bg-emerald-500/10"
          : "border-red-500/40 bg-red-500/10"}>
          <AlertDescription className="flex items-center gap-2">
            {verifyMut.data.isValid ? (
              <><CheckCircle2 size={16} className="text-emerald-400" />
                <span className="text-emerald-400 font-semibold">Lanț Merkle intact — {verifyMut.data.totalEntries} intrări verificate</span></>
            ) : (
              <><XCircle size={16} className="text-red-400" />
                <span className="text-red-400 font-semibold">Verificare EȘUATĂ — lanțul poate fi alterat</span></>
            )}
            <span className="text-foreground/30 font-mono text-xs ml-2">{verifyMut.data.message}</span>
          </AlertDescription>
        </Alert>
      )}

      <div className="card-premium p-0 overflow-hidden fadeIn stagger-1">
        <div className="px-6 py-4 border-b border-border/40 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-cyan-500/20">
              <ClipboardList size={16} className="text-cyan-400" />
            </div>
            <h2 className="text-base font-display font-semibold">Evenimente Audit</h2>
          </div>
          <span className="text-xs font-medium px-2.5 py-1 rounded-full bg-foreground/10 text-foreground/60">
            {listQ.data?.totalElements ?? 0} total
          </span>
        </div>
        {listQ.isLoading ? (
          <div className="p-5 space-y-3">
            {[...Array(8)].map((_, i) => <Skeleton key={i} className="h-12 bg-foreground/5 rounded-lg" />)}
          </div>
        ) : !listQ.data?.content.length ? (
          <p className="p-10 text-center text-foreground/30 text-sm">Nicio intrare în jurnal încă.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-foreground/40 text-xs font-semibold uppercase tracking-widest border-b border-border/30">
                  <th className="text-left px-6 py-3">#</th>
                  <th className="text-left px-6 py-3">Status</th>
                  <th className="text-left px-6 py-3">ID Tranzacție</th>
                  <th className="text-left px-6 py-3">Sumă</th>
                  <th className="text-left px-6 py-3">Fraudă</th>
                  <th className="text-left px-6 py-3">Hash Intrare</th>
                  <th className="text-left px-6 py-3">Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {listQ.data.content.map((entry) => (
                  <tr key={entry.id} className="border-b border-border/20 last:border-0 hover:bg-foreground/3 transition-colors duration-150">
                    <td className="px-6 py-4 text-foreground/30 text-xs font-mono">{entry.sequenceNumber}</td>
                    <td className="px-6 py-4">
                      <Badge className={STATUS_STYLE[entry.status] ?? "bg-foreground/10 text-foreground/50"}>
                        {entry.status}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 font-mono text-foreground/50 text-xs">{entry.txnId?.slice(0, 18)}…</td>
                    <td className="px-6 py-4 text-foreground/60 text-xs font-medium">
                      {(entry.amount / 100).toFixed(2)} {entry.currency}
                    </td>
                    <td className="px-6 py-4 text-xs">
                      <span className={
                        entry.fraudScore < 0.5 ? "text-emerald-400 font-semibold" :
                        entry.fraudScore < 0.8 ? "text-amber-400 font-semibold" :
                        "text-red-400 font-semibold"
                      }>
                        {(entry.fraudScore * 100).toFixed(0)}%
                      </span>
                    </td>
                    <td className="px-6 py-4 font-mono text-foreground/30 text-xs">{entry.entryHash?.slice(0, 16)}…</td>
                    <td className="px-6 py-4 text-foreground/50 text-xs">
                      {entry.eventTimestamp ? new Date(entry.eventTimestamp).toLocaleString() : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
