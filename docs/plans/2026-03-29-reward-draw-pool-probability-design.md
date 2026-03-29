# Reward Draw Pool Probability Design

## 배경

- 현재 `reward_items`는 제목/이미지/활성 여부를 관리하는 카탈로그 책임만 가진다.
- 기존 문서에서도 확률, 티어, 재고, 운영 노출 정책은 `reward_items`에서 분리해야 한다고 정리되어 있다.
- `was`에는 아직 draw 실행, inventory, result 저장 모델이 없고, 포인트는 조회/지급만 먼저 구현되어 있다.

## 문제 정의

아이템 뽑기에는 각 아이템별 확률이 필요하지만,
그 확률을 `reward_items`에 직접 저장하면 카탈로그와 운영 설정이 결합된다.

이 구조는 아래 문제를 만든다.

- 같은 아이템을 시즌/이벤트마다 다른 확률로 운영하기 어렵다.
- showcase/public item API와 draw 운영 규칙이 섞인다.
- 이후 재고, 이벤트 풀, 내부 운영 정책을 추가할 때 엔터티 책임이 흐려진다.

## 목표

이번 단계에서는 draw 전체를 한 번에 구현하지 않고,
**확률을 안전하게 저장·운영할 수 있는 백엔드 기반 구조**를 먼저 만든다.

구체적으로는 아래를 목표로 한다.

- `reward_items`와 분리된 draw pool 모델 추가
- pool 단위의 활성화 정책 추가
- pool 안에서 item별 weight 기반 확률 설정 추가
- 관리자용 생성/조회/수정/활성화 API 추가
- 현재 활성 pool 조회 API 추가
- 관련 문서, 스키마 문서, 테스트 추가

## 범위 포함

- `reward_draw_pools` 테이블 설계
- `reward_draw_pool_items` 테이블 설계
- 관리자 API
  - pool 생성
  - pool 목록 조회
  - pool 단건 조회
  - pool 수정
  - pool 활성화
  - 현재 활성 pool 조회
- weight 유효성 검증
- soft delete 제외 기본 조회 규칙
- 관리자 응답 DTO
- `docs/plans` 설계/구현 문서

## 범위 제외

- 실제 가챠 draw 실행
- weighted random 추첨 로직
- 포인트 차감 트랜잭션
- draw result 저장
- inventory API
- 사용자 공개용 확률 노출 API
- 재고/품절 정책

## 선택한 설계

### 1. 카탈로그와 운영 설정 분리

- `reward_items`: 아이템 메타데이터 카탈로그
- `reward_draw_pools`: 운영 가능한 draw pool 헤더
- `reward_draw_pool_items`: pool 내부 item weight 매핑

즉, 확률은 “아이템 자체의 속성”이 아니라
“특정 pool 안에서 그 아이템이 어떤 비중으로 등장하는가”로 모델링한다.

### 2. percentage 대신 raw weight 사용

확률값은 소수점 percentage가 아니라 `weight` 정수로 저장한다.

이 방식의 장점:

- 운영자가 합계 100을 강제로 맞출 필요가 없다.
- 아이템 추가/삭제 시 재계산 부담이 줄어든다.
- 추후 draw 실행 시 전체 weight 합 기반으로 쉽게 확률 계산이 가능하다.

예시:

- A: 60
- B: 30
- C: 10

총합 100이 아니어도 무방하며,
실제 추첨 시 전체 weight 합 대비 비율로 계산한다.

### 3. 활성 pool 단일 정책

초기 단계에서는 **동시에 활성화 가능한 pool은 하나만 허용**한다.

이유:

- 아직 사용자 draw API가 없다.
- 운영 복잡도를 낮추고 draw 실행 설계와 맞물리는 기준점을 먼저 고정할 수 있다.
- 이후 시즌/캠페인 요구가 생기면 active window/versioning으로 확장할 수 있다.

활성화 시에는:

- 대상 pool을 active=true 로 변경
- 기존 active pool들은 active=false 로 일괄 전환

### 4. pool item 최소 필드

`reward_draw_pool_items`는 아래 필드만 가진다.

- `reward_draw_pool_item_id`
- `reward_draw_pool_id`
- `reward_item_id`
- `weight`
- `created_at`
- `updated_at`
- `deleted_at`

이번 단계에서는 아래는 넣지 않는다.

- rarity
- stock
- exposedProbability
- sortOrder
- event window

## API 방향

관리자 전용 경로를 사용한다.

- `POST /api/admin/reward-draw-pools`
- `GET /api/admin/reward-draw-pools`
- `GET /api/admin/reward-draw-pools/{rewardDrawPoolId}`
- `PATCH /api/admin/reward-draw-pools/{rewardDrawPoolId}`
- `POST /api/admin/reward-draw-pools/{rewardDrawPoolId}/activate`
- `GET /api/admin/reward-draw-pools/active`

`GET /active`는 현재 운영 기준 pool을 빠르게 검증하기 위한 관리자 조회 API다.
사용자 공개 API로 두지 않는다.

## 요청/응답 계약 원칙

### 생성/수정 요청

- `name`: pool 이름
- `items`: `{ rewardItemId, weight }[]`

검증 규칙:

- 이름은 공백 불가
- item 목록은 비어 있으면 안 된다
- 동일 rewardItemId 중복 불가
- weight는 양수여야 한다
- 참조하는 reward item은 soft delete 제외 기준으로 존재해야 한다

### 응답

pool 응답은 아래를 포함한다.

- `rewardDrawPoolId`
- `name`
- `isActive`
- `items`
  - `rewardDrawPoolItemId`
  - `rewardItemId`
  - `title`
  - `imageUrl`
  - `weight`
- `createdAt`
- `updatedAt`

관리자 화면에서 식별과 검증이 쉬우면서,
추후 draw 실행 로직이 어떤 item set을 참조하는지도 명확해진다.

## 도메인 구조

```text
was/src/main/java/kr/withrun/was/domain/reward/
├── controller/
│   └── RewardDrawPoolAdminController.java
├── dto/
│   ├── CreateRewardDrawPoolRequest.java
│   ├── UpdateRewardDrawPoolRequest.java
│   ├── RewardDrawPoolItemRequest.java
│   ├── RewardDrawPoolItemResponse.java
│   ├── RewardDrawPoolResponse.java
│   └── RewardDrawPoolListResponse.java
├── entity/
│   ├── RewardDrawPool.java
│   └── RewardDrawPoolItem.java
├── repository/
│   ├── RewardDrawPoolRepository.java
│   └── RewardDrawPoolItemRepository.java
└── service/
    └── RewardDrawPoolAdminService.java
```

## 테스트 전략

### repository

- active pool 조회가 soft delete 제외 기준으로 동작하는지
- pool item 목록이 pool 기준으로 정상 조회되는지

### service

- pool 생성 시 item mapping 이 저장되는지
- weight/중복 item/빈 목록 검증이 동작하는지
- 단건 조회 시 item imageUrl 이 client-facing URL로 정규화되는지
- 활성화 시 기존 active pool이 비활성화되는지

### controller

- 생성/목록/단건/수정/활성화/active 조회가 `ApiResponse` 래퍼로 감싸지는지
- response shape 확인

## 후속 확장 방향

이 구조 위에서 다음 단계를 자연스럽게 추가할 수 있다.

- draw execution service
- weighted random selector
- point deduction transaction
- draw result persistence
- inventory linkage
- 이벤트/시즌별 multi-pool 운영

## 이번 설계의 의의

이번 단계는 “확률을 어디에 둘 것인가”를 안전하게 닫는 작업이다.

핵심 결정은 아래 한 줄로 요약된다.

> `reward_items`는 카탈로그이고, 확률은 `reward_draw_pool_items.weight`가 가진다.
