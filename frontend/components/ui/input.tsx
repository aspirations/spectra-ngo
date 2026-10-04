import { cn } from "@/lib/utils";
import { InputHTMLAttributes, LabelHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        "min-h-11 w-full rounded-xl border border-moss/20 bg-white px-3 py-2 text-sm text-ink outline-none ring-moss/30 placeholder:text-ink/35 focus:ring-2",
        className,
      )}
      {...props}
    />
  );
}

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        "min-h-11 w-full rounded-xl border border-moss/20 bg-white px-3 py-2 text-sm text-ink outline-none ring-moss/30 placeholder:text-ink/35 focus:ring-2",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
}

export function Textarea({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={cn(
        "min-h-[5.5rem] w-full rounded-xl border border-moss/20 bg-white px-3 py-2 text-sm text-ink outline-none ring-moss/30 placeholder:text-ink/35 focus:ring-2",
        className,
      )}
      {...props}
    />
  );
}

export function Label({ children, className, ...props }: LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label className={cn("mb-1.5 block text-[11px] font-semibold uppercase tracking-[0.08em] text-ink/50", className)} {...props}>
      {children}
    </label>
  );
}

export function Card({ className, children, id }: { className?: string; children: React.ReactNode; id?: string }) {
  return (
    <div
      id={id}
      className={cn("rounded-2xl border border-moss/10 bg-white p-3 shadow-[0_1px_2px_rgba(15,28,23,0.04)]", className)}>
      {children}
    </div>
  );
}
