"use client";

import { useEffect, useState } from "react";

export type PhotoKind = "dog" | "person";
export type PhotoDraft = { file: File | null; removed: boolean };

const DB_NAME = "spectra-photos";
const STORE = "photos";
const EVENT = "spectra-photo";

export function emptyPhotoDraft(): PhotoDraft {
  return { file: null, removed: false };
}

export function photoKey(kind: PhotoKind, id: number) {
  return `${kind}:${id}`;
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onupgradeneeded = () => {
      if (!req.result.objectStoreNames.contains(STORE)) {
        req.result.createObjectStore(STORE);
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

function notify(key: string) {
  window.dispatchEvent(new CustomEvent(EVENT, { detail: key }));
}

export async function resizePhoto(file: Blob, max = 512): Promise<Blob> {
  try {
    const bmp = await createImageBitmap(file);
    const scale = Math.min(1, max / Math.max(bmp.width, bmp.height));
    const w = Math.max(1, Math.round(bmp.width * scale));
    const h = Math.max(1, Math.round(bmp.height * scale));
    const canvas = document.createElement("canvas");
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext("2d");
    if (!ctx) {
      bmp.close();
      return file;
    }
    ctx.drawImage(bmp, 0, 0, w, h);
    bmp.close();
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.8));
    return blob ?? file;
  } catch {
    return file;
  }
}

export async function getLocalPhoto(kind: PhotoKind, id: number): Promise<Blob | undefined> {
  if (typeof indexedDB === "undefined") return undefined;
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const req = db.transaction(STORE, "readonly").objectStore(STORE).get(photoKey(kind, id));
    req.onsuccess = () => resolve(req.result as Blob | undefined);
    req.onerror = () => reject(req.error);
  });
}

export async function putLocalPhoto(kind: PhotoKind, id: number, file: Blob) {
  const blob = await resizePhoto(file);
  const db = await openDb();
  const key = photoKey(kind, id);
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).put(blob, key);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  notify(key);
}

export async function deleteLocalPhoto(kind: PhotoKind, id: number) {
  const db = await openDb();
  const key = photoKey(kind, id);
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).delete(key);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  notify(key);
}

export async function commitLocalPhoto(kind: PhotoKind, id: number, draft: PhotoDraft) {
  if (draft.file) await putLocalPhoto(kind, id, draft.file);
  else if (draft.removed) await deleteLocalPhoto(kind, id);
}

export function useLocalPhoto(kind: PhotoKind, id: number | undefined) {
  const [url, setUrl] = useState<string>();
  useEffect(() => {
    if (id == null || typeof indexedDB === "undefined") return;
    let dead = false;
    let objectUrl: string | undefined;
    const load = () => {
      getLocalPhoto(kind, id)
        .then((blob) => {
          if (dead) return;
          if (objectUrl) URL.revokeObjectURL(objectUrl);
          objectUrl = blob ? URL.createObjectURL(blob) : undefined;
          setUrl(objectUrl);
        })
        .catch(() => {
          if (!dead) setUrl(undefined);
        });
    };
    load();
    const key = photoKey(kind, id);
    const onChange = (e: Event) => {
      if ((e as CustomEvent<string>).detail === key) load();
    };
    window.addEventListener(EVENT, onChange);
    return () => {
      dead = true;
      window.removeEventListener(EVENT, onChange);
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [kind, id]);
  return url;
}
