"use client";

import { Button } from "@/components/ui/button";

export const PAGE_SIZE = 10;

export function pageSlice<T>(items: T[] | undefined, page: number, size = PAGE_SIZE) {
  const all = items ?? [];
  const pages = Math.max(1, Math.ceil(all.length / size) || 1);
  const safe = Math.min(Math.max(0, page), pages - 1);
  return {
    slice: all.slice(safe * size, safe * size + size),
    page: safe,
    pages,
    total: all.length,
    size,
  };
}

export function PageControls({
  page,
  pages,
  total,
  size,
  onPage,
}: {
  page: number;
  pages: number;
  total: number;
  size: number;
  onPage: (page: number) => void;
}) {
  if (total <= size) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, total);
  return (
    <div className="mt-2 flex items-center justify-between gap-2 text-sm">
      <Button type="button" variant="ghost" disabled={page <= 0} onClick={() => onPage(page - 1)}>
        Previous
      </Button>
      <span className="tabular-nums text-ink/50">
        {from}–{to} of {total}
      </span>
      <Button type="button" variant="ghost" disabled={page >= pages - 1} onClick={() => onPage(page + 1)}>
        Next
      </Button>
    </div>
  );
}
