"use client";

import { api, Branch, can, clearSession, getBranchId, getUser, Role, setBranchId, User } from "@/lib/api";
import { pinHref, unpinHref, visiblePins } from "@/lib/dock-pins";
import { Button } from "@/components/ui/button";
import { Combobox } from "@/components/ui/combobox";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Bone,
  Building2,
  CalendarDays,
  ClipboardList,
  FileText,
  LayoutDashboard,
  Menu,
  HeartHandshake,
  MoreHorizontal,
  Pin,
  PanelLeftClose,
  PanelLeftOpen,
  Receipt,
  ShoppingCart,
  TrendingUp,
  Users,
  Wallet,
  Warehouse,
  X,
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { PointerEvent, useEffect, useMemo, useRef, useState } from "react";

const NAV: { href: string; label: string; dock?: string; icon: typeof HeartHandshake; roles: Role[]; group: string }[] = [
  { href: "/ngos", label: "Organisations", icon: Building2, roles: ["PLATFORM_ADMIN"], group: "Platform" },
  { href: "/dashboard", label: "Today", icon: LayoutDashboard, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER", "VET_TECH_EMPLOYEE", "EMPLOYEE"], group: "Care" },
  { href: "/residents", label: "Dogs", dock: "Dogs", icon: HeartHandshake, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "VET_TECH_EMPLOYEE", "INVENTORY_MANAGER"], group: "Care" },
  { href: "/inventory/grn", label: "Receive stock", dock: "Receive", icon: Warehouse, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"], group: "Stock" },
  { href: "/inventory/po", label: "Purchase orders", icon: FileText, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER", "EMPLOYEE"], group: "Stock" },
  { href: "/inventory/consume", label: "Issue stock", dock: "Issue stock", icon: Bone, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER", "VET_TECH_EMPLOYEE", "EMPLOYEE"], group: "Stock" },
  { href: "/inventory/audit", label: "Stock audit", icon: ClipboardList, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"], group: "Stock" },
  { href: "/pos", label: "Staff shop", dock: "Shop", icon: ShoppingCart, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"], group: "Stock" },
  { href: "/pos/stock", label: "Shop stock", icon: TrendingUp, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"], group: "Stock" },
  { href: "/users", label: "People", icon: Users, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"], group: "People" },
  { href: "/passbook", label: "Passbook", icon: Wallet, roles: ["EMPLOYEE", "VET_TECH_EMPLOYEE"], group: "People" },
  { href: "/leave", label: "Leave / LOP", dock: "Leave", icon: CalendarDays, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "EMPLOYEE", "VET_TECH_EMPLOYEE", "INVENTORY_MANAGER"], group: "People" },
  { href: "/payroll", label: "Payroll", icon: Receipt, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "EMPLOYEE", "VET_TECH_EMPLOYEE", "INVENTORY_MANAGER"], group: "People" },
  { href: "/ops", label: "Ops & alerts", dock: "OpEx", icon: TrendingUp, roles: ["NGO_ADMIN", "BRANCH_ADMIN", "INVENTORY_MANAGER"], group: "Ops" },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const queryClient = useQueryClient();
  const [ready, setReady] = useState(false);
  const [open, setOpen] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [branchId, setLocalBranch] = useState<string | null>(null);
  const [pinnedHrefs, setPinnedHrefs] = useState<string[]>([]);
  const [collapsed, setCollapsed] = useState(false);
  const skipDockClick = useRef(false);
  const longPress = useRef<{ timer: number; href: string; x: number; y: number } | null>(null);
  const platform = user?.role === "PLATFORM_ADMIN";

  useEffect(() => {
    const current = getUser();
    setReady(true);
    if (!current) {
      router.replace("/login");
      return;
    }
    setUser(current);
    setLocalBranch(getBranchId());
    if (current.role === "PLATFORM_ADMIN" && pathname !== "/ngos") {
      router.replace("/ngos");
    } else if (current.role !== "PLATFORM_ADMIN" && pathname === "/ngos") {
      router.replace("/dashboard");
    }
  }, [router, pathname]);

  const branches = useQuery({
    queryKey: ["branches"],
    queryFn: () => api<Branch[]>("/api/branches"),
    enabled: !!user && !platform,
  });

  useEffect(() => {
    if (platform || !branches.data?.length) return;
    const stored = getBranchId();
    const valid = stored && branches.data.some((b) => String(b.id) === stored);
    if (!valid) {
      const id = String(branches.data[0].id);
      setBranchId(id);
      setLocalBranch(id);
      queryClient.invalidateQueries();
    }
  }, [branches.data, platform, queryClient]);

  const items = useMemo(() => NAV.filter((n) => can(user?.role, n.roles)), [user]);
  const allowedHrefs = useMemo(() => items.map((n) => n.href), [items]);
  const groups = useMemo(() => {
    const order: string[] = [];
    const map = new Map<string, typeof items>();
    for (const item of items) {
      if (!map.has(item.group)) {
        map.set(item.group, []);
        order.push(item.group);
      }
      map.get(item.group)!.push(item);
    }
    return order.map((label) => ({ label, items: map.get(label)! }));
  }, [items]);
  const activeHref = useMemo(() => {
    const matches = items.filter((n) => pathname === n.href || pathname.startsWith(`${n.href}/`));
    return matches.sort((a, b) => b.href.length - a.href.length)[0]?.href;
  }, [items, pathname]);

  useEffect(() => {
    if (!user) return;
    setPinnedHrefs(visiblePins(user.id, allowedHrefs));
    setCollapsed(localStorage.getItem("spectra_sidebar_collapsed") === "1");
  }, [user, allowedHrefs]);

  const dock = useMemo(() => {
    const byHref = new Map(items.map((i) => [i.href, i]));
    return pinnedHrefs.flatMap((href) => {
      const hit = byHref.get(href);
      return hit ? [hit] : [];
    });
  }, [items, pinnedHrefs]);

  if (!ready || !user) {
    return <div className="min-h-screen bg-sand" />;
  }

  const me = user;
  const showBranch = !platform && (branches.data?.length ?? 0) > 0;
  const brand = platform ? "Spectra Platform" : "Spectra";
  const pinnedSet = new Set(pinnedHrefs);

  function dockLabel(item: (typeof NAV)[number]) {
    if (item.href === "/inventory/consume" && me.role === "EMPLOYEE") return "Request";
    return item.dock ?? item.label.split(" ")[0];
  }

  function toggleCollapse() {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem("spectra_sidebar_collapsed", next ? "1" : "0");
      return next;
    });
  }

  function togglePin(href: string, already: boolean) {
    const next = already ? unpinHref(me.id, href, allowedHrefs) : pinHref(me.id, href, allowedHrefs);
    setPinnedHrefs(next);
    if (!already) {
      setOpen(false);
      router.push(href);
    }
  }

  function clearLongPress() {
    if (!longPress.current) return;
    window.clearTimeout(longPress.current.timer);
    longPress.current = null;
  }

  function onDockPointerDown(e: PointerEvent<HTMLAnchorElement>, href: string) {
    clearLongPress();
    longPress.current = {
      href,
      x: e.clientX,
      y: e.clientY,
      timer: window.setTimeout(() => {
        longPress.current = null;
        skipDockClick.current = true;
        setPinnedHrefs(unpinHref(me.id, href, allowedHrefs));
        if (typeof navigator !== "undefined" && navigator.vibrate) navigator.vibrate(12);
      }, 500),
    };
  }

  function onDockPointerMove(e: PointerEvent<HTMLAnchorElement>) {
    const hold = longPress.current;
    if (!hold) return;
    if (Math.hypot(e.clientX - hold.x, e.clientY - hold.y) > 10) clearLongPress();
  }

  function NavLinks({ compact, rail }: { compact?: boolean; rail?: boolean }) {
    return (
      <>
        {groups.map((group) => (
          <div key={group.label} className="mb-4">
            <p className={`mb-1 px-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-sand/45 ${rail ? "md:hidden" : ""}`}>
              {group.label}
            </p>
            <div className="space-y-0.5">
              {group.items.map((item) => {
                const Icon = item.icon;
                const active = item.href === activeHref;
                const pinned = pinnedSet.has(item.href);
                const label = item.href === "/inventory/consume" && me.role === "EMPLOYEE" ? "Request stock" : item.label;
                return (
                  <div key={item.href} className="flex items-center gap-0.5">
                    <Link
                      href={item.href}
                      title={label}
                      onClick={() => setOpen(false)}
                      className={`flex min-h-11 min-w-0 flex-1 items-center gap-3 rounded-xl text-sm px-3 ${
                        rail ? "md:justify-center md:px-0" : ""
                      } ${active ? "bg-leaf text-white shadow-sm" : "text-sand/85 hover:bg-white/10"} ${compact ? "text-[13px]" : ""}`}
                    >
                      <Icon size={18} />
                      <span className={rail ? "md:hidden" : ""}>{label}</span>
                    </Link>
                    <button
                      type="button"
                      className={`rounded-xl p-2 ${rail ? "md:hidden" : ""} ${pinned ? "text-leaf" : "text-sand/45"}`}
                      aria-label={pinned ? `Unpin ${item.label}` : `Pin ${item.label}`}
                      aria-pressed={pinned}
                      onClick={() => togglePin(item.href, pinned)}
                    >
                      <Pin size={16} className={pinned ? "fill-current" : ""} />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </>
    );
  }

  return (
    <div className={`min-h-screen md:grid ${collapsed ? "md:grid-cols-[72px_1fr]" : "md:grid-cols-[248px_1fr]"}`}>
      {open && <button type="button" className="fixed inset-0 z-30 bg-ink/40 md:hidden" aria-label="Close menu" onClick={() => setOpen(false)} />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-72 flex-col bg-ink text-sand transition md:static md:w-auto md:translate-x-0 ${
          open ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        }`}
      >
        <div className={`flex items-center py-5 ${collapsed ? "justify-between px-4 md:justify-center md:px-3" : "justify-between px-4"}`}>
          <div className={`min-w-0 ${collapsed ? "md:hidden" : ""}`}>
            <p className="text-lg font-bold tracking-tight">{brand}</p>
            <p className="truncate text-xs text-sand/60">{me.tenantName ?? me.tenantCode}</p>
          </div>
          <button type="button" className="rounded-xl p-2 md:hidden" onClick={() => setOpen(false)} aria-label="Close">
            <X size={20} />
          </button>
          <button
            type="button"
            className="hidden rounded-xl p-2 text-sand/70 hover:bg-white/10 hover:text-sand md:inline-flex"
            aria-label={collapsed ? "Expand menu" : "Collapse menu"}
            onClick={toggleCollapse}
          >
            {collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
          </button>
        </div>
        <nav className="flex-1 overflow-y-auto px-2 pb-4">
          <NavLinks rail={collapsed} />
        </nav>
        <div className="border-t border-white/10 p-3 md:hidden">
          <Button
            variant="ghost"
            className="w-full text-sand hover:bg-white/10"
            onClick={() => {
              clearSession();
              router.replace("/login");
            }}
          >
            Sign out
          </Button>
        </div>
      </aside>
      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex items-center gap-2 border-b border-moss/10 bg-sand/90 px-3 py-1 backdrop-blur md:px-4">
          <button type="button" className="rounded-xl p-2 md:hidden" onClick={() => setOpen(true)} aria-label="Open menu">
            <Menu size={20} />
          </button>
          <div className="min-w-0 shrink-0 md:max-w-[10rem]">
            <p className="truncate text-sm font-semibold">{me.fullName}</p>
            <p className="truncate text-xs text-ink/50">{me.role.replaceAll("_", " ")}</p>
          </div>
          <nav className="hidden min-w-0 flex-1 items-center justify-center gap-1 md:flex" aria-label="Pinned">
            {dock.map((item) => {
              const Icon = item.icon;
              const active = item.href === activeHref;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  title={item.label}
                  onClick={(e) => {
                    if (!skipDockClick.current) return;
                    e.preventDefault();
                    skipDockClick.current = false;
                  }}
                  onPointerDown={(e) => onDockPointerDown(e, item.href)}
                  onPointerMove={onDockPointerMove}
                  onPointerUp={clearLongPress}
                  onPointerCancel={clearLongPress}
                  onContextMenu={(e) => e.preventDefault()}
                  className={`inline-flex min-h-9 items-center gap-1.5 rounded-xl px-2.5 text-xs font-semibold ${
                    active ? "bg-moss/10 text-moss" : "text-ink/55 hover:bg-white/70 hover:text-ink"
                  }`}
                >
                  <Icon size={16} />
                  {dockLabel(item)}
                </Link>
              );
            })}
          </nav>
          {showBranch && (
            <div className="w-40 sm:w-52">
              <Combobox
                compact
                value={branchId ?? ""}
                onChange={(id) => {
                  setBranchId(id);
                  setLocalBranch(id);
                  queryClient.invalidateQueries();
                }}
                options={(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name, hint: b.code }))}
                placeholder="Centre"
                searchPlaceholder="Find a centre…"
                allowClear={false}
              />
            </div>
          )}
          <Button
            variant="outline"
            className="hidden min-h-8 px-3 py-1 sm:inline-flex"
            onClick={() => {
              clearSession();
              router.replace("/login");
            }}
          >
            Sign out
          </Button>
        </header>
        <main className="px-3 pt-2 pb-24 md:px-4 md:pt-2.5 md:pb-6">{children}</main>
      </div>
      <nav className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-4 border-t border-moss/10 bg-sand/95 px-1 pb-[max(0.35rem,env(safe-area-inset-bottom))] pt-1 backdrop-blur md:hidden">
        {dock.map((item) => {
          const Icon = item.icon;
          const active = item.href === activeHref;
          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={(e) => {
                if (!skipDockClick.current) return;
                e.preventDefault();
                skipDockClick.current = false;
              }}
              onPointerDown={(e) => onDockPointerDown(e, item.href)}
              onPointerMove={onDockPointerMove}
              onPointerUp={clearLongPress}
              onPointerCancel={clearLongPress}
              onContextMenu={(e) => e.preventDefault()}
              className={`flex min-h-12 flex-col items-center justify-center gap-0.5 rounded-xl text-[10px] font-semibold ${
                active ? "text-moss" : "text-ink/45"
              }`}
            >
              <Icon size={20} />
              {dockLabel(item)}
            </Link>
          );
        })}
        <button
          type="button"
          onClick={() => setOpen(true)}
          className="flex min-h-12 flex-col items-center justify-center gap-0.5 rounded-xl text-[10px] font-semibold text-ink/45"
        >
          <MoreHorizontal size={20} />
          More
        </button>
      </nav>
    </div>
  );
}
