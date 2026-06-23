"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { users, User } from "@/lib/api/users";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2, Search, UserPlus, User as UserIcon } from "lucide-react";

export default function UsersPage() {
  const [createForm, setCreateForm] = useState({ firstName: "", lastName: "", email: "", password: "", phone: "", iban: "" });
  const [searchEmail, setSearchEmail] = useState("");
  const [foundUser, setFoundUser] = useState<User | null>(null);
  const [createdUser, setCreatedUser] = useState<User | null>(null);

  const createMut = useMutation({
    mutationFn: () => users.create(createForm),
    onSuccess: (u) => { setCreatedUser(u); toast.success(`Utilizator creat: ${u.userId.slice(0, 8)}…`); },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Creare eșuată";
      toast.error(msg);
    },
  });

  const searchMut = useMutation({
    mutationFn: () => users.getByEmail(searchEmail),
    onSuccess: (u) => setFoundUser(u),
    onError: () => toast.error("Utilizatorul nu a fost găsit"),
  });

  const f = (k: keyof typeof createForm, v: string) => setCreateForm((prev) => ({ ...prev, [k]: v }));

  return (
    <div className="space-y-8 p-6">
      <div className="fadeIn">
        <h1 className="text-4xl font-display font-bold text-foreground">Utilizatori</h1>
        <p className="text-foreground/60 text-base mt-2 font-medium">Gestionare utilizatori prin user-svc</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Create User */}
        <div className="card-premium space-y-5 fadeIn stagger-1">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/20">
              <UserPlus size={18} className="text-primary" />
            </div>
            <h2 className="text-base font-display font-semibold">Creare utilizator</h2>
          </div>
          <div className="grid grid-cols-2 gap-4">
            {(["firstName", "lastName", "email", "password", "phone", "iban"] as const).map((key) => {
              const USER_LABELS: Record<string, string> = {
                firstName: "Prenume",
                lastName: "Nume",
                email: "email",
                password: "Parolă",
                phone: "Telefon",
                iban: "IBAN",
              };
              return (
              <div key={key} className={`space-y-2 ${key === "email" || key === "iban" ? "col-span-2" : ""}`}>
                <Label className="text-foreground/70 font-medium capitalize">
                  {USER_LABELS[key] ?? key.replace(/([A-Z])/g, ' $1')}
                </Label>
                <Input
                  type={key === "password" ? "password" : "text"}
                  value={createForm[key]}
                  onChange={(e) => f(key, e.target.value)}
                />
              </div>
            );
            })}
          </div>
          <Button className="w-full" onClick={() => createMut.mutate()} disabled={createMut.isPending}>
            {createMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Se creează…</> : "Creează utilizator"}
          </Button>
          {createdUser && (
            <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4 space-y-2">
              <p className="text-emerald-400 font-semibold">✅ Utilizator creat</p>
              <p className="text-foreground/60 text-sm">
                ID: <span className="font-mono text-foreground/80 text-xs">{createdUser.userId}</span>
              </p>
              <p className="text-foreground/60 text-sm">{createdUser.firstName} {createdUser.lastName}</p>
              <Badge className="bg-foreground/10 text-foreground/60 border-border/40">{createdUser.status}</Badge>
            </div>
          )}
        </div>

        {/* Search User */}
        <div className="card-premium space-y-5 fadeIn stagger-2">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-secondary/20">
              <Search size={18} className="text-secondary" />
            </div>
            <h2 className="text-base font-display font-semibold">Caută utilizator</h2>
          </div>
          <div className="space-y-2">
            <Label className="text-foreground/70 font-medium">Email Address</Label>
            <Input value={searchEmail} onChange={(e) => setSearchEmail(e.target.value)} placeholder="user@example.com" />
          </div>
          <Button className="w-full" variant="secondary" onClick={() => searchMut.mutate()} disabled={searchMut.isPending || !searchEmail}>
            {searchMut.isPending
              ? <><Loader2 size={16} className="animate-spin mr-2" />Se caută…</>
              : <><Search size={15} className="mr-2" />Caută</>}
          </Button>

          {foundUser && (
            <div className="rounded-xl border border-border/40 bg-foreground/5 p-4 space-y-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center">
                  <UserIcon size={16} className="text-primary" />
                </div>
                <div>
                  <p className="font-semibold text-foreground">{foundUser.firstName} {foundUser.lastName}</p>
                  <p className="text-xs text-foreground/50">{foundUser.email}</p>
                </div>
                <Badge className="bg-foreground/10 text-foreground/60 border-border/40 ml-auto">{foundUser.status}</Badge>
              </div>
              <div className="space-y-1.5 pt-2 border-t border-border/30 text-sm">
                <p className="text-foreground/50">ID: <span className="font-mono text-foreground/70 text-xs">{foundUser.userId}</span></p>
                {foundUser.iban && <p className="text-foreground/50">IBAN: <span className="font-mono text-foreground/70 text-xs">{foundUser.iban}</span></p>}
                <p className="text-foreground/50">Created: <span className="text-foreground/70">{new Date(foundUser.createdAt).toLocaleDateString()}</span></p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
