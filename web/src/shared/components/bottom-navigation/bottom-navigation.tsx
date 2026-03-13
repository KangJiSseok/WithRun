import { NavLink } from 'react-router-dom';
import { Calendar, Ghost, Home, Map, Navigation } from 'lucide-react';

const ITEMS = [
  { label: '홈', to: '/home', icon: Home },
  { label: '자유', to: '/free', icon: Navigation },
  { label: '일반', to: '/normal', icon: Map },
  { label: '경쟁', to: '/competition', icon: Ghost },
  { label: '캘린더', to: '/calendar', icon: Calendar },
];

// key는 리액트가 내부적으로 구분하려고 씀
// to는 라우터가 페이지 이동하려고 씀
// NavLink 안에 요소가 2개 들어가야 한다 (아이콘용 태그 + 텍스트용 태그)

export function BottomNavigation() {
  return (
    <nav className="flex items-center justify-between rounded-3xl px-6 py-4 border shadow-sm ">
      {ITEMS.map((item) => {
        const Icon = item.icon;

        return (
          <NavLink
            key={item.to}
            to={item.to}
            className="flex flex-col items-center gap-1 px-2 py-1"
          >
            {({ isActive }) => (
              <>
                <div
                  className={
                    isActive
                      ? 'rounded-full bg-primary p-2 text-primary-foreground'
                      : 'rounded-full p-2 text-muted-foreground'
                  }
                >
                  <Icon size={20} />
                </div>
                <span
                  className={
                    isActive
                      ? 'text-xs font-semibold text-primary'
                      : 'text-xs text-muted-foreground'
                  }
                >
                  {item.label}
                </span>
              </>
            )}
          </NavLink>
        );
      })}
    </nav>
  );
}
