# Reward Gacha Draw API Design

## 문서 목적

이 문서는 실제 사용자 가챠 실행 API와,
운영자가 확률표를 어떻게 입력해야 하는지의 최소 기준을 정리한다.

이번 범위는 아래를 닫는다.

- 사용자용 draw 실행 API
- 서버가 한 번의 draw 에서 전체 카드 결과를 미리 확정하는 규칙
- 관리자 운영 입력 순서
- pool 설정값(`cardsPerDraw`, `missWeight`, item `weight`)의 의미

## 핵심 원칙

### 1. 서버가 정답을 먼저 확정한다

- 사용자가 `뽑기 시작`을 누르면 서버는 그 draw 의 전체 카드 결과를 즉시 결정한다.
- 클라이언트는 이후 카드를 어떤 순서로 뒤집을지와 애니메이션만 담당한다.
- 즉, 서버는 정답 생성기이고 클라이언트는 연출기다.

### 2. 1회 API 호출은 카드 1장이 아니라 카드 세트 전체를 반환한다

- `POST /api/reward-gacha/draw`
- 요청 바디는 두지 않는다.
- 응답은 `cards[]` 전체를 포함한다.

### 3. 확률은 item 이 아니라 pool 설정이 가진다

- `reward_items`: 보상 카탈로그 메타데이터
- `reward_draw_pool_items.weight`: 각 보상 아이템의 raw weight
- `reward_draw_pools.miss_weight`: 꽝 raw weight
- `reward_draw_pools.cards_per_draw`: 한 번의 draw 에서 생성할 카드 수

## 관리자 운영 플로우

운영자가 실제로 확률표를 반영하는 순서는 아래와 같다.

1. `POST /api/admin/reward-items`
   - 보상 아이템 제목과 이미지를 먼저 등록한다.
2. `POST /api/admin/reward-draw-pools` 또는 `PATCH /api/admin/reward-draw-pools/{id}`
   - `cardsPerDraw` 입력
   - `missWeight` 입력
   - 각 item 의 `weight` 입력
3. `POST /api/admin/reward-draw-pools/{rewardDrawPoolId}/activate`
   - 실제 운영 pool 로 전환한다.

즉, 아이템 생성과 확률 입력은 분리된다.

## 관리자 입력 예시

### 1. 아이템 생성

예: 스타벅스 기프티콘, 배달의민족 상품권, 아메리카노 쿠폰을 먼저 만든다.

이 단계에서는 `title`, `imageUrl`만 관리한다.
확률은 아직 넣지 않는다.

### 2. pool 생성

예시 요청:

```json
{
  "name": "런칭 메인 풀",
  "cardsPerDraw": 3,
  "missWeight": 40,
  "items": [
    { "rewardItemId": 1, "weight": 30 },
    { "rewardItemId": 2, "weight": 20 },
    { "rewardItemId": 3, "weight": 10 }
  ]
}
```

이 뜻은 아래와 같다.

- 한 번 뽑을 때 카드 3장을 만든다.
- 꽝 비중은 40이다.
- 아이템 1, 2, 3은 각각 30, 20, 10 비중으로 참여한다.

실제 전체 weight 합은 `40 + 30 + 20 + 10 = 100`이다.

여기서 중요한 점은 합을 꼭 100으로 맞출 필요는 없다는 것이다.
예를 들어 `400, 300, 200, 100`처럼 넣어도 같은 비율이면 동일하게 동작한다.

## draw API 계약

### Endpoint

`POST /api/reward-gacha/draw`

### 인증

- 로그인 사용자만 호출 가능하다.

### 요청

- request body 없음

### 성공 응답 예시

```json
{
  "success": true,
  "code": "S001",
  "message": "요청이 성공했습니다.",
  "data": {
    "rewardGachaDrawId": 500,
    "spentPoint": 1,
    "remainingBalance": 6,
    "cards": [
      {
        "rewardGachaDrawCardId": 700,
        "cardIndex": 1,
        "cardType": "REWARD",
        "rewardItemId": 100,
        "title": "스타벅스 기프티콘",
        "imageUrl": "https://cdn.example.com/reward-item/100.png"
      },
      {
        "rewardGachaDrawCardId": 701,
        "cardIndex": 2,
        "cardType": "MISS",
        "rewardItemId": null,
        "title": null,
        "imageUrl": null
      },
      {
        "rewardGachaDrawCardId": 702,
        "cardIndex": 3,
        "cardType": "REWARD",
        "rewardItemId": 101,
        "title": "배달의민족 상품권",
        "imageUrl": "https://cdn.example.com/reward-item/101.png"
      }
    ],
    "createdAt": "2026-03-30T10:00:00"
  }
}
```

## draw 계산 규칙

현재 MVP 기준은 아래처럼 둔다.

- 카드마다 독립적으로 추첨한다.
- 추첨은 `with replacement` 방식이다.
- 즉, 같은 draw 안에서 같은 보상 아이템이 여러 번 나올 수 있다.

이 방식은 현재 문서와 구조에서 가장 단순하고 예측 가능하다.

## 포인트 처리 규칙

- draw 1회당 1포인트를 즉시 차감한다.
- 차감 이력은 `reward_point_histories.reason = GACHA_DRAW`로 기록한다.
- 현재 잔액은 `reward_point_balances`에서 집계 상태로 관리한다.

## 현재 범위에 포함하지 않는 것

- 인벤토리 API
- draw 재시도권
- pity / 천장 / 누적 보정
- 재고 정책
- rarity 기반 정렬

## 요약

운영자는 아래만 기억하면 된다.

1. 아이템은 먼저 만든다.
2. 확률은 item 이 아니라 pool 에서 넣는다.
3. `cardsPerDraw`는 카드 수,
   `missWeight`는 꽝 비중,
   각 item `weight`는 보상 비중이다.
4. 서버는 draw 시점에 카드 전체 결과를 이미 확정해서 내려준다.
