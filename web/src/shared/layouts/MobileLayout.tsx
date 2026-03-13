import { Outlet, useLocation } from 'react-router-dom';

import { BottomNavigation } from '@/shared/components/bottom-navigation';

// 하단 네비게이션바를 숨길 경로 목록
const HIDE_NAV_ROUTES = ['/freerun', '/freerun-result'];

export function MobileLayout() {
  const { pathname } = useLocation();
  const hideNav = HIDE_NAV_ROUTES.includes(pathname);

  return (
    <div className="relative mx-auto min-h-screen min-w-[375px] max-w-[430px] flex items-center justify-center bg-gray-100">
      <div className="min-h-screen w-full bg-white border shadow-xl overflow-hidden">
        <main className={`h-full ${hideNav ? '' : 'pb-24 p-4'}`}>
          <Outlet />
        </main>
        {/* 네비게이션 바는 항상 최상단에 존재해야 하기 때문에 z-tooltip으로 설정 */}
        <div className="fixed flex justify-center bottom-6 left-6 right-6 z-tooltip">
          <div className="w-full min-w-[375px] max-w-[430px] px-4">
            {!hideNav && <BottomNavigation />}
          </div>
        </div>
      </div>
    </div>
  );
}
