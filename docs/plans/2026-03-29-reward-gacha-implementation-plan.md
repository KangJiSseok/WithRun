# Reward Gacha Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 웹 앱에 리워드 가챠 MVP를 추가해 사용자가 포인트를 확인하고, 카드팩을 열고, 카드 뒤집기로 결과를 확인하며, 인벤토리에서 획득 아이템을 다시 볼 수 있게 한다.

**Architecture:** 화면 소유권은 `web/src/features/RewardGacha/`에 두고, `pages/`와 `routes/`는 얇은 연결 레이어만 맡긴다. 초기 구현은 문서화된 제품 규칙을 유지하면서, API 계약이 완전히 닫히기 전까지는 `web/src/apis/rewardGacha/`를 별도 seam 으로 만들어 UI 상태와 서버 계약을 느슨하게 분리한다.

**Tech Stack:** Vite, React 19, TypeScript, React Router 7, existing shadcn primitives, Tailwind v4 token bridge, Sonner, axios client

---

## 전제 및 제약

- 디자인 기준 문서:
  - `docs/plans/2026-03-28-reward-gacha-screen-reward-design.md`
  - `docs/plans/2026-03-29-reward-gacha-product-rules-design.md`
- 실제 화면 소유권은 `web/src/features/`에 둔다.
- 색상/간격/반경/그림자는 기존 토큰만 사용한다.
- `web/`에는 현재 테스트 러너가 설정되어 있지 않다. 각 Task 의 검증은 `pnpm lint`, `pnpm build`, 그리고 수동 QA 로 수행한다.
- 백엔드 API 명세가 아직 확정되지 않았으므로, 프론트는 별도 API seam 을 만들고 타입을 먼저 닫는다.

## Task 1: 라우트와 화면 소유권 뼈대 추가

**Files:**
- Create: `web/src/features/RewardGacha/RewardGachaScreen.tsx`
- Create: `web/src/features/RewardGacha/index.ts`
- Create: `web/src/pages/private/RewardGachaPage.tsx`
- Modify: `web/src/routes/privateRoutes.tsx`
- Modify: `web/src/features/Home/HomeScreen.tsx`

**Step 1: 뼈대 파일 추가**

- `RewardGachaScreen`는 빈 placeholder 가 아니라 제목, 설명, 추후 섹션 placeholder 를 가진 실제 화면 컴포넌트로 만든다.
- `RewardGachaPage`는 기존 `home.tsx`, `profile.tsx` 패턴처럼 feature 만 연결한다.
- `privateRoutes.tsx`에 `/reward-gacha` 경로를 `MainLayout showTopNav showBottomNav` 그룹에 추가한다.
- `HomeScreen.tsx`에는 가챠 화면으로 이동하는 진입 CTA 를 하나 추가한다.

**Step 2: 정적 검증 실행**

Run:

```bash
pnpm lint
pnpm build
```

Expected: 새 route / page / feature skeleton 이 lint 와 build 를 통과한다.

**Step 3: 수동 QA**

Run:

```bash
pnpm dev
```

Expected: `/home`에서 가챠 진입 CTA 가 보이고, 클릭하면 `/reward-gacha`로 이동해 placeholder 화면이 렌더링된다.

## Task 2: 메인 가챠 화면의 정적 레이아웃 구현

**Files:**
- Modify: `web/src/features/RewardGacha/RewardGachaScreen.tsx`
- Create: `web/src/features/RewardGacha/RewardPointPanel.tsx`
- Create: `web/src/features/RewardGacha/RewardCardStage.tsx`
- Create: `web/src/features/RewardGacha/RewardStartButton.tsx`
- Modify: `web/src/features/RewardGacha/index.ts`

**Step 1: 레이아웃 구현**

- 포인트 표시부와 인벤토리 버튼을 상단에 둔다.
- 중앙에는 미스터리 카드팩 중심 스테이지를 둔다.
- 하단에는 가장 강한 CTA 인 `뽑기 시작` 버튼을 둔다.
- `docs/plans/2026-03-28-reward-gacha-screen-reward-design.md`의 스타일 제약대로 raw hex/RGB 없이 토큰만 사용한다.

**Step 2: 상태 없는 정적 화면 검증**

Run:

```bash
pnpm lint
pnpm build
```

Expected: 화면 분해 컴포넌트가 정상 import/export 되고 type 오류가 없다.

**Step 3: 수동 QA**

Run:

```bash
pnpm dev
```

Expected: 모바일 폭에서 포인트 패널, 인벤토리 버튼, 카드팩, CTA 가 문서와 같은 위계로 보인다.

## Task 3: 카드팩 오픈과 카드 뒤집기 상태 머신 구현

**Files:**
- Modify: `web/src/features/RewardGacha/RewardGachaScreen.tsx`
- Create: `web/src/features/RewardGacha/types.ts`
- Create: `web/src/features/RewardGacha/mockData.ts`
- Create: `web/src/features/RewardGacha/RewardResultCard.tsx`

**Step 1: 상태 모델 작성**

- 최소 상태를 정의한다.
  - `idle`
  - `opening-pack`
  - `revealing-cards`
  - `completed`
- 결과 카드 타입에는 `MISS`, `REWARD` 정도의 최소 분류만 둔다.
- 꽝은 꽝 그대로 유지하고, 누적 보정 상태는 만들지 않는다.

**Step 2: 최소 구현 추가**

- `뽑기 시작` 클릭 시 CTA 가 사라지고 포인트 패널이 뒤로 물러난다.
- 카드팩 오픈 이후 카드 목록이 한 번에 나타난다.
- 사용자는 원하는 카드부터 뒤집을 수 있다.
- 열린 카드는 열린 상태로 유지한다.
- 모든 카드가 열린 뒤에만 `닫기` 버튼이 나타난다.
- `닫기`를 누르면 화면이 완전히 초기화된다.

**Step 3: 정적 검증 실행**

Run:

```bash
pnpm lint
pnpm build
```

Expected: 로컬 상태 머신과 카드 컴포넌트가 타입 오류 없이 통과한다.

**Step 4: 수동 QA**

Run:

```bash
pnpm dev
```

Expected:
- 시작 전에는 CTA 만 주요 액션으로 보인다.
- 시작 후 카드 수가 한 번에 나타난다.
- 카드는 임의 순서로 열 수 있다.
- 모든 카드 오픈 전에는 `닫기` 버튼이 보이지 않는다.

## Task 4: 인벤토리 보기 구현

**Files:**
- Create: `web/src/features/RewardGacha/RewardInventoryDialog.tsx`
- Modify: `web/src/features/RewardGacha/RewardGachaScreen.tsx`
- Modify: `web/src/features/RewardGacha/types.ts`
- Reuse: `web/src/shared/components/shadcn/dialog.tsx`

**Step 1: 인벤토리 UI 추가**

- 인벤토리 버튼 클릭 시 dialog 또는 sheet 형태의 오버레이를 연다.
- 꽝은 인벤토리에 포함하지 않는다.
- 아이템은 높은 등급 우선으로 정렬한다.
- 상세 텍스트를 길게 풀지 말고 큰 이미지 보기 중심으로 둔다.

**Step 2: 검증 실행**

Run:

```bash
pnpm lint
pnpm build
```

Expected: 인벤토리 오버레이가 기존 shadcn dialog 패턴과 충돌 없이 빌드된다.

**Step 3: 수동 QA**

Run:

```bash
pnpm dev
```

Expected: 인벤토리 버튼이 열리고, 꽝 제외 아이템만 보이며, 큰 이미지 보기 중심 UX 가 유지된다.

## Task 5: rewardGacha API seam 과 타입 계약 추가

**Files:**
- Create: `web/src/apis/rewardGacha/types.ts`
- Create: `web/src/apis/rewardGacha/apis.ts`
- Create: `web/src/apis/rewardGacha/index.ts`
- Modify: `web/src/apis/index.ts`
- Modify: `web/src/features/RewardGacha/types.ts`
- Modify: `web/src/features/RewardGacha/mockData.ts`

**Step 1: 타입 계약 정의**

- 최소 타입을 정의한다.
  - 포인트 잔액 조회 응답
  - 가챠 실행 응답
  - 인벤토리 조회 응답
  - 보상 카드 표현 타입
- 보상 카드 / 인벤토리 아이템 타입은 최소 `title`, `imageUrl` 필드를 포함한다.
- 아직 백엔드 명세가 미정이면, 현재 디자인 문서의 결정만 반영한 최소 필드만 둔다.

**Step 2: API 함수 추가**

- `getRewardGachaBalance`
- `drawRewardGacha`
- `getRewardInventory`

백엔드 미연동 단계에서는 실제 호출 경로를 TODO 로 비워두지 말고, 예상 엔드포인트를 명시한 seam 을 만든다.

**Step 3: 정적 검증 실행**

Run:

```bash
pnpm lint
pnpm build
```

Expected: 새 API barrel 과 타입 정의가 앱 전역 import 와 충돌하지 않는다.

## Task 6: 화면과 API seam 연결 및 최종 검증

**Files:**
- Modify: `web/src/features/RewardGacha/RewardGachaScreen.tsx`
- Modify: `web/src/features/RewardGacha/RewardPointPanel.tsx`
- Modify: `web/src/features/RewardGacha/RewardInventoryDialog.tsx`
- Modify: `web/src/features/RewardGacha/mockData.ts`
- Modify: `web/src/apis/rewardGacha/apis.ts`

**Step 1: 연결 구현**

- 포인트 패널은 API seam 또는 adapter 에서 받은 잔액 타입을 사용한다.
- 가챠 실행은 화면 로컬 mock 상태를 직접 뒤집는 대신 API seam 반환 타입을 기준으로 상태를 구성한다.
- 인벤토리도 동일하게 API 응답 타입을 기준으로 렌더링한다.

**Step 2: 최종 검증 실행**

Run:

```bash
pnpm lint
pnpm build
```

Expected: reward gacha 관련 추가 파일 전체가 정적 검증을 통과한다.

**Step 3: 최종 수동 QA**

Run:

```bash
pnpm dev
```

Expected:
- 홈에서 가챠 진입 가능
- 포인트 표시부와 인벤토리 버튼 동작
- 카드팩 오픈 → 카드 뒤집기 → 전체 오픈 후 닫기 순서 정상
- 화면 초기화 정상
- 꽝은 보정되지 않음

## 후속 백엔드 설계 필요 항목

- 포인트 적립/차감 API 확정
- 가챠 실행 결과 저장 모델 확정
- 인벤토리 조회 API 확정
- 보상 아이템 마스터 테이블(`title`, `imageUrl`) 설계
- 보상 아이템 등록 API(제목 + 이미지 업로드 → `imageUrl` 저장) 확정
- 운영 확률표/보상풀 관리 방식 확정

## 검증 명령 요약

```bash
cd web && pnpm lint
cd web && pnpm build
cd web && pnpm dev
```
