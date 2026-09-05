"use client";

import { api, getUser, User } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/input";
import { PageHeader, SearchField } from "@/components/ui/page-header";
import { inr, pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { useMemo, useState } from "react";

type Run = { id: number; periodYear: number; periodMonth: number; status: string };
type Item = {
  id: number;
  userId: number;
  baseSalary: number;
  workingDays: number;
  unpaidLeaveDays: number;
  lopDeduction: number;
  storeDues: number;
  otherDeductions: number;
  advanceDeduction?: number;
  reimbursementCredit?: number;
  netPayout: number;
  rolledOverStoreDebt: number;
};

export default function PayrollDetailPage() {
  const { id } = useParams<{ id: string }>();
  const me = getUser();
  const qc = useQueryClient();
  const runQ = useQuery({
    queryKey: ["payroll", id],
    queryFn: () => api<{ run: Run; items: Item[] }>(`/api/payroll/runs/${id}`),
  });
  const approve = useMutation({
    mutationFn: () => api(`/api/payroll/runs/${id}/approve`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["payroll", id] }),
  });
  const disburse = useMutation({
    mutationFn: () => api(`/api/payroll/runs/${id}/disburse`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["payroll", id] }),
  });
  const staff = useQuery({ queryKey: ["users"], queryFn: () => api<User[]>("/api/users") });
  const [search, setSearch] = useState("");
  const names = useMemo(() => {
    const map = new Map<number, string>();
    staff.data?.forEach((u) => map.set(u.id, u.fullName));
    return map;
  }, [staff.data]);
  if (!runQ.data) return <p className="text-sm text-ink/50">Loading payslips…</p>;
  const { run, items } = runQ.data;
  const needle = search.trim().toLowerCase();
  const visible = items.filter((item) => {
    if (!needle) return true;
    const name = names.get(item.userId) ?? "";
    return `${name} ${item.userId}`.toLowerCase().includes(needle);
  });

  return (
    <div className="desk">
      <PageHeader
        title={`${run.periodYear}-${String(run.periodMonth).padStart(2, "0")}`}
        description={pretty(run.status)}
        actions={
          <>
            {run.status === "DRAFT" && <Button onClick={() => approve.mutate()}>Approve</Button>}
            {run.status === "APPROVED" && me?.role === "NGO_ADMIN" && (
              <Button onClick={() => disburse.mutate()}>Disburse</Button>
            )}
            <Button variant="outline" onClick={() => window.print()}>
              Print
            </Button>
          </>
        }
      />
      <SearchField value={search} onChange={setSearch} placeholder="Find a worker…" count={visible.length} />
      {visible.map((item) => (
        <Card key={item.id} className="print:break-inside-avoid">
          <p className="text-xs uppercase text-ink/50">
            {names.get(item.userId) ?? `Staff ${item.userId}`} · payslip #{item.id}
          </p>
          <dl className="mt-2 grid grid-cols-2 gap-2 text-sm">
            <dt>Base</dt>
            <dd className="text-right">{inr(item.baseSalary)}</dd>
            <dt>Working days</dt>
            <dd className="text-right">{item.workingDays}</dd>
            <dt>Unpaid / LOP days</dt>
            <dd className="text-right">{item.unpaidLeaveDays}</dd>
            <dt>LOP deduction</dt>
            <dd className="text-right">{inr(item.lopDeduction)}</dd>
            <dt>Store dues</dt>
            <dd className="text-right">{inr(item.storeDues)}</dd>
            <dt>Advance</dt>
            <dd className="text-right">{inr(item.advanceDeduction ?? 0)}</dd>
            <dt>Pocket reimbursements</dt>
            <dd className="text-right">{inr(item.reimbursementCredit ?? 0)}</dd>
            <dt>Other</dt>
            <dd className="text-right">{inr(item.otherDeductions)}</dd>
            <dt>Rolled-over debt</dt>
            <dd className="text-right">{inr(item.rolledOverStoreDebt)}</dd>
            <dt className="font-bold">Net payout</dt>
            <dd className="text-right font-bold">{inr(item.netPayout)}</dd>
          </dl>
        </Card>
      ))}
    </div>
  );
}
