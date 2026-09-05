import { cn } from "@/lib/utils";
import { Search } from "lucide-react";
import { ReactNode } from "react";
import { Input } from "./input";

export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2">
      <div className="min-w-0">
        <h1 className="text-lg font-bold tracking-tight text-ink">{title}</h1>
        {description && <p className="mt-0 hidden max-w-2xl text-xs leading-snug text-ink/55 md:block">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function SearchField({
  value,
  onChange,
  placeholder,
  count,
  className,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  count?: number;
  className?: string;
}) {
  return (
    <div className={cn("relative", className)}>
      <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink/35" />
      <Input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="pl-9"
        autoComplete="off"
      />
      {count != null && (
        <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs tabular-nums text-ink/40">
          {count}
        </span>
      )}
    </div>
  );
}
