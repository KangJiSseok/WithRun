// 구간별 1km 페이스 계산 훅
// 현재: 평균 페이스 기준으로 sin 함수 변동을 줘서 mock 데이터 생성
// 추후 GPS 연동 시: useFreeRunning에서 매 1km 통과 시 실측 페이스를 기록하고
//   kmSplits: { km: number; seconds: number }[] 를 인자로 받아 실측 데이터 기반으로 교체
// (paceData, avgSeconds 계산)


interface PaceEntry {
  km: number;
  displayPace: string;
  seconds: number;
}

function parsePaceToSeconds(pace: string): number {
  const match = pace.match(/(\d+)'(\d+)"/);
  return match ? parseInt(match[1]) * 60 + parseInt(match[2]) : 330;
}

// 추후 연동 시 useGeneratePaceData(kmSplits: { km: number; seconds: number }[])
// 호출부는 그대로 쓰고, 인자만 kmSplits로 바꾸면 됨
export function useGeneratePaceData(pace: string, distance: string) {
  const km = Math.max(Math.ceil(parseFloat(distance) || 1), 1);
  const baseSeconds = parsePaceToSeconds(pace);

  const paceData: PaceEntry[] = Array.from({ length: km }, (_, i) => {
    const variation = Math.floor(Math.sin(i * 1.5) * 15);
    const secs = Math.max(baseSeconds + variation, 180);
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return {
      km: i + 1,
      displayPace: `${m}'${String(s).padStart(2, '0')}"`,
      seconds: secs,
    };
  });

  const avgSeconds = paceData.reduce((a, b) => a + b.seconds, 0) / paceData.length;

  return { paceData, avgSeconds };
}
