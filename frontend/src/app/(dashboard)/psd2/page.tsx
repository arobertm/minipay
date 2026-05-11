"use client";

import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { psd2 } from "@/lib/api/psd2";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Loader2, Globe, Send, FileCheck } from "lucide-react";

export default function Psd2Page() {
  const [consentId, setConsentId] = useState("");
  const [createdConsent, setCreatedConsent] = useState<string | null>(null);
  const [userId, setUserId] = useState("");
  const [iban, setIban] = useState("RO49AAAA1B31007593840000");

  const [sepaForm, setSepaForm] = useState({
    debtorIban: "RO49AAAA1B31007593840000",
    creditorIban: "RO49BBBB1B31007593840001",
    creditorName: "Beneficiary Corp",
    amount: 100,
    currency: "EUR",
    reference: "INV-2026-001",
  });
  const [paymentResult, setPaymentResult] = useState<{ paymentId: string; status: string } | null>(null);

  const consentMut = useMutation({
    mutationFn: () => psd2.createConsent(userId, [iban], ["ReadAccountList", "ReadBalances", "ReadTransactions"], new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]),
    onSuccess: (d) => { setCreatedConsent(d.consentId); setConsentId(d.consentId); toast.success("Consent created"); },
    onError: () => toast.error("Consent creation failed"),
  });

  const accountsQ = useQuery({
    queryKey: ["psd2-accounts", consentId],
    queryFn: () => psd2.getAccounts(consentId),
    enabled: !!consentId,
  });

  const sepaMut = useMutation({
    mutationFn: () => psd2.initiateSepa(sepaForm),
    onSuccess: (d) => { setPaymentResult(d); toast.success(`SEPA initiated: ${d.paymentId.slice(0, 8)}…`); },
    onError: () => toast.error("SEPA initiation failed"),
  });

  const sf = (k: keyof typeof sepaForm, v: string | number) => setSepaForm((p) => ({ ...p, [k]: v }));

  return (
    <div className="space-y-8 p-6">
      <div className="fadeIn">
        <h1 className="text-4xl font-display font-bold text-foreground">PSD2 Open Banking</h1>
        <p className="text-foreground/60 text-base mt-2 font-medium">Servicii informații cont (AIS) + Servicii inițiere plăți (PIS)</p>
      </div>

      <Tabs defaultValue="ais" className="fadeIn stagger-1">
        <TabsList className="bg-foreground/5 border border-border/40 p-1">
          <TabsTrigger value="ais" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground font-medium">
            AIS — Info Cont
          </TabsTrigger>
          <TabsTrigger value="pis" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground font-medium">
            PIS — Inițiere Plată
          </TabsTrigger>
        </TabsList>

        {/* AIS */}
        <TabsContent value="ais" className="space-y-5 mt-6">
          <div className="card-premium space-y-5">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-cyan-500/20">
                <FileCheck size={18} className="text-cyan-400" />
              </div>
              <h2 className="text-base font-display font-semibold">Creare Consimțământ</h2>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label className="text-foreground/70 font-medium">User ID</Label>
                <Input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="user UUID" className="font-mono text-xs" />
              </div>
              <div className="space-y-2">
                <Label className="text-foreground/70 font-medium">IBAN</Label>
                <Input value={iban} onChange={(e) => setIban(e.target.value)} className="font-mono text-xs" />
              </div>
            </div>
            <div className="flex items-end gap-3">
              <div className="flex-1 space-y-2">
                <Label className="text-foreground/70 font-medium">Consent ID</Label>
                <Input value={consentId} onChange={(e) => setConsentId(e.target.value)} placeholder="existing consent UUID" className="font-mono text-xs" />
              </div>
              <Button onClick={() => consentMut.mutate()} disabled={consentMut.isPending}>
                {consentMut.isPending ? <Loader2 size={16} className="animate-spin" /> : "Creează consimțământ"}
              </Button>
            </div>
            {createdConsent && (
              <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 p-3">
                <p className="text-xs text-emerald-400 font-mono font-semibold">✓ Consimțământ creat: {createdConsent}</p>
              </div>
            )}
          </div>

          {consentId && (
            <div className="card-premium">
              <h2 className="text-base font-display font-semibold mb-5">Conturi asociate</h2>
              {accountsQ.isLoading ? (
                <p className="text-foreground/30 text-sm">Loading…</p>
              ) : accountsQ.data?.length ? (
                <div className="divide-y divide-border/20">
                  {accountsQ.data.map((a) => (
                    <div key={a.accountId} className="py-4 flex items-center justify-between">
                      <div>
                        <p className="font-mono text-sm font-semibold text-foreground">{a.iban}</p>
                        <p className="text-xs text-foreground/50 mt-1">{a.currency}</p>
                      </div>
                      <Badge className="bg-cyan-500/20 text-cyan-400 border-cyan-500/30 font-mono">
                        {a.accountId.slice(0, 8)}
                      </Badge>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-foreground/30 text-sm">Niciun cont găsit pentru acest consimțământ.</p>
              )}
            </div>
          )}
        </TabsContent>

        {/* PIS */}
        <TabsContent value="pis" className="space-y-5 mt-6">
          <div className="card-premium space-y-5">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-primary/20">
                <Send size={18} className="text-primary" />
              </div>
              <h2 className="text-base font-display font-semibold">SEPA Credit Transfer</h2>
            </div>
            <div className="space-y-4">
              {(["debtorIban", "creditorIban", "creditorName", "reference"] as const).map((k) => {
                const SEPA_LABELS: Record<string, string> = {
                  debtorIban: "debtor Iban",
                  creditorIban: "creditor Iban",
                  creditorName: "Nume creditor",
                  reference: "Referință",
                };
                return (
                <div key={k} className="space-y-2">
                  <Label className="text-foreground/70 font-medium capitalize">{SEPA_LABELS[k] ?? k.replace(/([A-Z])/g, ' $1')}</Label>
                  <Input value={String(sepaForm[k])} onChange={(e) => sf(k, e.target.value)} className="font-mono text-xs" />
                </div>
                );
              })}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label className="text-foreground/70 font-medium">Amount</Label>
                  <Input type="number" value={sepaForm.amount} onChange={(e) => sf("amount", parseFloat(e.target.value))} />
                </div>
                <div className="space-y-2">
                  <Label className="text-foreground/70 font-medium">Currency</Label>
                  <Input value={sepaForm.currency} onChange={(e) => sf("currency", e.target.value)} />
                </div>
              </div>
            </div>
            <Button className="w-full" onClick={() => sepaMut.mutate()} disabled={sepaMut.isPending}>
              {sepaMut.isPending
                ? <><Loader2 size={16} className="animate-spin mr-2" />Se inițiază…</>
                : <><Globe size={15} className="mr-2" />Inițiază transfer SEPA</>}
            </Button>
            {paymentResult && (
              <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4 space-y-2">
                <p className="text-emerald-400 font-semibold">✅ SEPA Payment Initiated</p>
                <p className="text-xs text-foreground/50 font-mono">ID: {paymentResult.paymentId}</p>
                <Badge className="bg-emerald-500/20 text-emerald-400 border-emerald-500/30">{paymentResult.status}</Badge>
              </div>
            )}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
