"use client";

import { useQuery, useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { audit } from "@/lib/api/audit";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { CheckCircle2, XCircle, ShieldCheck, Loader2 } from "lucide-react";

export default function AuditPage() {
  const listQ = useQuery({ queryKey: ["audit-list"], queryFn: () => audit.list(0, 50) });

  const verifyMut = useMutation({
    mutationFn: audit.verify,
    onSuccess: (data) => {
      if (data.valid) toast.success("Merkle integrity verified ✅");
      else toast.error("Merkle integrity FAILED ❌");
    },
    onError: () => toast.error("Verify request failed"),
  });

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">Audit Log</h1>
          <p className="text-white/40 text-sm mt-1">Immutable event log with Merkle Tree integrity</p>
        </div>
        <Button
          className="bg-blue-600 hover:bg-blue-700"
          onClick={() => verifyMut.mutate()}
          disabled={verifyMut.isPending}
        >
          {verifyMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Verifying…</> : <><ShieldCheck size={16} className="mr-2" />Verify Merkle Integrity</>}
        </Button>
      </div>

      {verifyMut.data && (
        <Alert className={verifyMut.data.valid ? "border-green-500/40 bg-green-500/10" : "border-red-500/40 bg-red-500/10"}>
          <AlertDescription className="flex items-center gap-2">
            {verifyMut.data.valid
              ? <><CheckCircle2 size={16} className="text-green-400" /><span className="text-green-400 font-medium">Merkle Root valid — audit chain is intact</span></>
              : <><XCircle size={16} className="text-red-400" /><span className="text-red-400 font-medium">Integrity check FAILED — chain may be tampered</span></>}
            {verifyMut.data.merkleRoot && (
              <span className="text-white/30 font-mono text-xs ml-2">{verifyMut.data.merkleRoot.slice(0, 24)}…</span>
            )}
          </AlertDescription>
        </Alert>
      )}

      <div className="rounded-xl border border-white/10 bg-[#1a1d27] overflow-hidden">
        <div className="px-5 py-4 border-b border-white/10 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Events</h2>
          <span className="text-xs text-white/30">{listQ.data?.totalElements ?? 0} total</span>
        </div>
        {listQ.isLoading ? (
          <div className="p-5 space-y-3">{[...Array(8)].map((_, i) => <Skeleton key={i} className="h-10 bg-white/5" />)}</div>
        ) : !listQ.data?.content.length ? (
          <p className="p-8 text-center text-white/30 text-sm">No audit entries yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-white/30 text-xs uppercase tracking-wider border-b border-white/5">
                  <th className="text-left px-5 py-3">Action</th>
                  <th className="text-left px-5 py-3">Transaction ID</th>
                  <th className="text-left px-5 py-3">Hash</th>
                  <th className="text-left px-5 py-3">Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {listQ.data.content.map((entry) => (
                  <tr key={entry.id} className="border-b border-white/5 last:border-0 hover:bg-white/2">
                    <td className="px-5 py-3">
                      <Badge className="bg-blue-500/20 text-blue-300 border-blue-500/30 font-mono text-xs">{entry.action}</Badge>
                    </td>
                    <td className="px-5 py-3 font-mono text-white/50 text-xs">{entry.transactionId?.slice(0, 18)}…</td>
                    <td className="px-5 py-3 font-mono text-white/30 text-xs">{entry.merkleHash?.slice(0, 16) ?? "—"}…</td>
                    <td className="px-5 py-3 text-white/40 text-xs">{entry.timestamp ? new Date(entry.timestamp).toLocaleString() : "—"}</td>
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
