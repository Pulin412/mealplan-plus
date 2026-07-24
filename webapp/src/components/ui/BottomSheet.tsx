"use client";

import { useEffect, useRef } from "react";
import { cn } from "@/lib/utils/cn";

interface BottomSheetProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  className?: string;
}

export function BottomSheet({ open, onClose, title, children, className }: BottomSheetProps) {
  const sheetRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const handleKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col justify-end"
      style={{ background: "rgba(20,24,27,.34)" }}
      onClick={onClose}
    >
      <div
        ref={sheetRef}
        className={cn("bg-white rounded-t-[22px] pt-5 px-5 pb-7 max-h-[88%] overflow-y-auto shadow-2xl", className)}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="w-9 h-1 rounded-full mx-auto mb-4" style={{ background: "#dfe3e6" }} />
        <div className="text-[17px] font-semibold mb-4" style={{ color: "#14181b" }}>{title}</div>
        {children}
      </div>
    </div>
  );
}

interface SheetFieldProps {
  label: string;
  placeholder?: string;
  value: string;
  onChange: (v: string) => void;
  inputMode?: React.HTMLAttributes<HTMLInputElement>["inputMode"];
  className?: string;
}

export function SheetField({ label, placeholder, value, onChange, inputMode, className }: SheetFieldProps) {
  return (
    <div className={className}>
      <label className="block text-[11px] font-semibold mb-[5px]" style={{ color: "#5b666e" }}>{label}</label>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        inputMode={inputMode}
        className="w-full border rounded-[11px] px-3 py-[11px] text-[13px]"
        style={{ border: "1.5px solid #dfe6e8", color: "#14181b" }}
      />
    </div>
  );
}
