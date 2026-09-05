"use client";

import { api, Resident } from "@/lib/api";
import { CatalogProductForm } from "@/components/catalog-product-form";
import { DogForm, DogIntakeFields, INTAKE_SOURCES, SEXES, dogFormFrom, dogPayload, emptyDogForm } from "@/components/dog-intake-fields";
import { LocalThumb } from "@/components/photo-picker";
import { Button } from "@/components/ui/button";
import { Combobox, toProductOptions } from "@/components/ui/combobox";
import { Card, Input, Label, Select, Textarea } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { commitLocalPhoto, emptyPhotoDraft, useLocalPhoto } from "@/lib/local-photos";
import { inr, pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { FormEvent, useState } from "react";

type Treatment = {
  id: number;
  administeredAt?: string;
  nextDueDate?: string;
  quantity: number;
  notes?: string;
  status?: string;
};

function dash(value?: string) {
  const t = value?.trim();
  return t ? t : "—";
}

function optionLabel(opts: readonly { value: string; label: string }[], value?: string) {
  if (!value) return "—";
  return opts.find((o) => o.value === value)?.label ?? pretty(value);
}

export default function ResidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const qc = useQueryClient();
  const dogId = Number(id);
  const localPhoto = useLocalPhoto("dog", Number.isFinite(dogId) ? dogId : undefined);
  const resident = useQuery({ queryKey: ["resident", id], queryFn: () => api<Resident>(`/api/residents/${id}`) });
  const notes = useQuery({
    queryKey: ["notes", id],
    queryFn: () => api<{ noteText: string; noteType: string; createdDate: string }[]>(`/api/residents/${id}/notes`),
  });
  const treatments = useQuery({
    queryKey: ["treatments", id],
    queryFn: () => api<Treatment[]>(`/api/residents/${id}/treatments`),
  });
  const timeline = useQuery({
    queryKey: ["timeline", id],
    queryFn: () =>
      api<{ kind: string; at: string; title: string; detail: string; amount: number | null }[]>(`/api/residents/${id}/timeline`),
  });
  const products = useQuery({
    queryKey: ["shelter-products"],
    queryFn: () => api<{ id: number; name: string; category: string }[]>("/api/inventory/products"),
  });
  const [noteOpen, setNoteOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [txOpen, setTxOpen] = useState(false);
  const [productOpen, setProductOpen] = useState(false);
  const [editForm, setEditForm] = useState<DogForm>(emptyDogForm);
  const [editPhoto, setEditPhoto] = useState(emptyPhotoDraft);
  const [note, setNote] = useState({ noteText: "", noteType: "GENERAL" });
  const [shot, setShot] = useState({ shelterProductId: "", quantity: "1", notes: "", nextDueDate: "" });
  const [dueEdits, setDueEdits] = useState<Record<number, string>>({});

  const saveDog = useMutation({
    mutationFn: async () => {
      const updated = await api<Resident>(`/api/residents/${id}`, { method: "PUT", body: JSON.stringify(dogPayload(editForm)) });
      await commitLocalPhoto("dog", Number(id), editPhoto);
      return updated;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["resident", id] });
      qc.invalidateQueries({ queryKey: ["residents"] });
      setEditOpen(false);
    },
  });
  const addNote = useMutation({
    mutationFn: () => api(`/api/residents/${id}/notes`, { method: "POST", body: JSON.stringify(note) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["notes", id] });
      qc.invalidateQueries({ queryKey: ["timeline", id] });
      setNoteOpen(false);
    },
  });
  const giveTx = useMutation({
    mutationFn: () =>
      api(`/api/residents/${id}/treatments`, {
        method: "POST",
        body: JSON.stringify({
          shelterProductId: Number(shot.shelterProductId),
          quantity: Number(shot.quantity),
          notes: shot.notes,
          nextDueDate: shot.nextDueDate || null,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["treatments", id] });
      qc.invalidateQueries({ queryKey: ["timeline", id] });
      qc.invalidateQueries({ queryKey: ["consume"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      setShot({ shelterProductId: "", quantity: "1", notes: "", nextDueDate: "" });
      setTxOpen(false);
    },
  });
  const saveDue = useMutation({
    mutationFn: ({ treatmentId, nextDueDate }: { treatmentId: number; nextDueDate: string }) =>
      api(`/api/residents/${id}/treatments/${treatmentId}`, {
        method: "PATCH",
        body: JSON.stringify({ nextDueDate: nextDueDate || null }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["treatments", id] });
      qc.invalidateQueries({ queryKey: ["timeline", id] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  if (!resident.data) return <p>Loading record…</p>;
  const d = resident.data;

  return (
    <div className="desk">
      <Card>
        <div className="flex flex-wrap items-start gap-4">
          <LocalThumb
            kind="dog"
            id={d.id}
            remoteUrl={d.photoUrl}
            className="h-28 w-28 rounded-2xl text-4xl"
          />
          <div className="min-w-0 flex-1">
        <p className="text-xs uppercase text-moss">{pretty(d.category)}</p>
        <h1 className="text-3xl font-bold">{d.name}</h1>
        <p className="text-sm text-ink/60">
          {d.referenceId} · {pretty(d.status)}
        </p>
        <div className="mt-3 flex flex-wrap gap-2">
          <Button
            onClick={() => {
              setEditForm(dogFormFrom(d));
              setEditPhoto(emptyPhotoDraft());
              setEditOpen(true);
            }}
          >
            Edit dog
          </Button>
          <Button variant="outline" onClick={() => setNoteOpen(true)}>
            Add note
          </Button>
          <Button variant="outline" onClick={() => setTxOpen(true)}>
            Record treatment
          </Button>
          <Button variant="outline" onClick={() => window.print()}>
            Print medical record
          </Button>
        </div>
          </div>
        </div>
      </Card>
      <Card>
        <h2 className="mb-3 font-semibold">Details</h2>
        <dl className="grid gap-3 text-sm sm:grid-cols-2">
          {[
            ["Kennel file", d.referenceId],
            ["Collar / tag", d.collarNo],
            ["Sex", optionLabel(SEXES, d.sex)],
            ["Colour / markings", d.color],
            ["Approximate age", d.approxAge],
            ["How they arrived", optionLabel(INTAKE_SOURCES, d.intakeSource)],
            ["Section", pretty(d.category)],
            ["Intake date", d.intakeDate],
            ["Status", pretty(d.status)],
          ].map(([label, value]) => (
            <div key={label}>
              <dt className="text-xs uppercase text-ink/45">{label}</dt>
              <dd className="mt-0.5">{dash(value)}</dd>
            </div>
          ))}
          <div className="sm:col-span-2">
            <dt className="text-xs uppercase text-ink/45">Description</dt>
            <dd className="mt-0.5 whitespace-pre-wrap">{dash(d.description)}</dd>
          </div>
        </dl>
      </Card>
      <Card>
        <h2 className="mb-3 font-semibold">Health timeline</h2>
        <ul className="space-y-2">
          {timeline.data?.map((n, i) => (
            <li key={i} className="rounded-xl bg-sand px-3 py-2 text-sm">
              <p className="text-xs uppercase text-moss">
                {n.at?.slice(0, 10)} · {pretty(n.kind)}
                {n.amount != null ? ` · ${inr(n.amount)}` : ""}
              </p>
              <b>{pretty(n.title)}</b>
              <p>{n.detail}</p>
            </li>
          ))}
        </ul>
      </Card>
      <Card>
        <h2 className="mb-3 font-semibold">Timeline notes</h2>
        <ul className="space-y-2">
          {notes.data?.map((n, i) => (
            <li key={i} className="rounded-xl bg-sand px-3 py-2 text-sm">
              <b>{pretty(n.noteType)}</b>
              <p>{n.noteText}</p>
            </li>
          ))}
        </ul>
      </Card>
      <Card>
        <h2 className="mb-3 font-semibold">Treatments</h2>
        {saveDue.error && <p className="mb-2 text-sm text-clay">{(saveDue.error as Error).message}</p>}
        <ul className="space-y-2 text-sm">
          {treatments.data?.map((v) => {
            const waiting = v.status === "PENDING_ISSUE";
            const cancelled = v.status === "CANCELLED";
            const dueValue = dueEdits[v.id] ?? v.nextDueDate?.slice(0, 10) ?? "";
            return (
              <li key={v.id} className="rounded-xl bg-sand px-3 py-2">
                <div className="flex flex-wrap items-center gap-2">
                  <span
                    className={`inline-block rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase ${
                      waiting ? "bg-amber-100 text-amber-800" : cancelled ? "bg-ink/10 text-ink/50" : "bg-moss/15 text-moss"
                    }`}
                  >
                    {waiting ? "Waiting" : cancelled ? "Cancelled" : "Issued"}
                  </span>
                  <span>
                    {waiting
                      ? `qty ${v.quantity} · waiting for stores`
                      : cancelled
                        ? `qty ${v.quantity}`
                        : `${v.administeredAt?.slice(0, 10) ?? "—"} · qty ${v.quantity}`}
                  </span>
                </div>
                {!cancelled && (
                  <div className="mt-2 flex flex-wrap items-end gap-2">
                    <div className="min-w-[10rem] flex-1">
                      <Label>Next due</Label>
                      <Input
                        type="date"
                        value={dueValue}
                        onChange={(e) => setDueEdits((prev) => ({ ...prev, [v.id]: e.target.value }))}
                      />
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      disabled={saveDue.isPending || !dueValue}
                      onClick={() => saveDue.mutate({ treatmentId: v.id, nextDueDate: dueValue })}
                    >
                      Save due
                    </Button>
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      </Card>
      <Modal open={editOpen} title="Edit dog" onClose={() => setEditOpen(false)}>
        <form
          className="space-y-3"
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            saveDog.mutate();
          }}
        >
          <DogIntakeFields
            form={editForm}
            onChange={setEditForm}
            photo={editPhoto}
            onPhoto={setEditPhoto}
            existingPhotoUrl={d.photoUrl || localPhoto}
          />
          {saveDog.error && <p className="text-sm text-clay">{(saveDog.error as Error).message}</p>}
          <Button className="w-full" disabled={saveDog.isPending}>
            Save
          </Button>
        </form>
      </Modal>
      <Modal open={noteOpen} title="Add note" onClose={() => setNoteOpen(false)}>
        <form
          className="space-y-3"
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            addNote.mutate();
          }}
        >
          <Select value={note.noteType} onChange={(e) => setNote({ ...note, noteType: e.target.value })}>
            {["GENERAL", "BEHAVIOR", "SURGERY_RECOVERY", "DIET"].map((t) => (
              <option key={t}>{t}</option>
            ))}
          </Select>
          <Textarea rows={4} value={note.noteText} onChange={(e) => setNote({ ...note, noteText: e.target.value })} required />
          {addNote.error && <p className="text-sm text-clay">{(addNote.error as Error).message}</p>}
          <Button className="w-full">Append note</Button>
        </form>
      </Modal>
      <Modal open={txOpen} title="Request from stores" onClose={() => setTxOpen(false)}>
        <form
          className="space-y-3"
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            giveTx.mutate();
          }}
        >
          <Label>Product</Label>
          <Combobox
            value={shot.shelterProductId}
            onChange={(shelterProductId) => setShot({ ...shot, shelterProductId })}
            options={toProductOptions(products.data?.filter((p) => p.category === "VACCINE" || p.category === "MEDICINE"))}
            placeholder="Search vaccine or medicine"
            searchPlaceholder="Product name…"
            footer={
              <Button type="button" variant="ghost" className="w-full" onClick={() => setProductOpen(true)}>
                Add new product
              </Button>
            }
          />
          <Label>Quantity</Label>
          <Input type="number" step="0.001" value={shot.quantity} onChange={(e) => setShot({ ...shot, quantity: e.target.value })} />
          <Label>Next due</Label>
          <Input type="date" value={shot.nextDueDate} onChange={(e) => setShot({ ...shot, nextDueDate: e.target.value })} />
          <p className="text-xs text-ink/55">Leave blank to set the date when stores issue this shot.</p>
          <Textarea value={shot.notes} onChange={(e) => setShot({ ...shot, notes: e.target.value })} placeholder="Notes" />
          <p className="text-xs text-ink/55">Stock is not deducted until inventory issues this to a named taker.</p>
          {giveTx.error && <p className="text-sm text-clay">{(giveTx.error as Error).message}</p>}
          <Button className="w-full">Request from stores</Button>
        </form>
      </Modal>
      <Modal open={productOpen} title="Add product" onClose={() => setProductOpen(false)}>
        <CatalogProductForm
          onCreated={(p) => {
            setShot((s) => ({ ...s, shelterProductId: String(p.id) }));
            setProductOpen(false);
          }}
        />
      </Modal>
    </div>
  );
}
