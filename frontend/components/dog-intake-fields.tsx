"use client";

import { Input, Label, Select, Textarea } from "@/components/ui/input";
import { PhotoPicker } from "@/components/photo-picker";
import { PhotoDraft } from "@/lib/local-photos";
import { Resident } from "@/lib/api";

export const SECTIONS = ["ADULT", "JUVENILE", "POST_OP", "CRITICAL"] as const;
export const SEXES = [
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" },
  { value: "UNKNOWN", label: "Unknown" },
] as const;
export const INTAKE_SOURCES = [
  { value: "STRAY", label: "Stray" },
  { value: "OWNER_SURRENDER", label: "Owner surrender" },
  { value: "TRANSFER", label: "Transfer" },
  { value: "BORN_IN_SHELTER", label: "Born in shelter" },
  { value: "OTHER", label: "Other" },
] as const;
export const STATUSES = ["ACTIVE", "ADOPTED", "DECEASED", "RELEASED", "DISCHARGED"] as const;

export type DogForm = {
  referenceId: string;
  name: string;
  photoUrl: string;
  collarNo: string;
  description: string;
  sex: string;
  color: string;
  approxAge: string;
  intakeSource: string;
  category: string;
  intakeDate: string;
  status: string;
};

export function emptyDogForm(): DogForm {
  return {
    referenceId: "",
    name: "",
    photoUrl: "",
    collarNo: "",
    description: "",
    sex: "",
    color: "",
    approxAge: "",
    intakeSource: "",
    category: "ADULT",
    intakeDate: new Date().toISOString().slice(0, 10),
    status: "ACTIVE",
  };
}

export function dogFormFrom(r: Resident): DogForm {
  return {
    referenceId: r.referenceId ?? "",
    name: r.name ?? "",
    photoUrl: r.photoUrl ?? "",
    collarNo: r.collarNo ?? "",
    description: r.description ?? "",
    sex: r.sex ?? "",
    color: r.color ?? "",
    approxAge: r.approxAge ?? "",
    intakeSource: r.intakeSource ?? "",
    category: r.category,
    intakeDate: r.intakeDate?.slice(0, 10) ?? "",
    status: r.status,
  };
}

export function dogPayload(form: DogForm) {
  return {
    referenceId: form.referenceId.trim(),
    name: form.name.trim(),
    photoUrl: form.photoUrl.trim() || null,
    collarNo: form.collarNo.trim() || null,
    description: form.description.trim() || null,
    sex: form.sex || null,
    color: form.color.trim() || null,
    approxAge: form.approxAge.trim() || null,
    intakeSource: form.intakeSource || null,
    category: form.category,
    intakeDate: form.intakeDate,
    status: form.status,
  };
}

export function DogIntakeFields({
  form,
  onChange,
  photo,
  onPhoto,
  existingPhotoUrl,
}: {
  form: DogForm;
  onChange: (next: DogForm) => void;
  photo: PhotoDraft;
  onPhoto: (draft: PhotoDraft) => void;
  existingPhotoUrl?: string;
}) {
  const set = (patch: Partial<DogForm>) => onChange({ ...form, ...patch });
  return (
    <>
      <div>
        <Label>Name</Label>
        <Input value={form.name} onChange={(e) => set({ name: e.target.value })} required />
      </div>
      <div>
        <Label>Kennel file / reference</Label>
        <Input
          value={form.referenceId}
          onChange={(e) => set({ referenceId: e.target.value })}
          placeholder="File number, tag…"
          required
        />
      </div>
      <div>
        <Label>Collar / tag number</Label>
        <Input value={form.collarNo} onChange={(e) => set({ collarNo: e.target.value })} />
      </div>
      <div>
        <Label>Description</Label>
        <Textarea rows={3} value={form.description} onChange={(e) => set({ description: e.target.value })} />
      </div>
      <div>
        <Label>Sex</Label>
        <Select value={form.sex} onChange={(e) => set({ sex: e.target.value })}>
          <option value="">—</option>
          {SEXES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </Select>
      </div>
      <div>
        <Label>Colour / markings</Label>
        <Input value={form.color} onChange={(e) => set({ color: e.target.value })} />
      </div>
      <div>
        <Label>Approximate age</Label>
        <Input value={form.approxAge} onChange={(e) => set({ approxAge: e.target.value })} placeholder="~8 months" />
      </div>
      <div>
        <Label>How they arrived</Label>
        <Select value={form.intakeSource} onChange={(e) => set({ intakeSource: e.target.value })}>
          <option value="">—</option>
          {INTAKE_SOURCES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </Select>
      </div>
      <div>
        <Label>Section</Label>
        <Select value={form.category} onChange={(e) => set({ category: e.target.value })}>
          {SECTIONS.map((c) => (
            <option key={c} value={c}>
              {c.replaceAll("_", " ")}
            </option>
          ))}
        </Select>
      </div>
      <div>
        <Label>Intake date</Label>
        <Input type="date" value={form.intakeDate} onChange={(e) => set({ intakeDate: e.target.value })} required />
      </div>
      <PhotoPicker draft={photo} onChange={onPhoto} existingUrl={existingPhotoUrl} />
      <div>
        <Label>Status</Label>
        <Select value={form.status} onChange={(e) => set({ status: e.target.value })}>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s.replaceAll("_", " ")}
            </option>
          ))}
        </Select>
      </div>
    </>
  );
}
