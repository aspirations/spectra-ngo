"use client";

import { api, Branch } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, Input, Label } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { PageHeader } from "@/components/ui/page-header";
import { pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";

const emptyForm = { name: "", code: "", city: "", address: "" };

export default function BranchesPage() {
  const qc = useQueryClient();
  const branches = useQuery({ queryKey: ["branches"], queryFn: () => api<Branch[]>("/api/branches") });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Branch | null>(null);
  const [form, setForm] = useState(emptyForm);

  const save = useMutation({
    mutationFn: (status?: string) =>
      api(editing ? `/api/branches/${editing.id}` : "/api/branches", {
        method: editing ? "PUT" : "POST",
        body: JSON.stringify({ ...form, status: status ?? editing?.status ?? "ACTIVE" }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["branches"] });
      close();
    },
  });

  const toggle = useMutation({
    mutationFn: (b: Branch) =>
      api(`/api/branches/${b.id}`, {
        method: "PUT",
        body: JSON.stringify({
          name: b.name,
          code: b.code,
          city: b.city ?? "",
          address: b.address ?? "",
          status: b.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
        }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["branches"] }),
  });

  function openNew() {
    setEditing(null);
    setForm(emptyForm);
    setOpen(true);
  }

  function openEdit(b: Branch) {
    setEditing(b);
    setForm({ name: b.name, code: b.code, city: b.city ?? "", address: b.address ?? "" });
    setOpen(true);
  }

  function close() {
    setOpen(false);
    setEditing(null);
    setForm(emptyForm);
    save.reset();
  }

  return (
    <div className="desk">
      <PageHeader
        title="Branches"
        description="Each branch keeps its own stock, sales and staff. Assign people to branches from the People page."
        actions={<Button onClick={openNew}>Add branch</Button>}
      />
      <div className="grid gap-3 sm:grid-cols-2">
        {branches.data?.map((b) => (
          <Card key={b.id}>
            <p className="text-xs uppercase text-moss">
              {b.code} · {pretty(b.status)}
            </p>
            <h2 className="text-lg font-bold">{b.name}</h2>
            <p className="text-sm text-ink/60">{[b.city, b.address].filter(Boolean).join(" · ") || "No address"}</p>
            <div className="mt-3 flex gap-2">
              <Button variant="outline" onClick={() => openEdit(b)}>
                Edit
              </Button>
              <Button variant="outline" onClick={() => toggle.mutate(b)} disabled={toggle.isPending}>
                {b.status === "ACTIVE" ? "Disable" : "Enable"}
              </Button>
            </div>
          </Card>
        ))}
      </div>
      {branches.error && <p className="text-sm text-clay">{(branches.error as Error).message}</p>}
      {toggle.error && <p className="text-sm text-clay">{(toggle.error as Error).message}</p>}
      <Modal open={open} title={editing ? "Edit branch" : "New branch"} onClose={close}>
        <form
          className="space-y-3"
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            save.mutate(undefined);
          }}
        >
          <div>
            <Label>Name</Label>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </div>
          <div>
            <Label>Code</Label>
            <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="PUNE" required />
          </div>
          <div>
            <Label>City</Label>
            <Input value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
          </div>
          <div>
            <Label>Address</Label>
            <Input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          </div>
          {save.error && <p className="text-sm text-clay">{(save.error as Error).message}</p>}
          <Button className="w-full" disabled={save.isPending}>
            {save.isPending ? "Saving…" : editing ? "Save changes" : "Add branch"}
          </Button>
        </form>
      </Modal>
    </div>
  );
}
