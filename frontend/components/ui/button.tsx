import { cn } from "@/lib/utils";
import { ButtonHTMLAttributes } from "react";

export function Button({
  className,
  variant = "primary",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "ghost" | "danger" | "outline" }) {
  const styles = {
    primary: "bg-moss text-white hover:bg-leaf",
    ghost: "bg-transparent text-ink hover:bg-white/70",
    danger: "bg-clay text-white hover:bg-orange-700",
    outline: "border border-moss/30 bg-white text-moss hover:bg-sand",
  }[variant];
  return (
    <button
      className={cn(
        "inline-flex min-h-11 items-center justify-center rounded-xl px-4 py-2 text-sm font-semibold transition disabled:opacity-50",
        styles,
        className,
      )}
      {...props}
    />
  );
}
