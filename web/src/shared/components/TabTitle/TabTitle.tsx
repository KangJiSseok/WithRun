import type { LucideIcon } from 'lucide-react';

type TabTitleProps = {
  icon: LucideIcon;
  title: string;
};

export function TabTitle({ icon: Icon, title }: TabTitleProps) {
  return (
    <div className="flex items-center gap-[var(--space-3)]">
      <Icon size={28} className="text-[hsl(var(--primary-1))]" />
      <h1 className="h2-b text-[hsl(var(--text-primary))]">{title}</h1>
    </div>
  );
}
