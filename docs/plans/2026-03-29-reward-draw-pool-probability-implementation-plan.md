# Reward Draw Pool Probability Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `was`에 reward item 카탈로그와 분리된 draw pool/weight 관리 기능을 추가해 관리자 기준으로 확률 운영 기반을 닫는다.

**Architecture:** `reward_items`는 그대로 카탈로그로 유지하고, 새 `reward_draw_pools` / `reward_draw_pool_items` 구조로 pool별 item weight를 관리한다. 이번 단계에서는 draw 실행 없이 관리자 CRUD/활성화/활성 pool 조회까지만 구현하고, 후속 draw/inventory/result가 이 pool을 소비하도록 남긴다.

**Tech Stack:** Spring Boot 3.5, Spring Data JPA, Java 21, H2/JPA repository tests, MockMvc controller tests

---

### Task 1: 문서와 스키마 계약 추가

**Files:**
- Create: `docs/plans/2026-03-29-reward-draw-pool-probability-design.md`
- Create: `docs/plans/2026-03-29-reward-draw-pool-probability-implementation-plan.md`
- Modify: `DB/schema.sql`
- Modify: `DB/SCHEMA_GUIDE.md`

**Step 1: reward draw pool 설계 문서 작성**

- pool/pool item 분리 책임
- raw weight 사용 이유
- active pool 단일 정책
- 이번 범위 포함/제외

**Step 2: DB schema contract 반영**

- `reward_draw_pools`
- `reward_draw_pool_items`

**Step 3: SCHEMA_GUIDE 반영**

- 각 테이블 목적, 핵심 컬럼, 제약, 비고 작성

**Step 4: 문서 검토**

- 기존 reward 문서 톤/구조와 맞는지 확인

### Task 2: repository red 테스트 작성

**Files:**
- Create: `was/src/test/java/kr/withrun/was/domain/reward/repository/RewardDrawPoolRepositoryTest.java`
- Create: `was/src/test/java/kr/withrun/was/domain/reward/repository/RewardDrawPoolItemRepositoryTest.java`

**Step 1: active pool 조회 테스트 작성**

- active=true + deletedAt=null pool만 조회
- 생성 시점이 달라도 active 기준으로 조회

**Step 2: pool item 조회 테스트 작성**

- 특정 pool 기준 item 목록 반환
- soft delete 제외 확인

**Step 3: 테스트 실행해 실패 확인**

Run: `cd was && ./gradlew.bat test --tests "kr.withrun.was.domain.reward.repository.RewardDrawPoolRepositoryTest" --tests "kr.withrun.was.domain.reward.repository.RewardDrawPoolItemRepositoryTest"`

Expected: repository/entity 미구현으로 FAIL

### Task 3: service/controller red 테스트 작성

**Files:**
- Create: `was/src/test/java/kr/withrun/was/domain/reward/service/RewardDrawPoolAdminServiceTest.java`
- Create: `was/src/test/java/kr/withrun/was/domain/reward/controller/RewardDrawPoolAdminControllerTest.java`

**Step 1: service 테스트 작성**

- pool 생성 성공
- 빈 item 목록 실패
- 중복 rewardItemId 실패
- 잘못된 weight 실패
- 단건 조회 시 imageUrl 정규화
- 활성화 시 기존 active pool 비활성화

**Step 2: controller 테스트 작성**

- POST create
- GET list
- GET detail
- PATCH update
- POST activate
- GET active

**Step 3: 테스트 실행해 실패 확인**

Run: `cd was && ./gradlew.bat test --tests "kr.withrun.was.domain.reward.service.RewardDrawPoolAdminServiceTest" --tests "kr.withrun.was.domain.reward.controller.RewardDrawPoolAdminControllerTest"`

Expected: DTO/service/controller 미구현으로 FAIL

### Task 4: 엔터티/리포지토리 최소 구현

**Files:**
- Create: `was/src/main/java/kr/withrun/was/domain/reward/entity/RewardDrawPool.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/entity/RewardDrawPoolItem.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/repository/RewardDrawPoolRepository.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/repository/RewardDrawPoolItemRepository.java`

**Step 1: entity 작성**

- pool: name, active
- pool item: pool, reward item, weight

**Step 2: repository 메서드 작성**

- active pool 조회
- pool 목록 조회
- pool item 목록 조회
- active pool 일괄 비활성화 update

**Step 3: repository 테스트 실행**

Run: repository 대상 테스트 재실행

Expected: PASS

### Task 5: DTO/service/controller 구현

**Files:**
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/CreateRewardDrawPoolRequest.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/UpdateRewardDrawPoolRequest.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/RewardDrawPoolItemRequest.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/RewardDrawPoolItemResponse.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/RewardDrawPoolResponse.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/RewardDrawPoolListResponse.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/service/RewardDrawPoolAdminService.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/controller/RewardDrawPoolAdminController.java`
- Modify: `was/src/main/java/kr/withrun/was/global/response/ResponseCode.java`

**Step 1: request/response DTO 작성**

- pool 기본 정보 + item weight 응답

**Step 2: service 작성**

- create/list/detail/update/activate/getActive
- reward item 존재 검증
- request 유효성 검증
- imageUrl 정규화

**Step 3: controller 작성**

- `/api/admin/reward-draw-pools`
- `ApiResponse` 패턴 일치

**Step 4: service/controller 테스트 실행**

Run: service/controller 대상 테스트 재실행

Expected: PASS

### Task 6: 범위 검증

**Files:**
- Verify only

**Step 1: reward domain 테스트 실행**

Run: `cd was && ./gradlew.bat test --tests "kr.withrun.was.domain.reward.*"`

Expected: PASS

**Step 2: 전체 빌드 실행**

Run: `cd was && ./gradlew.bat build`

Expected: PASS

**Step 3: changed file diagnostics 확인**

- LSP diagnostics error 0건 확인

**Step 4: 범위 점검**

- draw 실행이 섞이지 않았는지
- probability가 `reward_items`에 추가되지 않았는지
- admin/current-active-pool까지만 구현됐는지 확인
