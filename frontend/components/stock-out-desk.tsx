"use client";

import { api, getUser, Role, User } from "@/lib/api";
import { CatalogProductForm } from "@/components/catalog-product-form";
import { ExportExcelButton } from "@/components/export-excel-button";
import { Button } from "@/components/ui/button";
import { Combobox, toResidentOptions, toStaffOptions } from "@/components/ui/combobox";
import { Card, Input, Label, Select } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { PageControls, pageSlice } from "@/components/ui/pager";
import { PageHeader, SearchField } from "@/components/ui/page-header";
import { downloadSpreadsheet, spreadsheetFilename } from "@/lib/export-spreadsheet";
import { cn, inr, pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useMemo, useState } from "react";

export type StockOutMode = "issue" | "shop";

type ShelterProduct = {
  id: number;
  name: string;
  sku?: string;
  category: string;
  lotTracked: boolean;
  qtyOnHand: number;
  unit?: string;
  barcode?: string;
  vaccineIntervalDays?: number;
  reorderLevel?: number;
  unitPrice?: number;
  unitCost?: number;
};
type ShopProduct = {
  id: number;
  name: string;
  sku: string;
  barcode?: string;
  unitPrice: number;
  unitCost?: number;
  qtyOnHand: number;
};
type Census = { theoreticalFeedKg: number; totalActive: number };
type ConsumeLine = {
  id: number;
  shelterProductId: number;
  qty: number;
  productName?: string;
  residentName?: string;
};
type Consume = {
  id: number;
  consumptionDate: string;
  type: string;
  status: string;
  actualQty: number;
  varianceAlert: boolean;
  variancePct: number;
  takenByName?: string;
  requestedByName?: string;
  notes?: string;
  lines?: ConsumeLine[];
};
type Taker = Pick<User, "id" | "fullName" | "role">;
type Credit = {
  creditLimit: number;
  outstanding: number;
  available: number;
};
type ShopSale = {
  employeeName?: string;
  order: { id: number; orderNumber: string; total: number; tender: string; status: string; orderAt: string };
  items: { id?: number; qty: number; unitPrice: number; lineTotal: number; productName?: string; productId?: number }[];
};
type IssueLine = { qty: number; costCenter: string };

const COST_CENTERS = ["KENNEL_FEEDING", "CLINICAL_TREATMENT", "EMERGENCY_RESCUE"] as const;
const MANAGERS: Role[] = ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"];

function defaultCostCenter(category: string) {
  return category === "FOOD" ? "KENNEL_FEEDING" : "CLINICAL_TREATMENT";
}

function costCenterLabel(value: string) {
  if (value === "KENNEL_FEEDING") return "Kennel feeding";
  if (value === "CLINICAL_TREATMENT") return "Clinical";
  return "Emergency rescue";
}

export function StockOutDesk({ mode }: { mode: StockOutMode }) {
  const me = getUser();
  const qc = useQueryClient();
  const router = useRouter();
  const manager = !!me && MANAGERS.includes(me.role);
  const caretaker = me?.role === "EMPLOYEE";
  const shop = mode === "shop";

  useEffect(() => {
    if (shop && !manager) router.replace("/inventory/consume");
  }, [shop, manager, router]);

  const [q, setQ] = useState("");
  const [takenByUserId, setTakenByUserId] = useState(me ? String(me.id) : "");
  const [residentId, setResidentId] = useState("");
  const [issueTaker, setIssueTaker] = useState<Record<number, string>>({});
  const [issueCart, setIssueCart] = useState<Record<number, IssueLine>>({});
  const [shopCart, setShopCart] = useState<Record<number, number>>({});
  const [employeeId, setEmployeeId] = useState("");
  const [tender, setTender] = useState("PAYROLL_CREDIT");
  const [productOpen, setProductOpen] = useState(false);
  const [skuOpen, setSkuOpen] = useState(false);
  const [sku, setSku] = useState({ sku: "", name: "", unitPrice: "", barcode: "" });
  const [editingSkuId, setEditingSkuId] = useState<number | null>(null);
  const [editProduct, setEditProduct] = useState<ShelterProduct | null>(null);

  const shelterProducts = useQuery({
    queryKey: ["shelter-products"],
    queryFn: () => api<ShelterProduct[]>("/api/inventory/products"),
    enabled: !shop,
  });
  const census = useQuery({
    queryKey: ["census"],
    queryFn: () => api<Census>("/api/residents/census"),
    enabled: !shop,
  });
  const history = useQuery({
    queryKey: ["consume"],
    queryFn: () => api<Consume[]>("/api/inventory/consume"),
    enabled: !shop,
  });
  const takers = useQuery({
    queryKey: ["consume-takers"],
    queryFn: () => api<Taker[]>("/api/inventory/takers"),
    enabled: !shop,
  });
  const residents = useQuery({
    queryKey: ["residents"],
    queryFn: () => api<{ id: number; name: string; referenceId?: string; category?: string }[]>("/api/residents"),
    enabled: !shop && !caretaker,
  });
  const shopProducts = useQuery({
    queryKey: ["pos-products", q],
    queryFn: () => api<ShopProduct[]>(`/api/pos/products${q ? `?q=${encodeURIComponent(q)}` : ""}`),
    enabled: shop,
  });
  const users = useQuery({
    queryKey: ["users"],
    queryFn: () => api<User[]>("/api/users"),
    enabled: shop,
  });
  const credit = useQuery({
    queryKey: ["credit", employeeId],
    queryFn: () => api<Credit>(`/api/pos/credit/${employeeId}`),
    enabled: shop && !!employeeId,
  });
  const shopOrders = useQuery({
    queryKey: ["pos-orders"],
    queryFn: () => api<ShopSale[]>("/api/pos/orders"),
    enabled: shop,
  });

  const catalog = useMemo(() => {
    const all = shelterProducts.data ?? [];
    if (caretaker) return all.filter((p) => p.category === "FOOD");
    return all.filter((p) => p.category !== "STAFF_RETAIL");
  }, [shelterProducts.data, caretaker]);

  const visibleCatalog = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return catalog;
    return catalog.filter(
      (p) =>
        p.name.toLowerCase().includes(needle) ||
        (p.sku ?? "").toLowerCase().includes(needle),
    );
  }, [catalog, q]);

  const drafts = history.data?.filter((c) => c.status === "DRAFT") ?? [];
  const issued = history.data?.filter((c) => c.status !== "DRAFT") ?? [];
  const [salesPage, setSalesPage] = useState(0);
  const [issuesPage, setIssuesPage] = useState(0);
  const sales = pageSlice(shopOrders.data, salesPage);
  const issues = pageSlice(issued, issuesPage);

  const issueLines = useMemo(
    () =>
      Object.entries(issueCart)
        .map(([id, line]) => {
          const product = catalog.find((p) => p.id === Number(id));
          return product ? { product, ...line } : null;
        })
        .filter((row): row is { product: ShelterProduct; qty: number; costCenter: string } => row != null),
    [issueCart, catalog],
  );
  const issueQty = issueLines.reduce((sum, line) => sum + line.qty, 0);

  const shopLines = useMemo(
    () =>
      (shopProducts.data ?? [])
        .filter((p) => (shopCart[p.id] || 0) > 0)
        .map((p) => ({ product: p, qty: shopCart[p.id], total: shopCart[p.id] * Number(p.unitPrice) })),
    [shopCart, shopProducts.data],
  );
  const shopTotal = shopLines.reduce((sum, line) => sum + line.total, 0);

  const postConsume = useMutation({
    mutationFn: () =>
      api("/api/inventory/consume", {
        method: "POST",
        body: JSON.stringify({
          type: "MIXED",
          takenByUserId: manager && takenByUserId ? Number(takenByUserId) : undefined,
          items: issueLines.map((line) => ({
            shelterProductId: line.product.id,
            quantity: line.qty,
            costCenter: line.costCenter,
            takenByUserId: manager && takenByUserId ? Number(takenByUserId) : undefined,
            residentId: residentId ? Number(residentId) : null,
          })),
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["consume"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      if (manager) qc.invalidateQueries({ queryKey: ["shelter-products"] });
      setIssueCart({});
    },
  });
  const issueDraft = useMutation({
    mutationFn: ({ id, takenByUserId: taker }: { id: number; takenByUserId: number }) =>
      api(`/api/inventory/consume/${id}/issue`, {
        method: "POST",
        body: JSON.stringify({ takenByUserId: taker }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["consume"] });
      qc.invalidateQueries({ queryKey: ["shelter-products"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      qc.invalidateQueries({ queryKey: ["treatments"] });
      qc.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
  const reject = useMutation({
    mutationFn: (id: number) => api(`/api/inventory/consume/${id}/reject`, { method: "POST" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["consume"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      qc.invalidateQueries({ queryKey: ["treatments"] });
      qc.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
  const checkout = useMutation({
    mutationFn: () =>
      api("/api/pos/orders", {
        method: "POST",
        body: JSON.stringify({
          employeeId: Number(employeeId),
          tender,
          items: Object.entries(shopCart)
            .filter(([, qty]) => qty > 0)
            .map(([productId, qty]) => ({ productId: Number(productId), qty })),
        }),
      }),
    onSuccess: () => {
      setShopCart({});
      qc.invalidateQueries({ queryKey: ["pos-products"] });
      qc.invalidateQueries({ queryKey: ["pos-orders"] });
      qc.invalidateQueries({ queryKey: ["credit", employeeId] });
    },
  });
  const addSku = useMutation({
    mutationFn: () =>
      api(editingSkuId != null ? `/api/pos/products/${editingSkuId}` : "/api/pos/products", {
        method: editingSkuId != null ? "PUT" : "POST",
        body: JSON.stringify({
          sku: sku.sku,
          name: sku.name,
          unitPrice: Number(sku.unitPrice),
          barcode: sku.barcode || null,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pos-products"] });
      closeSkuModal();
    },
  });

  function openNewSku() {
    setEditingSkuId(null);
    setSku({ sku: "", name: "", unitPrice: "", barcode: "" });
    setSkuOpen(true);
  }

  function openEditSku(p: ShopProduct) {
    setEditingSkuId(p.id);
    setSku({ sku: p.sku, name: p.name, unitPrice: String(p.unitPrice ?? ""), barcode: p.barcode ?? "" });
    setSkuOpen(true);
  }

  function closeSkuModal() {
    setSkuOpen(false);
    setEditingSkuId(null);
    setSku({ sku: "", name: "", unitPrice: "", barcode: "" });
  }

  function jumpTo(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function bumpIssue(product: ShelterProduct, delta: number) {
    setIssueCart((prev) => {
      const current = prev[product.id];
      const nextQty = Number(((current?.qty ?? 0) + delta).toFixed(3));
      if (nextQty <= 0) {
        const next = { ...prev };
        delete next[product.id];
        return next;
      }
      return {
        ...prev,
        [product.id]: {
          qty: nextQty,
          costCenter: current?.costCenter ?? defaultCostCenter(product.category),
        },
      };
    });
  }

  function setIssueQty(product: ShelterProduct, raw: string) {
    const qty = Number(raw);
    setIssueCart((prev) => {
      if (!raw || qty <= 0) {
        const next = { ...prev };
        delete next[product.id];
        return next;
      }
      return {
        ...prev,
        [product.id]: {
          qty,
          costCenter: prev[product.id]?.costCenter ?? defaultCostCenter(product.category),
        },
      };
    });
  }

  function bumpShop(id: number, delta: number) {
    setShopCart((prev) => {
      const nextQty = (prev[id] || 0) + delta;
      if (nextQty <= 0) {
        const next = { ...prev };
        delete next[id];
        return next;
      }
      return { ...prev, [id]: nextQty };
    });
  }

  const title = manager ? "Issue stock" : "Request stock";
  const description = shop
    ? "Search the buyer, then tap SKUs. Credit is checked against unpaid dues before you complete the sale."
    : manager
      ? "Fulfill requests or issue now from the same tiles as the shop. Stock deducts only when you issue to a named person."
      : "Ask stores for feed or medicine. Inventory will issue it to someone — nothing leaves the stockroom until then.";

  if (shop && !manager) {
    return null;
  }

  return (
    <div className="desk">
      <PageHeader
        title={title}
        description={description}
        actions={
          <>
            <Button variant="outline" onClick={() => jumpTo(shop ? "recent-sales" : "recent-issues")}>
              {shop ? "Recent sales ↓" : "Recent issues ↓"}
            </Button>
            {manager &&
              (shop ? (
                <Button variant="outline" onClick={openNewSku}>
                  New SKU
                </Button>
              ) : (
                <Button variant="outline" onClick={() => setProductOpen(true)}>
                  New product
                </Button>
              ))}
          </>
        }
      />

      {manager && (
        <div className="grid grid-cols-2 rounded-2xl bg-sand p-1">
          <Link
            href="/inventory/consume"
            className={cn(
              "flex min-h-11 items-center justify-center rounded-xl text-sm font-semibold",
              !shop ? "bg-white text-ink shadow-sm" : "text-ink/55",
            )}
          >
            Issue
          </Link>
          <Link
            href="/pos"
            className={cn(
              "flex min-h-11 items-center justify-center rounded-xl text-sm font-semibold",
              shop ? "bg-white text-ink shadow-sm" : "text-ink/55",
            )}
          >
            Shop
          </Link>
        </div>
      )}

      {!shop && (
        <>
          <p className="text-xs text-ink/55">
            Theoretical feed <b>{census.data?.theoreticalFeedKg ?? "—"} kg</b> · {census.data?.totalActive ?? 0} dogs
            · flag if food issue &gt; 15% over.
          </p>

          {manager && (
            <Card>
              <h2 className="mb-2 font-semibold">Pending requests</h2>
              {drafts.length === 0 && (
                <p className="text-sm text-ink/50">No drafts waiting. Vets and caretakers request from their login.</p>
              )}
              <ul className="space-y-3">
                {drafts.map((c) => (
                  <li key={c.id} className="rounded-xl bg-sand p-3">
                    <p className="text-xs uppercase text-moss">
                      {pretty(c.type)} · requested by {c.requestedByName ?? "—"}
                    </p>
                    <ul className="mt-1 text-sm">
                      {(c.lines ?? []).map((line) => (
                        <li key={line.id}>
                          {line.productName ?? `SKU ${line.shelterProductId}`} · {line.qty}
                          {line.residentName ? ` · ${line.residentName}` : ""}
                        </li>
                      ))}
                    </ul>
                    {c.notes && <p className="text-xs text-ink/55">{c.notes}</p>}
                    <div className="mt-2 grid gap-2 sm:grid-cols-[1fr_auto_auto]">
                      <Combobox
                        value={issueTaker[c.id] ?? ""}
                        onChange={(v) => setIssueTaker({ ...issueTaker, [c.id]: v })}
                        options={toStaffOptions(takers.data)}
                        placeholder="Who collects this"
                        searchPlaceholder="Staff name…"
                      />
                      <Button
                        type="button"
                        disabled={!issueTaker[c.id] || issueDraft.isPending}
                        onClick={() => issueDraft.mutate({ id: c.id, takenByUserId: Number(issueTaker[c.id]) })}
                      >
                        Issue
                      </Button>
                      <Button type="button" variant="outline" disabled={reject.isPending} onClick={() => reject.mutate(c.id)}>
                        Reject
                      </Button>
                    </div>
                    {issueDraft.error && <p className="mt-1 text-sm text-clay">{(issueDraft.error as Error).message}</p>}
                  </li>
                ))}
              </ul>
            </Card>
          )}

          {!manager && drafts.length > 0 && (
            <Card>
              <h2 className="mb-2 font-semibold">Waiting for stores</h2>
              <ul className="space-y-2 text-sm">
                {drafts.map((c) => (
                  <li key={c.id} className="rounded-xl bg-sand px-3 py-2">
                    {(c.lines ?? []).map((line) => line.productName ?? "Item").join(", ")} · requested {c.consumptionDate}
                  </li>
                ))}
              </ul>
            </Card>
          )}

          {manager && (
            <div>
              <Label>Taken by</Label>
              <Combobox
                value={takenByUserId}
                onChange={setTakenByUserId}
                options={toStaffOptions(takers.data)}
                placeholder="Who collected this stock"
                searchPlaceholder="Staff name…"
                allowClear={false}
              />
            </div>
          )}
          {!caretaker && (
            <div>
              <Label>For dog</Label>
              <Combobox
                value={residentId}
                onChange={setResidentId}
                options={toResidentOptions(residents.data)}
                placeholder="All kennel / no dog"
                searchPlaceholder="Dog name or tag…"
              />
            </div>
          )}
          <SearchField
            value={q}
            onChange={setQ}
            placeholder={caretaker ? "Search kennel feed" : "Search food, meds, vaccines"}
            count={visibleCatalog.length}
          />
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {visibleCatalog.map((p) => {
              const onTicket = issueCart[p.id];
              return (
                <Card key={p.id} className="flex h-full flex-col">
                  <button type="button" className="flex-1 text-left" onClick={() => bumpIssue(p, 1)}>
                    <p className="font-semibold">{p.name}</p>
                    <p className="text-xs text-ink/50">stock {p.qtyOnHand}</p>
                    {onTicket ? <p className="mt-1 text-lg font-bold">On ticket × {onTicket.qty}</p> : null}
                  </button>
                  <div className="mt-3 flex gap-2">
                    <Button className="flex-1" onClick={() => bumpIssue(p, 1)}>
                      Add
                    </Button>
                    {onTicket ? (
                      <Button variant="outline" onClick={() => bumpIssue(p, -1)}>
                        −
                      </Button>
                    ) : null}
                    {manager ? (
                      <Button variant="outline" aria-label={`Edit ${p.name}`} onClick={() => setEditProduct(p)}>
                        Edit
                      </Button>
                    ) : null}
                  </div>
                </Card>
              );
            })}
          </div>
        </>
      )}

      {shop && (
        <>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label>Sell to employee</Label>
              <Combobox
                value={employeeId}
                onChange={setEmployeeId}
                options={toStaffOptions(users.data?.filter((u) => u.status === "ACTIVE"))}
                placeholder="Search staff"
                searchPlaceholder="Name or email…"
              />
            </div>
            <div>
              <Label>Payment</Label>
              <Select value={tender} onChange={(e) => setTender(e.target.value)}>
                <option>PAYROLL_CREDIT</option>
                <option>CASH</option>
              </Select>
            </div>
          </div>
          {credit.data && (
            <Card>
              <p className="text-sm">
                Limit {inr(credit.data.creditLimit)} · outstanding {inr(credit.data.outstanding)} · available{" "}
                <b>{inr(credit.data.available)}</b>
              </p>
              <div className="mt-2 h-2 overflow-hidden rounded-full bg-sand">
                <div
                  className="h-full bg-moss"
                  style={{
                    width: `${Math.min(100, (Number(credit.data.outstanding) / Math.max(1, Number(credit.data.creditLimit))) * 100)}%`,
                  }}
                />
              </div>
            </Card>
          )}
          <SearchField value={q} onChange={setQ} placeholder="Search name / SKU / barcode" />
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {shopProducts.data?.map((p) => (
              <Card key={p.id} className="flex h-full flex-col">
                <button type="button" className="flex-1 text-left" onClick={() => bumpShop(p.id, 1)}>
                  <p className="font-semibold">{p.name}</p>
                  <p className="text-sm text-moss">{inr(p.unitPrice)}</p>
                  <p className="text-xs text-ink/50">stock {p.qtyOnHand}</p>
                  {shopCart[p.id] ? <p className="mt-1 text-lg font-bold">On bill × {shopCart[p.id]}</p> : null}
                </button>
                <div className="mt-3 flex gap-2">
                  <Button className="flex-1" onClick={() => bumpShop(p.id, 1)}>
                    Add to sale
                  </Button>
                  {shopCart[p.id] ? (
                    <Button variant="outline" onClick={() => bumpShop(p.id, -1)}>
                      −
                    </Button>
                  ) : null}
                  <Button variant="outline" aria-label={`Edit ${p.name}`} onClick={() => openEditSku(p)}>
                    Edit
                  </Button>
                </div>
              </Card>
            ))}
          </div>
        </>
      )}

      {shop && (
        <Card id="recent-sales" className="scroll-mt-4">
          <div className="mb-2 flex items-center justify-between gap-2">
            <h2 className="font-semibold">Recent sales</h2>
            <ExportExcelButton
              disabled={!shopOrders.data?.length}
              onClick={() => {
                const rows = (shopOrders.data ?? []).flatMap((row) => {
                  const items = row.items.length ? row.items : [{ qty: 0, unitPrice: 0, lineTotal: 0 }];
                  return items.map((item) => [
                    row.order.orderNumber,
                    row.order.orderAt?.replace("T", " ").slice(0, 16) ?? "",
                    pretty(row.order.status),
                    pretty(row.order.tender),
                    row.employeeName ?? "",
                    item.productName ?? (item.productId ? `SKU ${item.productId}` : ""),
                    Number(item.qty),
                    Number(item.unitPrice),
                    Number(item.lineTotal),
                    Number(row.order.total),
                  ]);
                });
                downloadSpreadsheet(
                  spreadsheetFilename("sales"),
                  [
                    "Order number",
                    "Time",
                    "Status",
                    "Tender",
                    "Employee",
                    "Product",
                    "Qty",
                    "Unit price",
                    "Line total",
                    "Order total",
                  ],
                  rows,
                );
              }}
            />
          </div>
          {sales.total === 0 && <p className="text-sm text-ink/50">Nothing sold yet.</p>}
          <ul className="space-y-2 text-sm">
            {sales.slice.map((row) => (
              <li key={row.order.id} className="rounded-xl bg-sand px-3 py-2">
                <p className="text-xs uppercase text-moss">
                  {pretty(row.order.status)} · {pretty(row.order.tender)} · {row.order.orderAt?.replace("T", " ").slice(0, 16)}
                </p>
                <ul className="mt-1">
                  {row.items.map((item, idx) => (
                    <li key={item.id ?? idx}>
                      {item.productName ?? (item.productId ? `SKU ${item.productId}` : "Item")} · {item.qty} × {inr(item.unitPrice)}
                    </li>
                  ))}
                </ul>
                <p className="mt-1 text-xs text-ink/55">
                  {row.order.orderNumber} · {row.employeeName ?? "—"} · {inr(row.order.total)}
                </p>
              </li>
            ))}
          </ul>
          <PageControls
            page={sales.page}
            pages={sales.pages}
            total={sales.total}
            size={sales.size}
            onPage={setSalesPage}
          />
        </Card>
      )}

      {!shop && (
        <Card id="recent-issues" className="scroll-mt-4">
          <div className="mb-2 flex items-center justify-between gap-2">
            <h2 className="font-semibold">Recent issues</h2>
            <ExportExcelButton
              disabled={issued.length === 0}
              onClick={() => {
                const rows = issued.flatMap((c) => {
                  const lines =
                    (c.lines ?? []).length > 0
                      ? c.lines!
                      : [{ productName: "food", qty: c.actualQty ?? 0, residentName: undefined, shelterProductId: 0, id: 0 }];
                  return lines.map((line) => [
                    c.consumptionDate,
                    pretty(c.type),
                    pretty(c.status),
                    line.productName ?? `SKU ${line.shelterProductId}`,
                    Number(line.qty),
                    line.residentName ?? "",
                    c.takenByName ?? "",
                    c.requestedByName ?? "",
                    c.notes ?? "",
                    c.varianceAlert ? Number(c.variancePct) : "",
                  ]);
                });
                downloadSpreadsheet(
                  spreadsheetFilename("issues"),
                  [
                    "Date",
                    "Type",
                    "Status",
                    "Product",
                    "Qty",
                    "Dog",
                    "Taken by",
                    "Requested by",
                    "Notes",
                    "Feed variance",
                  ],
                  rows,
                );
              }}
            />
          </div>
          {issues.total === 0 && <p className="text-sm text-ink/50">Nothing issued yet.</p>}
          <ul className="space-y-2 text-sm">
            {issues.slice.map((c) => (
              <li key={c.id} className={`rounded-xl px-3 py-2 ${c.varianceAlert ? "bg-orange-50" : "bg-sand"}`}>
                <p className="text-xs uppercase text-moss">
                  {pretty(c.status)} · {pretty(c.type)} · {c.consumptionDate}
                </p>
                <ul className="mt-1">
                  {(c.lines ?? []).length === 0 ? (
                    <li>food {c.actualQty ?? 0} kg</li>
                  ) : (
                    (c.lines ?? []).map((line) => (
                      <li key={line.id}>
                        {line.productName ?? `SKU ${line.shelterProductId}`} · {line.qty}
                        {line.residentName ? ` · ${line.residentName}` : ""}
                      </li>
                    ))
                  )}
                </ul>
                <p className="mt-1 text-xs text-ink/55">
                  Taken by {c.takenByName ?? "—"}
                  {c.requestedByName ? ` · requested by ${c.requestedByName}` : ""}
                </p>
                {c.notes && <p className="text-xs text-ink/55">{c.notes}</p>}
                {c.varianceAlert ? (
                  <p className="text-xs text-clay">Feed variance {(Number(c.variancePct) * 100).toFixed(1)}% vs census</p>
                ) : null}
              </li>
            ))}
          </ul>
          <PageControls
            page={issues.page}
            pages={issues.pages}
            total={issues.total}
            size={issues.size}
            onPage={setIssuesPage}
          />
        </Card>
      )}

      {!shop && issueQty > 0 && (
        <div className="sticky bottom-20 z-20 space-y-3 rounded-2xl bg-ink p-4 text-sand shadow-lg md:bottom-3">
          <p className="text-xs uppercase opacity-70">{manager ? "Issue ticket" : "Request ticket"}</p>
          <ul className="space-y-2">
            {issueLines.map((line) => (
              <li key={line.product.id} className="grid grid-cols-[1fr_5.5rem] gap-2 sm:grid-cols-[1fr_5.5rem_1fr]">
                <p className="self-center text-sm font-semibold">{line.product.name}</p>
                <Input
                  type="number"
                  step="0.001"
                  min="0"
                  className="bg-white text-ink"
                  value={line.qty}
                  onChange={(e) => setIssueQty(line.product, e.target.value)}
                />
                <Select
                  className="col-span-2 bg-white text-ink sm:col-span-1"
                  value={line.costCenter}
                  onChange={(e) =>
                    setIssueCart((prev) => ({
                      ...prev,
                      [line.product.id]: { ...prev[line.product.id], costCenter: e.target.value },
                    }))
                  }
                >
                  {COST_CENTERS.map((c) => (
                    <option key={c} value={c}>
                      {costCenterLabel(c)}
                    </option>
                  ))}
                </Select>
              </li>
            ))}
          </ul>
          {postConsume.error && <p className="text-sm text-orange-200">{(postConsume.error as Error).message}</p>}
          <Button
            className="w-full bg-leaf"
            disabled={postConsume.isPending || (manager && !takenByUserId)}
            onClick={() => postConsume.mutate()}
          >
            {postConsume.isPending ? "Sending…" : manager ? "Issue stock" : "Send to stores"}
          </Button>
        </div>
      )}

      {shop && shopLines.length > 0 && (
        <div className="sticky bottom-20 z-20 rounded-2xl bg-ink p-4 text-sand shadow-lg md:bottom-3">
          <ul className="mb-3 max-h-40 space-y-1 overflow-y-auto border-b border-sand/20 pb-3 text-sm">
            {shopLines.map((line) => (
              <li key={line.product.id} className="flex items-center justify-between gap-3">
                <span className="min-w-0 truncate">
                  {line.product.name} × {line.qty}
                </span>
                <span className="shrink-0 opacity-80">{inr(line.total)}</span>
              </li>
            ))}
          </ul>
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs uppercase opacity-70">Sale ticket {me?.fullName}</p>
              <p className="text-2xl font-bold">{inr(shopTotal)}</p>
              {!employeeId && <p className="text-xs text-orange-200">Select who is buying</p>}
            </div>
            <Button disabled={!employeeId || checkout.isPending} onClick={() => checkout.mutate()}>
              {checkout.isPending ? "Selling…" : "Complete sale"}
            </Button>
          </div>
          {checkout.error && <p className="mt-2 text-sm text-orange-200">{(checkout.error as Error).message}</p>}
        </div>
      )}

      <Modal open={productOpen} title="Add product" onClose={() => setProductOpen(false)}>
        <CatalogProductForm
          onCreated={(p) => {
            const next = catalog.find((x) => x.id === p.id);
            const category = next?.category ?? p.category;
            setIssueCart((prev) => ({
              ...prev,
              [p.id]: { qty: prev[p.id]?.qty ?? 1, costCenter: defaultCostCenter(category) },
            }));
            setProductOpen(false);
          }}
        />
      </Modal>
      <Modal open={!!editProduct} title="Edit product" onClose={() => setEditProduct(null)}>
        {editProduct && <CatalogProductForm key={editProduct.id} product={{ ...editProduct, sku: editProduct.sku ?? "" }} onCreated={() => setEditProduct(null)} />}
      </Modal>
      <Modal
        open={skuOpen}
        title={editingSkuId != null ? "Edit staff-store SKU" : "New staff-store SKU"}
        onClose={closeSkuModal}
      >
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
            <Input
              type="number"
              step="0.01"
              value={sku.unitPrice}
              onChange={(e) => setSku({ ...sku, unitPrice: e.target.value })}
              required
            />
          </div>
          <div>
            <Label>Barcode</Label>
            <Input value={sku.barcode} onChange={(e) => setSku({ ...sku, barcode: e.target.value })} />
          </div>
          {addSku.error && <p className="text-sm text-clay">{(addSku.error as Error).message}</p>}
          <Button className="w-full" disabled={addSku.isPending}>
            {editingSkuId != null ? "Save changes" : "Save SKU"}
          </Button>
        </form>
      </Modal>
    </div>
  );
}
