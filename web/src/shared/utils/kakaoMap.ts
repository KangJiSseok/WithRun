const KAKAO_APP_KEY = '00e95b3c23d98d12a07d3744fb0afcc7';

let sdkPromise: Promise<void> | null = null;

export function loadKakaoMapSdk(): Promise<void> {
  if (window.kakao?.maps) return Promise.resolve();
  if (sdkPromise) return sdkPromise;

  sdkPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_APP_KEY}&autoload=false`;
    script.async = true;
    script.onload = () => kakao.maps.load(resolve);
    script.onerror = () => {
      sdkPromise = null;
      reject(new Error('Kakao Maps SDK 로드 실패'));
    };
    document.head.appendChild(script);
  });

  return sdkPromise;
}

// ─── 범용 마커 ──────────────────────────────────────────────────────────────

/**
 * 레이블 + 색상 도트 마커 DOM 생성 (코스 시작점/도착점 등에 사용)
 */
export function createMarkerContent(label: string, color: string): HTMLElement {
  const wrapper = document.createElement('div');
  wrapper.style.cssText =
    'display:flex;flex-direction:column;align-items:center;gap:4px;pointer-events:none;user-select:none;';
  wrapper.draggable = false;

  const dot = document.createElement('div');
  dot.style.cssText = `
    width: 20px;
    height: 20px;
    background: ${color};
    border: 3px solid white;
    border-radius: 50%;
    box-shadow: 0 2px 6px rgba(0,0,0,0.3);
  `;

  const tag = document.createElement('span');
  tag.textContent = label;
  tag.style.cssText = `
    background: ${color};
    color: white;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 6px;
    border-radius: 4px;
    white-space: nowrap;
    box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  `;

  wrapper.appendChild(dot);
  wrapper.appendChild(tag);
  return wrapper;
}

// ─── 러닝 마커 DOM 생성 ─────────────────────────────────────────────────────

/**
 * 현재 사용자 위치 마커 DOM 생성
 * - 파란 도트 (16px) + 방향 cone (삼각형)
 * - coneEl.style.transform = `rotate(Xdeg)` 로 방향 제어
 * - coneEl.style.opacity = '0.75' 로 표시 (GPS 정확도 확보 후)
 */
export function createUserMarkerContent(): { element: HTMLElement; coneEl: HTMLElement } {
  const wrapper = document.createElement('div');
  wrapper.style.cssText = `
    position: relative;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  `;

  // 방향 삼각형 (CSS border trick)
  const cone = document.createElement('div');
  cone.style.cssText = `
    position: absolute;
    left: calc(50% - 8px);
    bottom: 50%;
    transform-origin: 8px 18px;
    transform: rotate(0deg);
    width: 0;
    height: 0;
    border-left: 8px solid transparent;
    border-right: 8px solid transparent;
    border-bottom: 18px solid rgba(59,130,246,0.5);
    opacity: 0;
    transition: opacity 0.3s;
  `;

  // 파란 도트
  const dot = document.createElement('div');
  dot.style.cssText = `
    position: relative;
    z-index: 1;
    width: 16px;
    height: 16px;
    background: #3B82F6;
    border: 2.5px solid white;
    border-radius: 50%;
    box-shadow: 0 0 0 6px rgba(59,130,246,0.2), 0 2px 4px rgba(0,0,0,0.3);
  `;

  wrapper.appendChild(cone);
  wrapper.appendChild(dot);

  return { element: wrapper, coneEl: cone };
}

/**
 * 고스트 러너 위치 마커 DOM 생성
 * - 오렌지 도트 (16px), cone 없음
 */
export function createGhostMarkerContent(): HTMLElement {
  const wrapper = document.createElement('div');
  wrapper.style.cssText = `
    position: relative;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  `;

  const dot = document.createElement('div');
  dot.style.cssText = `
    width: 16px;
    height: 16px;
    background: #F97316;
    border: 2.5px solid white;
    border-radius: 50%;
    box-shadow: 0 0 0 6px rgba(249,115,22,0.2), 0 2px 4px rgba(0,0,0,0.3);
  `;

  wrapper.appendChild(dot);
  return wrapper;
}

// ─── 경로 오버레이 DOM 생성 ─────────────────────────────────────────────────

/**
 * 경로 위 방향 화살표 CustomOverlay DOM 생성
 * @param bearingDeg 화살표가 가리킬 방향 (0 = 북, 시계방향 증가)
 */
export function createRouteArrowElement(bearingDeg: number): HTMLElement {
  const el = document.createElement('div');
  el.style.cssText = 'pointer-events:none;';
  el.innerHTML = `<svg viewBox="0 0 10 14" width="10" height="14" style="display:block;transform:rotate(${bearingDeg}deg);transform-origin:center"><path d="M5 0 L10 14 L5 9 L0 14Z" fill="white" fill-opacity="0.8"/></svg>`;
  return el;
}

// ─── 애니메이션 헬퍼 ────────────────────────────────────────────────────────

/**
 * 최단 경로 각도 보간 (359° → 1° = +2°, −358° 아님)
 * @param current 현재 각도 (0~360)
 * @param target 목표 각도 (0~360)
 * @param t 보간 계수 (0~1), MAP_LERP 사용 권장
 */
export function lerpAngle(current: number, target: number, t: number): number {
  const diff = ((target - current + 540) % 360) - 180;
  return (current + diff * t + 360) % 360;
}
