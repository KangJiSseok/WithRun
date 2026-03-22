import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Award, Home, MapPin, Activity, Timer, Check, UploadCloud, Heart } from 'lucide-react';
import { cn, formatDuration, formatPace } from '@/shared/utils';
import { CourseShareModal } from './CourseShareModal';
import { useRunningSessionStore } from '@/stores/runningSession';
import { useGeneratePaceData } from '@/hooks/FreeRun/useGeneratePaceData';
import { likeCourse } from '@/apis/courseLike';

// 회원 시스템 구현 전 임시 mock 유저
// 추후 변경 시 => auth context에서 실제 유저 정보 받아오기
const MOCK_USER = { name: '러너' };
const MOCK_USER_ID = 1;

// 심박수 mock 데이터 (워치 연동 예정)
// 추후 변경 시 => state.heartRate로 대체 (min, avg, max 계산해서 저장)
const MOCK_HEART_RATE = { min: 118, avg: 142, max: 168 };


interface StatItemProps {
  label: string;
  value: string;
  unit: string;
}

function StatItem({ label, value, unit }: StatItemProps) {
  return (
    <div className="flex-1 flex flex-col items-center gap-1 bg-[hsl(var(--secondary-2))] rounded-2xl py-4 px-2">
      <span className="caption-r text-[hsl(var(--secondary-5))]">{label}</span>
      <span className="h3-b text-[hsl(var(--text-inverse))]">{value}</span>
      {unit && <span className="caption-r text-[hsl(var(--secondary-5))]">{unit}</span>}
    </div>
  );
}



export function FreeRunResultScreen() {
  const navigate = useNavigate();
  const [isLiked, setIsLiked] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);

  const result = useRunningSessionStore((s) => s.result);
  const courseId = useRunningSessionStore((s) => s.courseId);
  const mode = useRunningSessionStore((s) => s.mode);

  const durationSec = result?.durationSec ?? 0;
  const distanceM = result?.distanceM ?? 0;
  const caloriesKcal = result?.caloriesKcal ?? 0;
  const avgPaceSecPerKm = result?.avgPaceSecPerKm ?? 0;

  // courseId가 있을 때만 API 호출, 없으면 로컬 토글만 (코스 등록 전 상태)
  async function handleLike() {
    if (courseId) {
      const res = await likeCourse(courseId, MOCK_USER_ID);
      setIsLiked(res.data.isLiked);
    } else {
      setIsLiked((prev) => !prev);
    }
  }

  const time = formatDuration(durationSec);
  const distance = (distanceM / 1000).toFixed(2);
  const pace = formatPace(avgPaceSecPerKm);
  const calories = caloriesKcal;

  const { paceData, avgSeconds } = useGeneratePaceData(pace, distance);

  return (
    <div className="absolute inset-0 bg-[hsl(var(--secondary-1))]">
    <div className={cn('absolute inset-0 flex flex-col', showShareModal ? 'overflow-hidden' : 'overflow-y-auto')}>
      {/* 헤더 */}
      <div className="relative flex flex-col items-center pt-10 pb-6 px-6">
        {/* 좋아요 버튼 — FREE 모드에서는 표시하지 않음 */}
        {mode !== 'FREE' && (
          <button
            onClick={handleLike}
            className="absolute top-10 right-6 p-2"
          >
            <Heart
              size={26}
              className={cn(isLiked ? 'fill-red-400 text-red-400' : 'text-[hsl(var(--secondary-5))]')}
            />
          </button>
        )}
        <div className="p-4 rounded-full bg-primary/20 border border-primary/30 mb-4">
          <Award size={40} className="text-primary" />
        </div>
        <h1 className="h2-b text-[hsl(var(--text-inverse))] mb-1">수고하셨습니다, {MOCK_USER.name}님!</h1>
      </div>

      {/* 지도 영역 (코스 경로 표시 - GPS 연동 예정) */}
      <div className="mx-6 mb-4 h-40 bg-[hsl(var(--secondary-2))] rounded-2xl flex items-center justify-center border border-[hsl(var(--secondary-3))]">
        <div className="flex flex-col items-center gap-2 text-[hsl(var(--secondary-5))]">
          <MapPin size={24} />
          <span className="caption-r">러닝 코스 지도 (GPS 연동 예정)</span>
        </div>
      </div>

      <div className="px-6 space-y-4 pb-8">
        {/* 이동 거리 - 크게 표시 */}
        <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-6 flex flex-col items-center relative overflow-hidden">
          <div className="absolute top-0 right-0 w-24 h-24 bg-primary/10 rounded-full blur-2xl -mr-6 -mt-6" />
          <span className="text-6xl font-bold text-[hsl(var(--text-inverse))] tabular-nums relative z-base">
            {distance}
          </span>
          <span className="caption-r text-[hsl(var(--secondary-5))] uppercase tracking-widest mt-1 relative z-base">
            킬로미터
          </span>
        </div>

        {/* 시간, 평균 페이스, 칼로리 */}
        <div className="flex gap-3">
          <StatItem label="시간" value={time} unit="" />
          <StatItem label="평균 페이스" value={pace} unit="/km" />
          <StatItem label="칼로리" value={String(calories)} unit="kcal" />
        </div>

        {/* 심박수 변동 (워치 연동 예정) */}
        <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-4">
          <div className="flex items-center gap-2 mb-4">
            <Activity size={16} className="text-primary" />
            <span className="caption-b text-[hsl(var(--text-inverse))]">심박수 변동</span>
            <span className="caption-r text-[hsl(var(--secondary-5))] ml-auto">워치 연동 예정</span>
          </div>
          <div className="flex gap-3">
            {[
              { label: '최소', value: String(MOCK_HEART_RATE.min) },
              { label: '평균', value: String(MOCK_HEART_RATE.avg), highlight: true },
              { label: '최대', value: String(MOCK_HEART_RATE.max) },
            ].map(({ label, value, highlight }) => (
              <div key={label} className="flex-1 flex flex-col items-center gap-1 bg-[hsl(var(--secondary-3))] rounded-xl py-3">
                <span className="caption-r text-[hsl(var(--secondary-5))]">{label}</span>
                <span className={cn('h3-b', highlight ? 'text-primary' : 'text-[hsl(var(--text-inverse))]')}>
                  {value}
                </span>
                <span className="caption-r text-[hsl(var(--secondary-5))]">bpm</span>
              </div>
            ))}
          </div>
        </div>

        {/* 구간별 페이스 */}
        <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-4">
          <div className="flex items-center gap-2 mb-4">
            <Timer size={16} className="text-primary" />
            <span className="caption-b text-[hsl(var(--text-inverse))]">구간별 페이스 (1km)</span>
          </div>
          <div className="rounded-xl overflow-hidden border border-[hsl(var(--secondary-3))]">
            <table className="w-full">
              <thead className="bg-[hsl(var(--secondary-3))]">
                <tr>
                  <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">구간</th>
                  <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">페이스</th>
                  <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[hsl(var(--secondary-3))]">
                {paceData.map((item) => (
                  <tr key={item.km}>
                    <td className="px-4 py-3 caption-m text-[hsl(var(--text-inverse))]">{item.km}km</td>
                    <td className="px-4 py-3 caption-m text-primary font-bold">{item.displayPace}</td>
                    <td className="px-4 py-3">
                      <div className={cn(
                        'w-2 h-2 rounded-full',
                        item.seconds < avgSeconds
                          ? 'bg-[hsl(var(--success))]'
                          : 'bg-[hsl(var(--warning))]',
                      )} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* 코스 등록 버튼 */}
        <button
          onClick={() => setShowShareModal(true)}
          disabled={courseId !== null}
          className={cn(
            'w-full py-4 rounded-2xl body-b flex items-center justify-center gap-2 transition-colors',
            courseId !== null
              ? 'bg-[hsl(var(--secondary-4))] text-[hsl(var(--secondary-5))] cursor-default'
              : 'bg-primary/20 border border-primary/30 text-primary',
          )}
        >
          {courseId !== null ? <Check size={18} /> : <UploadCloud size={18} />}
          {courseId !== null ? '등록 완료' : '코스 등록'}
        </button>

        {/* 홈으로 버튼 */}
        <button
          onClick={() => navigate('/free-run')}
          className="w-full py-4 rounded-2xl bg-primary text-[hsl(var(--text-inverse))] body-b flex items-center justify-center gap-2"
        >
          <Home size={18} />
          홈으로
        </button>
      </div>
    </div>

      <CourseShareModal
        isOpen={showShareModal}
        onClose={() => setShowShareModal(false)}
      />
    </div>
  );
}
