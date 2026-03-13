import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Award, Home, MapPin, Activity, Timer, Save, Check, UploadCloud } from 'lucide-react';
import { cn } from '@/shared/utils';
import { CourseRegisterModal } from './CourseRegisterModal';
import { CourseShareModal } from './CourseShareModal';

// 회원 시스템 구현 전 임시 mock 유저
// 추후 변경 시 => auth context에서 실제 유저 정보 받아오기
const MOCK_USER = { name: '러너' };

// 심박수 mock 데이터 (워치 연동 예정)
// 추후 변경 시 => state.heartRate로 대체 (min, avg, max 계산해서 저장)
const MOCK_HEART_RATE = { min: 118, avg: 142, max: 168 };

// 추후 추가 시 => 구간별 페이스, 심박수 추가
interface RunResultState {
  time: string;
  distance: string;
  pace: string;
  calories: number;
}

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

// 구간별 페이스 계산
// 추후 GPS 연동시 useFreeRunning에서 매 1km 통과 시마다
// 페이스를 기록해서 kmSplits: { km: number, pace: string }[]와 같은 형태로 관리하는 방식으로
// 변경 예정 + navigate state에 같이 넘겨야 함. (이건 추후 삭제 예정)
function generatePaceData(pace: string, distance: string) {
  const km = Math.max(Math.ceil(parseFloat(distance) || 1), 1);
  // match: 정규식으로 pace 문자열 파싱
  const match = pace.match(/(\d+)'(\d+)"/);
  // baseSeconds: 페이스를 초 단위로 환산 (예: 5'30" -> 330초)
  // 매칭 성공 시: baseSeconds = parseInt(match[1]) * 60 + parseInt(match[2]) = 330초
  const baseSeconds = match ? parseInt(match[1]) * 60 + parseInt(match[2]) : 330;

  return Array.from({ length: km }, (_, i) => {
    const variation = Math.floor((Math.sin(i * 1.5) * 15));
    const secs = Math.max(baseSeconds + variation, 180);
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return {
      km: i + 1,
      displayPace: `${m}'${String(s).padStart(2, '0')}"`,
      seconds: secs,
    };
  });
}

export function FreeRunResultScreen() {
  const navigate = useNavigate();
  // 코스 저장 상태 (추후 API 연동 시 POST /courses 호출로 교체)
  const [isSaved, setIsSaved] = useState(false);
  // 공식 코스 등록 모달 상태
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  // 공식 코스 등록 폼 모달 상태
  const [showShareModal, setShowShareModal] = useState(false);
  // useLocation: 현재 URL 정보를 읽는 훅
  // (URL 외에 navigate로 넘긴 state도 여기서 읽어올 수 있음)

  // FreeRunningScreen에서 navigate 시 state를 같이 전송
  // navigate('/freerun-result', { state: { time, distance, pace, calories } })

  // FreeRunResultScreen에서 그 state를 수신
  // const location = useLocation();
  // location.state  // → { time, distance, pace, calories }

  // 페이지 이동 시 브라우저 히스토리 스택에 state를 임시로 붙여서 전달하는 방식
  // URL에 보이지 않음
  
  // 백엔드에서 저장 후 조회 방식
  // 러닝 종료 → POST /runs (결과 저장) → 서버에서 runId 반환
        //  → navigate('/freerun-result', { state: { runId } })
        //  → 결과 화면에서 GET /runs/:runId → 데이터 표시

  // 저장 기록이 남고, 새로고침해도 데이터가 유지되는 방식

  const location = useLocation();
  const state = location.state as RunResultState | null;

  const time = state?.time ?? '00:00';
  const distance = state?.distance ?? '0.00';
  const pace = state?.pace ?? `--'--"`;
  const calories = state?.calories ?? 0;

  const paceData = generatePaceData(pace, distance);
  const avgSeconds = paceData.reduce((a, b) => a + b.seconds, 0) / paceData.length;

  return (
    <div className="absolute inset-0 bg-[hsl(var(--secondary-1))]">
    <div className="absolute inset-0 flex flex-col overflow-y-auto">
      {/* 헤더 */}
      <div className="flex flex-col items-center pt-10 pb-6 px-6">
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

        {/* 코스 저장 버튼 (추후 API 연동 시 POST /courses 호출과 같은 형식으로 교체) */}
        <button
          onClick={() => setIsSaved(true)}
          disabled={isSaved}
          className={cn(
            'w-full py-4 rounded-2xl body-b flex items-center justify-center gap-2 transition-colors',
            isSaved
              ? 'bg-[hsl(var(--secondary-4))] text-[hsl(var(--secondary-5))] cursor-default'
              : 'bg-[hsl(var(--secondary-3))] text-[hsl(var(--text-inverse))]',
          )}
        >
          {isSaved ? <Check size={18} /> : <Save size={18} />}
          {isSaved ? '저장 완료' : '코스 저장'}
        </button>
        
        {/* 공식 코스 등록 버튼 */}
        <button
          onClick={() => setShowRegisterModal(true)}
          className="w-full py-4 rounded-2xl bg-primary/20 border border-primary/30 text-primary body-b flex items-center justify-center gap-2"
        >
          <UploadCloud size={18} />
          공식 코스 등록
        </button>


        {/* 홈으로 버튼 */}
        <button
          onClick={() => navigate('/free')}
          className="w-full py-4 rounded-2xl bg-primary text-[hsl(var(--text-inverse))] body-b flex items-center justify-center gap-2"
        >
          <Home size={18} />
          홈으로
        </button>
      </div>
    </div>

      <CourseRegisterModal
        isOpen={showRegisterModal}
        onClose={() => setShowRegisterModal(false)}
        onConfirm={() => {
          setShowRegisterModal(false);
          setShowShareModal(true);
        }}
      />
      <CourseShareModal
        isOpen={showShareModal}
        onClose={() => setShowShareModal(false)}
      />
    </div>
  );
}
