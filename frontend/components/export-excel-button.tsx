"use client";

import { Button } from "@/components/ui/button";
import { MouseEvent } from "react";

export function ExportExcelButton({
  disabled,
  onClick,
}: {
  disabled?: boolean;
  onClick: () => void;
}) {
  function handleClick(e: MouseEvent<HTMLButtonElement>) {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled) onClick();
  }
  return (
    <Button type="button" variant="outline" className="min-h-9 px-3 text-xs" disabled={disabled} onClick={handleClick}>
      Export Excel
    </Button>
  );
}
