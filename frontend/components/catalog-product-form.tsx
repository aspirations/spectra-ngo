"use client";

import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { pretty } from "@/lib/utils";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";

const CATS = ["FOOD", "VACCINE", "MEDICINE", "CLEANING", "STAFF_RETAIL"] as const;
const UNITS = ["KG", "G", "VIAL", "TABLET", "PIECE", "LITRE"] as const;

export type CatalogProduct = { id: number; name: string; sku: string; category: string };

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

export function CatalogProductForm({ onCreated }: { onCreated: (product: CatalogProduct) => void }) {
  const qc = useQueryClient();
  const [form, setForm] = useState(empty);
  const save = useMutation({
    mutationFn: () =>
      api<CatalogProduct>("/api/inventory/products", {
        method: "POST",
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
      setForm(empty);
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    save.mutate();
  }

  return (
    <form className="space-y-3" onSubmit={onSubmit}>
      <p className="text-sm text-ink/60">Adds to this branch’s single catalog — food, vaccines, and staff goods share the same stockroom.</p>
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
        <input type="checkbox" checked={form.lotTracked} onChange={(e) => setForm({ ...form, lotTracked: e.target.checked })} />
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
      {save.error && <p className="text-sm text-clay">{(save.error as Error).message}</p>}
      <Button className="w-full" disabled={save.isPending}>
        {save.isPending ? "Saving…" : "Add product"}
      </Button>
    </form>
  );
}
