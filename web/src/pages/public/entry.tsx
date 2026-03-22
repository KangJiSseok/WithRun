import { Navigate } from 'react-router-dom';

const ONBOARDING_STORAGE_KEY = 'withrun:onboarding-complete';

function hasCompletedOnboarding(): boolean {
  return localStorage.getItem(ONBOARDING_STORAGE_KEY) === 'true';
}

export function EntryPage() {
  return (
    <Navigate
      to={hasCompletedOnboarding() ? '/home' : '/onboarding'}
      replace
    />
  );
}
