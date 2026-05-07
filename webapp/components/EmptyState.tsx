"use client";
import { type LucideIcon } from "lucide-react";

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  subtitle?: string;
  action?: { label: string; onClick: () => void };
}

export function EmptyState({ icon: Icon, title, subtitle, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
      {Icon && (
        <div className="w-14 h-14 rounded-2xl bg-bg-card border border-divider flex items-center justify-center mb-4">
          <Icon className="w-7 h-7 text-text-muted" />
        </div>
      )}
      <p className="text-[15px] font-semibold text-text-primary mb-1">{title}</p>
      {subtitle && <p className="text-sm text-text-muted mb-4">{subtitle}</p>}
      {action && (
        <button
          onClick={action.onClick}
          className="px-4 py-2 rounded-xl bg-green text-white text-sm font-medium hover:bg-green/90 transition-colors"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}
