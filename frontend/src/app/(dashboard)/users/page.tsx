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
    onSuccess: (u) => { setCreatedUser(u); toast.success(`User created: ${u.userId.slice(0, 8)}…`); },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Create failed";
      toast.error(msg);
    },
  });

  const searchMut = useMutation({
    mutationFn: () => users.getByEmail(searchEmail),
    onSuccess: (u) => { setFoundUser(u); },
    onError: () => toast.error("User not found"),
  });

  const f = (k: keyof typeof createForm, v: string) => setCreateForm((prev) => ({ ...prev, [k]: v }));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Users</h1>
        <p className="text-white/40 text-sm mt-1">User management via user-svc</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Create User */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider flex items-center gap-2">
            <UserPlus size={14} /> Create User
          </h2>
          <div className="grid grid-cols-2 gap-3">
            {(["firstName", "lastName", "email", "password", "phone", "iban"] as const).map((key) => (
              <div key={key} className={`space-y-1.5 ${key === "email" || key === "iban" ? "col-span-2" : ""}`}>
                <Label>{key.charAt(0).toUpperCase() + key.slice(1)}</Label>
                <Input
                  type={key === "password" ? "password" : "text"}
                  value={createForm[key]}
                  onChange={(e) => f(key, e.target.value)}
                  className="bg-white/5 border-white/10"
                />
              </div>
            ))}
          </div>
          <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => createMut.mutate()} disabled={createMut.isPending}>
            {createMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Creating…</> : "Create User"}
          </Button>
          {createdUser && (
            <div className="rounded-lg border border-green-500/30 bg-green-500/10 p-4 space-y-1 text-sm">
              <p className="text-green-400 font-medium">User created ✅</p>
              <p className="text-white/60">ID: <span className="font-mono text-white/80 text-xs">{createdUser.userId}</span></p>
              <p className="text-white/60">{createdUser.firstName} {createdUser.lastName}</p>
              <Badge className="bg-white/10 text-white/60 border-white/10">{createdUser.status}</Badge>
            </div>
          )}
        </div>

        {/* Search User */}
        <div className="rounded-xl border border-white/10 bg-[#1a1d27] p-6 space-y-4">
          <h2 className="text-sm font-semibold text-white/70 uppercase tracking-wider flex items-center gap-2">
            <Search size={14} /> Find User
          </h2>
          <div className="space-y-1.5">
            <Label>Email</Label>
            <Input value={searchEmail} onChange={(e) => setSearchEmail(e.target.value)} placeholder="user@example.com" className="bg-white/5 border-white/10" />
          </div>
          <Button className="w-full bg-blue-600 hover:bg-blue-700" onClick={() => searchMut.mutate()} disabled={searchMut.isPending || !searchEmail}>
            {searchMut.isPending ? <><Loader2 size={16} className="animate-spin mr-2" />Searching…</> : <><Search size={14} className="mr-2" />Search</>}
          </Button>

          {foundUser && (
            <div className="rounded-lg border border-white/10 bg-white/5 p-4 space-y-2 text-sm">
              <div className="flex items-center gap-2 mb-2">
                <UserIcon size={14} className="text-blue-400" />
                <span className="font-medium">{foundUser.firstName} {foundUser.lastName}</span>
                <Badge className="bg-white/10 text-white/60 border-white/10 ml-auto">{foundUser.status}</Badge>
              </div>
              <p className="text-white/50">Email: <span className="text-white/80">{foundUser.email}</span></p>
              <p className="text-white/50">ID: <span className="font-mono text-white/60 text-xs">{foundUser.userId}</span></p>
              {foundUser.iban && <p className="text-white/50">IBAN: <span className="font-mono text-white/60 text-xs">{foundUser.iban}</span></p>}
              <p className="text-white/50">Created: <span className="text-white/60">{new Date(foundUser.createdAt).toLocaleDateString()}</span></p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
