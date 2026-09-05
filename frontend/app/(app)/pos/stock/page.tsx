"use client";

import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Combobox, toProductOptions } from "@/components/ui/combobox";
import { Card, Input, Label } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { PageHeader } from "@/components/ui/page-header";
import { inr } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useMemo, useState } from "react";

type Product = {
  id: number;
  name: string;
  sku: string;
  barcode?: string;
  unitPrice: number;
  unitCost: number;
  qtyOnHand: number;
};
type PurchaseRow = {
  purchase: { id: number; purchaseNumber: string; supplierName?: string; totalAmount: number; purchasedAt: string };
  items: { productId: number; qty: number; unitCost: number; lineTotal: number }[];
};
type Pnl = {
  from: string;
  to: string;
  salesCount: number;
  revenue: number;
  cogs: number;
  grossProfit: number;
  margin: number;
  purchaseCount: number;
  purchaseSpend: number;
  inventoryValue: number;
  products: {
    productId: number;
    name: string;
    sku: string;
    qtyOnHand: number;
    unitCost: number;
    unitPrice: number;
    stockValue: number;
    soldQty: number;
    revenue: number;
    cogs: number;
    profit: number;
  }[];
};

export default function StoreStockPage() {
  const qc = useQueryClient();
  const products = useQuery({ queryKey: ["pos-products"], queryFn: () => api<Product[]>("/api/pos/products") });
  const purchases = useQuery({ queryKey: ["pos-purchases"], queryFn: () => api<PurchaseRow[]>("/api/pos/purchases") });
  const pnl = useQuery({ queryKey: ["pos-pnl"], queryFn: () => api<Pnl>("/api/pos/pnl") });
  const [skuOpen, setSkuOpen] = useState(false);
  const [buyOpen, setBuyOpen] = useState(false);
  const [sku, setSku] = useState({ sku: "", name: "", unitPrice: "", barcode: "" });
  const [buy, setBuy] = useState({ supplierName: "", productId: "", qty: "", unitCost: "" });

  const addSku = useMutation({
    mutationFn: () =>
      api("/api/pos/products", {
        method: "POST",
        body: JSON.stringify({
          sku: sku.sku,
          name: sku.name,
          unitPrice: Number(sku.unitPrice),
          barcode: sku.barcode || null,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pos-products"] });
      setSkuOpen(false);
    },
  });
  const receive = useMutation({
    mutationFn: () =>
      api("/api/pos/purchases", {
        method: "POST",
        body: JSON.stringify({
          supplierName: buy.supplierName,
          items: [{ productId: Number(buy.productId), qty: Number(buy.qty), unitCost: Number(buy.unitCost) }],
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pos-products"] });
      qc.invalidateQueries({ queryKey: ["pos-purchases"] });
      qc.invalidateQueries({ queryKey: ["pos-pnl"] });
      setBuyOpen(false);
    },
  });

  const names = useMemo(() => {
    const map = new Map<number, string>();
    products.data?.forEach((p) => map.set(p.id, p.name));
    return map;
  }, [products.data]);

  return (
    <div className="desk">
      <PageHeader
        title="Shop stock"
        description="Staff-store SKUs from the same warehouse. Search a product when recording a purchase."
        actions={
          <>
            <Button variant="outline" onClick={() => setSkuOpen(true)}>
              New SKU
            </Button>
            <Button onClick={() => setBuyOpen(true)}>Record purchase</Button>
          </>
        }
      />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <p className="text-xs uppercase text-ink/50">Sales</p>
          <p className="text-2xl font-bold">{inr(pnl.data?.revenue)}</p>
          <p className="text-xs text-ink/50">{pnl.data?.salesCount ?? 0} tickets this month</p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-ink/50">Cost of goods</p>
          <p className="text-2xl font-bold">{inr(pnl.data?.cogs)}</p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-ink/50">Gross profit</p>
          <p className="text-2xl font-bold">{inr(pnl.data?.grossProfit)}</p>
          <p className="text-xs text-ink/50">{((Number(pnl.data?.margin) || 0) * 100).toFixed(1)}% margin</p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-ink/50">Stock value</p>
          <p className="text-2xl font-bold">{inr(pnl.data?.inventoryValue)}</p>
          <p className="text-xs text-ink/50">Purchases this month {inr(pnl.data?.purchaseSpend)}</p>
        </Card>
      </div>
      <Card>
        <h2 className="mb-3 font-semibold">Catalog</h2>
        <ul className="space-y-2 text-sm">
          {pnl.data?.products.map((p) => (
            <li key={p.productId} className="rounded-xl bg-sand px-3 py-2">
              <div className="flex justify-between gap-2">
                <b>{p.name}</b>
                <span className={Number(p.profit) >= 0 ? "text-moss" : "text-clay"}>{inr(p.profit)} profit</span>
              </div>
              <p className="text-ink/60">
                Sell {inr(p.unitPrice)} · cost {inr(p.unitCost)} · stock {p.qtyOnHand} ({inr(p.stockValue)}) · sold {p.soldQty}
              </p>
              <Button
                className="mt-2"
                variant="outline"
                onClick={() => {
                  setBuy({ supplierName: "", productId: String(p.productId), qty: "", unitCost: String(p.unitCost ?? "") });
                  setBuyOpen(true);
                }}
              >
                Record purchase
              </Button>
            </li>
          ))}
        </ul>
      </Card>
      <Card>
        <h2 className="mb-3 font-semibold">Purchase history</h2>
        {purchases.data?.length === 0 && <p className="text-sm text-ink/50">No purchases yet. Record what you paid the supplier.</p>}
        <ul className="space-y-2 text-sm">
          {purchases.data?.map((row) => (
            <li key={row.purchase.id} className="rounded-xl bg-sand px-3 py-2">
              <div className="flex justify-between">
                <b>{row.purchase.purchaseNumber}</b>
                <span>{inr(row.purchase.totalAmount)}</span>
              </div>
              <p className="text-ink/60">
                {row.purchase.supplierName || "Supplier not named"} · {row.purchase.purchasedAt?.slice(0, 10)}
              </p>
              {row.items.map((item, i) => (
                <p key={i}>
                  {names.get(item.productId) ?? `SKU ${item.productId}`} · {item.qty} @ {inr(item.unitCost)}
                </p>
              ))}
            </li>
          ))}
        </ul>
      </Card>
      <Modal open={skuOpen} title="New staff-store SKU" onClose={() => setSkuOpen(false)}>
        <form
          className="space-y-3"
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            addSku.mutate();
          }}
        >
          <div>
            <Label>SKU</Label>
            <Input value={sku.sku} onChange={(e) => setSku({ ...sku, sku: e.target.value })} required />
          </div>
          <div>
            <Label>Name</Label>
            <Input value={sku.name} onChange={(e) => setSku({ ...sku, name: e.target.value })} required />
          </div>
          <div>
            <Label>Sell price</Label>
            <Input type="number" step="0.01" value={sku.unitPrice} onChange={(e) => setSku({ ...sku, unitPrice: e.target.value })} required />
          </div>
          <div>
            <Label>Barcode</Label>
            <Input value={sku.barcode} onChange={(e) => setSku({ ...sku, barcode: e.target.value })} />
          </div>
          {addSku.error && <p className="text-sm text-clay">{(addSku.error as Error).message}</p>}
          <Button className="w-full" disabled={addSku.isPending}>
            Save SKU
          </Button>
        </form>
      </Modal>
      <Modal open={buyOpen} title="Record purchase" onClose={() => setBuyOpen(false)}>
        <form
          className="space-y-3"
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            receive.mutate();
          }}
        >
          <p className="text-sm text-ink/60">Stock increases and average cost updates. Profit = sell price − this cost.</p>
          <div>
            <Label>Supplier</Label>
            <Input value={buy.supplierName} onChange={(e) => setBuy({ ...buy, supplierName: e.target.value })} placeholder="Kirana / wholesaler" />
          </div>
          <div>
            <Label>Product</Label>
            <Combobox
              value={buy.productId}
              onChange={(productId) => setBuy({ ...buy, productId })}
              options={toProductOptions(products.data)}
              placeholder="Search product"
              searchPlaceholder="Name or SKU…"
            />
          </div>
          <div>
            <Label>Quantity</Label>
            <Input type="number" step="0.001" value={buy.qty} onChange={(e) => setBuy({ ...buy, qty: e.target.value })} required />
          </div>
          <div>
            <Label>Unit cost (what you paid)</Label>
            <Input type="number" step="0.01" value={buy.unitCost} onChange={(e) => setBuy({ ...buy, unitCost: e.target.value })} required />
          </div>
          {receive.error && <p className="text-sm text-clay">{(receive.error as Error).message}</p>}
          <Button className="w-full" disabled={receive.isPending}>
            Receive stock
          </Button>
        </form>
      </Modal>
    </div>
  );
}
