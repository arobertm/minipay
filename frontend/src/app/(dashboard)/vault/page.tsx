"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { vault } from "@/lib/api/vault";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Loader2, ArrowRight, KeyRound, Copy, Lock, Unlock } from "lucide-react";

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
    <div className="space-y-8 p-6">
      <div className="fadeIn">
        <h1 className="text-4xl font-display font-bold text-foreground">Token Vault</h1>
        <p className="text-foreground/60 text-base mt-2 font-medium">Tokenizare EMV — PAN → DPAN prin AES-256-GCM</p>
      </div>

      {/* Visual flow diagram */}
      <div className="card-premium fadeIn stagger-1">
        <div className="flex items-center justify-center gap-4 py-2">
          <div className="flex flex-col items-center gap-2">
            <div className="rounded-xl border border-border/40 bg-foreground/5 px-6 py-3 text-sm font-mono font-semibold text-foreground/70">
              PAN
            </div>
            <span className="text-xs text-foreground/40 font-medium">Card Number</span>
          </div>
          <ArrowRight size={22} className="text-foreground/30 shrink-0" />
          <div className="flex flex-col items-center gap-2">
            <div className="rounded-xl border border-primary/40 bg-primary/10 px-6 py-3 text-sm text-primary flex items-center gap-2.5 font-semibold">
              <KeyRound size={16} /> AES-256-GCM Vault
            </div>
            <span className="text-xs text-foreground/40 font-medium">Encrypted Storage</span>
          </div>
          <ArrowRight size={22} className="text-foreground/30 shrink-0" />
          <div className="flex flex-col items-center gap-2">
            <div className="rounded-xl border border-cyan-500/40 bg-cyan-500/10 px-6 py-3 text-sm font-mono font-semibold text-cyan-400">
              DPAN
            </div>
            <span className="text-xs text-foreground/40 font-medium">Token</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Tokenize */}
        <div className="card-premium space-y-5 fadeIn stagger-2">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/20">
              <Lock size={18} className="text-primary" />
            </div>
            <h2 className="text-base font-display font-semibold">Tokenizează PAN</h2>
          </div>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label className="text-foreground/70 font-medium">Număr card (PAN)</Label>
              <Input value={panForm.pan} onChange={(e) => setPanForm((f) => ({ ...f, pan: e.target.value }))} className="font-mono" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label className="text-foreground/70 font-medium">Expiry</Label>
                <Input value={panForm.expiry} onChange={(e) => setPanForm((f) => ({ ...f, expiry: e.target.value }))} />
              </div>
              <div className="space-y-2">
                <Label className="text-foreground/70 font-medium">CVV</Label>
                <Input value={panForm.cvv} onChange={(e) => setPanForm((f) => ({ ...f, cvv: e.target.value }))} />
              </div>
            </div>
          </div>
          <Button className="w-full" onClick={() => tokenizeMut.mutate()} disabled={tokenizeMut.isPending}>
            {tokenizeMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Se tokenizează…</> : "Tokenizează cardul"}
          </Button>
          {dpanResult && (
            <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4">
              <p className="text-xs text-emerald-400/70 font-semibold uppercase tracking-wider mb-2">DPAN Generat</p>
              <div className="flex items-center justify-between gap-3">
                <p className="font-mono text-emerald-400 text-sm break-all">{dpanResult}</p>
                <button onClick={() => copyToClipboard(dpanResult)}
                  className="shrink-0 p-2 rounded-lg bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/30 transition-colors">
                  <Copy size={14} />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Detokenize */}
        <div className="card-premium space-y-5 fadeIn stagger-3">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-cyan-500/20">
              <Unlock size={18} className="text-cyan-400" />
            </div>
            <h2 className="text-base font-display font-semibold">Detokenizează DPAN</h2>
          </div>
          <div className="space-y-2">
            <Label className="text-foreground/70 font-medium">DPAN Token</Label>
            <Input value={dpanInput} onChange={(e) => setDpanInput(e.target.value)} placeholder="Paste DPAN here" className="font-mono" />
          </div>
          {dpanResult && (
            <Button variant="outline" size="sm" className="text-xs"
              onClick={() => setDpanInput(dpanResult)}>
              Folosește DPAN tokenizat ↑
            </Button>
          )}
          <Button className="w-full" variant="secondary" onClick={() => detokenizeMut.mutate()} disabled={detokenizeMut.isPending || !dpanInput}>
            {detokenizeMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Se detokenizează…</> : "Detokenizează"}
          </Button>
          {deTokenResult && (
            <div className="rounded-xl border border-cyan-500/30 bg-cyan-500/10 p-4">
              <p className="text-xs text-cyan-400/70 font-semibold uppercase tracking-wider mb-2">PAN original (mascat)</p>
              <p className="font-mono text-cyan-400 text-sm">{deTokenResult}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
