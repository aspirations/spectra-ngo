"use client";

import { api } from "@/lib/api";
import { CatalogProductForm } from "@/components/catalog-product-form";
import { Button } from "@/components/ui/button";
import { Combobox, toProductOptions } from "@/components/ui/combobox";
import { Fold } from "@/components/ui/fold";
import { Card, Input, Label } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { PageControls, pageSlice } from "@/components/ui/pager";
import { PageHeader } from "@/components/ui/page-header";
import { inr } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useMemo, useState } from "react";

type Product = { id: number; name: string; sku: string; lotTracked: boolean; category: string; qtyOnHand: number };
type Supplier = { id: number; name: string; payableBalance: number };
type Grn = {
  id: number;
  grnNumber: string;
  totalAmount: number;
  receivedAt: string;
  notes?: string;
  purchaseOrderId?: number;
};
type GrnItem = {
  id: number;
  shelterProductId: number;
  batchNumber?: string;
  expiryDate?: string;
  quantity: number;
  unitLandedCost: number;
  lineTotal: number;
  damagedQty?: number;
  purchaseOrderItemId?: number;
};

type Line = {
  shelterProductId: string;
  purchaseOrderItemId?: string;
  batchNumber: string;
  expiryDate: string;
  quantity: string;
  unitLandedCost: string;
  damagedQty: string;
  expectedQty?: string;
  expectedCost?: string;
  productName?: string;
};

const emptyLine = (): Line => ({
  shelterProductId: "",
  batchNumber: "",
  expiryDate: "",
  quantity: "",
  unitLandedCost: "",
  damagedQty: "0",
});

type PoOption = { id: number; poNumber: string; supplierId: number; supplierName?: string; status: string };
type PreviewLine = {
  purchaseOrderItemId: number;
  shelterProductId: number;
  productName: string;
  lotTracked: boolean;
  qtyRemaining: number;
  unitCost: number;
};
type Preview = { po: { id: number; supplierId: number }; lines: PreviewLine[] };

export default function GrnPage() {
  const qc = useQueryClient();
  const products = useQuery({ queryKey: ["shelter-products"], queryFn: () => api<Product[]>("/api/inventory/products") });
  const suppliers = useQuery({ queryKey: ["suppliers"], queryFn: () => api<Supplier[]>("/api/inventory/suppliers") });
  const grns = useQuery({ queryKey: ["grns"], queryFn: () => api<Grn[]>("/api/inventory/grn") });
  const receivable = useQuery({
    queryKey: ["po-receivable"],
    queryFn: () => api<PoOption[]>("/api/inventory/po/receivable"),
  });
  const poLookup = useQuery({
    queryKey: ["pos"],
    queryFn: () => api<{ id: number; poNumber: string }[]>("/api/inventory/po"),
  });
  const [openGrn, setOpenGrn] = useState<number | null>(null);
  const [recentPage, setRecentPage] = useState(0);
  const recent = pageSlice(grns.data, recentPage);
  const grnItems = useQuery({
    queryKey: ["grn-items", openGrn],
    queryFn: () => api<GrnItem[]>(`/api/inventory/grn/${openGrn}/items`),
    enabled: openGrn != null,
  });
  const [poId, setPoId] = useState("");
  const preview = useQuery({
    queryKey: ["po-preview", poId],
    queryFn: () => api<Preview>(`/api/inventory/po/${poId}/receive-preview`),
    enabled: !!poId,
  });
  const [supplierId, setSupplierId] = useState("");
  const [payAmount, setPayAmount] = useState("");
  const [payNotes, setPayNotes] = useState("");
  const [productOpen, setProductOpen] = useState(false);
  const [productLine, setProductLine] = useState(0);
  const lowStock = useQuery({
    queryKey: ["low-stock"],
    queryFn: () => api<{ id: number; sku: string; name: string; qtyOnHand: number; reorderLevel: number }[]>("/api/inventory/low-stock"),
  });
  const payments = useQuery({
    queryKey: ["supplier-payments", supplierId],
    queryFn: () => api<{ id: number; amount: number; paidAt: string; notes?: string }[]>(`/api/inventory/suppliers/${supplierId}/payments`),
    enabled: !!supplierId,
  });
  const [newSupplier, setNewSupplier] = useState("");
  const [lines, setLines] = useState<Line[]>([emptyLine()]);

  const addSupplier = useMutation({
    mutationFn: () => api("/api/inventory/suppliers", { method: "POST", body: JSON.stringify({ name: newSupplier }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["suppliers"] }),
  });
  const pay = useMutation({
    mutationFn: () =>
      api(`/api/inventory/suppliers/${supplierId}/payments`, {
        method: "POST",
        body: JSON.stringify({ amount: Number(payAmount), notes: payNotes }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["suppliers"] });
      qc.invalidateQueries({ queryKey: ["supplier-payments", supplierId] });
      setPayAmount("");
      setPayNotes("");
    },
  });
  const post = useMutation({
    mutationFn: () =>
      api("/api/inventory/grn", {
        method: "POST",
        body: JSON.stringify({
          supplierId: Number(supplierId),
          purchaseOrderId: poId ? Number(poId) : null,
          items: lines.map((l) => {
            const product = products.data?.find((p) => String(p.id) === l.shelterProductId);
            const lot = product?.lotTracked !== false;
            return {
              shelterProductId: Number(l.shelterProductId),
              purchaseOrderItemId: l.purchaseOrderItemId ? Number(l.purchaseOrderItemId) : null,
              batchNumber: lot ? l.batchNumber : null,
              expiryDate: lot ? l.expiryDate : null,
              quantity: Number(l.quantity),
              unitLandedCost: Number(l.unitLandedCost),
              damagedQty: Number(l.damagedQty || 0),
            };
          }),
        }),
      }),
    onSuccess: () => {
      setPoId("");
      setSupplierId("");
      setLines([emptyLine()]);
      qc.removeQueries({ queryKey: ["po-preview"] });
      qc.invalidateQueries({ queryKey: ["grns"] });
      qc.invalidateQueries({ queryKey: ["suppliers"] });
      qc.invalidateQueries({ queryKey: ["shelter-products"] });
      qc.invalidateQueries({ queryKey: ["low-stock"] });
      qc.invalidateQueries({ queryKey: ["po-receivable"] });
      qc.invalidateQueries({ queryKey: ["pos"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  useEffect(() => {
    if (!poId) return;
    if (preview.isError) {
      setPoId("");
      setLines([emptyLine()]);
      return;
    }
    const data = preview.data;
    if (!data) return;
    const leftover = data.lines.filter((l) => Number(l.qtyRemaining) > 0);
    if (leftover.length === 0) {
      setPoId("");
      setLines([emptyLine()]);
      return;
    }
    setSupplierId(String(data.po.supplierId));
    setLines(
      leftover.map((l) => ({
        shelterProductId: String(l.shelterProductId),
        purchaseOrderItemId: String(l.purchaseOrderItemId),
        batchNumber: "",
        expiryDate: "",
        quantity: String(l.qtyRemaining),
        unitLandedCost: String(l.unitCost),
        damagedQty: "0",
        expectedQty: String(l.qtyRemaining),
        expectedCost: String(l.unitCost),
        productName: l.productName,
      })),
    );
  }, [poId, preview.data, preview.isError]);

  const totals = useMemo(() => {
    const expected = lines.reduce(
      (s, l) => s + Number(l.expectedQty || 0) * Number(l.expectedCost || 0),
      0,
    );
    const actual = lines.reduce((s, l) => s + Number(l.quantity || 0) * Number(l.unitLandedCost || 0), 0);
    return { expected, actual };
  }, [lines]);

  function submit(e: FormEvent) {
    e.preventDefault();
    post.mutate();
  }

  function productOf(line: Line) {
    return products.data?.find((p) => String(p.id) === line.shelterProductId);
  }

  function update(i: number, key: keyof Line, value: string) {
    setLines((prev) => prev.map((l, idx) => (idx === i ? { ...l, [key]: value } : l)));
  }

  return (
    <div className="desk">
      <PageHeader
        title="Receive stock"
        description="Receive against an approved PO, or walk in a challan with no PO. One stockroom — food, vaccines, staff retail."
        actions={
          <Button variant="outline" onClick={() => { setProductLine(0); setProductOpen(true); }}>
            New product
          </Button>
        }
      />
      <Fold title="Post GRN" hint="Receive stock" defaultOpen>
        <form className="space-y-3" onSubmit={submit}>
          <Label>Receive against PO</Label>
          <Combobox
            value={poId}
            onChange={(v) => {
              setPoId(v);
              if (!v) setLines([emptyLine()]);
            }}
            options={[
              { value: "", label: "Walk-in (no PO)" },
              ...(receivable.data ?? []).map((p) => ({
                value: String(p.id),
                label: p.poNumber,
                hint: `${p.supplierName ?? ""} · ${p.status.replaceAll("_", " ")}`,
              })),
            ]}
            placeholder="Walk-in or pick an approved PO"
            searchPlaceholder="PO number…"
            allowClear
          />
          <Label>Supplier</Label>
          <Combobox
            value={supplierId}
            onChange={setSupplierId}
            disabled={!!poId}
            options={(suppliers.data ?? []).map((s) => ({
              value: String(s.id),
              label: s.name,
              hint: `payable ${inr(s.payableBalance)}`,
            }))}
            placeholder="Search supplier"
            searchPlaceholder="Supplier name…"
          />
          {!poId && (
          <div className="flex gap-2">
            <Input placeholder="New supplier" value={newSupplier} onChange={(e) => setNewSupplier(e.target.value)} />
            <Button type="button" variant="outline" onClick={() => addSupplier.mutate()}>
              Add
            </Button>
          </div>
          )}
          {lines.map((line, i) => {
            const product = productOf(line);
            const lot = product ? product.lotTracked : true;
            return (
              <div key={i} className="grid gap-2 rounded-xl bg-sand p-3 sm:grid-cols-2">
                <div className="sm:col-span-2">
                  <Combobox
                    value={line.shelterProductId}
                    onChange={(v) => update(i, "shelterProductId", v)}
                    options={toProductOptions(products.data)}
                    placeholder="Search product"
                    searchPlaceholder="Name or SKU…"
                    disabled={!!line.purchaseOrderItemId}
                    footer={
                      line.purchaseOrderItemId ? undefined : (
                      <Button
                        type="button"
                        variant="ghost"
                        className="w-full"
                        onClick={() => {
                          setProductLine(i);
                          setProductOpen(true);
                        }}
                      >
                        Add new product
                      </Button>
                      )
                    }
                  />
                </div>
                {line.expectedQty != null && (
                  <p className="sm:col-span-2 text-xs text-ink/55">
                    PO remaining {line.expectedQty} @ {inr(Number(line.expectedCost || 0))}
                    {Number(line.quantity) !== Number(line.expectedQty) || Number(line.unitLandedCost) !== Number(line.expectedCost)
                      ? " · this receipt differs — approver will be notified"
                      : ""}
                  </p>
                )}
                {lot && (
                  <>
                    <Input placeholder="Batch / lot no" value={line.batchNumber} onChange={(e) => update(i, "batchNumber", e.target.value)} required={lot} />
                    <Input type="date" value={line.expiryDate} onChange={(e) => update(i, "expiryDate", e.target.value)} required={lot} />
                  </>
                )}
                {!lot && <p className="sm:col-span-2 text-xs text-ink/50">Simple SKU — quantity is stored on the product (no expiry lot).</p>}
                <Input type="number" step="0.001" placeholder="Qty" value={line.quantity} onChange={(e) => update(i, "quantity", e.target.value)} required />
                <Input type="number" step="0.0001" placeholder="Landed cost" value={line.unitLandedCost} onChange={(e) => update(i, "unitLandedCost", e.target.value)} required />
                {poId && (
                  <Input
                    type="number"
                    step="0.001"
                    placeholder="Damaged qty"
                    value={line.damagedQty}
                    onChange={(e) => update(i, "damagedQty", e.target.value)}
                  />
                )}
              </div>
            );
          })}
          {!poId && (
          <Button type="button" variant="ghost" onClick={() => setLines([...lines, emptyLine()])}>
            Add line
          </Button>
          )}
          {poId && (
            <p className="text-sm text-ink/70">
              Expected {inr(totals.expected)} · this GRN {inr(totals.actual)}
              {totals.actual !== totals.expected ? " · variance" : ""}
            </p>
          )}
          {post.error && <p className="text-sm text-clay">{(post.error as Error).message}</p>}
          <Button className="w-full" disabled={post.isPending}>
            {post.isPending ? "Posting…" : "Post GRN"}
          </Button>
        </form>
      </Fold>
      {supplierId && (
        <Fold title="Supplier settlement">
          <p className="mb-2 text-sm text-ink/60">
            Payable {inr(suppliers.data?.find((s) => String(s.id) === supplierId)?.payableBalance ?? 0)}
          </p>
          <form
            className="mb-3 flex flex-wrap gap-2"
            onSubmit={(e) => {
              e.preventDefault();
              pay.mutate();
            }}
          >
            <Input type="number" step="0.01" placeholder="Pay ₹" value={payAmount} onChange={(e) => setPayAmount(e.target.value)} required />
            <Input placeholder="Notes" value={payNotes} onChange={(e) => setPayNotes(e.target.value)} />
            <Button type="submit">Record payment</Button>
          </form>
          {pay.error && <p className="text-sm text-clay">{(pay.error as Error).message}</p>}
          <ul className="space-y-2 text-sm">
            {payments.data?.map((p) => (
              <li key={p.id} className="flex justify-between rounded-xl bg-sand px-3 py-2">
                <span>
                  {p.paidAt?.slice(0, 10)} {p.notes ? `· ${p.notes}` : ""}
                </span>
                <b>{inr(p.amount)}</b>
              </li>
            ))}
          </ul>
        </Fold>
      )}
      {!!lowStock.data?.length && (
        <Card>
          <h2 className="mb-2 font-semibold">Low stock</h2>
          <ul className="space-y-2 text-sm">
            {lowStock.data.map((p) => (
              <li key={p.id} className="rounded-xl bg-orange-50 px-3 py-2">
                {p.name} · {p.qtyOnHand} left (reorder {p.reorderLevel})
              </li>
            ))}
          </ul>
        </Card>
      )}
      <Fold title="Recent GRNs">
        <ul className="space-y-2 text-sm">
          {recent.slice.map((g) => {
            const po = poLookup.data?.find((p) => p.id === g.purchaseOrderId);
            const open = openGrn === g.id;
            return (
              <li key={g.id} className="rounded-xl bg-sand">
                <button
                  type="button"
                  className="flex min-h-11 w-full items-center justify-between gap-2 px-3 py-2 text-left"
                  onClick={() => setOpenGrn(open ? null : g.id)}
                  aria-expanded={open}
                >
                  <span>
                    <b>{g.grnNumber}</b>
                    <span className="text-ink/55">
                      {po ? ` · ${po.poNumber}` : " · walk-in"}
                      {g.receivedAt ? ` · ${String(g.receivedAt).slice(0, 10)}` : ""}
                    </span>
                  </span>
                  <b>{inr(g.totalAmount)}</b>
                </button>
                {open && (
                  <ul className="space-y-1 border-t border-moss/10 px-3 py-2 text-ink/80">
                    {grnItems.isLoading && <li>Loading lines…</li>}
                    {grnItems.error && <li className="text-clay">{(grnItems.error as Error).message}</li>}
                    {grnItems.data?.map((item) => {
                      const product = products.data?.find((p) => p.id === item.shelterProductId);
                      return (
                        <li key={item.id}>
                          {product?.name ?? `Product ${item.shelterProductId}`}
                          {product?.sku ? ` · ${product.sku}` : ""}
                          {" · "}
                          {item.quantity} @ {inr(item.unitLandedCost)}
                          {item.batchNumber ? ` · lot ${item.batchNumber}` : ""}
                          {item.expiryDate ? ` · exp ${item.expiryDate}` : ""}
                          {Number(item.damagedQty) > 0 ? ` · damaged ${item.damagedQty}` : ""}
                        </li>
                      );
                    })}
                    {g.notes && <li className="text-ink/50">{g.notes}</li>}
                  </ul>
                )}
              </li>
            );
          })}
          {recent.total === 0 && <li className="text-ink/50">No GRNs yet.</li>}
        </ul>
        <PageControls
          page={recent.page}
          pages={recent.pages}
          total={recent.total}
          size={recent.size}
          onPage={setRecentPage}
        />
      </Fold>
      <Modal open={productOpen} title="Add product" onClose={() => setProductOpen(false)}>
        <CatalogProductForm
          onCreated={(p) => {
            update(productLine, "shelterProductId", String(p.id));
            setProductOpen(false);
          }}
        />
      </Modal>
    </div>
  );
}
