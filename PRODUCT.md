# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary users are kennel staff on phones at a dog-shelter branch: inventory managers, vets/techs, caretakers, and branch/NGO admins. They switch roles by login. They are often outdoors or in a stockroom, not at a desk. PLATFORM_ADMIN exists only to provision NGOs and is out of kennel-day scope.

## Product Purpose

Spectra is a multi-tenant NGO ERP for dog shelters. It records dogs, treatments, unified warehouse stock (food, meds, retail), staff shop credit, leave/LOP, and payroll for one organisation at a time, scoped to the working branch.

Success is a complete kennel day without leaking data across tenants/branches, and without collapsing vet request and stores issue into one login.

## Positioning

One physical stockroom per branch. Food, vaccines/meds, and staff retail share the same GRN, batches, and FEFO engine. Clinical and feed outwards are request → named-person issue → FEFO; managers may still issue food directly. Net pay = base − LOP − store dues. Working days are present by default; only absences are recorded.

## Operating Context

Typical jobs: census and dog chart notes; request treatment/feed; fulfill Issue stock; receive GRN; staff POS on payroll credit; leave; month-end payroll. Demo NGO is Dog Care (Pune Kennel) with logins `vet@dogcare.org`, `inventory@dogcare.org`, `care@dogcare.org`, password `Spectra@123`.

## Capabilities and Constraints

- Frontend: Next.js App Router, TypeScript, Tailwind, TanStack Query, Lucide. Backend unchanged for this redesign.
- Mobile-first touch UI; `X-Branch-Id` + JWT. Schema only via Flyway.
- Binding for this redesign (user, 2026-09-03): replace the visual world (current moss/sand/Nunito is evidence, not the new identity). Design system first, then every signed-in screen plus login. No decorative motion. No dense data-wall. No Inter / purple / glass LLM defaults.
- Do not change API contracts, roles, or domain rules.

## Brand Commitments

Name: Spectra NGO. Voice: kennel-day English, not office-ERP jargon. Product copy already uses Request from stores, Issue stock, Waiting, Issued.

## Evidence on Hand

Live app at `frontend/` against `/api`. Representative surfaces: login demo chips, app shell + mobile dock, dashboard KPIs/charts, residents list (~thousands), dog chart, Issue stock queue, GRN, POS, leave, payroll. Do not invent testimonials, other NGOs’ metrics, or capabilities the product does not have.

## Product Principles

- One branch, one stockroom, one FEFO — the UI must make request vs issued vs on-hand obvious.
- Phone in the yard beats desktop density.
- Role is the product: the same route should read as Request or Issue depending on who signed in.
- Status is operational (Waiting, Issued, Rejected, due), not decorative color noise.

## Accessibility & Inclusion

WCAG 2.1 AA contrast on text and primary controls. 44px minimum tap targets. Do not set `maximumScale: 1` as a way to “optimize”; pinch-zoom must remain available. Keyboard and screen-reader labels on comboboxes, docks, and dialogs.
