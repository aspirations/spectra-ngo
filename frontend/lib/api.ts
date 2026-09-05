export type Role =
  | "PLATFORM_ADMIN"
  | "NGO_ADMIN"
  | "BRANCH_ADMIN"
  | "INVENTORY_MANAGER"
  | "VET_TECH_EMPLOYEE"
  | "EMPLOYEE";

export type ApiEnvelope<T> = {
  success: boolean;
  data: T;
  message: string | null;
  errorCode: string | null;
};

export type User = {
  id: number;
  tenantId: number;
  branchId: number | null;
  branchIds?: number[];
  email: string;
  fullName: string;
  aadhaarNo?: string;
  homeAddress?: string;
  familyDetails?: string;
  role: Role;
  creditLimit: number;
  baseMonthlySalary: number;
  otherFixedDeductions: number;
  rolledOverStoreDebt: number;
  status: string;
  tenantCode?: string;
  tenantName?: string;
};

export type Branch = {
  id: number;
  name: string;
  code: string;
  address?: string;
  city?: string;
  status: string;
  warehouses: { id: number; type: string; name: string }[];
};

export type Resident = {
  id: number;
  referenceId: string;
  name: string;
  photoUrl?: string;
  collarNo?: string;
  description?: string;
  sex?: string;
  color?: string;
  approxAge?: string;
  intakeSource?: string;
  kind: string;
  category: string;
  intakeDate: string;
  status: string;
};

const TOKEN_KEY = "spectra_token";
const USER_KEY = "spectra_user";
const BRANCH_KEY = "spectra_branch";

export function getToken() {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function getUser(): User | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(USER_KEY);
  return raw ? (JSON.parse(raw) as User) : null;
}

export function getBranchId(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(BRANCH_KEY);
}

export function setSession(token: string, user: User) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  if (user.role === "PLATFORM_ADMIN") {
    localStorage.removeItem(BRANCH_KEY);
  } else if (user.branchId) {
    localStorage.setItem(BRANCH_KEY, String(user.branchId));
  } else if (user.branchIds?.length) {
    localStorage.setItem(BRANCH_KEY, String(user.branchIds[0]));
  }
}

export function setBranchId(id: string) {
  localStorage.setItem(BRANCH_KEY, id);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(BRANCH_KEY);
}

export async function fetchPdfObjectUrl(path: string) {
  const headers = new Headers();
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const branch = getBranchId();
  if (branch) headers.set("X-Branch-Id", branch);
  const res = await fetch(path, { headers });
  if (!res.ok) {
    let message = `Could not open PDF (${res.status})`;
    try {
      const body = (await res.json()) as ApiEnvelope<unknown>;
      if (body.message) message = body.message;
    } catch {
      /* not json */
    }
    throw new Error(message);
  }
  const blob = new Blob([await res.arrayBuffer()], { type: "application/pdf" });
  return URL.createObjectURL(blob);
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const branch = getBranchId();
  if (branch) headers.set("X-Branch-Id", branch);
  const res = await fetch(path, { ...init, headers });
  const body = (await res.json()) as ApiEnvelope<T>;
  if (!res.ok || !body.success) {
    throw new Error(body.message || `Request failed (${res.status})`);
  }
  return body.data;
}

export function can(role: Role | undefined, allowed: Role[]) {
  return !!role && allowed.includes(role);
}

export const ROLE_MENUS: Record<Role, string[]> = {
  PLATFORM_ADMIN: ["Organisations (create / enable / disable)"],
  NGO_ADMIN: ["Dashboard", "All menus", "People (login, salary, passbook)", "Branches"],
  BRANCH_ADMIN: ["Dashboard", "Residents", "Inventory", "Staff POS", "People", "Payroll", "Leave / LOP"],
  INVENTORY_MANAGER: [
    "Dashboard",
    "Dogs / residents",
    "Inventory",
    "Staff POS",
    "People (login, salary, passbook)",
    "Leave / LOP & payroll",
    "Ops & alerts (OpEx, fuel, theft)",
  ],
  VET_TECH_EMPLOYEE: ["Dashboard", "Residents", "Request stock", "Passbook", "Leave / LOP", "Own payslip"],
  EMPLOYEE: ["Dashboard", "Purchase orders", "Request stock (food)", "Passbook", "Leave / LOP", "Own payslip"],
};
