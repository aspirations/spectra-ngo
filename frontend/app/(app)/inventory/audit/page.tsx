"use client";

import { api } from "@/lib/api";
import { CatalogProductForm } from "@/components/catalog-product-form";
import { Button } from "@/components/ui/button";
import { Combobox, toProductOptions } from "@/components/ui/combobox";
import { Fold } from "@/components/ui/fold";
import { Input, Select } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { PageHeader } from "@/components/ui/page-header";
import { pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";

type Product = { id: number; name: string };
type Batch = { id: number; batchNumber: string; qtyOnHand: number; expiryDate: string; shelterProductId: number };
type Audit = { id: number; shelterProductId: number; systemQty: number; physicalQty: number; reason: string; status: string };

export default function AuditPage() {
  const qc = useQueryClient();
  const products = useQuery({ queryKey: ["shelter-products"], queryFn: () => api<Product[]>("/api/inventory/products") });
  const batches = useQuery({ queryKey: ["batches"], queryFn: () => api<Batch[]>("/api/inventory/batches") });
  const audits = useQuery({ queryKey: ["audits"], queryFn: () => api<Audit[]>("/api/inventory/audits") });
  const [form, setForm] = useState({ shelterProductId: "", batchId: "", physicalQty: "", reason: "SPOILAGE" });
  const [productOpen, setProductOpen] = useState(false);
  const flag = useMutation({
    mutationFn: () =>
      api("/api/inventory/audits", {
        method: "POST",
        body: JSON.stringify({
          shelterProductId: Number(form.shelterProductId),
          batchId: form.batchId ? Number(form.batchId) : null,
          physicalQty: Number(form.physicalQty),
          reason: form.reason,
        }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["audits"] }),
  });
  const review = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      api(`/api/inventory/audits/${id}/review`, { method: "POST", body: JSON.stringify({ status }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["audits"] });
      qc.invalidateQueries({ queryKey: ["batches"] });
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    flag.mutate();
  }

  return (
    <div className="desk">
      <PageHeader
        title="Stock discrepancy"
        description="Search the SKU, then flag what you counted. Write-off still needs a branch admin."
        actions={
          <Button variant="outline" onClick={() => setProductOpen(true)}>
            New product
          </Button>
        }
      />
      <Fold title="Flag a count">
        <form className="space-y-3" onSubmit={submit}>
          <Combobox
            value={form.shelterProductId}
            onChange={(shelterProductId) => setForm({ ...form, shelterProductId, batchId: "" })}
            options={toProductOptions(products.data)}
            placeholder="Search product"
            searchPlaceholder="Name or SKU…"
          />
          <Combobox
            value={form.batchId}
            onChange={(batchId) => setForm({ ...form, batchId })}
            options={(batches.data ?? [])
              .filter((b) => !form.shelterProductId || String(b.shelterProductId) === form.shelterProductId)
              .map((b) => ({
                value: String(b.id),
                label: b.batchNumber,
                hint: `qty ${b.qtyOnHand} · exp ${b.expiryDate}`,
              }))}
            placeholder="All batches (system sum)"
            searchPlaceholder="Lot number…"
          />
          <Input type="number" step="0.001" placeholder="Physical qty" value={form.physicalQty} onChange={(e) => setForm({ ...form, physicalQty: e.target.value })} required />
          <Select value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })}>
            {["SPOILAGE", "SPILLAGE", "UNACCOUNTED_LEAKAGE", "BATCH_DAMAGE"].map((r) => (
              <option key={r}>{r}</option>
            ))}
          </Select>
          {flag.error && <p className="text-sm text-clay">{(flag.error as Error).message}</p>}
          <Button className="w-full">Flag uncertain</Button>
        </form>
      </Fold>
      <div className="overflow-x-auto rounded-2xl bg-white">
        <table className="w-full min-w-[640px] text-left text-sm">
          <thead>
            <tr className="border-b text-xs uppercase text-ink/50">
              <th className="p-3">ID</th>
              <th>System</th>
              <th>Physical</th>
              <th>Reason</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {audits.data?.map((a) => (
              <tr key={a.id} className="border-b border-sand">
                <td className="p-3">{a.id}</td>
                <td>{a.systemQty}</td>
                <td>{a.physicalQty}</td>
                <td>{pretty(a.reason)}</td>
                <td>{pretty(a.status)}</td>
                <td className="space-x-2 p-2">
                  {a.status === "FLAGGED_UNCERTAIN" && (
                    <>
                      <Button variant="outline" onClick={() => review.mutate({ id: a.id, status: "APPROVED_WRITE_OFF" })}>
                        Write off
                      </Button>
                      <Button variant="ghost" onClick={() => review.mutate({ id: a.id, status: "REJECTED" })}>
                        Reject
                      </Button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Modal open={productOpen} title="Add product" onClose={() => setProductOpen(false)}>
        <CatalogProductForm
          onCreated={(p) => {
            setForm((f) => ({ ...f, shelterProductId: String(p.id) }));
            setProductOpen(false);
          }}
        />
      </Modal>
    </div>
  );
}
