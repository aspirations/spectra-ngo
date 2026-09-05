"use client";

import { api, can, getUser } from "@/lib/api";
import { Card } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
import { inr, pretty } from "@/lib/utils";
import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import {
  AlertTriangle,
  Bone,
  ClipboardList,
  Dog,
  FileText,
  Fuel,
  ShieldAlert,
  Users,
  Wallet,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

type NamedValue = { name: string; value: number };
type FeedPoint = { date: string; actual: number; theoretical: number };
type SpendPoint = { date: string; total: number };
type FuelPoint = { date: string; kmPerLitre: number; vehicle: string };

type Dash = {
  census: {
    totalActive: number;
    byCategory: Record<string, number>;
    theoreticalFeedKg: number;
  };
  treatmentsDue: { residentName: string; nextDueDate: string; productName: string }[];
  consumptionAlerts: { id: number; consumptionDate: string; variancePct: number; actualQty: number; theoreticalQty: number }[];
  myCredit: { creditLimit: number; outstanding: number; available: number };
  kpis: {
    openAudits: number;
    openAlerts: number;
    opexTotal: number;
    treatmentsDue: number;
    expiringBatches: number;
    onLeaveToday?: number;
    pendingIssues?: number;
    pendingPoApprovals?: number;
    pendingPoWaiting?: number;
  };
  charts: {
    censusByCategory: NamedValue[];
    feedTrend: FeedPoint[];
    opexByCategory: NamedValue[];
    leaveMix: NamedValue[];
    staffSpendTrend: SpendPoint[];
    alertSeverity: NamedValue[];
    fuelEfficiency: FuelPoint[];
  };
  expiringBatches: { batchNumber: string; qtyOnHand: number; expiryDate: string; shelterProductId: number }[];
  opsAlerts: { id: number; title: string; severity: string; detail: string }[];
};

const MOSS = "#1f6b4a";
const LEAF = "#2f9e6b";
const CLAY = "#c45c26";
const INK = "#0f1c17";
const SAND_LINE = "#d9d0c1";
const PIE = [MOSS, LEAF, CLAY, "#5b7c6a", "#8a6a4a", "#3d5a80"];

function shortDay(iso: string) {
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function ChartShell({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <Card className="overflow-hidden">
      <div className="mb-3">
        <h2 className="text-base font-semibold tracking-tight">{title}</h2>
        {subtitle && <p className="text-xs text-ink/50">{subtitle}</p>}
      </div>
      <div className="h-40 w-full min-w-0 sm:h-52">{children}</div>
    </Card>
  );
}

function Kpi({
  label,
  value,
  hint,
  icon: Icon,
  tone = "default",
}: {
  label: string;
  value: string | number;
  hint?: string;
  icon: typeof Dog;
  tone?: "default" | "warn" | "alert";
}) {
  const ring =
    tone === "alert" ? "ring-clay/30 bg-clay/[0.06]" : tone === "warn" ? "ring-amber-500/20 bg-amber-50/80" : "ring-moss/15 bg-white";
  return (
    <div className={`rounded-2xl p-3 ring-1 sm:p-4 ${ring}`}>
      <div className="mb-2 flex items-center justify-between gap-2">
        <p className="text-[10px] font-medium uppercase tracking-wide text-ink/45 sm:text-xs">{label}</p>
        <Icon className={`h-4 w-4 ${tone === "alert" ? "text-clay" : tone === "warn" ? "text-amber-700" : "text-moss"}`} />
      </div>
      <p className="text-xl font-bold tabular-nums tracking-tight sm:text-2xl">{value}</p>
      {hint && <p className="mt-1 text-xs text-ink/45">{hint}</p>}
    </div>
  );
}

export default function DashboardPage() {
  const q = useQuery({ queryKey: ["dashboard"], queryFn: () => api<Dash>("/api/dashboard") });
  if (q.isLoading) return <p className="text-sm text-ink/60">Loading operations dashboard…</p>;
  if (q.error) return <p className="text-clay">{(q.error as Error).message}. Pick a working centre first.</p>;
  const d = q.data!;
  const charts = d.charts ?? {
    censusByCategory: [],
    feedTrend: [],
    opexByCategory: [],
    staffSpendTrend: [],
    alertSeverity: [],
    fuelEfficiency: [],
    leaveMix: [],
  };
  const kpis = d.kpis ?? {
    openAudits: 0,
    openAlerts: 0,
    opexTotal: 0,
    treatmentsDue: d.treatmentsDue?.length ?? 0,
    expiringBatches: 0,
    onLeaveToday: 0,
    pendingIssues: 0,
    pendingPoApprovals: 0,
    pendingPoWaiting: 0,
  };
  const manager = can(getUser()?.role, ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"]);
  const poApprover = can(getUser()?.role, ["NGO_ADMIN", "BRANCH_ADMIN"]);
  const poDrafter = can(getUser()?.role, ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER", "EMPLOYEE"]);

  const feedData = charts.feedTrend.map((p) => ({
    ...p,
    label: shortDay(p.date),
    actual: Number(p.actual),
    theoretical: Number(p.theoretical),
  }));
  const spendData = charts.staffSpendTrend.map((p) => ({
    ...p,
    label: shortDay(p.date),
    total: Number(p.total),
  }));
  const censusPie = charts.censusByCategory.map((c) => ({
    name: pretty(c.name),
    value: Number(c.value),
  }));
  const opexBars = charts.opexByCategory.map((c) => ({
    name: pretty(c.name),
    value: Number(c.value),
  }));
  const leaveBars = (charts.leaveMix ?? []).map((c) => ({
    name: pretty(c.name),
    value: Number(c.value),
  }));
  const alertPie = charts.alertSeverity.map((c) => ({
    name: pretty(c.name),
    value: Number(c.value),
  }));
  const fuelData = charts.fuelEfficiency.map((p) => ({
    ...p,
    label: shortDay(p.date),
    kmPerLitre: Number(p.kmPerLitre),
  }));

  const creditPct =
    d.myCredit.creditLimit > 0
      ? Math.min(100, Math.round((Number(d.myCredit.outstanding) / Number(d.myCredit.creditLimit)) * 100))
      : 0;

  return (
    <div className="desk">
      <PageHeader
        title="Today at the kennel"
        description="Census, feed burn, OpEx, leave, and risk signals for this centre — glance, then tap through."
      />

      <div className="grid grid-cols-2 gap-2 sm:gap-3 lg:grid-cols-3 xl:grid-cols-6">
        <Kpi label="Active dogs" value={d.census.totalActive} hint={`${d.census.theoreticalFeedKg} kg feed/day`} icon={Dog} />
        <Kpi label="Treatments due" value={kpis.treatmentsDue} icon={AlertTriangle} tone={kpis.treatmentsDue > 0 ? "warn" : "default"} />
        {manager && (
          <Kpi
            label="Pending issues"
            value={kpis.pendingIssues ?? 0}
            icon={Bone}
            tone={(kpis.pendingIssues ?? 0) > 0 ? "warn" : "default"}
          />
        )}
        {poApprover && (
          <Kpi
            label="POs to approve"
            value={kpis.pendingPoApprovals ?? 0}
            icon={FileText}
            tone={(kpis.pendingPoApprovals ?? 0) > 0 ? "warn" : "default"}
          />
        )}
        {poDrafter && !poApprover && (
          <Kpi
            label="POs waiting"
            value={kpis.pendingPoWaiting ?? 0}
            icon={FileText}
            tone={(kpis.pendingPoWaiting ?? 0) > 0 ? "warn" : "default"}
          />
        )}
        <Kpi label="Open audits" value={kpis.openAudits} icon={ClipboardList} tone={kpis.openAudits > 0 ? "alert" : "default"} />
        <Kpi label="Ops alerts" value={kpis.openAlerts} icon={ShieldAlert} tone={kpis.openAlerts > 0 ? "alert" : "default"} />
        <Kpi label="On leave today" value={kpis.onLeaveToday ?? 0} icon={Users} />
        <Kpi label="OpEx total" value={inr(kpis.opexTotal)} icon={Wallet} />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <ChartShell title="Feed actual vs theoretical" subtitle="Last 14 days · kg issued">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={feedData} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
              <defs>
                <linearGradient id="feedActual" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={CLAY} stopOpacity={0.35} />
                  <stop offset="100%" stopColor={CLAY} stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke={SAND_LINE} strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11, fill: INK }} tickLine={false} axisLine={false} />
              <YAxis tick={{ fontSize: 11, fill: INK }} tickLine={false} axisLine={false} width={36} />
              <Tooltip contentStyle={{ borderRadius: 12, borderColor: SAND_LINE, fontSize: 12 }} />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <Area type="monotone" dataKey="actual" name="Actual" stroke={CLAY} fill="url(#feedActual)" strokeWidth={2} />
              <Line type="monotone" dataKey="theoretical" name="Theoretical" stroke={MOSS} strokeWidth={2} dot={false} strokeDasharray="4 4" />
            </AreaChart>
          </ResponsiveContainer>
        </ChartShell>

        <ChartShell title="Census by ward" subtitle="Active dogs">
          {censusPie.length === 0 ? (
            <p className="flex h-full items-center justify-center text-sm text-ink/45">No active dogs</p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={censusPie} dataKey="value" nameKey="name" innerRadius="48%" outerRadius="78%" paddingAngle={2}>
                  {censusPie.map((_, i) => (
                    <Cell key={i} fill={PIE[i % PIE.length]} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ borderRadius: 12, borderColor: SAND_LINE, fontSize: 12 }} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </ChartShell>

        <ChartShell title="Operating expenses by category" subtitle="Branch OpEx mix">
          {opexBars.length === 0 ? (
            <p className="flex h-full items-center justify-center text-sm text-ink/45">No expenses yet</p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={opexBars} layout="vertical" margin={{ top: 4, right: 12, left: 8, bottom: 0 }}>
                <CartesianGrid stroke={SAND_LINE} strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" tick={{ fontSize: 11 }} tickLine={false} axisLine={false} />
                <YAxis type="category" dataKey="name" width={88} tick={{ fontSize: 10 }} tickLine={false} axisLine={false} />
                <Tooltip formatter={(v: number) => inr(v)} contentStyle={{ borderRadius: 12, borderColor: SAND_LINE, fontSize: 12 }} />
                <Bar dataKey="value" name="Amount" fill={MOSS} radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartShell>

        <ChartShell title="Staff store spend" subtitle="Last 14 days · credit + cash">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={spendData} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
              <CartesianGrid stroke={SAND_LINE} strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} tickLine={false} axisLine={false} />
              <YAxis tick={{ fontSize: 11 }} tickLine={false} axisLine={false} width={40} />
              <Tooltip formatter={(v: number) => inr(v)} contentStyle={{ borderRadius: 12, borderColor: SAND_LINE, fontSize: 12 }} />
              <Bar dataKey="total" name="Spend" fill={LEAF} radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartShell>

        <ChartShell title="Leave this month" subtitle="Recorded absences only — unmarked days are present">
          {leaveBars.length === 0 ? (
            <p className="flex h-full items-center justify-center text-sm text-ink/45">No leave recorded</p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={leaveBars} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                <CartesianGrid stroke={SAND_LINE} strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 10 }} tickLine={false} axisLine={false} interval={0} angle={-18} textAnchor="end" height={48} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} tickLine={false} axisLine={false} width={28} />
                <Tooltip contentStyle={{ borderRadius: 12, borderColor: SAND_LINE, fontSize: 12 }} />
                <Bar dataKey="value" name="Days" fill={MOSS} radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartShell>

        <ChartShell title="Ambulance fuel efficiency" subtitle="km per litre between fills">
          {fuelData.length === 0 ? (
            <p className="flex h-full items-center justify-center text-sm text-ink/45">Need 2+ fuel fills</p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={fuelData} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                <CartesianGrid stroke={SAND_LINE} strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} tickLine={false} axisLine={false} />
                <YAxis tick={{ fontSize: 11 }} tickLine={false} axisLine={false} width={36} />
                <Tooltip contentStyle={{ borderRadius: 12, borderColor: SAND_LINE, fontSize: 12 }} />
                <Line type="monotone" dataKey="kmPerLitre" name="km/L" stroke={CLAY} strokeWidth={2.5} dot={{ r: 4, fill: CLAY }} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </ChartShell>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card>
          <div className="mb-3 flex items-center gap-2">
            <Fuel className="h-4 w-4 text-moss" />
            <h2 className="font-semibold">Store credit</h2>
          </div>
          <p className="text-2xl font-bold tabular-nums">{inr(d.myCredit.available)}</p>
          <p className="text-xs text-ink/45">available of {inr(d.myCredit.creditLimit)}</p>
          <div className="mt-3 h-2 overflow-hidden rounded-full bg-sand">
            <div className="h-full rounded-full bg-moss transition-all" style={{ width: `${creditPct}%` }} />
          </div>
          <p className="mt-1 text-xs text-ink/50">{creditPct}% of limit used</p>
        </Card>

        <Card>
          <h2 className="mb-2 font-semibold">Due for treatment today</h2>
          {d.treatmentsDue.length === 0 && <p className="text-sm text-ink/50">None due.</p>}
          <ul className="max-h-48 space-y-2 overflow-y-auto text-sm">
            {d.treatmentsDue.map((v, i) => (
              <li key={i} className="rounded-xl bg-sand/80 px-3 py-2">
                <b>{v.residentName}</b> · {v.productName}
                <div className="text-xs text-ink/45">{v.nextDueDate}</div>
              </li>
            ))}
          </ul>
        </Card>

        <Card>
          <h2 className="mb-2 font-semibold">Open ops alerts</h2>
          {(d.opsAlerts?.length ?? 0) === 0 && <p className="text-sm text-ink/50">All clear.</p>}
          <ul className="max-h-48 space-y-2 overflow-y-auto text-sm">
            {d.opsAlerts?.map((a) => (
              <li key={a.id} className="rounded-xl bg-orange-50 px-3 py-2">
                <div className="flex items-center justify-between gap-2">
                  <b>{a.title}</b>
                  <span className="text-[10px] uppercase text-clay">{a.severity}</span>
                </div>
                <p className="mt-0.5 text-xs text-ink/55 line-clamp-2">{a.detail}</p>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <h2 className="mb-2 font-semibold">Feed variance alerts</h2>
          {d.consumptionAlerts.length === 0 && <p className="text-sm text-ink/50">No &gt;15% deviations.</p>}
          <ul className="space-y-2 text-sm">
            {d.consumptionAlerts.map((a) => (
              <li key={a.id} className="rounded-xl bg-orange-50 px-3 py-2">
                {a.consumptionDate}: actual {a.actualQty} kg vs theoretical {a.theoreticalQty} kg (
                {(Number(a.variancePct) * 100).toFixed(1)}%)
              </li>
            ))}
          </ul>
        </Card>
        <Card>
          <h2 className="mb-2 font-semibold">Batches expiring ≤45 days</h2>
          {(d.expiringBatches?.length ?? 0) === 0 && <p className="text-sm text-ink/50">None in window.</p>}
          <div className="overflow-x-auto">
            <table className="w-full min-w-[280px] text-left text-sm">
              <thead>
                <tr className="text-xs uppercase text-ink/45">
                  <th className="py-1">Batch</th>
                  <th>Qty</th>
                  <th>Expiry</th>
                </tr>
              </thead>
              <tbody>
                {d.expiringBatches?.map((b, i) => (
                  <tr key={i} className="border-t border-sand">
                    <td className="py-2 font-medium">{b.batchNumber}</td>
                    <td>{b.qtyOnHand}</td>
                    <td>{b.expiryDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>

      {alertPie.length > 0 && (
        <ChartShell title="Open alert severity" subtitle="Unacknowledged analytics alerts">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={alertPie} dataKey="value" nameKey="name" innerRadius="45%" outerRadius="75%">
                {alertPie.map((_, i) => (
                  <Cell key={i} fill={[CLAY, "#d97706", MOSS, LEAF][i % 4]} />
                ))}
              </Pie>
              <Tooltip contentStyle={{ borderRadius: 12, borderColor: SAND_LINE, fontSize: 12 }} />
              <Legend wrapperStyle={{ fontSize: 12 }} />
            </PieChart>
          </ResponsiveContainer>
        </ChartShell>
      )}
    </div>
  );
}
