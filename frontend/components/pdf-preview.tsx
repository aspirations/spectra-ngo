"use client";

import { Button } from "@/components/ui/button";
import { useEffect, useRef } from "react";

export function PdfPreview({ src, title, onClose }: { src: string; title: string; onClose: () => void }) {
  const frame = useRef<HTMLIFrameElement>(null);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

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
