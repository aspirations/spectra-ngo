"use client";

import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, Input, Label } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { PageHeader } from "@/components/ui/page-header";
import { pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";

type Ngo = {
  id: number;
  name: string;
  code: string;
  timezone: string;
  currency: string;
  status: string;
  adminEmail?: string;
};

export default function NgosPage() {
  const qc = useQueryClient();
  const ngos = useQuery({ queryKey: ["ngos"], queryFn: () => api<Ngo[]>("/api/platform/ngos") });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({
    name: "",
    code: "",
    adminName: "",
    adminEmail: "",
    adminPassword: "",
    firstBranchName: "Main Centre",
    firstBranchCode: "MAIN",
    timezone: "Asia/Kolkata",
    currency: "INR",
  });

  const create = useMutation({
    mutationFn: () =>
      api("/api/platform/ngos", {
        method: "POST",
        body: JSON.stringify(form),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["ngos"] });
      setOpen(false);
    },
  });

  const toggle = useMutation({
    mutationFn: (ngo: Ngo) =>
      api(`/api/platform/ngos/${ngo.id}/status`, {
        method: "PUT",
        body: JSON.stringify({ status: ngo.status === "ACTIVE" ? "INACTIVE" : "ACTIVE" }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ngos"] }),
  });

  return (
    <div className="desk">
      <PageHeader
        title="Organisations"
        description="Platform admin only. Create an NGO, then sign in as that NGO’s admin. Tenants never see each other’s dogs or people."
        actions={<Button onClick={() => setOpen(true)}>Create NGO</Button>}
      />
      <div className="grid gap-3 sm:grid-cols-2">
        {ngos.data?.map((n) => (
          <Card key={n.id}>
            <p className="text-xs uppercase text-moss">
              {n.code} · {pretty(n.status)}
            </p>
            <h2 className="text-lg font-bold">{n.name}</h2>
            <p className="text-sm text-ink/60">
              {n.timezone} · {n.currency}
            </p>
            {n.adminEmail && <p className="text-xs text-ink/50">{n.adminEmail}</p>}
            <Button className="mt-3" variant="outline" onClick={() => toggle.mutate(n)} disabled={toggle.isPending}>
              {n.status === "ACTIVE" ? "Disable" : "Enable"}
            </Button>
          </Card>
        ))}
      </div>
      {ngos.error && <p className="text-sm text-clay">{(ngos.error as Error).message}</p>}
      <Modal open={open} title="New NGO" onClose={() => setOpen(false)}>
        <form
          className="space-y-3"
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            create.mutate();
          }}
        >
          <div>
            <Label>Name</Label>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </div>
          <div>
            <Label>Code</Label>
            <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} required />
          </div>
          <div>
            <Label>NGO admin name</Label>
            <Input value={form.adminName} onChange={(e) => setForm({ ...form, adminName: e.target.value })} required />
          </div>
          <div>
            <Label>NGO admin email</Label>
            <Input type="email" value={form.adminEmail} onChange={(e) => setForm({ ...form, adminEmail: e.target.value })} required />
          </div>
          <div>
            <Label>NGO admin password</Label>
            <Input
              type="password"
              value={form.adminPassword}
              onChange={(e) => setForm({ ...form, adminPassword: e.target.value })}
              required
            />
          </div>
          <div>
            <Label>First centre</Label>
            <Input value={form.firstBranchName} onChange={(e) => setForm({ ...form, firstBranchName: e.target.value })} />
          </div>
          {create.error && <p className="text-sm text-clay">{(create.error as Error).message}</p>}
          <Button className="w-full" disabled={create.isPending}>
            Create organisation
          </Button>
        </form>
      </Modal>
    </div>
  );
}
