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
