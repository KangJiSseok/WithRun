import { Search } from 'lucide-react';
import { Input } from '../shadcn/input';

interface RunningCourseSearchBarProps {
  searchKeyword: string;
  handleSearchKeyword: (searchKeywordText: string) => void;
}

export function RunningCourseSearchBar({
  searchKeyword,
  handleSearchKeyword,
}: RunningCourseSearchBarProps) {
  return (
    <div className="relative flex-1">
      <Search
        size={18}
        className="pointer-events-none absolute left-[var(--space-4)] top-1/2 -translate-y-1/2 text-[hsl(var(--text-secondary))]"
      />
      <Input
        value={searchKeyword}
        onChange={(event) => handleSearchKeyword(event.target.value)}
        placeholder="코스 이름 검색..."
        className="h-14 rounded-[var(--radius-xl)] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] pl-11 text-[hsl(var(--text-primary))] placeholder:text-[hsl(var(--text-secondary))]"
      />
    </div>
  );
}
