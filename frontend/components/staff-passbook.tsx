"use client";

import { api } from "@/lib/api";
import { Card } from "@/components/ui/input";
import { inr, pretty } from "@/lib/utils";
import { useQuery } from "@tanstack/react-query";

type Passbook = {
  employee: { fullName: string; creditLimit: number; rolledOverStoreDebt: number };
  credit: { outstanding: number; unpaidDuesThisMonth: number };
  orders: {
    order: { id: number; orderNumber: string; total: number; tender: string; status: string; orderAt: string };
    items: { qty: number; unitPrice: number; lineTotal: number; productName?: string; productId?: number }[];
  }[];
};

export function StaffPassbook({ userId }: { userId: string }) {
  const book = useQuery({
    queryKey: ["passbook", userId],
    queryFn: () => api<Passbook>(`/api/pos/passbook/${userId}`),
    enabled: !!userId,
  });

  if (book.isLoading) return <p className="text-sm text-ink/50">Loading passbook…</p>;
  if (book.error) return <p className="text-sm text-clay">{(book.error as Error).message}</p>;
  if (!book.data) return null;

  return (
    <div className="space-y-3">
      <Card>
        <h2 className="text-lg font-bold">{book.data.employee.fullName}</h2>
        <p className="text-sm">
          This month unpaid {inr(book.data.credit.unpaidDuesThisMonth)} · rolled over{" "}
          {inr(book.data.employee.rolledOverStoreDebt)} · outstanding {inr(book.data.credit.outstanding)}
        </p>
      </Card>
      {book.data.orders.length === 0 && <p className="text-sm text-ink/50">No shop receipts yet.</p>}
      {book.data.orders.map((row) => (
        <Card key={row.order.id}>
          <div className="flex justify-between text-sm">
            <b>{row.order.orderNumber}</b>
            <span>{inr(row.order.total)}</span>
          </div>
          <p className="text-xs text-ink/50">
            {row.order.orderAt?.replace("T", " ").slice(0, 16)} · {pretty(row.order.tender)} · {pretty(row.order.status)}
          </p>
          <ul className="mt-2 text-sm">
            {row.items.map((i, idx) => (
              <li key={idx}>
                {i.productName ?? (i.productId ? `SKU ${i.productId}` : "Item")} · {i.qty} × {inr(i.unitPrice)} = {inr(i.lineTotal)}
              </li>
            ))}
          </ul>
        </Card>
      ))}
    </div>
  );
}
