"use client";

import { getUser } from "@/lib/api";
import { StaffPassbook } from "@/components/staff-passbook";
import { PageHeader } from "@/components/ui/page-header";

export default function PassbookPage() {
  const me = getUser();
  const id = me?.id ? String(me.id) : "";

  return (
    <div className="desk">
      <PageHeader title="Store passbook" description="Your shop receipts and this month’s unpaid store balance." />
      {id ? <StaffPassbook userId={id} /> : <p className="text-sm text-ink/50">Sign in to see your passbook.</p>}
    </div>
  );
}
