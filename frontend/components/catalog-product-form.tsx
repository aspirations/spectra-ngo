"use client";

import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";

const CATS = ["FOOD", "VACCINE", "MEDICINE", "CLEANING", "STAFF_RETAIL"] as const;
const UNITS = ["KG", "G", "VIAL", "TABLET", "PIECE", "LITRE"] as const;

export type CatalogProduct = { id: number; name: string; sku: string; category: string };
export type EditableProduct = CatalogProduct & {
  unit?: string;
  barcode?: string;
  vaccineIntervalDays?: number;
  reorderLevel?: number;
  lotTracked?: boolean;
  unitPrice?: number;
  unitCost?: number;
};

const empty = {
  sku: "",
  name: "",
  unit: "KG",
  category: "FOOD",
  barcode: "",
  vaccineIntervalDays: "",
  reorderLevel: "0",
  lotTracked: true,
  unitPrice: "",
  unitCost: "",
};

type Lot = { id: number; shelterProductId: number; batchNumber: string; expiryDate: string; qtyOnHand: number };

function LotRow({ lot }: { lot: Lot }) {
  const qc = useQueryClient();
  const [expiry, setExpiry] = useState(lot.expiryDate);
  const save = useMutation({
    mutationFn: () =>
      api(`/api/inventory/batches/${lot.id}`, { method: "PUT", body: JSON.stringify({ expiryDate: expiry }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["stock-batches"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      qc.invalidateQueries({ queryKey: ["low-stock"] });
    },
  });
  return (
    <li className="rounded-xl bg-sand p-2">
      <p className="text-sm font-semibold">
        Lot {lot.batchNumber} · {lot.qtyOnHand} left
      </p>
      <div className="mt-1 flex gap-2">
        <Input type="date" value={expiry} onChange={(e) => setExpiry(e.target.value)} />
        <Button type="button" variant="outline" disabled={save.isPending || !expiry || expiry === lot.expiryDate} onClick={() => save.mutate()}>
          {save.isPending ? "Saving…" : "Update"}
        </Button>
      </div>
      {save.error && <p className="text-xs text-clay">{(save.error as Error).message}</p>}
    </li>
  );
}

function LotList({ productId }: { productId: number }) {
  const batches = useQuery({ queryKey: ["stock-batches"], queryFn: () => api<Lot[]>("/api/inventory/batches") });
  const lots = (batches.data ?? []).filter((b) => b.shelterProductId === productId && Number(b.qtyOnHand) > 0);
  return (
    <div>
      <Label>Available lots (change expiry)</Label>
      {batches.isLoading && <p className="text-sm text-ink/50">Loading lots…</p>}
      {!batches.isLoading && lots.length === 0 && <p className="text-sm text-ink/50">No stock in any lot right now.</p>}
      <ul className="space-y-2">
        {lots.map((lot) => (
          <LotRow key={`${lot.id}-${lot.expiryDate}`} lot={lot} />
        ))}
      </ul>
    </div>
  );
}

function toForm(p?: EditableProduct) {
  if (!p) return empty;
  return {
    sku: p.sku,
    name: p.name,
    unit: p.unit ?? "KG",
    category: p.category,
    barcode: p.barcode ?? "",
    vaccineIntervalDays: p.vaccineIntervalDays != null ? String(p.vaccineIntervalDays) : "",
    reorderLevel: String(p.reorderLevel ?? 0),
    lotTracked: p.lotTracked ?? true,
    unitPrice: p.unitPrice ? String(p.unitPrice) : "",
    unitCost: p.unitCost ? String(p.unitCost) : "",
  };
}

export function CatalogProductForm({
  onCreated,
  product,
}: {
  onCreated: (product: CatalogProduct) => void;
  product?: EditableProduct;
}) {
  const qc = useQueryClient();
  const editing = !!product;
  const [form, setForm] = useState(() => toForm(product));
  const save = useMutation({
    mutationFn: () =>
      api<CatalogProduct>(editing ? `/api/inventory/products/${product.id}` : "/api/inventory/products", {
        method: editing ? "PUT" : "POST",
        body: JSON.stringify({
          sku: form.sku,
          name: form.name,
          unit: form.unit,
          category: form.category,
          barcode: form.barcode || null,
          vaccineIntervalDays: form.category === "VACCINE" && form.vaccineIntervalDays ? Number(form.vaccineIntervalDays) : null,
          reorderLevel: Number(form.reorderLevel || 0),
          lotTracked: form.lotTracked,
          staffSale: form.category === "STAFF_RETAIL",
          clinicalUse: form.category !== "STAFF_RETAIL",
          unitPrice: form.unitPrice ? Number(form.unitPrice) : 0,
          unitCost: form.unitCost ? Number(form.unitCost) : 0,
        }),
      }),
    onSuccess: (product) => {
      qc.invalidateQueries({ queryKey: ["shelter-products"] });
      qc.invalidateQueries({ queryKey: ["pos-products"] });
      qc.invalidateQueries({ queryKey: ["low-stock"] });
      onCreated(product);
      if (!editing) setForm(empty);
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    save.mutate();
  }

  return (
    <form className="space-y-3" onSubmit={onSubmit}>
      {!editing && (
        <p className="text-sm text-ink/60">Adds to this branch’s single catalog — food, vaccines, and staff goods share the same stockroom.</p>
      )}
      <div>
        <Label>Name</Label>
        <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Adult kibble / Rabies vial / Staff rice" required />
      </div>
      <div className="grid gap-2 sm:grid-cols-2">
        <div>
          <Label>SKU</Label>
          <Input value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} placeholder="FOOD-KIB-20" required />
        </div>
        <div>
          <Label>Category</Label>
          <Select
            value={form.category}
            disabled={editing}
            onChange={(e) => {
              const category = e.target.value;
              setForm({
                ...form,
                category,
                lotTracked: category !== "STAFF_RETAIL",
                unit: category === "FOOD" ? "KG" : category === "VACCINE" ? "VIAL" : form.unit,
              });
            }}
          >
            {CATS.map((c) => (
              <option key={c} value={c}>
                {pretty(c)}
              </option>
            ))}
          </Select>
        </div>
      </div>
      <div className="grid gap-2 sm:grid-cols-2">
        <div>
          <Label>Unit</Label>
          <Select value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })}>
            {UNITS.map((u) => (
              <option key={u}>{u}</option>
            ))}
          </Select>
        </div>
        <div>
          <Label>Reorder at</Label>
          <Input type="number" step="0.001" value={form.reorderLevel} onChange={(e) => setForm({ ...form, reorderLevel: e.target.value })} />
        </div>
      </div>
      {form.category === "VACCINE" && (
        <div>
          <Label>Booster interval (days)</Label>
          <Input type="number" value={form.vaccineIntervalDays} onChange={(e) => setForm({ ...form, vaccineIntervalDays: e.target.value })} placeholder="365" />
        </div>
      )}
      <label className="flex min-h-11 items-center gap-2 text-sm">
        <input type="checkbox" disabled={editing} checked={form.lotTracked} onChange={(e) => setForm({ ...form, lotTracked: e.target.checked })} />
        Lot-tracked (batch + expiry / FEFO)
      </label>
      <div className="grid gap-2 sm:grid-cols-2">
        <div>
          <Label>Unit cost ₹ (optional)</Label>
          <Input type="number" step="0.01" value={form.unitCost} onChange={(e) => setForm({ ...form, unitCost: e.target.value })} />
        </div>
        {form.category === "STAFF_RETAIL" && (
          <div>
            <Label>Staff sell price ₹</Label>
            <Input type="number" step="0.01" value={form.unitPrice} onChange={(e) => setForm({ ...form, unitPrice: e.target.value })} />
          </div>
        )}
      </div>
      <div>
        <Label>Barcode (optional)</Label>
        <Input value={form.barcode} onChange={(e) => setForm({ ...form, barcode: e.target.value })} />
      </div>
      {editing && form.lotTracked && <LotList productId={product.id} />}
      {save.error && <p className="text-sm text-clay">{(save.error as Error).message}</p>}
      <Button className="w-full" disabled={save.isPending}>
        {save.isPending ? "Saving…" : editing ? "Save changes" : "Add product"}
      </Button>
    </form>
  );
}
