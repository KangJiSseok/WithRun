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

> **참고:** 지도의 GPS(`navigator.geolocation`)와 세션 생성용 GPS(Bridge or `navigator.geolocation`)는 별개로 동작한다.
> 지도는 시각적 추적용이고, 세션 생성은 시작 시점 좌표 1회 캡처용이다.

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
  ├─ error        → 에러 메시지 + 재시도 버튼
  └─ sessionReady → FreeRunningScreen
```

### 왜 `store.clear()`를 진입 시점에 호출하는가

결과 화면은 store에서 데이터를 읽어 표시하기 때문에, 러닝이 끝난 직후 store를 비우면 결과 화면이 표시할 데이터가 없어진다.
반대로 결과 화면을 떠날 때 비우려면 이탈 시점을 감지하는 로직이 별도로 필요해 복잡해진다.
따라서 **다음 세션 시작 시점**인 `FreeRunSessionPage` 마운트 시 비우는 것이 가장 자연스럽다.

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
  store.setRunningSessionId(runningSessionId)
  store.setMode(mode)                          ← mode도 함께 저장
```

### 스토어 상태 (세션 생성 후)

```
useRunningSessionStore {
  runningSessionId: 123,   ← POST 응답
  mode: 'FREE',            ← POST 응답 (이전에는 저장하지 않았으나 결과 화면 분기 등에서 필요)
  courseId: null,          ← FREE 모드에서는 null
  result: null,            ← 러닝 종료 후 PATCH 응답 시 채워짐
```

> **보완 이력:**
> 초기 구현에서는 `runningSessionId`만 store에 저장했다.
> 그러나 추후 COURSE/GHOST 모드 추가 시 결과 화면에서 모드별 UI 분기가 필요하므로
> `mode`도 함께 저장하도록 변경했다.
> store 필드명도 API 응답의 `mode`와 일치하도록 `runningMode` → `mode`로 통일했다.

### `useRunningSession` 내부 동작

```
마운트
  │
  ├─ calledRef.current === true → 즉시 return  (StrictMode 이중 마운트 방지)
  │
  ├─ gpsLocation (Bridge GPS) 존재
  │    └─ startSession(lat, lng) 즉시 호출
  │
  └─ gpsLocation 없음
       └─ setTimeout(5000)
            └─ 5초 후 navigator.geolocation.getCurrentPosition
                  ├─ 성공 → startSession(lat, lng)
                  └─ 실패 → setError('위치 권한 오류')

startSession(lat, lng)
  ├─ cancelled === true → return  (언마운트 후 응답 방지)
  ├─ calledRef.current = true
  ├─ POST /api/running-sessions
  │    ├─ 성공 → setRunningSessionId / setMode
  │    └─ 실패 → setError('서버 연결 실패')
  └─ finally → setSessionReady(true)

cleanup (언마운트 or effect 재실행)
  ├─ cancelled = true   (스탈 콜백 방지)
  └─ clearTimeout(timeout)  (타이머 중복 방지)
```

> **보완 이력 — `cancelled` 패턴 추가:**
> 초기 구현에서는 `calledRef`만으로 중복 호출을 방지했다.
> 그러나 GPS 5초 대기 중 언마운트가 발생하면, `getCurrentPosition` 자체는 취소 API가 없어
> 콜백이 언마운트된 컴포넌트에 `setState`를 호출하는 문제가 있었다.
> (`clearWatch`는 `watchPosition` 전용이며 `getCurrentPosition`에는 적용되지 않는다.)
> 이를 해결하기 위해 `cancelled` 플래그를 추가해 언마운트 후 상태 업데이트를 차단했다.

> **보완 이력 — 재시도 버튼 추가:**
> 초기 구현에서 POST 실패 시 에러 화면만 표시하고 재시도 수단이 없었다.
> 사용자가 홈으로 나갔다 다시 진입해야만 재시도할 수 있는 문제가 있어,
> `retry()` 함수를 추가했다. `calledRef`를 리셋하고 `retryCount`를 증가시켜
> effect를 재실행하는 방식이다.

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
| `calories` | `watchHealthData.caloriesKcal` | 워치 전용, 미연결 시 0 |
| `heartRate` | `watchHealthData.heartRate` | 워치 전용, 미연결 시 0 |
| `cadenceSpm` | 워치 연결: `watchHealthData.cadenceSpm` / 미연결: `gpsLocation.cadenceSpm` | 워치 우선, 가속도계 폴백 |

### Bridge에서 수신하는 타입

**`GpsLocation`** (1초마다):
| 필드 | 설명 |
|---|---|
| `latitude`, `longitude` | 위도/경도 |
| `accuracy` | 정확도 (m) |
| `altitude` | 고도 (m) → 추후 `elevationGainM` 계산에 활용 가능 |
| `heading` | 이동 방향 (도) |
| `speedMps` | GPS Doppler 속도 → 페이스 계산에 사용 |
| `cadenceSpm` | 모바일 가속도계 케이던스 → 워치 없을 때 폴백 |

**`WatchHealthData`** (워치에서 수신):
| 필드 | 설명 |
|---|---|
| `timestamp` | 수신 시각 |
| `heartRate` | 심박수 |
| `caloriesKcal` | 누적 칼로리 |
| `cadenceSpm` | 만보계 기반 케이던스 (가속도계보다 정확) |
| `distanceM` | 만보계 기반 누적 거리 |

### 워치 연결 판단

- `lastWatchMessageAt` 기준 3초 이내 메시지 수신 여부 → `isWatchConnected`
- 1초마다 체크

### 화면 구성

- 상단: 거리 / 페이스 / 칼로리 카드
- 중앙: 타이머 + 러닝 중 / 일시정지 배지
- 하단: 일시정지/재개 버튼 (항상), 종료 버튼 (일시정지 시 슬라이드)
- PATCH 실패 시: 재시도 / 결과 보기 버튼

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

> **참고 — `runningSessionId`를 path variable에 넣는 방법:**
> `useRunningSession`에서 POST 성공 시 `store.setRunningSessionId(res.data.runningSessionId)`로 저장하고,
> `FreeRunningScreen`에서 `useRunningSessionStore((s) => s.runningSessionId)`로 꺼내 사용한다.
> store가 두 컴포넌트 사이의 중간 다리 역할을 한다.

### 스토어 상태 (종료 후)

```
useRunningSessionStore {
  runningSessionId: 123,
  mode: 'FREE',
  courseId: null,
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
| 좋아요 버튼 | FREE 모드 미표시 / 그 외 완료 | `POST /api/courses/{id}/like` |
| 코스 등록 버튼 | 완료 | `CourseShareModal` |
| 홈으로 버튼 | 완료 | `/free-run`으로 navigate |

### 코스 등록 (`CourseShareModal`)

```
"코스 등록" 버튼
  → CourseShareModal 오픈
       ├─ GET /meta/course/register  ← 코스 타입/난이도/저장 유형 선택지
       │    응답: {
       │      courseTypes: [{ data: "RIVERSIDE", label: "강변" }, ...]
       │      difficulties: [{ data: "EASY", label: "쉬움" }, ...]
       │      mode: [{ data: "COMMUNITY", label: "커뮤니티 코스" },
       │             { data: "PRIVATE",   label: "개인 코스" }]
       │    }
       │    data: 서버 enum 값 (POST 요청 시 사용)
       │    label: 화면 표시 한국어 (하드코딩 맵핑 없이 그대로 렌더)
       └─ 등록 버튼
            → POST /running-sessions/{runningSessionId}/register-course
                 요청: {
                   title: string,
                   mode: "COMMUNITY" | "PRIVATE",
                   difficulty: "EASY" | "MEDIUM" | "HARD",
                   courseTypes: CourseType[],
                   snapshotImageUrl?: string,   ← 현재 미전송 (선택값)
                 }
                 응답: { courseId, ... }
                          ↓
                 store.setCourseId(courseId)
                 onClose() → CourseShareModal 닫힘
                 코스 등록 버튼 → "등록 완료" 상태로 변경 (disabled)
```

> **변경 이력:**
> 기존 "코스 저장" 버튼(로컬 토글만) 제거, "코스 등록" 버튼으로 통합.
> "공식 코스 등록" → "코스 등록"으로 명칭 변경.
> 좋아요 버튼은 FREE 모드 결과 화면에서 미표시 — 자유러닝 결과에는 좋아요 맥락이 없기 때문.
> 결과 화면은 FREE/GHOST/COURSE 모드 공용이며 `store.mode` 값으로 모드별 UI를 분기한다.
> survey API 엔드포인트 `/courses/surveys` → `/meta/course/register` 변경.
> 응답 필드 구조 변경: `{ index, label(enum값) }` → `{ data(enum값), label(한국어) }`.
> 한국어 맵핑 하드코딩(`COURSE_TYPE_LABEL`, `DIFFICULTY_LABEL`) 제거 — API `label` 값을 그대로 사용.
> 저장 유형(mode) 선택지도 survey API에서 동적으로 받도록 변경 (COMMUNITY / PRIVATE).

---

## 미구현 항목 (TODO)

### 러닝 중 데이터 수집

현재 `useFreeRunning()`은 최신값만 유지하고 이전 값은 버린다.
PATCH 요청의 `gpsSamples`, `healthSamples`, `splits`를 채우려면 배열로 누적 수집하는 로직이 필요하다.

```
useFreeRunning() 안에서 매 업데이트마다 배열에 쌓아야 함
  gpsSamples: GpsSample[]      ← gpsLocation 변경마다 push
    { latitude, longitude, altitude, speedMps, accuracyM, sampledAt }
  healthSamples: HealthSample[] ← watchHealthData 변경마다 push
    { heartRate, caloriesKcal, cadenceSpm, sampledAt }
  splits: SplitData[]           ← 1km 달성마다 push
    { splitIndex, splitDistanceM, splitDurationSec, splitPaceSecPerKm }
```

`altitude`는 이미 `GpsLocation`에 포함되어 있으므로 `elevationGainM` 누적도 같은 맥락에서 구현 가능하다.

### 결과 화면

- 지도에 러닝 경로 폴리라인 표시 (`gpsSamples` 필요)
- 심박수 실제 데이터 (최소/평균/최대 계산, `healthSamples` 필요)
- 구간별 페이스 실제 데이터 (`splits` 필요)
- 고도 변화 (`elevationGainM`, `gpsSamples.altitude` 필요)

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
               store.clear()  ← 이전 세션 초기화 (진입 시점이 가장 자연스러운 타이밍)
               GPS 확보 (Bridge or navigator.geolocation 5초 폴백)
               POST /api/running-sessions → runningSessionId, mode → store
               실패 시 → 에러 화면 + 재시도 버튼

[러닝 중]    Bridge(GPS + 워치) → useFreeRunning() → 화면 실시간 렌더
               타이머 / 거리 / 페이스 / 칼로리
               일시정지/재개 가능

[종료]       종료 버튼 (일시정지 상태에서만 노출)
               PATCH /api/running-sessions/{id}
               성공: 서버 result → store
               실패: 재시도 또는 로컬 계산값으로 결과 화면 진입

[결과]       store.result 읽어서 표시
               좋아요 버튼: FREE 모드 미표시, 그 외 표시
               코스 등록: CourseShareModal → 개인/커뮤니티 선택 → POST → courseId → store
               홈으로: /free-run navigate
```
