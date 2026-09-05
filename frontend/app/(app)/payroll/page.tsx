"use client";

import { api, getUser } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Combobox, toStaffOptions } from "@/components/ui/combobox";
import { Fold } from "@/components/ui/fold";
import { Card, Input, Label } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
import { inr, pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";

type Run = { id: number; periodYear: number; periodMonth: number; status: string };
type Item = { id: number; userId: number; baseSalary: number; lopDeduction: number; storeDues: number; netPayout: number; unpaidLeaveDays: number };
type Staff = { id: number; fullName: string };
type Advance = { id: number; userId: number; amount: number; advancedAt: string; status: string; notes?: string };

export default function PayrollPage() {
  const me = getUser();
  const qc = useQueryClient();
  const admin = me?.role === "NGO_ADMIN" || me?.role === "BRANCH_ADMIN" || me?.role === "INVENTORY_MANAGER";
  const runs = useQuery({ queryKey: ["payroll-runs"], queryFn: () => api<Run[]>("/api/payroll/runs"), enabled: admin });
  const mine = useQuery({ queryKey: ["my-payslips"], queryFn: () => api<Item[]>("/api/payroll/payslips") });
  const staff = useQuery({ queryKey: ["users"], queryFn: () => api<Staff[]>("/api/users"), enabled: admin });
  const advances = useQuery({ queryKey: ["advances"], queryFn: () => api<Advance[]>("/api/payroll/advances"), enabled: admin });
  const [advUser, setAdvUser] = useState("");
  const [advAmt, setAdvAmt] = useState("");
  const [advNotes, setAdvNotes] = useState("");
  const generate = useMutation({
    mutationFn: () => api("/api/payroll/runs/generate", { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["payroll-runs"] }),
  });
  const recordAdvance = useMutation({
    mutationFn: () =>
      api("/api/payroll/advances", {
        method: "POST",
        body: JSON.stringify({ userId: Number(advUser), amount: Number(advAmt), notes: advNotes }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["advances"] });
      setAdvAmt("");
      setAdvNotes("");
    },
  });

  return (
    <div className="desk">
      <PageHeader
        title="Payroll"
        description="Base minus LOP, store dues, and advances. Search the worker — this roster is dozens, not a handful."
        actions={
          admin ? (
            <Button onClick={() => generate.mutate()} disabled={generate.isPending}>
              Generate last month
            </Button>
          ) : undefined
        }
      />
      {generate.error && <p className="text-sm text-clay">{(generate.error as Error).message}</p>}
      {admin && (
        <Fold title="Salary advances">
          <form
            className="mb-3 grid gap-2 sm:grid-cols-3"
            onSubmit={(e) => {
              e.preventDefault();
              recordAdvance.mutate();
            }}
          >
            <div>
              <Label>Staff</Label>
              <Combobox
                value={advUser}
                onChange={setAdvUser}
                options={toStaffOptions(staff.data)}
                placeholder="Search staff"
                searchPlaceholder="Name…"
              />
            </div>
            <div>
              <Label>Amount</Label>
              <Input type="number" step="0.01" value={advAmt} onChange={(e) => setAdvAmt(e.target.value)} required />
            </div>
            <div>
              <Label>Notes</Label>
              <Input value={advNotes} onChange={(e) => setAdvNotes(e.target.value)} />
            </div>
            <Button className="sm:col-span-3">Record advance</Button>
          </form>
          {recordAdvance.error && <p className="text-sm text-clay">{(recordAdvance.error as Error).message}</p>}
          <ul className="space-y-2 text-sm">
            {advances.data?.map((a) => (
              <li key={a.id} className="flex justify-between rounded-xl bg-sand px-3 py-2">
                <span>
                  Staff #{a.userId} · {a.advancedAt} · {pretty(a.status)}
                </span>
                <b>{inr(a.amount)}</b>
              </li>
            ))}
          </ul>
        </Fold>
      )}
      {admin &&
        runs.data?.map((r) => (
          <Link key={r.id} href={`/payroll/${r.id}`}>
            <Card className="mb-2 hover:border-moss/40">
              <p className="font-semibold">
                {r.periodYear}-{String(r.periodMonth).padStart(2, "0")}
              </p>
              <p className="text-sm">{pretty(r.status)}</p>
            </Card>
          </Link>
        ))}
      <h2 className="font-semibold">My payslips</h2>
      {mine.data?.map((p) => (
        <Card key={p.id}>
          <p>Net {inr(p.netPayout)}</p>
          <p className="text-sm text-ink/60">
            Base {inr(p.baseSalary)} − LOP {inr(p.lopDeduction)} − store {inr(p.storeDues)} · unpaid days {p.unpaidLeaveDays}
          </p>
        </Card>
      ))}
    </div>
  );
}
