"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { vault } from "@/lib/api/vault";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2, ArrowRight, KeyRound, Copy } from "lucide-react";

export default function VaultPage() {
  const [panForm, setPanForm] = useState({ pan: "4111111111111111", expiry: "12/28", cvv: "123" });
  const [dpanResult, setDpanResult] = useState<string | null>(null);
  const [dpanInput, setDpanInput] = useState("");
  const [deTokenResult, setDeTokenResult] = useState<string | null>(null);

  const tokenizeMut = useMutation({
    mutationFn: () => vault.tokenize(panForm),
    onSuccess: (d) => { setDpanResult(d.dpan); toast.success("Tokenized successfully"); },
    onError: () => toast.error("Tokenization failed"),
  });

  const detokenizeMut = useMutation({
    mutationFn: () => vault.detokenize(dpanInput),
    onSuccess: (d) => { setDeTokenResult(d.maskedPan); toast.success("Detokenized"); },
    onError: () => toast.error("Detokenize failed"),
  });

  function copyToClipboard(text: string) {
    navigator.clipboard.writeText(text);
    toast.info("Copied to clipboard");
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Token Vault</h1>
        <p className="text-white/40 text-sm mt-1">EMV tokenization — PAN → DPAN via AES-256-GCM</p>
      </div>

      {/* Visual diagram */}
      <div className="flex items-center justify-center gap-4 py-4">
        <div className="rounded-lg border border-white/10 bg-white/5 px-4 py-2 text-sm font-mono text-white/70">PAN</div>
        <ArrowRight size={20} className="text-white/30" />
        <div className="rounded-lg border border-blue-500/40 bg-blue-500/10 px-4 py-2 text-sm text-blue-400 flex items-center gap-2">
          <KeyRound size={14} /> AES-256-GCM Vault
        </div>
        <ArrowRight size={20} className="text-white/30" />
        <div className="rounded-lg border border-white/10 bg-white/5 px-4 py-2 text-sm font-mono text-white/70">DPAN</div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Tokenize */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Tokenize PAN</h2>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>PAN</Label>
              <Input value={panForm.pan} onChange={(e) => setPanForm((f) => ({ ...f, pan: e.target.value }))} className="bg-white/5 border-white/10 font-mono" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>Expiry</Label>
                <Input value={panForm.expiry} onChange={(e) => setPanForm((f) => ({ ...f, expiry: e.target.value }))} className="bg-white/5 border-white/10" />
              </div>
              <div className="space-y-1.5">
                <Label>CVV</Label>
                <Input value={panForm.cvv} onChange={(e) => setPanForm((f) => ({ ...f, cvv: e.target.value }))} className="bg-white/5 border-white/10" />
              </div>
            </div>
          </div>
          <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => tokenizeMut.mutate()} disabled={tokenizeMut.isPending}>
            {tokenizeMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Tokenizing…</> : "Tokenize"}
          </Button>
          {dpanResult && (
            <div className="rounded-lg border border-green-500/30 bg-green-500/10 p-4">
              <p className="text-xs text-green-400/70 mb-1">DPAN</p>
              <div className="flex items-center justify-between">
                <p className="font-mono text-green-400 text-sm break-all">{dpanResult}</p>
                <button onClick={() => copyToClipboard(dpanResult)} className="ml-2 shrink-0 text-green-400/60 hover:text-green-400">
                  <Copy size={14} />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Detokenize */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Detokenize DPAN</h2>
          <div className="space-y-1.5">
            <Label>DPAN</Label>
            <Input value={dpanInput} onChange={(e) => setDpanInput(e.target.value)} placeholder="Paste DPAN here" className="bg-white/5 border-white/10 font-mono" />
          </div>
          {dpanResult && (
            <Button variant="outline" size="sm" className="text-xs border-white/20" onClick={() => setDpanInput(dpanResult)}>
              Use tokenized DPAN
            </Button>
          )}
          <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => detokenizeMut.mutate()} disabled={detokenizeMut.isPending || !dpanInput}>
            {detokenizeMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Detokenizing…</> : "Detokenize"}
          </Button>
          {deTokenResult && (
            <div className="rounded-lg border border-blue-500/30 bg-blue-500/10 p-4">
              <p className="text-xs text-blue-400/70 mb-1">Original PAN (masked)</p>
              <p className="font-mono text-blue-400 text-sm">{deTokenResult}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
