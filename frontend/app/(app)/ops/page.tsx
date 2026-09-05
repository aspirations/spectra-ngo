"use client";

import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Combobox, toResidentOptions, toStaffOptions } from "@/components/ui/combobox";
import { Fold } from "@/components/ui/fold";
import { Card, Input, Select, Textarea } from "@/components/ui/input";
import { PageHeader, SearchField } from "@/components/ui/page-header";
import { inr, pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown } from "lucide-react";
import { FormEvent, ReactNode, useMemo, useState } from "react";

type Snapshot = {
  opexTotal: number;
  opexCount: number;
  dogExpenseTotal: number;
  dogExpenseCount: number;
  fuelFillCount: number;
  openAlerts: number;
  recentAlerts: { id: number; alertType: string; severity: string; title: string; detail: string; acknowledged: boolean }[];
  recentExpenses: { id: number; category: string; description: string; amount: number; paymentSource: string; expenseDate: string; vendorName?: string; receiptUrl?: string }[];
  recentFuel: { id: number; vehicleLabel: string; odometerKm: number; litres: number; amount: number; filledAt: string }[];
};

type DogExpense = { id: number; residentId: number; label: string; amount: number; expenseDate: string };
type SlaRow = { userId: number; fullName: string; workingDays: number; presentDays: number; paidLeaveDays: number; unpaidLeaveDays: number; slaPercent: number };
type Activity = { at: string; kind: string; title: string; detail: string };
type Resident = { id: number; name: string };
type Staff = { id: number; fullName: string };

const OPEX_CATS = ["FUEL", "VET_FEE", "MAINTENANCE", "MEDICATION", "FOOD", "TRANSPORT", "OTHER"];
const SOURCES = ["NGO_CASH", "WORKER_PAID", "DONOR_FUND"];

export default function OpsPage() {
  const qc = useQueryClient();
  const snap = useQuery({ queryKey: ["ops-snapshot"], queryFn: () => api<Snapshot>("/api/ops/snapshot") });
  const dogExp = useQuery({ queryKey: ["dog-expenses"], queryFn: () => api<DogExpense[]>("/api/ops/dog-expenses") });
  const sla = useQuery({ queryKey: ["ops-sla"], queryFn: () => api<SlaRow[]>("/api/ops/sla") });
  const activity = useQuery({ queryKey: ["ops-activity"], queryFn: () => api<Activity[]>("/api/ops/activity") });
  const residents = useQuery({ queryKey: ["residents"], queryFn: () => api<Resident[]>("/api/residents") });
  const staff = useQuery({ queryKey: ["users"], queryFn: () => api<Staff[]>("/api/users") });

  const [opex, setOpex] = useState({
    category: "MAINTENANCE",
    description: "",
    amount: "",
    paymentSource: "NGO_CASH",
    vendorName: "",
    receiptUrl: "",
    paidByUserId: "",
    residentId: "",
  });
  const [fuel, setFuel] = useState({
    vehicleLabel: "",
    odometerKm: "",
    litres: "",
    amount: "",
    paymentSource: "NGO_CASH",
    paidByUserId: "",
    receiptUrl: "",
  });
  const [dog, setDog] = useState({ residentId: "", label: "", amount: "" });
  const [slaQ, setSlaQ] = useState("");
  const slaRows = useMemo(() => {
    const needle = slaQ.trim().toLowerCase();
    return (sla.data ?? []).filter((s) => !needle || s.fullName.toLowerCase().includes(needle));
  }, [sla.data, slaQ]);

  const saveOpex = useMutation({
    mutationFn: () =>
      api("/api/ops/expenses", {
        method: "POST",
        body: JSON.stringify({
          ...opex,
          amount: Number(opex.amount),
          paidByUserId: opex.paidByUserId ? Number(opex.paidByUserId) : null,
          residentId: opex.residentId ? Number(opex.residentId) : null,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["ops-snapshot"] });
      qc.invalidateQueries({ queryKey: ["dog-expenses"] });
      qc.invalidateQueries({ queryKey: ["ops-activity"] });
    },
  });
  const saveFuel = useMutation({
    mutationFn: () =>
      api("/api/ops/fuel", {
        method: "POST",
        body: JSON.stringify({
          ...fuel,
          odometerKm: Number(fuel.odometerKm),
          litres: Number(fuel.litres),
          amount: Number(fuel.amount),
          paidByUserId: fuel.paidByUserId ? Number(fuel.paidByUserId) : null,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["ops-snapshot"] });
      qc.invalidateQueries({ queryKey: ["ops-activity"] });
    },
  });
  const saveDog = useMutation({
    mutationFn: () =>
      api("/api/ops/dog-expenses", {
        method: "POST",
        body: JSON.stringify({ residentId: Number(dog.residentId), label: dog.label, amount: Number(dog.amount) }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["dog-expenses"] });
      qc.invalidateQueries({ queryKey: ["ops-snapshot"] });
    },
  });
  const ack = useMutation({
    mutationFn: (id: number) => api(`/api/ops/alerts/${id}/ack`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ops-snapshot"] }),
  });

  if (snap.isLoading) return <p>Loading ops…</p>;
  if (snap.error) return <p className="text-clay">{(snap.error as Error).message}</p>;
  const d = snap.data!;

  function onOpex(e: FormEvent) {
    e.preventDefault();
    saveOpex.mutate();
  }
  function onFuel(e: FormEvent) {
    e.preventDefault();
    saveFuel.mutate();
  }
  function onDog(e: FormEvent) {
    e.preventDefault();
    saveDog.mutate();
  }

  return (
    <div className="desk">
      <PageHeader title="Ops & alerts" description="OpEx ledger, per-dog costs, fuel mileage, SLA, and theft / anomaly alerts. Search dogs and staff instead of scrolling a long list." />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <p className="text-xs uppercase text-ink/50">OpEx total</p>
          <p className="text-2xl font-bold">{inr(d.opexTotal)}</p>
          <p className="text-xs text-ink/50">{d.opexCount} entries</p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-ink/50">Per-dog costs</p>
          <p className="text-2xl font-bold">{inr(d.dogExpenseTotal)}</p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-ink/50">Fuel fills</p>
          <p className="text-2xl font-bold">{d.fuelFillCount}</p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-ink/50">Open alerts</p>
          <p className="text-2xl font-bold text-clay">{d.openAlerts}</p>
        </Card>
      </div>

      <div className="grid gap-3 lg:grid-cols-2">
        <Fold title="Record OpEx" defaultOpen>
          <form className="space-y-2" onSubmit={onOpex}>
            <Select value={opex.category} onChange={(e) => setOpex({ ...opex, category: e.target.value })}>
              {OPEX_CATS.map((c) => (
                <option key={c} value={c}>
                  {pretty(c)}
                </option>
              ))}
            </Select>
            <Textarea rows={2} placeholder="What was this for" value={opex.description} onChange={(e) => setOpex({ ...opex, description: e.target.value })} required />
            <Input type="number" step="0.01" placeholder="Amount ₹" value={opex.amount} onChange={(e) => setOpex({ ...opex, amount: e.target.value })} required />
            <Select value={opex.paymentSource} onChange={(e) => setOpex({ ...opex, paymentSource: e.target.value })}>
              {SOURCES.map((s) => (
                <option key={s} value={s}>
                  {s === "WORKER_PAID" ? "Worker pocket (reimburse on payroll)" : pretty(s)}
                </option>
              ))}
            </Select>
            {opex.paymentSource === "WORKER_PAID" && (
              <Combobox
                value={opex.paidByUserId}
                onChange={(paidByUserId) => setOpex({ ...opex, paidByUserId })}
                options={toStaffOptions(staff.data)}
                placeholder="Who paid from pocket"
                searchPlaceholder="Staff name…"
              />
            )}
            <Input placeholder="Vendor" value={opex.vendorName} onChange={(e) => setOpex({ ...opex, vendorName: e.target.value })} />
            <Input placeholder="Receipt URL" value={opex.receiptUrl} onChange={(e) => setOpex({ ...opex, receiptUrl: e.target.value })} />
            <Combobox
              value={opex.residentId}
              onChange={(residentId) => setOpex({ ...opex, residentId })}
              options={toResidentOptions(residents.data)}
              placeholder="Link to a dog (optional)"
              searchPlaceholder="Dog name or tag…"
            />
            {saveOpex.error && <p className="text-sm text-clay">{(saveOpex.error as Error).message}</p>}
            <Button className="w-full">Save expense</Button>
          </form>
        </Fold>
        <Fold title="Fuel / odometer" defaultOpen>
          <form className="space-y-2" onSubmit={onFuel}>
            <Input placeholder="Vehicle" value={fuel.vehicleLabel} onChange={(e) => setFuel({ ...fuel, vehicleLabel: e.target.value })} required />
            <Input type="number" step="0.1" placeholder="Odometer km" value={fuel.odometerKm} onChange={(e) => setFuel({ ...fuel, odometerKm: e.target.value })} required />
            <Input type="number" step="0.01" placeholder="Litres" value={fuel.litres} onChange={(e) => setFuel({ ...fuel, litres: e.target.value })} required />
            <Input type="number" step="0.01" placeholder="Amount ₹" value={fuel.amount} onChange={(e) => setFuel({ ...fuel, amount: e.target.value })} required />
            <Select value={fuel.paymentSource} onChange={(e) => setFuel({ ...fuel, paymentSource: e.target.value })}>
              {SOURCES.map((s) => (
                <option key={s} value={s}>
                  {pretty(s)}
                </option>
              ))}
            </Select>
            <Input placeholder="Receipt URL" value={fuel.receiptUrl} onChange={(e) => setFuel({ ...fuel, receiptUrl: e.target.value })} />
            {saveFuel.error && <p className="text-sm text-clay">{(saveFuel.error as Error).message}</p>}
            <Button className="w-full">Log fill</Button>
          </form>
        </Fold>
      </div>

      <Fold title="Per-dog expense">
        <form className="mb-3 grid gap-2 sm:grid-cols-3" onSubmit={onDog}>
          <Combobox
            value={dog.residentId}
            onChange={(residentId) => setDog({ ...dog, residentId })}
            options={toResidentOptions(residents.data)}
            placeholder="Search dog"
            searchPlaceholder="Name or tag…"
          />
          <Input placeholder="Label" value={dog.label} onChange={(e) => setDog({ ...dog, label: e.target.value })} required />
          <Input type="number" step="0.01" placeholder="₹" value={dog.amount} onChange={(e) => setDog({ ...dog, amount: e.target.value })} required />
          <Button className="sm:col-span-3">Link cost</Button>
        </form>
        {saveDog.error && <p className="text-sm text-clay">{(saveDog.error as Error).message}</p>}
        <ul className="space-y-2 text-sm">
          {dogExp.data?.map((x) => (
            <li key={x.id} className="flex justify-between rounded-xl bg-sand px-3 py-2">
              <span>
                Dog #{x.residentId} · {x.label}
                <span className="block text-xs text-ink/60">{x.expenseDate}</span>
              </span>
              <b>{inr(x.amount)}</b>
            </li>
          ))}
        </ul>
      </Fold>

      <Fold title="Attendance SLA">
        <p className="mb-2 text-xs text-ink/50">Present = working days − paid leave − LOP. Daily attendance is not marked.</p>
        <SearchField value={slaQ} onChange={setSlaQ} placeholder="Find a worker…" count={slaRows.length} className="mb-3" />
        <ul className="space-y-2 text-sm">
          {slaRows.map((s) => (
            <li key={s.userId} className="flex justify-between rounded-xl bg-sand px-3 py-2">
              <span>
                <b>{s.fullName}</b>
                <span className="block text-xs text-ink/60">
                  present {s.presentDays} / {s.workingDays} · paid leave {s.paidLeaveDays} · LOP {s.unpaidLeaveDays}
                </span>
              </span>
              <b>{s.slaPercent}%</b>
            </li>
          ))}
        </ul>
      </Fold>
      <CollapsibleCard title="Analytics & theft alerts" count={d.recentAlerts.length}>
        <ul className="mt-1 space-y-2 text-sm">
          {d.recentAlerts.length === 0 && <li className="text-ink/50">No alerts for this centre.</li>}
          {d.recentAlerts.map((a) => (
            <li key={a.id} className={`rounded-xl px-3 py-2 ${a.acknowledged ? "bg-sand" : "bg-orange-50"}`}>
              <p className="text-xs uppercase text-moss">
                {pretty(a.alertType)} · {a.severity}
                {a.acknowledged ? " · closed" : ""}
              </p>
              <b>{a.title}</b>
              <p className="text-ink/60">{a.detail}</p>
              {!a.acknowledged && (
                <Button variant="ghost" className="mt-1" onClick={() => ack.mutate(a.id)}>
                  Acknowledge
                </Button>
              )}
            </li>
          ))}
        </ul>
      </CollapsibleCard>
      <CollapsibleCard title="Recent OpEx" count={d.recentExpenses.length}>
        <ul className="mt-1 space-y-2 text-sm">
          {d.recentExpenses.length === 0 && <li className="text-ink/50">No expenses logged yet.</li>}
          {d.recentExpenses.map((e) => (
            <li key={e.id} className="flex justify-between gap-2 rounded-xl bg-sand px-3 py-2">
              <span>
                <b>{pretty(e.category)}</b>
                <span className="block text-xs text-ink/60">
                  {e.description} · {pretty(e.paymentSource)} · {e.expenseDate}
                  {e.receiptUrl ? " · receipt" : ""}
                </span>
              </span>
              <b>{inr(e.amount)}</b>
            </li>
          ))}
        </ul>
      </CollapsibleCard>
      <CollapsibleCard title="Fuel / odometer log" count={d.recentFuel.length}>
        <ul className="mt-1 space-y-2 text-sm">
          {d.recentFuel.length === 0 && <li className="text-ink/50">No fills logged yet.</li>}
          {d.recentFuel.map((f) => (
            <li key={f.id} className="rounded-xl bg-sand px-3 py-2">
              <b>{f.vehicleLabel}</b>
              <p className="text-ink/60">
                {f.filledAt}: {f.odometerKm} km · {f.litres} L · {inr(f.amount)}
              </p>
            </li>
          ))}
        </ul>
      </CollapsibleCard>
      <CollapsibleCard title="System activity log" count={activity.data?.length ?? 0}>
        <ul className="mt-1 space-y-2 text-sm">
          {activity.data?.map((a, i) => (
            <li key={i} className="rounded-xl bg-sand px-3 py-2">
              <p className="text-xs uppercase text-moss">
                {a.at} · {pretty(a.kind)}
              </p>
              <b>{a.title}</b>
              <p className="text-ink/60">{a.detail}</p>
            </li>
          ))}
        </ul>
      </CollapsibleCard>
    </div>
  );
}

function CollapsibleCard({ title, count, children }: { title: string; count: number; children: ReactNode }) {
  return (
    <Card>
      <details className="group">
        <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-3 font-semibold marker:content-none [&::-webkit-details-marker]:hidden">
          {title}
          <span className="flex items-center gap-2 text-sm font-normal text-ink/50">
            {count}
            <ChevronDown className="size-4 transition-transform duration-200 ease-out group-open:rotate-180" />
          </span>
        </summary>
        {children}
      </details>
    </Card>
  );
}
