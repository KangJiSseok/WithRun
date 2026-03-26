import { Navigate, useLocation } from 'react-router-dom';

export function LoginPage() {
  const location = useLocation();

  return (
    // 로그인 UX 는 다시 온보딩 흐름 안으로 합쳤기 때문에 /login 은 온보딩의 별칭으로만 유지합니다.
    <Navigate
      to={`/onboarding${location.search}`}
      replace
    />
  );
}
