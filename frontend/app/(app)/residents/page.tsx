"use client";

import { api, Resident } from "@/lib/api";
import { DogIntakeFields, SECTIONS, dogPayload, emptyDogForm } from "@/components/dog-intake-fields";
import { LocalThumb } from "@/components/photo-picker";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { PageHeader, SearchField } from "@/components/ui/page-header";
import { commitLocalPhoto, emptyPhotoDraft } from "@/lib/local-photos";
import { pretty } from "@/lib/utils";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { FormEvent, useMemo, useState } from "react";

const LIST_CAP = 60;

export default function ResidentsPage() {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const [section, setSection] = useState("");
  const residents = useQuery({ queryKey: ["residents"], queryFn: () => api<Resident[]>("/api/residents") });
  const [form, setForm] = useState(emptyDogForm);
  const [photo, setPhoto] = useState(emptyPhotoDraft);
  const save = useMutation({
    mutationFn: async () => {
      const created = await api<Resident>("/api/residents", { method: "POST", body: JSON.stringify(dogPayload(form)) });
      await commitLocalPhoto("dog", created.id, photo);
      return created;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["residents"] });
      setForm(emptyDogForm());
      setPhoto(emptyPhotoDraft());
      setOpen(false);
    },
  });

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return (residents.data ?? []).filter((r) => {
      if (section && r.category !== section) return false;
      if (!needle) return true;
      return `${r.name} ${r.referenceId} ${r.collarNo ?? ""} ${r.color ?? ""} ${r.description ?? ""} ${r.category} ${r.status}`
        .toLowerCase()
        .includes(needle);
    });
  }, [residents.data, q, section]);

  const shown = filtered.slice(0, LIST_CAP);

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    save.mutate();
  }

  return (
    <div className="desk">
      <PageHeader
        title="Dogs"
        description="Search by name, collar, colour, or file number. Type to jump; don’t scroll the whole yard."
        actions={<Button onClick={() => { setPhoto(emptyPhotoDraft()); setForm(emptyDogForm()); setOpen(true); }}>Add dog</Button>}
      />
      <SearchField
        value={q}
        onChange={setQ}
        placeholder="Name, collar, colour, file number…"
        count={filtered.length}
      />
      <div className="flex flex-wrap gap-1.5">
        <button
          type="button"
          onClick={() => setSection("")}
          className={`min-h-9 rounded-full px-3 text-xs font-semibold ${section === "" ? "bg-moss text-white" : "bg-white text-ink/70 ring-1 ring-moss/15"}`}
        >
          All sections
        </button>
        {SECTIONS.map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => setSection(s)}
            className={`min-h-9 rounded-full px-3 text-xs font-semibold ${section === s ? "bg-moss text-white" : "bg-white text-ink/70 ring-1 ring-moss/15"}`}
          >
            {pretty(s)}
          </button>
        ))}
      </div>
      {!q.trim() && (residents.data?.length ?? 0) > LIST_CAP && (
        <p className="text-sm text-ink/50">Showing the first {LIST_CAP} of {residents.data?.length}. Type a name or tag to find the rest.</p>
      )}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {shown.map((resident) => (
          <Link key={resident.id} href={`/residents/${resident.id}`}>
            <Card className="h-full transition hover:border-moss/40">
              <LocalThumb
                kind="dog"
                id={resident.id}
                remoteUrl={resident.photoUrl}
                className="h-16 rounded-xl text-3xl"
              />
              <h2 className="mt-3 text-lg font-bold">{resident.name}</h2>
              <p className="text-sm text-ink/60">{resident.referenceId}</p>
              <p className="mt-1 text-xs uppercase text-moss">
                {resident.collarNo ? `Collar ${resident.collarNo} · ` : ""}
                {pretty(resident.category)}
              </p>
              <p className="text-xs">{pretty(resident.status)}</p>
            </Card>
          </Link>
        ))}
      </div>
      {shown.length === 0 && <p className="py-10 text-center text-sm text-ink/50">No dogs match that search.</p>}
      {filtered.length > shown.length && (
        <p className="text-center text-sm text-ink/45">Keep typing — {filtered.length - shown.length} more matches.</p>
      )}
      <Modal open={open} title="New dog" onClose={() => setOpen(false)}>
        <form className="space-y-3" onSubmit={onSubmit}>
          <DogIntakeFields form={form} onChange={setForm} photo={photo} onPhoto={setPhoto} />
          {save.error && <p className="text-sm text-clay">{(save.error as Error).message}</p>}
          <Button className="w-full" disabled={save.isPending}>
            Save
          </Button>
        </form>
      </Modal>
    </div>
  );
}
