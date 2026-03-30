# Reward Item Admin Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** WAS 에 관리자용 보상 아이템 카탈로그를 추가해 제목과 이미지를 등록/조회/수정/비활성화할 수 있게 한다.

**Architecture:** `was/src/main/java/kr/withrun/was/domain/reward/`에 전용 도메인을 만들고, `reward_items` 엔터티가 카탈로그의 최소 책임(`title`, `imageUrl`, `isActive`)만 가진다. 이미지 저장은 기존 S3 + CloudFront signed/public URL 패턴을 재사용하고, 가챠 확률/운영 정책은 이번 작업 범위에서 분리한다.

**Tech Stack:** Spring Boot 3.5, Java 21, Spring Web, Spring Data JPA, Swagger/OpenAPI, AWS S3, CloudFront URL signing, JUnit 5, MockMvc

---

## Task 1: 스키마와 도메인 엔터티 골격 추가

**Files:**
- Create: `was/src/main/java/kr/withrun/was/domain/reward/entity/RewardItem.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/repository/RewardItemRepository.java`
- Create: `was/src/test/java/kr/withrun/was/domain/reward/repository/RewardItemRepositoryTest.java`
- Modify: `DB/schema.sql`
- Modify: `DB/SCHEMA_GUIDE.md`

**Step 1: 저장소 테스트 초안 작성**

- `RewardItemRepositoryTest`에서 아래를 먼저 검증한다.
  - 활성 아이템만 필터링해 조회할 수 있다.
  - soft delete 된 아이템은 기본 조회에서 제외된다.
  - 생성 시 `isActive=true` 기본값이 적용된다.

**Step 2: 실패 확인**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.repository.RewardItemRepositoryTest"
```

Expected: `RewardItem` / `RewardItemRepository` / 테이블 부재로 실패한다.

**Step 3: 최소 엔터티/리포지토리 구현**

- `RewardItem`은 `BaseEntity`를 상속한다.
- PK 는 `reward_item_id`, 필드는 `title`, `image_url`, `is_active`를 가진다.
- `updateTitle`, `updateImageUrl`, `activate`, `deactivate` 메서드를 둔다.
- `DB/schema.sql`에 `reward_items` 테이블을 추가한다.
- `DB/SCHEMA_GUIDE.md`에 테이블 목적, 핵심 컬럼, 제약을 반영한다.

**Step 4: 테스트 재실행**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.repository.RewardItemRepositoryTest"
```

Expected: 저장소 테스트가 통과한다.

## Task 2: 관리자 조회/수정 DTO 와 서비스 추가

**Files:**
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/RewardItemResponse.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/RewardItemListResponse.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/dto/UpdateRewardItemRequest.java`
- Create: `was/src/main/java/kr/withrun/was/domain/reward/service/RewardItemAdminService.java`
- Create: `was/src/test/java/kr/withrun/was/domain/reward/service/RewardItemAdminServiceTest.java`

**Step 1: 서비스 테스트 작성**

- 아래 유스케이스를 Mockito 기반 서비스 테스트로 먼저 작성한다.
  - 단건 조회 시 signed/public URL로 `imageUrl`이 변환된다.
  - 목록 조회 시 `active` 필터가 반영된다.
  - `PATCH`에서 `title` 또는 `isActive`가 변경된다.
  - soft deleted 또는 없는 아이템 수정 시 예외가 발생한다.

**Step 2: 실패 확인**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.service.RewardItemAdminServiceTest"
```

Expected: 서비스/DTO 미구현으로 실패한다.

**Step 3: 최소 서비스/DTO 구현**

- `RewardItemResponse`는 `rewardItemId`, `title`, `imageUrl`, `isActive`, `createdAt`, `updatedAt`를 가진다.
- `RewardItemListResponse`는 page 기반 응답 구조를 따른다.
- `UpdateRewardItemRequest`는 `title`, `isActive`를 optional 로 가진다.
- `RewardItemAdminService`는 목록/상세/메타데이터 수정 책임만 먼저 가진다.
- 응답 생성 시 `CloudFrontSignedUrlService`를 사용해 `imageUrl`을 정규화한다.

**Step 4: 테스트 재실행**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.service.RewardItemAdminServiceTest"
```

Expected: 서비스 테스트가 통과한다.

## Task 3: 이미지 저장 seam 추가

**Files:**
- Modify: `was/src/main/java/kr/withrun/was/domain/file/service/S3FileService.java`
- Create: `was/src/test/java/kr/withrun/was/domain/file/service/S3FileServiceRewardItemTest.java`
- Modify: `was/src/main/resources/application.yml` (필요 시 prefix/config 주석 보강만)

**Step 1: 업로드 테스트 초안 작성**

- reward item 이미지 업로드용 메서드에 대해 아래를 검증하는 테스트를 먼저 작성한다.
  - key prefix 가 `reward-item/`으로 생성된다.
  - reward item id 기반 파일명이 생성된다.
  - 빈 바이트 업로드는 허용하지 않는다.

**Step 2: 실패 확인**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.file.service.S3FileServiceRewardItemTest"
```

Expected: reward item 업로드 메서드 부재로 실패한다.

**Step 3: 최소 구현 추가**

- `S3FileService`에 reward item 전용 업로드 메서드를 추가한다.
- 반환값은 stored key 문자열로 유지한다.
- 기존 course snapshot 업로드 로직과 충돌하지 않게 별도 메서드로 분리한다.

**Step 4: 테스트 재실행**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.file.service.S3FileServiceRewardItemTest"
```

Expected: 업로드 테스트가 통과한다.

## Task 4: 관리자 생성/이미지 교체 유스케이스 추가

**Files:**
- Modify: `was/src/main/java/kr/withrun/was/domain/reward/service/RewardItemAdminService.java`
- Modify: `was/src/test/java/kr/withrun/was/domain/reward/service/RewardItemAdminServiceTest.java`

**Step 1: 생성/이미지 교체 테스트 추가**

- 아래 케이스를 서비스 테스트에 추가한다.
  - `title + file`로 아이템 생성 시 엔터티가 저장된다.
  - 이미지 업로드 성공 후 저장된 key 가 `imageUrl` 필드에 반영된다.
  - 이미지 교체 시 기존 아이템의 `imageUrl`이 갱신된다.
  - 파일이 비어 있으면 예외가 발생한다.

**Step 2: 실패 확인**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.service.RewardItemAdminServiceTest"
```

Expected: create/image replace 경로 미구현으로 실패한다.

**Step 3: 최소 구현 추가**

- create 유스케이스는 엔터티 저장 후 업로드 key 를 반영하는 흐름으로 구현한다.
- image replace 는 파일 업로드 성공 후에만 엔터티를 갱신한다.
- 공통 응답 DTO 매핑은 재사용한다.

**Step 4: 테스트 재실행**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.service.RewardItemAdminServiceTest"
```

Expected: 서비스 테스트가 통과한다.

## Task 5: 관리자 컨트롤러와 API 계약 추가

**Files:**
- Create: `was/src/main/java/kr/withrun/was/domain/reward/controller/RewardItemAdminController.java`
- Modify: `was/src/main/java/kr/withrun/was/global/response/ResponseCode.java`
- Create: `was/src/test/java/kr/withrun/was/domain/reward/controller/RewardItemAdminControllerTest.java`

**Step 1: 컨트롤러 테스트 작성**

- 아래 엔드포인트를 MockMvc 테스트로 먼저 작성한다.
  - `POST /api/admin/reward-items`
  - `GET /api/admin/reward-items`
  - `GET /api/admin/reward-items/{rewardItemId}`
  - `PATCH /api/admin/reward-items/{rewardItemId}`
  - `PUT /api/admin/reward-items/{rewardItemId}/image`
- 모든 응답은 `ApiResponse<T>` 래퍼 구조를 검증한다.

**Step 2: 실패 확인**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.controller.RewardItemAdminControllerTest"
```

Expected: 컨트롤러/응답 코드 미구현으로 실패한다.

**Step 3: 최소 컨트롤러 구현**

- `multipart/form-data`는 `@RequestPart` 패턴을 따른다.
- 목록 조회는 page/size/active query parameter 를 받는다.
- 수정은 `@RequestBody UpdateRewardItemRequest`를 받는다.
- 성공 시 `ApiResponse.successEntity(...)`를 사용한다.
- Swagger annotation 은 기존 controller 패턴과 맞춘다.

**Step 4: 테스트 재실행**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.controller.RewardItemAdminControllerTest"
```

Expected: 컨트롤러 테스트가 통과한다.

## Task 6: 최종 검증 및 문서 정리

**Files:**
- Verify: `was/src/main/java/kr/withrun/was/domain/reward/**`
- Verify: `was/src/test/java/kr/withrun/was/domain/reward/**`
- Verify: `was/src/main/java/kr/withrun/was/domain/file/service/S3FileService.java`
- Verify: `DB/schema.sql`
- Verify: `DB/SCHEMA_GUIDE.md`

**Step 1: 리워드 관련 테스트 전체 실행**

Run:

```bash
cd was && .\gradlew.bat test --tests "kr.withrun.was.domain.reward.*" --tests "kr.withrun.was.domain.file.service.S3FileServiceRewardItemTest"
```

Expected: reward item 관련 테스트가 모두 통과한다.

**Step 2: 전체 WAS 테스트 실행**

Run:

```bash
cd was && .\gradlew.bat test
```

Expected: 기존 회귀 없이 전체 테스트가 통과한다.

**Step 3: 빌드 검증**

Run:

```bash
cd was && .\gradlew.bat build
```

Expected: WAS 모듈 빌드가 성공한다.

**Step 4: 수동 계약 확인**

- Swagger/OpenAPI 또는 MockMvc 결과로 각 엔드포인트의 request/response shape 를 확인한다.
- `imageUrl`이 raw key 가 아니라 client-facing URL로 내려오는지 확인한다.
- soft deleted item 이 목록/상세에서 제외되는지 확인한다.

## Implementation Notes

- `/api/admin/...` 경로는 기존 저장소 관례를 따르되, 새로운 role 체계는 이 작업에서 발명하지 않는다.
- 가챠 확률표/보상풀 설계는 이 작업에 포함하지 않는다.
- 프론트 reward gacha seam 이 참조할 수 있도록 응답 필드명은 `title`, `imageUrl`을 유지한다.
