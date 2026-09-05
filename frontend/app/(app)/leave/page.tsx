"use client";

import { api, getUser, User } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Combobox, toStaffOptions } from "@/components/ui/combobox";
import { Fold } from "@/components/ui/fold";
import { Card, Input, Label, Select, Textarea } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
import { pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";

type Leave = { id: number; userId: number; type: string; fromDate: string; toDate: string; days: number; status: string; reason?: string };

export default function LeavePage() {
  const me = getUser();
  const qc = useQueryClient();
  const admin = me?.role === "NGO_ADMIN" || me?.role === "BRANCH_ADMIN" || me?.role === "INVENTORY_MANAGER";
  const list = useQuery({ queryKey: ["leave"], queryFn: () => api<Leave[]>("/api/leave") });
  const users = useQuery({ queryKey: ["users"], queryFn: () => api<User[]>("/api/users"), enabled: admin });
  const [form, setForm] = useState({ type: "UNPAID_LEAVE_LOP", fromDate: "", toDate: "", reason: "", userId: "" });
  const apply = useMutation({
    mutationFn: () =>
      api("/api/leave", {
        method: "POST",
        body: JSON.stringify({
          type: form.type,
          fromDate: form.fromDate,
          toDate: form.toDate,
          reason: form.reason,
          userId: form.userId ? Number(form.userId) : undefined,
        }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["leave"] }),
  });
  const review = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      api(`/api/leave/${id}/review`, { method: "POST", body: JSON.stringify({ status }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["leave"] }),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    apply.mutate();
  }

  return (
    <div className="desk">
      <PageHeader
        title="Leave & LOP"
        description="Everyone is treated as present. Record only absences — paid leave (no pay cut) or LOP (deducted on payroll)."
      />
      <Fold title="Record absence">
        <form className="space-y-3" onSubmit={submit}>
          {admin && (
            <>
              <Label>Staff</Label>
              <Combobox
                value={form.userId}
                onChange={(userId) => setForm({ ...form, userId })}
                options={[{ value: "", label: "Myself" }, ...toStaffOptions(users.data)]}
                placeholder="Myself or search staff"
                searchPlaceholder="Staff name…"
                allowClear={false}
              />
            </>
          )}
          <Label>Absence type</Label>
          <Select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
            <option value="UNPAID_LEAVE_LOP">LOP — unpaid (salary deducted)</option>
            <option value="HALF_DAY_LOP">Half-day LOP</option>
            <option value="PAID_CASUAL">Paid casual</option>
            <option value="PAID_SICK">Paid sick</option>
          </Select>
          <Input type="date" value={form.fromDate} onChange={(e) => setForm({ ...form, fromDate: e.target.value })} required />
          <Input type="date" value={form.toDate} onChange={(e) => setForm({ ...form, toDate: e.target.value })} required />
          <Textarea placeholder="Reason" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
          {apply.error && <p className="text-sm text-clay">{(apply.error as Error).message}</p>}
          <Button className="w-full">{admin ? "Record absence" : "Apply"}</Button>
        </form>
      </Fold>
      {list.data?.map((l) => (
        <Card key={l.id}>
          <p className="font-semibold">{pretty(l.type)}</p>
          <p className="text-sm">
            {l.fromDate} → {l.toDate} ({l.days} days) · {pretty(l.status)}
          </p>
          {l.reason && <p className="text-sm text-ink/60">{l.reason}</p>}
          {admin && l.status === "PENDING" && (
            <div className="mt-2 flex gap-2">
              <Button onClick={() => review.mutate({ id: l.id, status: "APPROVED" })}>Approve</Button>
              <Button variant="danger" onClick={() => review.mutate({ id: l.id, status: "REJECTED" })}>
                Reject
              </Button>
            </div>
          )}
        </Card>
      ))}
    </div>
  );
}
