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
import { Loader2, Globe, Send } from "lucide-react";

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
    mutationFn: () => psd2.createConsent(userId, [iban]),
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
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">PSD2 Open Banking</h1>
        <p className="text-white/40 text-sm mt-1">Account Information Services (AIS) + Payment Initiation Services (PIS)</p>
      </div>

      <Tabs defaultValue="ais">
        <TabsList className="bg-white/5 border border-white/10">
          <TabsTrigger value="ais" className="data-[state=active]:bg-blue-600">AIS — Account Info</TabsTrigger>
          <TabsTrigger value="pis" className="data-[state=active]:bg-blue-600">PIS — Payment Init</TabsTrigger>
        </TabsList>

        {/* AIS */}
        <TabsContent value="ais" className="space-y-4 mt-6">
          <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
            <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider">Create Consent</h2>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>User ID</Label>
                <Input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="user UUID" className="bg-white/5 border-white/10 font-mono text-xs" />
              </div>
              <div className="space-y-1.5">
                <Label>IBAN</Label>
                <Input value={iban} onChange={(e) => setIban(e.target.value)} className="bg-white/5 border-white/10 font-mono text-xs" />
              </div>
            </div>
            <div className="flex items-end gap-3">
              <div className="flex-1 space-y-1.5">
                <Label>Consent ID (or create new)</Label>
                <Input value={consentId} onChange={(e) => setConsentId(e.target.value)} placeholder="existing consent UUID" className="bg-white/5 border-white/10 font-mono text-xs" />
              </div>
              <Button className="bg-blue-600 hover:bg-blue-700" onClick={() => consentMut.mutate()} disabled={consentMut.isPending}>
                {consentMut.isPending ? <Loader2 size={16} className="animate-spin" /> : "Create Consent"}
              </Button>
            </div>
            {createdConsent && <p className="text-xs text-green-400 font-mono">Consent: {createdConsent}</p>}
          </div>

          {consentId && (
            <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6">
              <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider mb-4">Accounts</h2>
              {accountsQ.isLoading ? <p className="text-white/30 text-sm">Loading…</p> :
                accountsQ.data?.length ? (
                  <div className="divide-y divide-white/5">
                    {accountsQ.data.map((a) => (
                      <div key={a.accountId} className="py-3 flex items-center justify-between">
                        <div>
                          <p className="font-mono text-sm">{a.iban}</p>
                          <p className="text-xs text-white/40">{a.ownerName} · {a.currency}</p>
                        </div>
                        <Badge className="bg-blue-500/20 text-blue-400 border-blue-500/30">{a.accountId.slice(0, 8)}</Badge>
                      </div>
                    ))}
                  </div>
                ) : <p className="text-white/30 text-sm">No accounts found for this consent.</p>}
            </div>
          )}
        </TabsContent>

        {/* PIS */}
        <TabsContent value="pis" className="space-y-4 mt-6">
          <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
            <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider flex items-center gap-2">
              <Send size={14} /> SEPA Credit Transfer
            </h2>
            <div className="grid grid-cols-1 gap-3">
              {(["debtorIban", "creditorIban", "creditorName", "reference"] as const).map((k) => (
                <div key={k} className="space-y-1.5">
                  <Label>{k}</Label>
                  <Input value={String(sepaForm[k])} onChange={(e) => sf(k, e.target.value)} className="bg-white/5 border-white/10 font-mono text-xs" />
                </div>
              ))}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label>Amount</Label>
                  <Input type="number" value={sepaForm.amount} onChange={(e) => sf("amount", parseFloat(e.target.value))} className="bg-white/5 border-white/10" />
                </div>
                <div className="space-y-1.5">
                  <Label>Currency</Label>
                  <Input value={sepaForm.currency} onChange={(e) => sf("currency", e.target.value)} className="bg-white/5 border-white/10" />
                </div>
              </div>
            </div>
            <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => sepaMut.mutate()} disabled={sepaMut.isPending}>
              {sepaMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Initiating…</> : <><Globe size={14} className="mr-2" />Initiate SEPA Transfer</>}
            </Button>
            {paymentResult && (
              <div className="rounded-lg border border-green-500/30 bg-green-500/10 p-4">
                <p className="text-green-400 font-medium">SEPA Payment Initiated ✅</p>
                <p className="text-xs text-white/50 mt-1 font-mono">ID: {paymentResult.paymentId}</p>
                <Badge className="mt-2 bg-green-500/20 text-green-400 border-green-500/30">{paymentResult.status}</Badge>
              </div>
            )}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
