import type { ReactNode } from "react";

import { BottomNavigation } from '@/shared/components/bottom-navigation';

type MobileLayoutProps = {
  children: ReactNode;
};

export function MobileLayout({ children }: MobileLayoutProps) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="relative w-[375px] h-[812px] bg-white rounded-[40px] border shadow-xl overflow-hidden">
        <main className="h-full pb-24 p-4">{children}</main>
        {/* 네비게이션 바는 항상 최상단에 존재해야 하기 때문에 z-tooltip으로 설정 */}
        <div className="absolute bottom-6 left-6 right-6 z-tooltip">
          <BottomNavigation />
        </div>
      </div>
    </div>
  );
}