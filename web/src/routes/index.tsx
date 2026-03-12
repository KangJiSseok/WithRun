import { Route, Routes } from 'react-router-dom';
import { HomePage } from '@/pages/private/home';
import { FreePage } from '@/pages/private/FreeRun';
import { FreeRunningPage } from '@/pages/private/FreeRunning';
import { NormalPage } from '@/pages/private/normal';
import { CompetitionPage } from '@/pages/private/competition';
import { CalendarPage } from '@/pages/private/calendar';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/home" element={<HomePage />} />
      <Route path="/free" element={<FreePage />} />
      <Route path="/freerun" element={<FreeRunningPage />} />
      <Route path="/normal" element={<NormalPage />} />
      <Route path="/competition" element={<CompetitionPage />} />
      <Route path="/calendar" element={<CalendarPage />} />
    </Routes>
  );
}
