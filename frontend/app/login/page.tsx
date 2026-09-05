"use client";

import { api, setSession, User } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import {
  Eye,
  EyeOff,
  HeartHandshake,
  LockKeyhole,
  Mail,
  PawPrint,
  Receipt,
  ShieldCheck,
  Stethoscope,
  Warehouse,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { FormEvent, useRef, useState } from "react";

const DEMO = [
  {
    id: "dogcare",
    label: "DOG Care admin",
    email: "admin@dogcare.org",
    hint: "Dogs-only NGO — full access",
    icon: PawPrint,
  },
  {
    id: "doginv",
    label: "DOG Care inventory",
    email: "inventory@dogcare.org",
    hint: "Runs kennel ops end-to-end",
    icon: Warehouse,
  },
  {
    id: "dogvet",
    label: "DOG Care vet",
    email: "vet@dogcare.org",
    hint: "Dr. Vani — request treatments",
    icon: Stethoscope,
  },
  {
    id: "dogstaff",
    label: "DOG Care caretaker",
    email: "care@dogcare.org",
    hint: "Karan — request kennel feed",
    icon: PawPrint,
  },
] as const;

const DEMO_PASSWORD = "Spectra@123";

const FEATURES = [
  { icon: PawPrint, title: "Residents", copy: "Census, treatments, feed by centre" },
  { icon: Warehouse, title: "Stores", copy: "GRN, consumption, staff POS" },
  { icon: Receipt, title: "People", copy: "Leave, LOP, payroll" },
] as const;

export default function LoginPage() {
  const router = useRouter();
  const errorRef = useRef<HTMLDivElement>(null);
  const [email, setEmail] = useState<string>(DEMO[0].email);
  const [password, setPassword] = useState(DEMO_PASSWORD);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const selectedDemo = DEMO.find((d) => d.email === email)?.id ?? null;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const data = await api<{ token: string; user: User }>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setSession(data.token, data.user);
      router.replace(data.user.role === "PLATFORM_ADMIN" ? "/ngos" : "/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
      requestAnimationFrame(() => errorRef.current?.focus());
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="relative min-h-dvh overflow-hidden bg-sand">
      <div
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_80%_60%_at_50%_-10%,rgba(47,158,107,0.14),transparent)]"
        aria-hidden="true"
      />
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.35] [background-image:linear-gradient(rgba(31,107,74,0.06)_1px,transparent_1px),linear-gradient(90deg,rgba(31,107,74,0.06)_1px,transparent_1px)] [background-size:28px_28px]"
        aria-hidden="true"
      />

      <div className="relative mx-auto flex min-h-dvh max-w-6xl flex-col lg:grid lg:grid-cols-[minmax(0,1.08fr)_minmax(0,0.92fr)] lg:gap-0">
        <aside className="relative flex flex-col justify-between px-5 pb-6 pt-8 text-sand sm:px-8 lg:min-h-dvh lg:rounded-r-[2rem] lg:bg-ink lg:px-10 lg:py-12 lg:shadow-[inset_-1px_0_0_rgba(255,255,255,0.06)]">
          <div
            className="pointer-events-none absolute -left-16 -top-20 h-56 w-56 rounded-full bg-moss/40 blur-3xl motion-reduce:blur-none lg:-left-24 lg:-top-24 lg:h-80 lg:w-80"
            aria-hidden="true"
          />
          <div
            className="pointer-events-none absolute bottom-0 right-0 h-48 w-48 rounded-full bg-leaf/25 blur-3xl motion-reduce:blur-none lg:h-72 lg:w-72"
            aria-hidden="true"
          />

          <div className="relative">
            <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-leaf backdrop-blur-sm lg:border-white/15">
              <ShieldCheck size={14} aria-hidden="true" />
              Secure sign-in
            </div>
            <p className="mt-5 inline-flex items-center gap-2 text-base font-bold tracking-tight text-sand lg:text-lg">
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-leaf/20 text-leaf">
                <HeartHandshake size={20} aria-hidden="true" />
              </span>
              Spectra NGO
            </p>
            <h1 className="mt-4 max-w-lg text-[1.75rem] font-bold leading-[1.15] tracking-tight sm:text-3xl lg:mt-6 lg:text-[2.125rem]">
              Care for every dog — without hunting through lists
            </h1>
            <p className="mt-3 max-w-md text-sm leading-relaxed text-sand/75 lg:text-[0.9375rem]">
              Sign in with your work email. You only see your organisation — residents, stores, and payroll follow the
              centre you pick in the header.
            </p>
          </div>

          <ul className="relative mt-6 hidden gap-3 sm:grid lg:mt-10">
            {FEATURES.map(({ icon: Icon, title, copy }) => (
              <li
                key={title}
                className="flex gap-3 rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-3.5 backdrop-blur-sm transition hover:border-white/20 hover:bg-white/[0.07]"
              >
                <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-leaf/15 text-leaf">
                  <Icon size={18} aria-hidden="true" />
                </span>
                <span>
                  <b className="block text-sm font-semibold text-sand">{title}</b>
                  <span className="text-sm text-sand/65">{copy}</span>
                </span>
              </li>
            ))}
          </ul>

          <ul className="relative mt-5 flex gap-2 overflow-x-auto pb-1 sm:hidden" aria-label="Platform features">
            {FEATURES.map(({ icon: Icon, title }) => (
              <li
                key={title}
                className="flex shrink-0 items-center gap-2 rounded-full border border-ink/10 bg-ink/5 px-3 py-2 text-xs font-medium text-ink/80"
              >
                <Icon size={14} className="text-moss" aria-hidden="true" />
                {title}
              </li>
            ))}
          </ul>

          <p className="relative mt-6 hidden text-xs text-sand/50 lg:block">
            Multi-tenant · Branch-scoped data · Role-based access
          </p>
        </aside>

        <main className="flex flex-1 items-center justify-center px-4 py-8 sm:px-8 lg:px-10 lg:py-12">
          <div className="w-full max-w-[26rem]">
            <div className="rounded-[1.75rem] border border-moss/10 bg-white/90 p-6 shadow-[0_20px_50px_rgba(15,28,23,0.08)] backdrop-blur-sm sm:p-8">
              <header className="text-center sm:text-left">
                <h2 className="text-2xl font-bold tracking-tight text-ink">Welcome back</h2>
                <p className="mt-1.5 text-sm leading-relaxed text-ink/60">
                  Use a demo account below or your own credentials.
                </p>
              </header>

              <div className="mt-6 space-y-2" role="group" aria-label="Demo accounts">
                <p className="text-xs font-semibold uppercase tracking-wide text-ink/45">Quick demo</p>
                <div className="grid gap-2 sm:grid-cols-2">
                  {DEMO.map((account) => {
                    const Icon = account.icon;
                    const active = selectedDemo === account.id;
                    return (
                      <button
                        key={account.id}
                        type="button"
                        aria-pressed={active}
                        onClick={() => {
                          setEmail(account.email);
                          setPassword(DEMO_PASSWORD);
                          setError("");
                        }}
                        className={`group flex min-h-[4.25rem] cursor-pointer flex-col justify-center rounded-2xl border px-3.5 py-3 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-moss/40 ${
                          active
                            ? "border-moss bg-moss/[0.08] shadow-[inset_0_0_0_1px_rgba(31,107,74,0.15)]"
                            : "border-moss/15 bg-sand/50 hover:border-moss/35 hover:bg-sand/80"
                        }`}
                      >
                        <span className="flex items-center gap-2">
                          <span
                            className={`flex h-8 w-8 items-center justify-center rounded-lg ${
                              active ? "bg-moss text-white" : "bg-white text-moss group-hover:bg-moss/10"
                            }`}
                          >
                            <Icon size={16} aria-hidden="true" />
                          </span>
                          <span className="text-sm font-semibold text-ink">{account.label}</span>
                        </span>
                        <span className="mt-1 pl-10 text-xs leading-snug text-ink/55">{account.hint}</span>
                      </button>
                    );
                  })}
                </div>
                <p className="text-center text-[11px] text-ink/45 sm:text-left">
                  Demo password: <span className="font-mono font-medium text-ink/70">{DEMO_PASSWORD}</span>
                </p>
              </div>

              <div className="relative my-6 flex items-center gap-3" aria-hidden="true">
                <span className="h-px flex-1 bg-moss/15" />
                <span className="text-[11px] font-semibold uppercase tracking-wider text-ink/40">Sign in</span>
                <span className="h-px flex-1 bg-moss/15" />
              </div>

              <form className="space-y-4" onSubmit={onSubmit} aria-busy={busy} noValidate>
                {error && (
                  <div
                    ref={errorRef}
                    tabIndex={-1}
                    role="alert"
                    aria-labelledby="login-error-title"
                    className="rounded-2xl border border-clay/25 bg-orange-50/90 px-3.5 py-3 text-sm text-clay outline-none focus-visible:ring-2 focus-visible:ring-clay/30"
                  >
                    <p id="login-error-title" className="font-semibold">
                      Could not sign in
                    </p>
                    <p className="mt-1 leading-relaxed">{error}</p>
                    <a className="mt-2 inline-block font-semibold underline underline-offset-2" href="#email">
                      Check email
                    </a>
                  </div>
                )}

                <div>
                  <Label htmlFor="email">Email</Label>
                  <div className="relative">
                    <Mail
                      className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink/35"
                      size={18}
                      aria-hidden="true"
                    />
                    <Input
                      id="email"
                      name="email"
                      type="email"
                      autoComplete="username"
                      inputMode="email"
                      value={email}
                      aria-invalid={error ? true : undefined}
                      onChange={(e) => setEmail(e.target.value)}
                      className="pl-10"
                      required
                    />
                  </div>
                </div>

                <div>
                  <Label htmlFor="password">Password</Label>
                  <div className="relative">
                    <LockKeyhole
                      className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink/35"
                      size={18}
                      aria-hidden="true"
                    />
                    <Input
                      id="password"
                      name="password"
                      type={showPassword ? "text" : "password"}
                      autoComplete="current-password"
                      value={password}
                      aria-invalid={error ? true : undefined}
                      onChange={(e) => setPassword(e.target.value)}
                      className="pl-10 pr-12"
                      required
                    />
                    <button
                      type="button"
                      className="absolute inset-y-0 right-0 flex min-h-11 min-w-11 cursor-pointer items-center justify-center rounded-r-xl text-ink/45 transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-moss/30"
                      aria-pressed={showPassword}
                      aria-label={showPassword ? "Hide password" : "Show password"}
                      onClick={() => setShowPassword((v) => !v)}
                    >
                      {showPassword ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
                    </button>
                  </div>
                </div>

                <Button className="mt-1 w-full py-3 text-[0.9375rem]" disabled={busy}>
                  {busy ? "Signing in…" : "Sign in to Spectra"}
                </Button>
              </form>
            </div>

            <p className="mt-5 text-center text-xs leading-relaxed text-ink/45">
              By signing in you agree to use Spectra only for your assigned organisation and centres.
            </p>
          </div>
        </main>
      </div>
    </div>
  );
}
