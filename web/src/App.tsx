import { Route, Routes } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { bridge } from './bridge';
import useWatchMessage from '@/hooks/useWatchMessage';
import { BottomNavigation } from '@/shared/components/bottom-navigation';
import { HomePage } from '@/pages/private/home';
import { FreePage } from '@/pages/private/free';
import { NormalPage } from '@/pages/private/normal';
import { CompetitionPage } from '@/pages/private/competition';
import { CalendarPage } from '@/pages/private/calendar';

function App() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="relative w-[375px] h-[812px] bg-white rounded-[40px] border shadow-xl overflow-hidden">
        <main className="h-full pb-24 p-4">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/home" element={<HomePage />} />
            <Route path="/free" element={<FreePage />} />
            <Route path="/normal" element={<NormalPage />} />
            <Route path="/competition" element={<CompetitionPage />} />
            <Route path="/calendar" element={<CalendarPage />} />
          </Routes>

        </main>

        <div className="absolute bottom-6 left-6 right-6">
          <BottomNavigation />
        </div>
      </div>
    </div>
  );
}

export default App;
