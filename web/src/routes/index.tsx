import { Route, Routes } from 'react-router-dom';
import { HomePage } from '@/pages/private/Home';
import { FreePage } from '@/pages/private/FreeRun';
import { NormalPage } from '@/pages/private/Normal';
import { CompetitionPage } from '@/pages/private/Competition';
import { CalendarPage } from '@/pages/private/Calendar';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/home" element={<HomePage />} />
      <Route path="/free" element={<FreePage />} />
      <Route path="/normal" element={<NormalPage />} />
      <Route path="/competition" element={<CompetitionPage />} />
      <Route path="/calendar" element={<CalendarPage />} />
    </Routes>
  );
}
