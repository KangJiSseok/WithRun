# 자유러닝 전체 흐름 (+ NormalRun 비교)

> **이 문서는 현재 구현 상태 기준입니다.**
> 이전 store 기반 구현과 다른 부분이 많으므로 주의하세요.

---

## 라우트 구조 비교

```
FreeRun                              NormalRun
──────────────────────────────────   ──────────────────────────────────────────
/free-run        FreeRunPage         /normal-run              NormalRunPage
/free-run-session  FreeRunSessionPage  /normal-run/:courseId    NormalRunDetailPage
                                     /normal-run-session/:id  NormalRunSessionPage
```

- FreeRun은 **2단계** 라우트 (시작 화면 → 세션 화면)
- NormalRun은 **3단계** 라우트 (코스 목록 → 코스 상세 → 세션 화면)

---

## 1단계 — 시작 전 화면

### FreeRun: `/free-run` (FreeRunPage → FreeRunScreen)

```
FreeRunPage
  ├─ FreeRunScreen
  │    ├─ FreeRunMap          ← 카카오 지도 + 현재 위치 마커
  │    └─ RunCharacter        ← 클릭 시 세션 생성 + 이동
  └─ RunningGuideModal        ← 첫 방문 시만 표시 (localStorage)
```

**캐릭터 클릭 시 동작:**

```
클릭
  │
  ├─ Bridge GPS 있으면 → 즉시 POST
  └─ Bridge GPS 없으면 → navigator.geolocation.getCurrentPosition
       ├─ 성공 → POST
       └─ 실패 → 에러 메시지 (화면 내 표시, 재클릭으로 재시도 가능)
```

**API:**

```
POST /running-sessions
  요청: { userId, mode: 'FREE', startLatitude, startLongitude }
  성공 → navigate('/free-run-session', { state: { runningSessionId } })
  실패 → FreeRunScreen 내 에러 메시지 표시
```

---

### NormalRun: `/normal-run/:courseId` (NormalRunDetailPage → NormalRunDetailScreen)

```
NormalRunDetailPage
  └─ NormalRunDetailScreen
       ├─ NormalRunDetailMap  ← 코스 경로 표시 + 출발지 거리 안내
       └─ "시작" 버튼         ← 출발지 근처 도착 시 활성화
```

**시작 버튼 클릭 시 동작:**

```
클릭
  │
  ├─ Bridge GPS 없으면 → 즉시 return (아무 동작 없음)
  └─ Bridge GPS 있으면 → POST
       성공 → store.setSession(res.data)
            → navigate('/normal-run-session/:courseId')
```

**API:**

```
POST /running-sessions
  요청: { userId, mode: 'COURSE', courseId, startLatitude, startLongitude }
  성공 → useNormalRunSessionStore.setSession(res.data)
       → navigate
```

---

### 세션 생성 방식 비교

| | FreeRun | NormalRun |
|---|---|---|
| **생성 위치** | `FreeRunScreen` (시작 화면) | `NormalRunDetailScreen` (코스 상세 화면) |
| **GPS 폴백** | Bridge 없으면 browser geolocation | Bridge 없으면 시작 불가 |
| **세션 ID 전달** | `navigate` state로 전달 | Zustand store (`useNormalRunSessionStore`) |
| **실패 처리** | 화면 내 에러 메시지 | 없음 (조용히 실패) |

> **FreeRun에 browser geolocation 폴백이 있는 이유:**
> FreeRun은 웹 브라우저에서도 테스트할 수 있어야 한다.
> NormalRun은 코스 출발지 근접 판단에 이미 Bridge GPS가 필수이므로 폴백이 의미 없다.

> **NormalRun이 store를 쓰는 이유:**
> 세션 생성(`NormalRunDetailScreen`)과 세션 사용(`NormalRunSessionScreen`)이 서로 다른 라우트에 있어
> navigate state보다 store가 더 안정적이다.
> FreeRun은 바로 navigate하므로 state로 충분하다.

---

## 2단계 — 세션 페이지 진입

### FreeRun: `/free-run-session` (FreeRunSessionPage)

```
FreeRunSessionPage
  ├─ useLocation().state.runningSessionId  ← 이전 화면에서 전달
  ├─ phase: 'running' → FreeRunningScreen
  └─ phase: 'result'  → FreeRunResultScreen
```

- 진입 시 별도 로딩 없음 — 세션은 이미 이전 화면에서 생성 완료
- `phase` state로 러닝 중 ↔ 결과 화면 전환 (단일 페이지)

---

### NormalRun: `/normal-run-session/:courseId` (NormalRunSessionPage)

```
NormalRunSessionPage
  ├─ useNormalRunCourseDetail(courseId)   ← 코스 상세 재조회 (지도 렌더에 필요)
  ├─ useNormalRunSessionStore.result      ← 결과 있으면 결과 화면으로
  │
  ├─ result 있음  → NormalRunResultScreen
  └─ result 없음  → NormalRunSessionScreen
```

- 진입 시 코스 상세 API를 **다시 호출** (지도가 코스 경로를 표시하는 데 필요)
- store의 `result` 유무로 러닝 중 ↔ 결과 화면 전환

---

### 세션 페이지 구조 비교

| | FreeRun | NormalRun |
|---|---|---|
| **데이터 소스** | `useLocation().state` | Zustand store |
| **화면 전환** | `phase` state | `store.result` 유무 |
| **진입 시 API 호출** | 없음 | 코스 상세 재조회 |
| **페이지 수** | 1개 (phase로 전환) | 1개 (store값으로 전환) |

---

## 3단계 — 러닝 중

### 실시간 데이터 흐름 (공통)

```
네이티브 앱
  ├─ GPS 모듈   → gpsLocation   (1초마다)
  └─ 워치 모듈  → watchHealthData
        ↓ Bridge (WebView postMessage)
  useBridgeDataStore (Zustand)
        ↓
  useFreeRunning() / useNormalRunning()
        ↓
  FreeRunningScreen / NormalRunSessionScreen
```

### 출력값 비교

| 출력값 | FreeRun (`useFreeRunning`) | NormalRun (`useNormalRunning`) |
|---|---|---|
| `time` | 문자열 `"MM:SS"` / `"HH:MM:SS"` | 초(number) |
| `distance` | 문자열 km `"1.23"` | 문자열 km `"1.23"` |
| `pace` | 문자열 `"5'30\""` | 문자열 `"5'30\""` |
| `calories` | number | number |
| `getCollectedData()` | **없음** | **있음** — gpsSamples/healthSamples/splits 수집 |

### 화면 구성 비교

| | FreeRun (`FreeRunningScreen`) | NormalRun (`NormalRunSessionScreen`) |
|---|---|---|
| **배경** | 단색 (`bg-secondary-foreground`) | 지도 위 오버레이 |
| **통계 위치** | 상단 카드 3개 | 상단 오버레이 (지도 위) |
| **타이머** | 중앙 크게 (포커스) | 하단 컨트롤 영역 |
| **PATCH 실패 처리** | 재시도 / 결과 보기 버튼 | 홈으로 navigate + `clearAll()` |
| **비정상 종료 처리** | **없음** | useEffect cleanup → fire-and-forget PATCH |

---

## 4단계 — 종료 → PATCH

### FreeRun: `FreeRunningScreen.handleStop()`

```
종료 버튼 (일시정지 상태에서만 표시)
  → handleStop()
       ├─ buildPayload()
       │    ├─ durationSec  ← time 문자열 파싱
       │    ├─ distanceM    ← distance(km) × 1000
       │    ├─ avgSpeedMps  ← distanceM / durationSec
       │    └─ avgPaceSecPerKm ← 1000 / avgSpeedMps (0이면 임시값 300)
       │
       ├─ setLocalResult(built)  ← 서버 실패 대비 로컬값 보관
       │
       └─ PATCH /running-sessions/{runningSessionId}
            요청: {
              userId,
              distanceM, caloriesKcal,
              endLatitude, endLongitude,  ← gpsLocation (없으면 0)
              avgSpeedMps, durationSec, avgPaceSecPerKm,
              elevationGainM: 0,    ← 미구현
              distanceGapM: 0,      ← 미구현
              gpsSamples: [],       ← 미구현
              healthSamples: [],    ← 미구현
              splits: [],           ← 미구현
            }
            ├─ 성공 → onFinish(res.data) → phase = 'result'
            └─ 실패 → 재시도 / 결과 보기 버튼 (localResult로 결과 진입 가능)
```

### NormalRun: `NormalRunSessionScreen.handleStopRunning()`

```
종료 버튼 (일시정지 상태에서만 표시)
  → handleStopRunning()
       ├─ getCollectedData()  ← 누적 수집된 실제 데이터
       │    { gpsSamples, healthSamples, splits,
       │      distanceM, caloriesKcal, avgSpeedMps,
       │      durationSec, avgPaceSecPerKm, elevationGainM }
       │
       └─ PATCH /running-sessions/{session.runningSessionId}
            요청: {
              userId,
              endLatitude, endLongitude,
              distanceGapM: 0,
              ...getCollectedData()   ← 실제 수집 데이터
            }
            ├─ 성공 → store.setResult(res.data) → 결과 화면으로 전환
            └─ 실패 → store.clearAll() → navigate('/normal-run')
```

### PATCH 요청 비교

| | FreeRun | NormalRun |
|---|---|---|
| **gpsSamples** | `[]` (미구현) | 실제 수집 배열 |
| **healthSamples** | `[]` (미구현) | 실제 수집 배열 |
| **splits** | `[]` (미구현) | 실제 수집 배열 |
| **실패 처리** | 재시도 / 로컬값으로 결과 진입 | store 초기화 후 홈으로 이동 |

---

## 5단계 — 결과 화면

### FreeRun: `FreeRunResultScreen`

- `result`: `onFinish(result)` props로 전달받음 (store 없음)
- `runningSessionId`: `FreeRunSessionPage`에서 props로 전달
- `courseId`: 코스 등록 성공 후 `onCourseRegistered(courseId)`로 업데이트

**섹션 구성:**

| 섹션 | 구현 상태 |
|---|---|
| 거리 (대형 표시) | 완료 |
| 시간 / 페이스 / 칼로리 | 완료 |
| 지도 (러닝 경로) | **미구현** (gpsSamples 필요) |
| 심박수 (최소/평균/최대) | **mock** (healthSamples 필요) |
| 구간별 페이스 테이블 | **mock** (`useGeneratePaceData`로 생성) |
| 코스 등록 버튼 | 완료 → `CourseShareModal` |
| 홈으로 | 완료 → `/free-run` navigate |

### NormalRun: `NormalRunResultScreen`

- `result`: `useNormalRunSessionStore.result` (store에서 읽음)
- 실제 `healthSamples` 기반 심박수 표시
- 실제 `gpsSamples` 기반 경로 지도 표시 (`NormalRunResultMap`)
- 코스 등록 버튼 **없음** (이미 등록된 코스를 달렸으므로)

---

## 코스 등록 (`CourseShareModal`) — FreeRun 전용

```
"코스 등록" 버튼 클릭
  → CourseShareModal 열림
       │
       ├─ GET /meta/course/register  (useCourseSurveys)
       │    응답: GetCourseRegisterMetaResponse
       │      courseTypes: [{ data: "RIVERSIDE", label: "강변" }, ...]
       │      difficulties: [{ data: "EASY", label: "쉬움" }, ...]
       │      mode: [{ data: "COMMUNITY", label: "커뮤니티 코스" },
       │             { data: "PRIVATE",   label: "개인 코스" }]
       │
       └─ 등록 버튼 클릭
            → POST /running-sessions/{runningSessionId}/register-course
                 요청: { title, difficulty, courseTypes, mode }
                 성공 → onCourseRegistered(courseId)
                      → 버튼 "등록 완료" 상태로 변경 (disabled)
```

> **왜 FreeRun에만 코스 등록이 있는가:**
> NormalRun은 기존 코스를 달리는 것이므로 새 코스 등록이 불필요하다.
> FreeRun은 경로가 없으므로 러닝 후 공식 코스로 남기고 싶은 경우에만 선택적으로 등록한다.

---

## 전체 흐름 요약 비교

```
FreeRun                                  NormalRun
──────────────────────────────────────   ──────────────────────────────────────
[시작 전]                                [코스 선택]
/free-run                                /normal-run
  지도 + 캐릭터 표시                         코스 목록 + 필터 표시
                                         ↓
                                         /normal-run/:courseId
                                           코스 상세 지도 + 정보 표시
                                           출발지 근접 시 시작 버튼 활성화

[세션 생성] ← 두 모드 모두 이 시점에서 POST
  GPS 확보 (FreeRun: Bridge or browser)  GPS 확보 (NormalRun: Bridge 필수)
  POST /running-sessions                 POST /running-sessions
    mode: 'FREE'                           mode: 'COURSE', courseId
  → navigate state로 ID 전달             → store.setSession() 후 navigate

[러닝 중]
/free-run-session                        /normal-run-session/:courseId
  단색 배경                                카카오 지도 오버레이 (코스 경로 표시)
  useFreeRunning() — 현재값만 유지         useNormalRunning() — 배열 누적 수집
  PATCH 실패: 재시도 / 로컬값 결과 진입    PATCH 실패: 홈으로 이동
  비정상 종료 처리: 없음                   비정상 종료: cleanup fire-and-forget

[결과]
  props로 result 전달                     store에서 result 읽음
  gpsSamples/healthSamples: mock         gpsSamples/healthSamples: 실제 데이터
  코스 등록 가능 (CourseShareModal)       코스 등록 없음
```

---

## 겹치는 부분 (공유 로직)

| 항목 | 공유 방식 |
|---|---|
| **세션 생성 API** | `createRunningSession()` — 동일 함수, `mode` 필드로 분기 |
| **세션 완료 API** | `completeRunningSession()` — 동일 함수 |
| **Bridge 데이터** | `useBridgeDataStore` — 동일 store |
| **GPS 위치** | `gpsLocation` — 동일 타입 |
| **일시정지/재개 UI** | 동일 패턴 (일시정지 시 종료 버튼 슬라이드) |

---

## 미구현 항목 (FreeRun)

### 러닝 중 데이터 수집

NormalRun의 `useNormalRunning`처럼 배열로 누적 수집하는 로직이 필요하다.

```
useFreeRunning() 안에 추가 필요
  gpsSamples: GpsSample[]      ← gpsLocation 변경마다 push
  healthSamples: HealthSample[] ← watchHealthData 변경마다 push
  splits: SplitData[]           ← 1km 달성마다 push
```

### 비정상 종료 처리

NormalRun처럼 `useEffect` cleanup에 fire-and-forget PATCH 추가 필요:

```ts
// FreeRunningScreen에 추가 필요
useEffect(() => {
  return () => {
    // 러닝 중 이탈 시 세션 강제 종료
  };
}, []);
```

### 결과 화면

- 지도에 러닝 경로 폴리라인 (`gpsSamples` 필요)
- 심박수 실제 데이터 (`healthSamples` 필요)
- 구간별 페이스 실제 데이터 (`splits` 필요)

### 인증

`MOCK_USER_ID = 1` → auth context 실제 유저 ID 교체 필요
- `FreeRunScreen.tsx`
- `FreeRunningScreen.tsx`
