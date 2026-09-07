"use client";

import { api, can, fetchPdfObjectUrl, getUser } from "@/lib/api";
import { ExportExcelButton } from "@/components/export-excel-button";
import { Button } from "@/components/ui/button";
import { Combobox, toProductOptions } from "@/components/ui/combobox";
import { Fold } from "@/components/ui/fold";
import { Card, Input, Label, Select, Textarea } from "@/components/ui/input";
import { PageControls, pageSlice } from "@/components/ui/pager";
import { PageHeader } from "@/components/ui/page-header";
import { PdfPreview } from "@/components/pdf-preview";
import { downloadSpreadsheet, spreadsheetFilename } from "@/lib/export-spreadsheet";
import { inr, pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useMemo, useState } from "react";

type Product = { id: number; name: string; sku: string; category: string; unitCost?: number };
type Supplier = { id: number; name: string; payableBalance: number };
type Approver = { id: number; fullName: string; role: string; email: string };
type PoLine = {
  id: number;
  shelterProductId: number;
  qtyOrdered: number;
  unitCost: number;
  qtyReceived: number;
  damagedQty: number;
  notes?: string;
  productName?: string;
  sku?: string;
};
type Po = {
  id: number;
  poNumber: string;
  status: string;
  supplierId: number;
  supplierName?: string;
  requestedByName?: string;
  approverName?: string;
  notes?: string;
  rejectReason?: string;
  expectedTotal: number;
  receivedTotal: number;
  approverUserId?: number;
  canApprove?: boolean;
  canEdit?: boolean;
  lines?: PoLine[];
};
type Note = { id: number; type: string; title: string; body: string; readAt?: string; createdDate?: string };
type Settings = {
  poApproverRole: string;
  poApproverUserId?: number | null;
  poQtyTolerancePct: number;
  poCostTolerancePct: number;
  poNotifyOnSubmit: boolean;
  poNotifyOnApprove: boolean;
  poNotifyOnVariance: boolean;
};

const emptyItem = () => ({ shelterProductId: "", qtyOrdered: "", unitCost: "", notes: "" });

export default function PurchaseOrderPage() {
  const me = getUser();
  const qc = useQueryClient();
  const ngo = me?.role === "NGO_ADMIN";
  const admin = can(me?.role, ["NGO_ADMIN", "BRANCH_ADMIN"]);
  const pos = useQuery({ queryKey: ["pos"], queryFn: () => api<Po[]>("/api/inventory/po") });
  const products = useQuery({ queryKey: ["shelter-products"], queryFn: () => api<Product[]>("/api/inventory/products") });
  const suppliers = useQuery({ queryKey: ["suppliers"], queryFn: () => api<Supplier[]>("/api/inventory/suppliers") });
  const approvers = useQuery({ queryKey: ["po-approvers"], queryFn: () => api<Approver[]>("/api/inventory/po/approvers") });
  const settings = useQuery({ queryKey: ["po-settings"], queryFn: () => api<Settings>("/api/inventory/po/settings") });
  const notes = useQuery({ queryKey: ["po-notes"], queryFn: () => api<Note[]>("/api/inventory/notifications") });
  const manager = can(me?.role, ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"]);
  const linkedGrns = useQuery({
    queryKey: ["grns"],
    queryFn: () => api<{ id: number; grnNumber: string; purchaseOrderId?: number; totalAmount: number }[]>("/api/inventory/grn"),
    enabled: manager,
  });
  const [openPo, setOpenPo] = useState<number | null>(null);
  const [recentPage, setRecentPage] = useState(0);
  const [supplierId, setSupplierId] = useState("");
  const [approverUserId, setApproverUserId] = useState("");
  const [poNotes, setPoNotes] = useState("");
  const [items, setItems] = useState([emptyItem()]);
  const [rejectId, setRejectId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [pdfBusy, setPdfBusy] = useState<number | null>(null);
  const [pdfError, setPdfError] = useState("");
  const [poPdf, setPoPdf] = useState<{ src: string; title: string } | null>(null);
  const [route, setRoute] = useState({
    poApproverRole: "BRANCH_ADMIN",
    poApproverUserId: "",
    poQtyTolerancePct: "0",
    poCostTolerancePct: "0",
  });

  useEffect(() => {
    if (!settings.data) return;
    setRoute({
      poApproverRole: settings.data.poApproverRole || "BRANCH_ADMIN",
      poApproverUserId: settings.data.poApproverUserId ? String(settings.data.poApproverUserId) : "",
      poQtyTolerancePct: String(settings.data.poQtyTolerancePct ?? 0),
      poCostTolerancePct: String(settings.data.poCostTolerancePct ?? 0),
    });
  }, [settings.data]);

  const draft = useMutation({
    mutationFn: () =>
      api<Po>("/api/inventory/po", {
        method: "POST",
        body: JSON.stringify({
          supplierId: Number(supplierId),
          notes: poNotes,
          approverUserId: approverUserId ? Number(approverUserId) : null,
          items: items
            .filter((i) => i.shelterProductId && i.qtyOrdered)
            .map((i) => ({
              shelterProductId: Number(i.shelterProductId),
              qtyOrdered: Number(i.qtyOrdered),
              unitCost: Number(i.unitCost || 0),
              notes: i.notes,
            })),
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pos"] });
      qc.invalidateQueries({ queryKey: ["po-receivable"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      setItems([emptyItem()]);
      setPoNotes("");
    },
  });
  const submit = useMutation({
    mutationFn: (id: number) => api(`/api/inventory/po/${id}/submit`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pos"] }),
  });
  const approve = useMutation({
    mutationFn: (id: number) => api(`/api/inventory/po/${id}/approve`, { method: "POST" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pos"] });
      qc.invalidateQueries({ queryKey: ["po-notes"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
  const reject = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      api(`/api/inventory/po/${id}/reject`, { method: "POST", body: JSON.stringify({ reason }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pos"] });
      qc.invalidateQueries({ queryKey: ["po-notes"] });
      setRejectId(null);
      setRejectReason("");
    },
  });
  const saveRoute = useMutation({
    mutationFn: () =>
      api("/api/inventory/po/settings", {
        method: "PUT",
        body: JSON.stringify({
          poApproverRole: route.poApproverRole,
          poApproverUserId: route.poApproverUserId ? Number(route.poApproverUserId) : 0,
          poQtyTolerancePct: Number(route.poQtyTolerancePct),
          poCostTolerancePct: Number(route.poCostTolerancePct),
        }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["po-settings"] }),
  });
  const readNote = useMutation({
    mutationFn: (id: number) => api(`/api/inventory/notifications/${id}/read`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["po-notes"] }),
  });

  const unread = notes.data?.filter((n) => !n.readAt) ?? [];
  const toApprove = pos.data?.filter((p) => p.canApprove) ?? [];
  const mineDraft = pos.data?.filter((p) => p.status === "DRAFT" && p.canEdit) ?? [];
  const waiting = pos.data?.filter((p) => p.status === "SUBMITTED") ?? [];
  const recent = pageSlice(pos.data, recentPage);

  const expected = useMemo(
    () =>
      items.reduce((sum, i) => sum + Number(i.qtyOrdered || 0) * Number(i.unitCost || 0), 0),
    [items],
  );

  function addLine() {
    setItems((prev) => [...prev, emptyItem()]);
  }

  function updateItem(i: number, key: keyof ReturnType<typeof emptyItem>, value: string) {
    setItems((prev) =>
      prev.map((row, idx) => {
        if (idx !== i) return row;
        const next = { ...row, [key]: value };
        if (key === "shelterProductId") {
          const product = products.data?.find((p) => String(p.id) === value);
          if (product?.unitCost != null && !row.unitCost) {
            next.unitCost = String(product.unitCost);
          }
        }
        return next;
      }),
    );
  }

  function onCreate(e: FormEvent) {
    e.preventDefault();
    draft.mutate();
  }

  async function savePoPdf(p: Po) {
    setPdfError("");
    setPdfBusy(p.id);
    try {
      const src = await fetchPdfObjectUrl(`/api/inventory/po/${p.id}/pdf`);
      if (poPdf) URL.revokeObjectURL(poPdf.src);
      setPoPdf({ src, title: p.poNumber });
    } catch (err) {
      setPdfError((err as Error).message);
    } finally {
      setPdfBusy(null);
    }
  }

  function closePoPdf() {
    if (poPdf) URL.revokeObjectURL(poPdf.src);
    setPoPdf(null);
  }

  return (
    <div className="desk">
      <PageHeader
        title="Purchase orders"
        description={
          admin
            ? "Raise a PO and it is approved at once. Stores receive on the GRN. Walk-in challans still work without a PO."
            : "Draft mixed food, meds, and staff retail. An admin approves, then stores receive on the GRN. Walk-in challans still work without a PO."
        }
      />
      {pdfError && <p className="text-sm text-clay">{pdfError}</p>}

      {unread.length > 0 && (
        <Card>
          <h2 className="mb-2 font-semibold">Inbox</h2>
          <ul className="space-y-2">
            {unread.map((n) => (
              <li key={n.id} className="rounded-xl bg-amber-50 px-3 py-2">
                <p className="text-sm font-semibold">{n.title}</p>
                <p className="text-sm text-ink/70">{n.body}</p>
                <Button variant="ghost" className="mt-1" onClick={() => readNote.mutate(n.id)}>
                  Mark read
                </Button>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <Fold title={admin ? "Raise PO" : "New draft"} hint={admin ? "Approved at once" : "Create a PO"} defaultOpen>
        <form className="space-y-3" onSubmit={onCreate}>
          <Label>Supplier</Label>
          <Combobox
            value={supplierId}
            onChange={setSupplierId}
            options={(suppliers.data ?? []).map((s) => ({
              value: String(s.id),
              label: s.name,
              hint: `payable ${inr(s.payableBalance)}`,
            }))}
            placeholder="Search supplier"
            searchPlaceholder="Supplier name…"
          />
          {!admin && (
            <>
              <Label>Approver override (optional)</Label>
              <Select value={approverUserId} onChange={(e) => setApproverUserId(e.target.value)}>
                <option value="">Tenant default</option>
                {(approvers.data ?? []).map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.fullName} · {pretty(a.role)}
                  </option>
                ))}
              </Select>
            </>
          )}
          {items.map((item, i) => (
            <div key={i} className="grid gap-2 rounded-xl bg-sand p-3 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <Combobox
                  value={item.shelterProductId}
                  onChange={(v) => updateItem(i, "shelterProductId", v)}
                  options={toProductOptions(products.data)}
                  placeholder="Food, vaccine, or staff retail"
                  searchPlaceholder="Name or SKU…"
                />
              </div>
              <Input
                type="number"
                step="0.001"
                placeholder="Qty ordered"
                value={item.qtyOrdered}
                onChange={(e) => updateItem(i, "qtyOrdered", e.target.value)}
                required
              />
              <Input
                type="number"
                step="0.0001"
                placeholder="Expected unit cost"
                value={item.unitCost}
                onChange={(e) => updateItem(i, "unitCost", e.target.value)}
                required
              />
            </div>
          ))}
          <Button type="button" variant="ghost" onClick={addLine}>
            Add line
          </Button>
          <p className="text-sm text-ink/60">Expected {inr(expected)}</p>
          <Textarea placeholder="Notes" value={poNotes} onChange={(e) => setPoNotes(e.target.value)} />
          {draft.error && <p className="text-sm text-clay">{(draft.error as Error).message}</p>}
          <Button className="w-full">{admin ? "Raise PO" : "Save draft"}</Button>
        </form>
      </Fold>

      {admin && toApprove.length > 0 && (
        <Card>
          <h2 className="mb-2 font-semibold">To approve</h2>
          <ul className="space-y-3">
            {toApprove.map((p) => (
              <li key={p.id} className="rounded-xl bg-sand p-3">
                <p className="font-semibold">
                  {p.poNumber} · {p.supplierName}
                </p>
                <p className="text-sm text-ink/60">
                  {p.requestedByName} · {inr(p.expectedTotal)}
                </p>
                <ul className="mt-1 text-sm">
                  {(p.lines ?? []).map((l) => (
                    <li key={l.id}>
                      {l.productName} · {l.qtyOrdered} × {inr(l.unitCost)}
                    </li>
                  ))}
                </ul>
                <div className="mt-2 flex flex-wrap gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    disabled={pdfBusy === p.id}
                    onClick={() => void savePoPdf(p)}
                  >
                    {pdfBusy === p.id ? "Opening…" : "Print"}
                  </Button>
                  <Button onClick={() => approve.mutate(p.id)}>Approve</Button>
                  {rejectId === p.id ? (
                    <>
                      <Input
                        placeholder="Reject reason"
                        value={rejectReason}
                        onChange={(e) => setRejectReason(e.target.value)}
                      />
                      <Button
                        variant="danger"
                        disabled={!rejectReason.trim()}
                        onClick={() => reject.mutate({ id: p.id, reason: rejectReason.trim() })}
                      >
                        Confirm reject
                      </Button>
                    </>
                  ) : (
                    <Button variant="danger" onClick={() => setRejectId(p.id)}>
                      Reject
                    </Button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}

      {mineDraft.length > 0 && (
        <Card>
          <h2 className="mb-2 font-semibold">My drafts</h2>
          <ul className="space-y-2">
            {mineDraft.map((p) => (
              <li key={p.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-sand px-3 py-2">
                <span>
                  <b>{p.poNumber}</b> · {p.supplierName} · {inr(p.expectedTotal)}
                </span>
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    disabled={pdfBusy === p.id}
                    onClick={() => void savePoPdf(p)}
                  >
                    {pdfBusy === p.id ? "Opening…" : "Print"}
                  </Button>
                  <Button onClick={() => submit.mutate(p.id)}>{admin ? "Approve" : "Submit"}</Button>
                </div>
              </li>
            ))}
          </ul>
          {submit.error && <p className="mt-2 text-sm text-clay">{(submit.error as Error).message}</p>}
        </Card>
      )}

      <Card>
        <div className="mb-2 flex items-center justify-between gap-2">
          <h2 className="font-semibold">Recent</h2>
          <ExportExcelButton
            disabled={!pos.data?.length}
            onClick={() => {
              const rows = (pos.data ?? []).flatMap((p) => {
                const lines = p.lines?.length ? p.lines : [undefined];
                return lines.map((l) => [
                  p.poNumber,
                  pretty(p.status),
                  p.supplierName ?? "",
                  p.requestedByName ?? "",
                  Number(p.expectedTotal),
                  Number(p.receivedTotal ?? 0),
                  l ? (l.productName ?? `Product ${l.shelterProductId}`) : "",
                  l?.sku ?? "",
                  l ? Number(l.qtyOrdered) : "",
                  l ? Number(l.unitCost) : "",
                  l ? Number(l.qtyReceived) : "",
                  l ? Number(l.damagedQty) : "",
                  p.notes ?? "",
                ]);
              });
              downloadSpreadsheet(
                spreadsheetFilename("po"),
                [
                  "PO",
                  "Status",
                  "Supplier",
                  "Requested by",
                  "Expected",
                  "Received",
                  "Product",
                  "SKU",
                  "Qty ordered",
                  "Unit cost",
                  "Qty received",
                  "Damaged",
                  "Notes",
                ],
                rows,
              );
            }}
          />
        </div>
        <ul className="space-y-2 text-sm">
          {recent.slice.map((p) => {
            const open = openPo === p.id;
            const receipts = (linkedGrns.data ?? []).filter((g) => g.purchaseOrderId === p.id);
            return (
              <li key={p.id} className="rounded-xl bg-sand">
                <div className="flex items-start gap-1">
                  <button
                    type="button"
                    className="min-h-11 min-w-0 flex-1 px-3 py-2 text-left"
                    onClick={() => setOpenPo(open ? null : p.id)}
                    aria-expanded={open}
                  >
                    <div className="flex justify-between gap-2">
                      <b>{p.poNumber}</b>
                      <span>{pretty(p.status)}</span>
                    </div>
                    <p className="text-ink/60">
                      {p.supplierName} · {p.requestedByName} · {inr(p.expectedTotal)}
                      {p.receivedTotal ? ` · received ${inr(p.receivedTotal)}` : ""}
                    </p>
                  </button>
                  <Button
                    type="button"
                    variant="outline"
                    className="mt-1 mr-2 shrink-0 px-3"
                    disabled={pdfBusy === p.id}
                    onClick={() => void savePoPdf(p)}
                  >
                    {pdfBusy === p.id ? "Opening…" : "Print"}
                  </Button>
                </div>
                {open && (
                  <div className="space-y-1 border-t border-moss/10 px-3 py-2">
                    {(p.lines ?? []).map((l) => (
                      <p key={l.id}>
                        {l.productName ?? `Product ${l.shelterProductId}`}
                        {l.sku ? ` · ${l.sku}` : ""}
                        {" · ordered "}
                        {l.qtyOrdered} @ {inr(l.unitCost)}
                        {" · received "}
                        {l.qtyReceived}
                        {Number(l.damagedQty) > 0 ? ` · damaged ${l.damagedQty}` : ""}
                      </p>
                    ))}
                    {receipts.length > 0 && (
                      <p className="text-ink/55">
                        GRN {receipts.map((g) => `${g.grnNumber} (${inr(g.totalAmount)})`).join(", ")}
                      </p>
                    )}
                    {p.notes && <p className="text-ink/50">{p.notes}</p>}
                    {p.rejectReason && <p className="text-clay">{p.rejectReason}</p>}
                  </div>
                )}
              </li>
            );
          })}
          {waiting.length === 0 && recent.total === 0 && <li className="text-ink/50">No purchase orders yet.</li>}
        </ul>
        <PageControls
          page={recent.page}
          pages={recent.pages}
          total={recent.total}
          size={recent.size}
          onPage={setRecentPage}
        />
      </Card>

      {ngo && (
        <Fold title="PO routing" hint="Approvers">
          <p className="mb-3 text-sm text-ink/60">Default approver role for this NGO. You can always approve. Named admin is optional.</p>
          <div className="grid gap-2 sm:grid-cols-2">
            <div>
              <Label>Approver role</Label>
              <Select
                value={route.poApproverRole}
                onChange={(e) => setRoute({ ...route, poApproverRole: e.target.value })}
              >
                <option value="BRANCH_ADMIN">Branch admin</option>
                <option value="NGO_ADMIN">NGO admin</option>
              </Select>
            </div>
            <div>
              <Label>Named approver (optional)</Label>
              <Select
                value={route.poApproverUserId}
                onChange={(e) => setRoute({ ...route, poApproverUserId: e.target.value })}
              >
                <option value="">Anyone with the role</option>
                {(approvers.data ?? []).map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.fullName} · {pretty(a.role)}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label>Qty variance %</Label>
              <Input
                type="number"
                step="0.01"
                value={route.poQtyTolerancePct}
                onChange={(e) => setRoute({ ...route, poQtyTolerancePct: e.target.value })}
              />
            </div>
            <div>
              <Label>Cost variance %</Label>
              <Input
                type="number"
                step="0.01"
                value={route.poCostTolerancePct}
                onChange={(e) => setRoute({ ...route, poCostTolerancePct: e.target.value })}
              />
            </div>
          </div>
          {settings.data && (
            <p className="mt-2 text-xs text-ink/50">
              Saved: {pretty(settings.data.poApproverRole)} · qty {settings.data.poQtyTolerancePct}% · cost{" "}
              {settings.data.poCostTolerancePct}%
            </p>
          )}
          {saveRoute.error && <p className="mt-2 text-sm text-clay">{(saveRoute.error as Error).message}</p>}
          <Button className="mt-3 w-full sm:w-auto" onClick={() => saveRoute.mutate()}>
            Save routing
          </Button>
        </Fold>
      )}
      {poPdf && <PdfPreview src={poPdf.src} title={poPdf.title} onClose={closePoPdf} />}
    </div>
  );
}
