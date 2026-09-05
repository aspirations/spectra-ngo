"use client";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/input";
import { PhotoDraft, PhotoKind, useLocalPhoto } from "@/lib/local-photos";
import { cn } from "@/lib/utils";
import { useEffect, useMemo, useRef } from "react";

export function PhotoPicker({
  draft,
  onChange,
  existingUrl,
  label = "Photo",
}: {
  draft: PhotoDraft;
  onChange: (draft: PhotoDraft) => void;
  existingUrl?: string;
  label?: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const pickedUrl = useMemo(() => (draft.file ? URL.createObjectURL(draft.file) : undefined), [draft.file]);
  useEffect(() => () => {
    if (pickedUrl) URL.revokeObjectURL(pickedUrl);
  }, [pickedUrl]);
  const preview = draft.file ? pickedUrl : draft.removed ? undefined : existingUrl;
  return (
    <div>
      <Label>{label}</Label>
      <div className="mt-1 flex items-center gap-3">
        <div className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-moss/10 text-2xl">
          {preview ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={preview} alt="" className="h-full w-full object-cover" />
          ) : (
            "📷"
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="outline" onClick={() => inputRef.current?.click()}>
            Choose photo
          </Button>
          {(preview || draft.file) && (
            <Button type="button" variant="ghost" onClick={() => onChange({ file: null, removed: true })}>
              Remove
            </Button>
          )}
        </div>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0];
            e.target.value = "";
            if (file) onChange({ file, removed: false });
          }}
        />
      </div>
      <p className="mt-1 text-xs text-ink/50">Saved on this device only until we add server upload.</p>
    </div>
  );
}

export function LocalThumb({
  kind,
  id,
  remoteUrl,
  fallback,
  className,
  imgClassName,
}: {
  kind: PhotoKind;
  id: number;
  remoteUrl?: string;
  fallback?: string;
  className?: string;
  imgClassName?: string;
}) {
  const local = useLocalPhoto(kind, id);
  const src = remoteUrl || local;
  return (
    <div className={cn("flex items-center justify-center overflow-hidden bg-moss/10", className)}>
      {src ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={src} alt="" className={cn("h-full w-full object-cover", imgClassName)} />
      ) : (
        <span className="text-2xl">{fallback ?? "🐾"}</span>
      )}
    </div>
  );
}
