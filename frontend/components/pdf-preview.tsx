"use client";

import { Button } from "@/components/ui/button";
import { useEffect, useRef, useState } from "react";

function isTouchPrimary() {
  if (typeof window === "undefined") return false;
  if (window.matchMedia("(pointer: coarse)").matches) return true;
  return /iPad|iPhone|iPod|Android/i.test(navigator.userAgent);
}

function openPdf(src: string, allowSameWindow = false) {
  const a = document.createElement("a");
  a.href = src;
  a.target = "_blank";
  a.rel = "noopener noreferrer";
  document.body.appendChild(a);
  a.click();
  a.remove();
  if (allowSameWindow) {
    // Explicit tap: if the tab was blocked, open in this window so the PDF still appears.
    window.setTimeout(() => {
      try {
        if (document.hasFocus()) window.location.assign(src);
      } catch {
        /* ignore */
      }
    }, 400);
  }
}

export function PdfPreview({ src, title, onClose }: { src: string; title: string; onClose: () => void }) {
  const frame = useRef<HTMLIFrameElement>(null);
  const [mobile] = useState(isTouchPrimary);
  const opened = useRef(false);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  useEffect(() => {
    if (!mobile || opened.current) return;
    opened.current = true;
    openPdf(src);
  }, [mobile, src]);

  if (mobile) {
    const fileName = `${title.replace(/[^\w.-]+/g, "_") || "document"}.pdf`;
    return (
      <div className="fixed inset-0 z-[80] flex flex-col bg-white">
        <div className="flex shrink-0 items-center justify-between gap-2 border-b border-moss/10 px-3 py-2">
          <p className="font-semibold">{title}</p>
          <Button type="button" variant="ghost" onClick={onClose}>
            Close
          </Button>
        </div>
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
          <p className="max-w-sm text-sm text-ink/70">
            Mobile browsers cannot show PDFs in this screen. Open or download the file to view or print it.
          </p>
          <div className="flex w-full max-w-xs flex-col gap-2">
            <Button type="button" className="w-full" onClick={() => openPdf(src, true)}>
              Open PDF
            </Button>
            <a
              href={src}
              download={fileName}
              className="inline-flex h-11 w-full items-center justify-center rounded-xl border border-moss/20 bg-white px-4 text-sm font-semibold text-moss"
            >
              Download
            </a>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-[80] flex flex-col bg-white">
      <div className="flex shrink-0 items-center justify-between gap-2 border-b border-moss/10 px-3 py-2 print:hidden">
        <p className="font-semibold">{title}</p>
        <div className="flex gap-2">
          <Button type="button" variant="outline" onClick={() => frame.current?.contentWindow?.print()}>
            Print
          </Button>
          <Button type="button" variant="ghost" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
      <iframe ref={frame} src={src} title={title} className="min-h-0 w-full flex-1" />
    </div>
  );
}
