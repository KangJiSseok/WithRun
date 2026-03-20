# 자유러닝 전체 흐름

## 라우트 구조

```
/free-run          → FreeRunPage          (지도 + 캐릭터)
/free-run-session  → FreeRunSessionPage   (러닝 중 + 결과, 내부 phase 전환)
```

---

## 1단계 — `/free-run` 진입

### 컴포넌트 트리

```
FreeRunPage
  ├─ FreeRunScreen
  │    ├─ FreeRunMap          ← 카카오 지도
  │    └─ RunCharacter        ← 시작 버튼 역할
  └─ RunningGuideModal        ← 첫 방문 시 가이드 모달
```

### 동작

- 마운트 시 `localStorage('freerun-guide-seen')` 확인 → 첫 방문이면 가이드 모달 표시
- `FreeRunMap` (`useFreeRunMap`)
  - 카카오맵 서울 기본 좌표로 초기 렌더
  - `getCurrentPosition` (낮은 정확도, 캐시 30초) → 즉시 현재 위치로 이동
  - `watchPosition` (GPS 칩, 높은 정확도) → 이후 실시간 마커 추적
  - 페이지 이탈 시 `clearWatch` → 배터리 소모 방지
- **API 호출 없음**
- 캐릭터 클릭 → `/free-run-session` navigate

---

## 2단계 — `/free-run-session` 진입, 세션 생성

### 컴포넌트 트리

```
FreeRunSessionPage
  ├─ useEffect: store.clear()           ← 이전 세션 데이터 초기화
  ├─ useRunningSession({ mode: 'FREE' })
  │    ├─ Bridge GPS 있으면 → 즉시 POST
  │    └─ Bridge GPS 없으면 → 5초 대기 → navigator.geolocation 폴백
  │
  ├─ !sessionReady → "위치 확인 중..." 로딩
  ├─ error        → 에러 메시지
  └─ sessionReady → FreeRunningScreen
```

### API

```
POST /api/running-sessions
  요청: { userId, startLatitude, startLongitude, mode: 'FREE' }
  응답: {
    runningSessionId: number,
    mode: 'FREE',
    courseId: null,                      ← FREE 모드에서는 null
    ghostTargetRunningSessionId: null,   ← FREE 모드에서는 null
    distanceM: 0,
    caloriesKcal: 0,
    isCompleted: false,
  }
         ↓
  store.setRunningSessionId(runningSessionId)  ← runningSessionId만 저장
```

### 스토어 상태 (세션 생성 후)

```
useRunningSessionStore {
  runningSessionId: 123,   ← 방금 받음
  courseId: null,
  runningMode: null,
  result: null,
}
```

---

## 3단계 — 러닝 중 (`FreeRunningScreen`)

### 실시간 데이터 흐름

```
네이티브 앱
  ├─ GPS 모듈   → gpsLocation   (위도/경도/속도/고도/방향/정확도/케이던스)
  └─ 워치 모듈  → watchHealthData (거리/칼로리/심박수/케이던스)
        ↓ Bridge (WebView postMessage)
  useBridgeDataStore (Zustand)
        ↓
  useFreeRunning() — 최종 출력값 계산
        ↓
  FreeRunningScreen — 화면 렌더
```

### `useFreeRunning()` 출력값 계산 로직

| 출력값 | 소스 | 로직 |
|---|---|---|
| `time` | 내부 `setInterval` | 1초마다 증가, 일시정지 시 누적값 보존 |
| `distance` | 워치 연결 시: `watchHealthData.distanceM` / 미연결 시: GPS Haversine 누적 | 2m~50m 이내 이동만 누적 (노이즈 필터) |
| `pace` | `gpsLocation.speedMps` (GPS Doppler) | 0.5 m/s 이하 정지 판단 → `--'--"` 표시 |
| `calories` | `watchHealthData.caloriesKcal` | 워치 전용, 모바일 불가 |
| `heartRate` | `watchHealthData.heartRate` | 워치 전용 |
| `cadenceSpm` | 워치 연결: `watchHealthData.cadenceSpm` / 미연결: `gpsLocation.cadenceSpm` | 워치 우선, 가속도계 폴백 |

### 워치 연결 판단

- `lastWatchMessageAt` 기준 3초 이내 메시지 수신 여부 → `isWatchConnected`
- 1초마다 체크

### 화면 구성

- 상단: 거리 / 페이스 / 칼로리 카드
- 중앙: 타이머 + 러닝 중 / 일시정지 배지
- 하단: 일시정지/재개 버튼 (항상), 종료 버튼 (일시정지 시 슬라이드)
- 에러 발생 시: 재시도 / 결과 보기 버튼

---

## 4단계 — 종료 → PATCH 세션 완료

### 흐름

```
종료 버튼 클릭
  → handleStop()
       ├─ buildPayload()
       │    ├─ durationSec      ← time 문자열 파싱 (MM:SS or HH:MM:SS)
       │    ├─ distanceM        ← distance(km) × 1000
       │    ├─ avgSpeedMps      ← distanceM / durationSec  (전체 평균)
       │    └─ avgPaceSecPerKm  ← 1000 / avgSpeedMps  (웹 테스트 시 0이면 임시값 300)
       │
       ├─ setLocalResult(...)   ← 서버 실패 대비 로컬 계산값 임시 보관
       ├─ setRunningMode('FREE')
       ├─ setIsSubmitting(true)
       │
       └─ PATCH /api/running-sessions/{runningSessionId}
            요청: {
              userId,
              distanceM,
              caloriesKcal,      ← useFreeRunning()의 calories (워치 or 0)
              endLatitude,       ← gpsLocation?.latitude (없으면 0)
              endLongitude,      ← gpsLocation?.longitude (없으면 0)
              avgSpeedMps,
              durationSec,
              avgPaceSecPerKm,
              elevationGainM: 0,     ← 미구현
              distanceGapM: 0,       ← 미구현
              gpsSamples: [],        ← 미구현
              healthSamples: [],     ← 미구현
              splits: [],            ← 미구현
            }
            │
            ├─ 성공 → setResult(res.data) → store 업데이트 → onFinish() → phase = 'result'
            └─ 실패 → setSubmitError → 재시도 / 결과 보기 버튼 UI
                          └─ "결과 보기" → setResult(localResult) → onFinish()
```

### 스토어 상태 (종료 후)

```
useRunningSessionStore {
  runningSessionId: 123,
  courseId: null,
  runningMode: 'FREE',
  result: {
    durationSec,
    distanceM,
    caloriesKcal,
    avgPaceSecPerKm,
  },
}
```

---

## 5단계 — 결과 화면 (`FreeRunResultScreen`)

### 데이터 소스

```
useRunningSessionStore → result, courseId
```

### 화면 구성

| 섹션 | 구현 상태 | 데이터 소스 |
|---|---|---|
| 거리 (대형) | 완료 | `result.distanceM` |
| 시간 / 페이스 / 칼로리 | 완료 | `result.*` |
| 지도 (러닝 경로) | **미구현** (GPS 연동 예정) | gpsSamples 필요 |
| 심박수 (최소/평균/최대) | **mock** (워치 연동 예정) | healthSamples 필요 |
| 구간별 페이스 테이블 | **mock** (`useGeneratePaceData`로 생성) | splits 필요 |
| 좋아요 버튼 | 완료 (courseId 없으면 로컬 토글) | `POST /api/courses/{id}/like` |
| 코스 저장 버튼 | **미구현** (로컬 상태 토글만) | 추후 API 연동 |
| 공식 코스 등록 버튼 | 완료 | `CourseShareModal` |
| 홈으로 버튼 | 완료 | `/free-run`으로 navigate |

### 공식 코스 등록 (`CourseShareModal`)

```
"공식 코스 등록" 버튼
  → CourseShareModal 오픈
       ├─ GET /api/courses/surveys  ← 코스 타입/난이도 선택지
       └─ "커뮤니티에 업로드" 버튼
            → POST /api/running-sessions/{runningSessionId}/courses
                 요청: { title, difficulty, courseTypes }
                 응답: { courseId }
                          ↓
                 store.setCourseId(courseId)
```

---

## 미구현 항목 (TODO)

### 러닝 중 데이터 수집

```
useFreeRunning() 안에서 매 업데이트마다 배열에 쌓아야 함
  gpsSamples: GpsSample[]      ← gpsLocation 변경마다 push
    { latitude, longitude, altitude, speedMps, accuracyM, recordedAt }
  healthSamples: HealthSample[] ← watchHealthData 변경마다 push
    { heartRate, caloriesKcal, cadenceSpm, recordedAt }
  splits: SplitData[]           ← 1km 달성마다 push
    { splitNumber, distanceM, durationSec, avgPaceSecPerKm }
```

### 결과 화면

- 지도에 러닝 경로 폴리라인 표시 (`gpsSamples` 필요)
- 심박수 실제 데이터 (최소/평균/최대 계산, `healthSamples` 필요)
- 구간별 페이스 실제 데이터 (`splits` 필요)
- 고도 변화 (`elevationGainM`, `gpsSamples.altitude` 필요)
- 코스 저장 API 연동

### 인증

- `MOCK_USER_ID = 1` → auth context에서 실제 유저 ID로 교체 필요
  - `useRunningSession.ts`, `FreeRunningScreen.tsx`, `FreeRunResultScreen.tsx`

---

## 전체 흐름 요약

```
[진입]       /free-run
               카카오 지도 표시 (watchPosition 실시간 마커)
               캐릭터 클릭

[세션 생성]  /free-run-session 이동
               store.clear()  ← 이전 세션 초기화
               GPS 확보 (Bridge or navigator.geolocation)
               POST /api/running-sessions → runningSessionId → store

[러닝 중]    Bridge(GPS + 워치) → useFreeRunning() → 화면 실시간 렌더
               타이머 / 거리 / 페이스 / 칼로리
               일시정지/재개 가능

[종료]       종료 버튼 (일시정지 상태에서만 노출)
               PATCH /api/running-sessions/{id}
               성공: 서버 result → store
               실패: 재시도 또는 로컬 계산값으로 결과 화면 진입

[결과]       store.result 읽어서 표시
               공식 코스 등록: POST /api/.../courses → courseId → store
               홈으로: /free-run navigate
```
