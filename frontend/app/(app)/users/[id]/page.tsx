"use client";

import { api, Branch, fetchPdfObjectUrl, getUser, Role, ROLE_MENUS, User } from "@/lib/api";
import { LocalThumb, PhotoPicker } from "@/components/photo-picker";
import { StaffPassbook } from "@/components/staff-passbook";
import { PdfPreview } from "@/components/pdf-preview";
import { Button } from "@/components/ui/button";
import { Card, Input, Label, Select, Textarea } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
import { commitLocalPhoto, emptyPhotoDraft, useLocalPhoto } from "@/lib/local-photos";
import { inr, pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams } from "next/navigation";
import { FormEvent, useEffect, useMemo, useState } from "react";

const ALL_ROLES: Role[] = ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER", "VET_TECH_EMPLOYEE", "EMPLOYEE"];
type Tab = "login" | "pay" | "passbook";

function toggleId(ids: number[], id: number) {
  return ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];
}

type MonthPay = {
  userId: number;
  fullName: string;
  periodLabel: string;
  locked: boolean;
  runStatus?: string;
  baseSalary: number;
  workingDays: number;
  unpaidLeaveDays: number;
  lopDeduction: number;
  storeDues: number;
  otherDeductions: number;
  advanceDeduction: number;
  reimbursementCredit: number;
  netPayout: number;
  rolledOverStoreDebt: number;
  periodYear: number;
  periodMonth: number;
};

export default function PersonFilePage() {
  const { id } = useParams<{ id: string }>();
  const personId = Number(id);
  const me = getUser();
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>("login");
  const [photo, setPhoto] = useState(emptyPhotoDraft);
  const localPhoto = useLocalPhoto("person", Number.isFinite(personId) ? personId : undefined);
  const person = useQuery({ queryKey: ["user", id], queryFn: () => api<User>(`/api/users/${id}`) });
  const monthPay = useQuery({
    queryKey: ["staff-pay", id],
    queryFn: () => api<MonthPay>(`/api/payroll/staff/${id}/current`),
  });
  const [slipBusy, setSlipBusy] = useState(false);
  const [slipError, setSlipError] = useState("");
  const [slipPdf, setSlipPdf] = useState<string | null>(null);
  const branches = useQuery({ queryKey: ["branches"], queryFn: () => api<Branch[]>("/api/branches") });
  const roles = useMemo(
    () => (me?.role === "NGO_ADMIN" ? ALL_ROLES : ALL_ROLES.filter((r) => r !== "NGO_ADMIN")),
    [me?.role],
  );
  const [login, setLogin] = useState({
    fullName: "",
    email: "",
    role: "EMPLOYEE" as Role,
    password: "",
    branchIds: [] as number[],
    aadhaarNo: "",
    homeAddress: "",
    familyDetails: "",
  });
  const [pay, setPay] = useState({ baseMonthlySalary: "", creditLimit: "", otherFixedDeductions: "" });

  useEffect(() => {
    setPhoto(emptyPhotoDraft());
  }, [id]);

  useEffect(() => {
    if (!person.data) return;
    setLogin({
      fullName: person.data.fullName,
      email: person.data.email,
      role: person.data.role,
      password: "",
      branchIds: person.data.branchIds ?? (person.data.branchId ? [person.data.branchId] : []),
      aadhaarNo: person.data.aadhaarNo ?? "",
      homeAddress: person.data.homeAddress ?? "",
      familyDetails: person.data.familyDetails ?? "",
    });
    setPay({
      baseMonthlySalary: String(person.data.baseMonthlySalary ?? 0),
      creditLimit: String(person.data.creditLimit ?? 0),
      otherFixedDeductions: String(person.data.otherFixedDeductions ?? 0),
    });
  }, [person.data]);

  const saveLogin = useMutation({
    mutationFn: async () => {
      await api(`/api/users/${id}`, {
        method: "PUT",
        body: JSON.stringify({
          fullName: login.fullName,
          email: login.email,
          role: login.role,
          password: login.password || undefined,
          branchIds: login.role === "NGO_ADMIN" ? undefined : login.branchIds,
          aadhaarNo: login.aadhaarNo,
          homeAddress: login.homeAddress,
          familyDetails: login.familyDetails,
        }),
      });
      await commitLocalPhoto("person", personId, photo);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["user", id] });
      qc.invalidateQueries({ queryKey: ["users"] });
      setLogin((prev) => ({ ...prev, password: "" }));
      setPhoto(emptyPhotoDraft());
    },
  });
  const savePay = useMutation({
    mutationFn: () =>
      api(`/api/users/${id}`, {
        method: "PUT",
        body: JSON.stringify({
          baseMonthlySalary: Number(pay.baseMonthlySalary),
          creditLimit: Number(pay.creditLimit),
          otherFixedDeductions: Number(pay.otherFixedDeductions),
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["user", id] });
      qc.invalidateQueries({ queryKey: ["users"] });
    },
  });
  const setStatus = useMutation({
    mutationFn: (status: string) => api(`/api/users/${id}`, { method: "PUT", body: JSON.stringify({ status }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["user", id] });
      qc.invalidateQueries({ queryKey: ["users"] });
    },
  });

  async function printPayslip() {
    setSlipError("");
    setSlipBusy(true);
    try {
      const url = await fetchPdfObjectUrl(`/api/payroll/staff/${id}/payslip-pdf`);
      if (slipPdf) URL.revokeObjectURL(slipPdf);
      setSlipPdf(url);
    } catch (err) {
      setSlipError((err as Error).message);
    } finally {
      setSlipBusy(false);
    }
  }

  function closePayslip() {
    if (slipPdf) URL.revokeObjectURL(slipPdf);
    setSlipPdf(null);
  }

  if (person.error) return <p className="text-clay">{(person.error as Error).message}</p>;
  if (!person.data) return <p>Loading person…</p>;
  const u = person.data;
  const self = me?.id === u.id;
  const slip = monthPay.data;
  const tabs: { id: Tab; label: string }[] = [
    { id: "login", label: "Login" },
    { id: "pay", label: "Salary" },
    { id: "passbook", label: "Passbook" },
  ];

  return (
    <div className="desk">
      <Link href="/users" className="text-sm font-semibold text-moss">
        ← People
      </Link>
      <div className="flex items-start gap-3">
        <LocalThumb
          kind="person"
          id={u.id}
          fallback="👤"
          className="h-20 w-20 shrink-0 rounded-full text-3xl"
        />
        <div className="min-w-0 flex-1">
          <PageHeader
            title={u.fullName}
            description={`${pretty(u.role)} · ${u.email} · ${inr(u.baseMonthlySalary)} / month`}
          />
        </div>
      </div>
      <p className={`text-xs font-semibold uppercase ${u.status === "ACTIVE" ? "text-moss" : "text-clay"}`}>
        {pretty(u.status)}
      </p>
      <Card>
        <h2 className="mb-3 font-semibold">Personal details</h2>
        <dl className="grid gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-xs uppercase text-ink/45">Aadhaar</dt>
            <dd className="mt-0.5 whitespace-pre-wrap">{u.aadhaarNo || "—"}</dd>
          </div>
          <div className="sm:col-span-2">
            <dt className="text-xs uppercase text-ink/45">Home address</dt>
            <dd className="mt-0.5 whitespace-pre-wrap">{u.homeAddress || "—"}</dd>
          </div>
          <div className="sm:col-span-2">
            <dt className="text-xs uppercase text-ink/45">Family / emergency contact</dt>
            <dd className="mt-0.5 whitespace-pre-wrap">{u.familyDetails || "—"}</dd>
          </div>
        </dl>
      </Card>
      <div className="grid grid-cols-3 gap-1 rounded-2xl bg-sand p-1">
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => setTab(t.id)}
            className={`min-h-11 rounded-xl text-sm font-semibold ${tab === t.id ? "bg-moss text-white shadow-sm" : "text-ink/70"}`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "login" && (
        <Card>
          <form
            className="space-y-3"
            onSubmit={(e: FormEvent) => {
              e.preventDefault();
              saveLogin.mutate();
            }}
          >
            <div>
              <Label>Name</Label>
              <Input value={login.fullName} onChange={(e) => setLogin({ ...login, fullName: e.target.value })} required />
            </div>
            <PhotoPicker draft={photo} onChange={setPhoto} existingUrl={localPhoto} />
            <div>
              <Label>Username (login email)</Label>
              <Input
                type="email"
                value={login.email}
                onChange={(e) => setLogin({ ...login, email: e.target.value })}
                required
                autoComplete="off"
              />
            </div>
            <div>
              <Label>New password</Label>
              <Input
                type="password"
                value={login.password}
                onChange={(e) => setLogin({ ...login, password: e.target.value })}
                placeholder="Leave blank to keep current"
                autoComplete="new-password"
              />
            </div>
            <div>
              <Label>Role</Label>
              <Select value={login.role} onChange={(e) => setLogin({ ...login, role: e.target.value as Role })}>
                {roles.map((r) => (
                  <option key={r}>{r}</option>
                ))}
              </Select>
              <p className="mt-1 text-xs text-ink/50">{ROLE_MENUS[login.role].join(" · ")}</p>
            </div>
            {login.role !== "NGO_ADMIN" && (
              <div>
                <Label>Centres they can work in</Label>
                <ul className="space-y-1 text-sm">
                  {branches.data?.map((b) => (
                    <li key={b.id}>
                      <label className="flex min-h-11 items-center gap-2">
                        <input
                          type="checkbox"
                          checked={login.branchIds.includes(b.id)}
                          onChange={() => setLogin({ ...login, branchIds: toggleId(login.branchIds, b.id) })}
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
                value={login.aadhaarNo}
                onChange={(e) => setLogin({ ...login, aadhaarNo: e.target.value })}
                maxLength={16}
              />
            </div>
            <div>
              <Label>Home address</Label>
              <Textarea rows={3} value={login.homeAddress} onChange={(e) => setLogin({ ...login, homeAddress: e.target.value })} />
            </div>
            <div>
              <Label>Family / emergency contact</Label>
              <Textarea
                rows={3}
                value={login.familyDetails}
                onChange={(e) => setLogin({ ...login, familyDetails: e.target.value })}
                placeholder="Names, relation, phone…"
              />
            </div>
            {saveLogin.error && <p className="text-sm text-clay">{(saveLogin.error as Error).message}</p>}
            <Button className="w-full" disabled={saveLogin.isPending}>
              Save login
            </Button>
          </form>
          <div className="mt-4 border-t border-moss/10 pt-3">
            {self ? (
              <p className="text-sm text-ink/55">You cannot deactivate your own login.</p>
            ) : (
              <Button
                type="button"
                variant={u.status === "ACTIVE" ? "danger" : "primary"}
                className="w-full"
                disabled={setStatus.isPending}
                onClick={() => setStatus.mutate(u.status === "ACTIVE" ? "INACTIVE" : "ACTIVE")}
              >
                {u.status === "ACTIVE" ? "Deactivate login" : "Activate login"}
              </Button>
            )}
            {setStatus.error && <p className="mt-2 text-sm text-clay">{(setStatus.error as Error).message}</p>}
          </div>
        </Card>
      )}

      {tab === "pay" && (
        <>
      <Card>
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 className="font-semibold">This month</h2>
            <p className="text-sm text-ink/55">
              {monthPay.isPending
                ? "Loading…"
                : slip
                  ? `${slip.periodLabel} · ${slip.locked ? pretty(slip.runStatus ?? "LOCKED") : "live estimate"}`
                  : "Pay not available"}
            </p>
          </div>
          {slip && <p className="text-lg font-semibold tabular-nums">{inr(slip.netPayout)}</p>}
        </div>
        {monthPay.error && <p className="mt-2 text-sm text-clay">{(monthPay.error as Error).message}</p>}
        {slip && (
          <>
            <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
              <dt className="text-ink/55">Base</dt>
              <dd className="text-right tabular-nums">{inr(slip.baseSalary)}</dd>
              <dt className="text-ink/55">LOP</dt>
              <dd className="text-right tabular-nums">{inr(slip.lopDeduction)}</dd>
              <dt className="text-ink/55">Store dues</dt>
              <dd className="text-right tabular-nums">{inr(slip.storeDues)}</dd>
              <dt className="text-ink/55">Other</dt>
              <dd className="text-right tabular-nums">{inr(slip.otherDeductions)}</dd>
            </dl>
            <Button className="mt-3 w-full" type="button" variant="outline" disabled={slipBusy} onClick={() => void printPayslip()}>
              {slipBusy ? "Opening…" : "Print payslip"}
            </Button>
          </>
        )}
        {slipError && <p className="mt-2 text-sm text-clay">{slipError}</p>}
      </Card>
        <Card>
          <form
            className="space-y-3"
            onSubmit={(e: FormEvent) => {
              e.preventDefault();
              savePay.mutate();
            }}
          >
            <div>
              <Label>Monthly salary</Label>
              <Input
                type="number"
                step="0.01"
                value={pay.baseMonthlySalary}
                onChange={(e) => setPay({ ...pay, baseMonthlySalary: e.target.value })}
                required
              />
            </div>
            <div>
              <Label>Store credit limit</Label>
              <Input
                type="number"
                step="0.01"
                value={pay.creditLimit}
                onChange={(e) => setPay({ ...pay, creditLimit: e.target.value })}
                required
              />
            </div>
            <div>
              <Label>Other fixed deductions</Label>
              <Input
                type="number"
                step="0.01"
                value={pay.otherFixedDeductions}
                onChange={(e) => setPay({ ...pay, otherFixedDeductions: e.target.value })}
              />
            </div>
            {savePay.error && <p className="text-sm text-clay">{(savePay.error as Error).message}</p>}
            <Button className="w-full" disabled={savePay.isPending}>
              Save salary
            </Button>
          </form>
        </Card>
        </>
      )}

      {tab === "passbook" && <StaffPassbook userId={id} />}
      {slipPdf && <PdfPreview src={slipPdf} title="Payslip" onClose={closePayslip} />}
    </div>
  );
}
