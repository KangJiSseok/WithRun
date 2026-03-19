import { Outlet } from 'react-router-dom';

export function FullscreenLayout() {
  return (
    <div className="relative mx-auto flex min-h-screen min-w-[375px] max-w-[430px] items-center justify-center bg-gray-100">
      <div className="min-h-screen w-full overflow-hidden border bg-white shadow-xl">
        <Outlet />
      </div>
    </div>
  );
}
