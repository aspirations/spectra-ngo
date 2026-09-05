"use client";

import { cn } from "@/lib/utils";
import { Check, ChevronsUpDown, Search, X } from "lucide-react";
import { KeyboardEvent, ReactNode, useEffect, useMemo, useRef, useState } from "react";

export type ComboOption = {
  value: string;
  label: string;
  hint?: string;
};

const VISIBLE_CAP = 60;

function hay(option: ComboOption) {
  return `${option.label} ${option.hint ?? ""}`.toLowerCase();
}

export function Combobox({
  value,
  onChange,
  options,
  placeholder = "Search to select",
  searchPlaceholder = "Type a name…",
  emptyText = "No matches",
  disabled,
  allowClear = true,
  footer,
  className,
  compact,
}: {
  value: string;
  onChange: (value: string) => void;
  options: ComboOption[];
  placeholder?: string;
  searchPlaceholder?: string;
  emptyText?: string;
  disabled?: boolean;
  allowClear?: boolean;
  footer?: ReactNode;
  className?: string;
  compact?: boolean;
}) {
  const root = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);

  const selected = useMemo(() => options.find((o) => o.value === value), [options, value]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return options;
    return options.filter((o) => hay(o).includes(q));
  }, [options, query]);

  const shown = filtered.slice(0, VISIBLE_CAP);

  useEffect(() => {
    if (!open) return;
    setQuery("");
    setActive(0);
    const t = window.setTimeout(() => searchRef.current?.focus(), 20);
    function onDoc(e: MouseEvent) {
      if (!root.current?.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDoc);
    return () => {
      window.clearTimeout(t);
      document.removeEventListener("mousedown", onDoc);
    };
  }, [open]);

  function pick(next: string) {
    onChange(next);
    setOpen(false);
  }

  function onKey(e: KeyboardEvent) {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActive((i) => Math.min(shown.length - 1, i + 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActive((i) => Math.max(0, i - 1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const hit = shown[active];
      if (hit) pick(hit.value);
    } else if (e.key === "Escape") {
      e.preventDefault();
      setOpen(false);
    }
  }

  return (
    <div ref={root} className={cn("relative", className)}>
      <button
        type="button"
        disabled={disabled}
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className={cn(
          "flex w-full items-center gap-2 rounded-xl border border-moss/20 bg-white px-3 text-left text-sm outline-none ring-moss/30 focus:ring-2 disabled:opacity-50",
          compact ? "min-h-8" : "min-h-11",
        )}
      >
        <Search size={16} className="shrink-0 text-ink/35" />
        <span className={cn("min-w-0 flex-1 truncate", selected ? "text-ink" : "text-ink/40")}>
          {selected ? selected.label : placeholder}
        </span>
        {allowClear && value ? (
          <span
            role="button"
            tabIndex={-1}
            className="rounded-lg p-1 text-ink/40 hover:bg-sand hover:text-ink"
            onClick={(e) => {
              e.stopPropagation();
              onChange("");
            }}
          >
            <X size={14} />
          </span>
        ) : (
          <ChevronsUpDown size={16} className="shrink-0 text-ink/35" />
        )}
      </button>
      {open && (
        <div className="absolute z-[70] mt-1 w-full overflow-hidden rounded-2xl border border-moss/15 bg-white shadow-lg">
          <div className="border-b border-moss/10 p-2">
            <input
              ref={searchRef}
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setActive(0);
              }}
              onKeyDown={onKey}
              placeholder={searchPlaceholder}
              className="min-h-11 w-full rounded-xl bg-sand px-3 text-sm outline-none ring-moss/30 focus:ring-2"
            />
          </div>
          <ul className="max-h-64 overflow-y-auto py-1" role="listbox">
            {shown.map((option, i) => {
              const isOn = option.value === value;
              return (
                <li key={option.value}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={isOn}
                    onMouseEnter={() => setActive(i)}
                    onClick={() => pick(option.value)}
                    className={cn(
                      "flex min-h-11 w-full items-center gap-2 px-3 text-left text-sm",
                      i === active ? "bg-moss/10" : "hover:bg-sand",
                    )}
                  >
                    <Check size={16} className={cn("shrink-0", isOn ? "text-moss" : "text-transparent")} />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-medium">{option.label}</span>
                      {option.hint && <span className="block truncate text-xs text-ink/50">{option.hint}</span>}
                    </span>
                  </button>
                </li>
              );
            })}
            {shown.length === 0 && <li className="px-3 py-6 text-center text-sm text-ink/50">{emptyText}</li>}
            {filtered.length > shown.length && (
              <li className="px-3 py-2 text-center text-xs text-ink/45">
                Showing {shown.length} of {filtered.length} — keep typing to narrow
              </li>
            )}
          </ul>
          {footer && <div className="border-t border-moss/10 p-2">{footer}</div>}
        </div>
      )}
    </div>
  );
}

export function toStaffOptions(rows: { id: number; fullName: string; email?: string; role?: string }[] | undefined): ComboOption[] {
  return (rows ?? []).map((u) => ({
    value: String(u.id),
    label: u.fullName,
    hint: [u.role?.replaceAll("_", " "), u.email].filter(Boolean).join(" · "),
  }));
}

export function toResidentOptions(
  rows: { id: number; name: string; referenceId?: string; category?: string }[] | undefined,
): ComboOption[] {
  return (rows ?? []).map((r) => ({
    value: String(r.id),
    label: r.name,
    hint: [r.referenceId, r.category?.replaceAll("_", " ")].filter(Boolean).join(" · "),
  }));
}

export function toProductOptions(
  rows: { id: number; name: string; sku?: string; category?: string; qtyOnHand?: number }[] | undefined,
): ComboOption[] {
  return (rows ?? []).map((p) => ({
    value: String(p.id),
    label: p.name,
    hint: [p.sku, p.category, p.qtyOnHand != null ? `qty ${p.qtyOnHand}` : null].filter(Boolean).join(" · "),
  }));
}
