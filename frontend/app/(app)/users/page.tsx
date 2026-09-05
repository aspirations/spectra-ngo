"use client";

import { api, Branch, getUser, Role, ROLE_MENUS, User } from "@/lib/api";
import { LocalThumb, PhotoPicker } from "@/components/photo-picker";
import { Button } from "@/components/ui/button";
import { Input, Label, Select, Textarea } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { PageHeader, SearchField } from "@/components/ui/page-header";
import { commitLocalPhoto, emptyPhotoDraft } from "@/lib/local-photos";
import { inr, pretty } from "@/lib/utils";
import { ChevronRight } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { FormEvent, useMemo, useState } from "react";

const ALL_ROLES: Role[] = ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER", "VET_TECH_EMPLOYEE", "EMPLOYEE"];

function toggleId(ids: number[], id: number) {
  return ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];
}

export default function UsersPage() {
  const me = getUser();
  const qc = useQueryClient();
  const users = useQuery({ queryKey: ["users"], queryFn: () => api<User[]>("/api/users") });
  const branches = useQuery({ queryKey: ["branches"], queryFn: () => api<Branch[]>("/api/branches") });
  const roles = useMemo(
    () => (me?.role === "NGO_ADMIN" ? ALL_ROLES : ALL_ROLES.filter((r) => r !== "NGO_ADMIN")),
    [me?.role],
  );
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("");
  const [photo, setPhoto] = useState(emptyPhotoDraft);
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    role: "EMPLOYEE" as Role,
    branchIds: [] as number[],
    baseMonthlySalary: "0",
    creditLimit: "0",
    otherFixedDeductions: "0",
    aadhaarNo: "",
    homeAddress: "",
    familyDetails: "",
  });

  const add = useMutation({
    mutationFn: async () => {
      const created = await api<User>("/api/users", {
        method: "POST",
        body: JSON.stringify({
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          role: form.role,
          branchIds: form.role === "NGO_ADMIN" ? undefined : form.branchIds,
          baseMonthlySalary: Number(form.baseMonthlySalary),
          creditLimit: Number(form.creditLimit),
          otherFixedDeductions: Number(form.otherFixedDeductions),
          aadhaarNo: form.aadhaarNo.trim() || null,
          homeAddress: form.homeAddress.trim() || null,
          familyDetails: form.familyDetails.trim() || null,
        }),
      });
      await commitLocalPhoto("person", created.id, photo);
      return created;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["users"] });
      setPhoto(emptyPhotoDraft());
      setOpen(false);
    },
  });

  const visible = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return (users.data ?? []).filter((u) => {
      if (status && u.status !== status) return false;
      if (!needle) return true;
      return `${u.fullName} ${u.email} ${u.role} ${u.status}`.toLowerCase().includes(needle);
    });
  }, [users.data, q, status]);

  return (
    <div className="desk">
      <PageHeader
        title="People"
        description="Search the roster, then open someone for login, salary, and store passbook."
        actions={
          <Button
            onClick={() => {
              setPhoto(emptyPhotoDraft());
              setOpen(true);
            }}
          >
            Add person
          </Button>
        }
      />
      <SearchField value={q} onChange={setQ} placeholder="Name, email, role…" count={visible.length} />
      <div className="flex flex-wrap gap-1.5">
        {[
          { id: "", label: "Everyone" },
          { id: "ACTIVE", label: "Active" },
          { id: "INACTIVE", label: "Inactive" },
        ].map((chip) => (
          <button
            key={chip.id || "all"}
            type="button"
            onClick={() => setStatus(chip.id)}
            className={`min-h-9 rounded-full px-3 text-xs font-semibold ${
              status === chip.id ? "bg-moss text-white" : "bg-white text-ink/70 ring-1 ring-moss/15"
            }`}
          >
            {chip.label}
          </button>
        ))}
      </div>
      <ul className="overflow-hidden rounded-2xl border border-moss/10 bg-white">
        {visible.map((u) => (
          <li key={u.id} className="border-b border-moss/10 last:border-b-0">
            <Link href={`/users/${u.id}`} className="flex min-h-12 items-center justify-between gap-2 px-3 py-2">
              <LocalThumb
                kind="person"
                id={u.id}
                fallback="👤"
                className="h-10 w-10 shrink-0 rounded-full text-lg"
              />
              <span className="min-w-0 flex-1">
                <b className="block truncate">{u.fullName}</b>
                <span className="block truncate text-xs text-ink/55">
                  {pretty(u.role)} · {pretty(u.status)} · {inr(u.baseMonthlySalary)}
                </span>
              </span>
              <ChevronRight size={18} className="shrink-0 text-ink/30" />
            </Link>
          </li>
        ))}
      </ul>
      {visible.length === 0 && <p className="py-10 text-center text-sm text-ink/50">No people match that search.</p>}
      <Modal open={open} title="New person for this NGO" onClose={() => setOpen(false)}>
        <form
          className="space-y-3"
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            add.mutate();
          }}
        >
          <div>
            <Label>Name</Label>
            <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          </div>
          <PhotoPicker draft={photo} onChange={setPhoto} />
          <div>
            <Label>Username (login email)</Label>
            <Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          </div>
          <div>
            <Label>Password</Label>
            <Input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
          </div>
          <div>
            <Label>Role</Label>
            <Select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
              {roles.map((r) => (
                <option key={r}>{r}</option>
              ))}
            </Select>
            <p className="mt-1 text-xs text-ink/50">{ROLE_MENUS[form.role].join(" · ")}</p>
          </div>
          {form.role !== "NGO_ADMIN" && (
            <div>
              <Label>Centres they can work in</Label>
              <ul className="space-y-1 text-sm">
                {branches.data?.map((b) => (
                  <li key={b.id}>
                    <label className="flex min-h-11 items-center gap-2">
                      <input
                        type="checkbox"
                        checked={form.branchIds.includes(b.id)}
                        onChange={() => setForm({ ...form, branchIds: toggleId(form.branchIds, b.id) })}
                      />
                      {b.name}
                    </label>
                  </li>
                ))}
              </ul>
            </div>
          )}
          <div>
            <Label>Aadhaar number</Label>
            <Input
              value={form.aadhaarNo}
              onChange={(e) => setForm({ ...form, aadhaarNo: e.target.value })}
              maxLength={16}
            />
          </div>
          <div>
            <Label>Home address</Label>
            <Textarea rows={3} value={form.homeAddress} onChange={(e) => setForm({ ...form, homeAddress: e.target.value })} />
          </div>
          <div>
            <Label>Family / emergency contact</Label>
            <Textarea
              rows={3}
              value={form.familyDetails}
              onChange={(e) => setForm({ ...form, familyDetails: e.target.value })}
              placeholder="Names, relation, phone…"
            />
          </div>
          <div>
            <Label>Monthly salary</Label>
            <Input
              type="number"
              step="0.01"
              value={form.baseMonthlySalary}
              onChange={(e) => setForm({ ...form, baseMonthlySalary: e.target.value })}
            />
          </div>
          <div>
            <Label>Store credit limit</Label>
            <Input
              type="number"
              step="0.01"
              value={form.creditLimit}
              onChange={(e) => setForm({ ...form, creditLimit: e.target.value })}
            />
          </div>
          {add.error && <p className="text-sm text-clay">{(add.error as Error).message}</p>}
          <Button className="w-full" disabled={add.isPending}>
            Create person
          </Button>
        </form>
      </Modal>
    </div>
  );
}
