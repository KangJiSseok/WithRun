# 좋아요 / 북마크 상태 관리 리팩토링

**작업일**: 2026-03-30  
**브랜치**: `feat-web/bookmarked-course`  
**작업자**: danny

---

## 1. 배경 및 문제

### 1-1. 백엔드 API 변경

백엔드 `NearbyCourseItemResponse`에 두 개의 필드가 새로 추가됨:

```java
// was/.../dto/NearbyCourseItemResponse.java
boolean isLiked,      // 현재 사용자의 좋아요 여부
boolean isBookmarked, // 현재 사용자의 북마크 여부
```

기존에는 nearby 리스트에서 `likeCount`, `bookmarkCount`(숫자)만 내려왔으나, 이제 **현재 로그인 사용자 기준의 토글 상태**도 함께 내려오게 됨.

### 1-2. 프론트 기존 구조의 문제

기존 프론트엔드는 다음과 같은 구조를 사용:

```
Zustand Store (Set<number>)
 ├── useLikeStore.likedIds: Set<number>     → has(id) = true면 좋아요한 상태
 └── useBookmarkStore.savedIds: Set<number> → has(id) = true면 저장한 상태
```

**문제점**:

1. **프론트 `CourseItem` 타입에 `isLiked`/`isBookmarked` 필드 없음** — 백엔드가 내려주는데 타입이 안 맞음
2. **`Set` 자료구조의 한계** — `has()`가 `false`를 반환하면 "사용자가 좋아요를 취소한 것"인지 "아직 초기화되지 않은 것"인지 구분 불가
3. **시드(seed) 타이밍 문제** — 서버 데이터로 스토어를 초기화하면, React Query 캐시의 오래된 데이터가 사용자의 낙관적 업데이트를 덮어쓰는 현상 발생

---

## 2. 시도 과정

### 2-1. 1차: 서버 데이터로 Zustand 시드

nearby 데이터를 fetch한 뒤 `useEffect`로 스토어를 초기화:

```ts
// useNormalRunMapCourses.ts (1차 시도)
useEffect(() => {
  data?.items.forEach((item) => {
    setLiked(item.courseId, item.isLiked);
    addBookmark(item.courseId);
  });
}, [data]);
```

**결과**: 좋아요를 누르고 페이지 이동 → 복귀하면 React Query **캐시(오래된 데이터)**로 스토어를 다시 시드 → 좋아요가 풀린 것처럼 보임 ❌

### 2-2. 2차: `invalidateQueries`로 캐시 무효화

토글 성공 후 nearby 쿼리도 invalidate:

```ts
void queryClient.invalidateQueries({ queryKey: ['nearbyCourses'] });
```

**결과**: 현재 보고 있는 페이지에서 즉시 refetch → 서버 응답이 다시 시드 → **버튼이 깜빡임** ❌

### 2-3. 3차: `refetchType: 'none'`으로 stale 표시만

```ts
void queryClient.invalidateQueries({ queryKey: ['nearbyCourses'], refetchType: 'none' });
```

**결과**: 깜빡임은 해결되었으나, 페이지 복귀 시 stale된 캐시가 다시 시드 → **근본 문제 동일** ❌

### 2-4. 근본 원인 파악

> **"서버 데이터로 클라이언트 스토어를 시드하는 것" 자체가 설계 결함**

서버 데이터 시드와 낙관적 업데이트는 본질적으로 충돌함. React Query 캐시는 "마지막 fetch 시점의 서버 상태"이지 "현재 클라이언트 상태"가 아님.

---

## 3. 최종 해결: 오버라이드 패턴

### 3-1. 핵심 아이디어

```
서버 데이터 = 기본값 (baseline)
Zustand    = 사용자가 직접 토글한 오버라이드만 저장
```

- **서버 데이터를 Zustand에 시드하지 않음**
- 화면에서 값을 읽을 때: `오버라이드가 있으면 오버라이드, 없으면 서버 값`

### 3-2. 데이터 흐름

```
서버 API 응답 (isLiked: false)
        ↓
  오버라이드 존재 여부 확인 (Map.get(courseId))
        ↓
  ┌─ 있음 → 오버라이드 값 사용 (예: true)
  └─ 없음 → 서버 값 사용 (예: false)
        ↓
     UI 렌더링
```

### 3-3. 구조 비교

| | Before | After |
|---|---|---|
| **스토어 타입** | `Set<number>` | `Map<number, boolean>` |
| **상태 판별** | `has(id)` — 초기화 안 됨 = false | `get(id)` — `undefined` = "오버라이드 없음" |
| **시드 로직** | 서버 데이터 fetch마다 시드 | **시드 없음** |
| **읽기 방식** | `likedIds.has(courseId)` | `isLiked(courseId, course.isLiked)` |
| **페이지 복귀** | 캐시가 스토어 덮어씀 ❌ | 오버라이드 유지 ✅ |

### 3-4. 동작 시나리오

```
1. nearby 화면 진입 → 서버: isLiked=false → UI: ♡ (서버 기본값)
2. 사용자가 ❤️ 클릭 → Map에 {courseId: true} 저장 → UI: ❤️ (즉시 반영)
3. 다른 화면으로 이동 → Map 유지 (Zustand는 SPA 전역)
4. nearby 화면 복귀 → 서버 캐시: isLiked=false이지만
   → Map에 override=true가 있으므로 → UI: ❤️ (정확!)
5. 캐시 만료 → refetch → 서버: isLiked=true → Map override 무관하게 정확
```

---

## 4. 변경 파일 상세

### 4-1. Zustand 스토어

#### `stores/Like/useLikeStore.ts` [NEW]

```ts
// Before
likedIds: Set<number>        // has() → true/false, 초기화 구분 불가

// After
likeOverrides: Map<number, boolean>  // get() → true/false/undefined
```

- `setLiked(courseId, isLiked)` → Map에 오버라이드 기록
- `setCount(courseId, count)` → 좋아요 수 캐싱 (기존과 동일)

#### `stores/Bookmark/useBookmarkStore.ts` [NEW]

```ts
// Before
savedIds: Set<number>
add(courseId) / remove(courseId)

// After
bookmarkOverrides: Map<number, boolean>
setBookmarked(courseId, isBookmarked)
```

---

### 4-2. 토글 훅

#### `shared/hooks/useLikeToggle.ts` [NEW]

**변경된 API**:

```ts
// Before
const { likedIds, getLikeCount, toggle } = useLikeToggle();
toggle(courseId, fallbackCount);

// After
const { isLiked, getLikeCount, toggle } = useLikeToggle();
isLiked(courseId, serverIsLiked);                         // resolver
toggle(courseId, serverIsLiked, fallbackCount);            // 서버 기본값 전달
```

- `isLiked(courseId, serverDefault)` — 오버라이드 > 서버 기본값 순으로 resolve
- `toggle()` — 내부에서 resolve 후 낙관적 업데이트 + API 호출
- 성공 시 `nearbyCourses`/`nearbyGhostCourses` 캐시를 `refetchType: 'none'`으로 stale 마킹

#### `shared/hooks/useBookmarkToggle.ts` [NEW]

```ts
// Before
const { savedIds, toggle } = useBookmarkToggle();
toggle(courseId);

// After
const { isBookmarked, toggle } = useBookmarkToggle();
isBookmarked(courseId, serverIsBookmarked);    // resolver
toggle(courseId, serverIsBookmarked);           // 서버 기본값 전달
```

---

### 4-3. API 타입

#### `apis/course/types.ts` [MODIFY]

```diff
 export type CourseItem = {
   ...
   likeCount: number;
   bookmarkCount: number;
+  isLiked: boolean;
+  isBookmarked: boolean;
 };
```

백엔드 `NearbyCourseItemResponse`와 동기화.

#### `apis/courseLike/apis.ts` + `types.ts` [MODIFY]

- 주석의 `/{userId}` 제거 — 실제 API는 토큰 기반 인증이므로 URL에 userId 없음

---

### 4-4. 데이터 fetch 훅 (시드 로직 제거)

#### `hooks/NormalRun/useNormalRunMapCourses.ts` [MODIFY]
#### `hooks/GhostRun/useGhostRunMapCourses.ts` [MODIFY]
#### `hooks/BookmarkedCourses/useBookmarkedCourses.ts` [MODIFY]

세 파일 모두에서 `useEffect`로 Zustand를 시드하던 로직을 **완전 제거**. 이제 데이터는 React Query 캐시에만 유지되고, 화면에서 resolver로 최종 상태를 결정.

---

### 4-5. 스크린 컴포넌트 (새 API 적용)

#### `features/NormalRun/NormalRunScreen.tsx` [MODIFY]

```tsx
// Before
isLiked: likedIds.has(course.courseId),
isSaved={savedIds.has(course.courseId)}
onToggleSave={toggleBookmark}
onToggleLike={() => toggleLike(course.courseId, course.likeCount)}

// After
isLiked: isLiked(course.courseId, course.isLiked),
isSaved={isBookmarked(course.courseId, course.isBookmarked)}
onToggleSave={() => toggleBookmark(course.courseId, course.isBookmarked)}
onToggleLike={() => toggleLike(course.courseId, course.isLiked, course.likeCount)}
```

#### `features/GhostRun/GhostRunScreen.tsx` [MODIFY]

NormalRunScreen과 동일한 패턴 적용.

#### `features/BookmarkedCourses/BookmarkedCoursesScreen.tsx` [MODIFY]

- `remove: removeBookmark` → `setBookmarked(courseId, false)`
- 좋아요 표시도 resolver 패턴으로 전환

---

### 4-6. 러닝 결과 화면 북마크 연결

#### `features/NormalRun/NormalRunResultScreen.tsx` [MODIFY]

- `disabled` 플레이스홀더 버튼 → `useBookmarkToggle`로 실제 동작 연결
- `courseId`는 `useNormalRunSessionStore`의 `session.courseId`에서 가져옴
- `courseId`가 `null`인 경우 (자유러닝 등) 버튼 자체를 숨김
- 저장 상태에 따라 아이콘 filled/outline + 텍스트 변경

---

## 5. 검증

- `npx tsc --noEmit` — 타입 에러 **0건** ✅
- 백엔드 `NearbyCourseItemResponse` 필드와 프론트 `CourseItem` 타입 **일치 확인** ✅
- `CourseLikeController` 엔드포인트 (`POST/DELETE /courses/{courseId}/like`)와 프론트 API 호출 경로 **일치 확인** ✅
