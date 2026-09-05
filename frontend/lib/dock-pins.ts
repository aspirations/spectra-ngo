export const DOCK_SLOTS = 3;
export const DOCK_DEFAULTS = ["/inventory/grn", "/inventory/consume", "/ops"];

function storageKey(userId: number) {
  return `spectra_dock_pins_${userId}`;
}

export function loadPins(userId: number): string[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(storageKey(userId));
    if (!raw) return [];
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((href): href is string => typeof href === "string" && href.length > 0);
  } catch {
    return [];
  }
}

export function savePins(userId: number, hrefs: string[]) {
  if (typeof window === "undefined") return;
  localStorage.setItem(storageKey(userId), JSON.stringify(hrefs));
}

export function padDock(hrefs: string[], allowed: string[]): string[] {
  const out: string[] = [];
  const seen = new Set<string>();
  const allowedSet = new Set(allowed);
  for (const href of hrefs) {
    if (!allowedSet.has(href) || seen.has(href)) continue;
    seen.add(href);
    out.push(href);
    if (out.length === DOCK_SLOTS) return out;
  }
  for (const href of [...DOCK_DEFAULTS, ...allowed]) {
    if (!allowedSet.has(href) || seen.has(href)) continue;
    seen.add(href);
    out.push(href);
    if (out.length === DOCK_SLOTS) break;
  }
  return out;
}

export function visiblePins(userId: number, allowed: string[]): string[] {
  const stored = loadPins(userId);
  return padDock(stored.length === 0 ? DOCK_DEFAULTS : stored, allowed);
}

export function pinHref(userId: number, href: string, allowed: string[]): string[] {
  if (!allowed.includes(href)) return visiblePins(userId, allowed);
  const current = visiblePins(userId, allowed);
  if (current.includes(href)) {
    savePins(userId, current);
    return current;
  }
  const next = current.length >= DOCK_SLOTS ? current.slice(1) : [...current];
  next.push(href);
  const resolved = padDock(next, allowed);
  savePins(userId, resolved);
  return resolved;
}

export function unpinHref(userId: number, href: string, allowed: string[]): string[] {
  const current = visiblePins(userId, allowed).filter((item) => item !== href);
  const resolved = padDock(current, allowed);
  savePins(userId, resolved);
  return resolved;
}
