"use client";

import { cn } from "@/lib/utils";
import { ChevronDown } from "lucide-react";
import { ReactNode, useState } from "react";

export function Fold({
  title,
  hint,
  children,
  defaultOpen = false,
  className,
}: {
  title: string;
  hint?: ReactNode;
  children: ReactNode;
  defaultOpen?: boolean;
  className?: string;
}) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <details
      className={cn(
        "group rounded-2xl border border-moss/10 bg-white p-3 shadow-[0_1px_2px_rgba(15,28,23,0.04)]",
        className,
      )}
      open={open}
      onToggle={(e) => setOpen(e.currentTarget.open)}
    >
      <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-3 font-semibold marker:content-none [&::-webkit-details-marker]:hidden">
        {title}
        <span className="flex items-center gap-2 text-sm font-normal text-ink/50">
          {hint}
          <ChevronDown className="size-4 shrink-0 transition-transform duration-200 ease-out group-open:rotate-180" />
        </span>
      </summary>
      <div className="mt-3 border-t border-moss/10 pt-3">{children}</div>
    </details>
  );
}
