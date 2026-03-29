# Reward Item Admin Design

## Goal

리워드 가챠에서 노출되는 보상 아이템 메타데이터를 프론트 하드코딩이 아니라 백엔드 카탈로그로 관리한다. 관리자는 제목과 이미지를 등록/수정/비활성화할 수 있고, 웹 가챠/인벤토리 화면은 이 카탈로그의 `title`, `imageUrl`을 기준으로 렌더링한다.

## Context

- 선행 문서:
  - `docs/plans/2026-03-28-reward-gacha-screen-reward-design.md`
  - `docs/plans/2026-03-29-reward-gacha-product-rules-design.md`
  - `docs/plans/2026-03-29-reward-gacha-implementation-plan.md`
- `was/`는 package-by-domain 구조다.
- 표준 응답은 `ApiResponse<T>` 래퍼를 사용한다.
- 이미지/파일은 현재 S3 업로드 후 도메인 엔터티에 문자열 컬럼으로 저장하고, 응답 시 signed/public URL로 변환하는 패턴을 사용한다.
- 현재 저장소에서 `/api/admin/...` 경로는 존재하지만, 강한 role 기반 admin 권한 모델은 아직 분명하게 드러나지 않는다. 따라서 본 설계는 **경로와 책임 분리**를 먼저 정하고, 세부 권한 정책은 기존 보안 구조에 맞춰 후속 구현에서 닫는다.

## Approaches Considered

### 1. 전용 보상 아이템 카탈로그 + 이미지 전용 업로드 엔드포인트 분리 **(권장)**

- `reward_items` 테이블을 별도로 둔다.
- 생성은 `title + file` multipart 요청으로 처리한다.
- 메타데이터 수정과 이미지 교체를 별도 API 로 분리한다.
- 장점:
  - 현재 `was/`의 도메인/파일 처리 패턴과 가장 잘 맞는다.
  - 부분 수정이 단순해지고 테스트 경계가 명확하다.
  - 향후 reward pool, 확률표, 재고 정책과 분리하기 쉽다.
- 단점:
  - 생성/수정 플로우가 1~2개의 API 로 나뉠 수 있다.

### 2. 생성/수정 모두 multipart 단일 API

- `POST`, `PATCH` 모두 `title + file`을 함께 받는다.
- 장점: 관리자 입장에서 호출 횟수가 적다.
- 단점: JSON 기반 부분 수정이 불편하고, 이미지 변경 없는 메타데이터 수정도 multipart 로 강제된다.

### 3. 일반화된 미디어/에셋 테이블을 별도 도입

- 보상 아이템과 파일 메타데이터를 분리한다.
- 장점: 장기적으로 확장성이 좋다.
- 단점: 현재 범위에 비해 과하고, 저장소에도 아직 일반화된 파일 메타데이터 엔터티 패턴이 없다.

## Chosen Design

이번 범위에서는 **접근 1**을 채택한다.

- 보상 아이템 정의와 가챠 운영 규칙은 분리한다.
- `reward_items`는 “무슨 아이템을 사용자에게 보여줄지”만 관리한다.
- 확률, 티어, 재고, 운영 노출 정책은 이 테이블에 넣지 않고 후속 reward pool/운영 설정 설계로 분리한다.
- 이미지 저장은 기존 `snapshotImageUrl` 패턴을 따라 엔터티에는 문자열 하나만 저장한다.
- DB 에 저장된 값은 최종 public URL일 수도 있고 object key 일 수도 있지만, 응답에서는 항상 client-facing `imageUrl`로 정규화한다.

## Domain / Package Layout

신규 백엔드 도메인은 아래처럼 둔다.

```text
was/src/main/java/kr/withrun/was/domain/reward/
├── controller/
├── dto/
├── entity/
├── repository/
└── service/
```

권장 주 책임:

- `entity/RewardItem.java`: 보상 아이템 카탈로그 엔터티
- `repository/RewardItemRepository.java`: 기본 CRUD 및 관리 목록 조회
- `service/RewardItemAdminService.java`: 관리자 생성/조회/수정/비활성화/이미지 교체 유스케이스
- `controller/RewardItemAdminController.java`: `/api/admin/reward-items` 엔드포인트
- `dto/*`: 관리자 요청/응답 DTO

## Table Draft

### `reward_items`

| Column | Type | Null | Notes |
|---|---|---:|---|
| `reward_item_id` | BIGSERIAL | N | PK |
| `title` | VARCHAR(100) | N | 관리자 입력 제목 |
| `image_url` | VARCHAR(500) | N | S3 object key 또는 저장된 URL 문자열 |
| `is_active` | BOOLEAN | N | 기본값 `TRUE`, 가챠 풀 편성 가능 여부와 분리된 “관리상 사용 가능” 플래그 |
| `created_at` | TIMESTAMP | N | `BaseEntity` |
| `updated_at` | TIMESTAMP | N | `BaseEntity` |
| `deleted_at` | TIMESTAMP | Y | soft delete |

권장 인덱스:

- PK: `reward_item_id`
- 관리 목록 조회용 보조 인덱스: `(is_active, created_at desc)`

비고:

- `title`은 현재 단계에서 unique 로 강제하지 않는다.
- 보상 등급, 확률, 재고, 노출 우선순위는 이 테이블 범위에 포함하지 않는다.

## Entity Draft

- 엔터티명: `RewardItem`
- 테이블명: `reward_items`
- PK 필드: `Long id` + `@Column(name = "reward_item_id")`
- BaseEntity 상속
- 필드:
  - `String title`
  - `String imageUrl`
  - `boolean isActive`
- 도메인 메서드:
  - `updateTitle(String title)`
  - `updateImageUrl(String imageUrl)`
  - `activate()`
  - `deactivate()`

## API Draft

### 1. 보상 아이템 생성

`POST /api/admin/reward-items`

- 목적: 제목 + 이미지로 새 보상 아이템 등록
- 권한: admin 경로용 내부/관리자 호출 전제
- Content-Type: `multipart/form-data`
- Parts:
  - `title`: string, required
  - `file`: image file, required

성공 응답 (`201`):

```json
{
  "success": true,
  "code": "S001",
  "message": "요청이 성공했습니다.",
  "data": {
    "rewardItemId": 1,
    "title": "스타벅스 기프티콘",
    "imageUrl": "https://cdn.example.com/reward-item/1.png",
    "isActive": true,
    "createdAt": "2026-03-29T17:00:00"
  }
}
```

실패 예시:

- `400`: 제목 누락, 파일 누락, 빈 파일
- `404`: 후속 구현에서 별도 연관 자원 검증이 필요해질 경우
- `500`: S3 업로드 실패

### 2. 보상 아이템 목록 조회

`GET /api/admin/reward-items?active=true&page=0&size=20`

- 목적: 관리자 목록 조회
- Query:
  - `active`: optional boolean
  - `page`: optional, default `0`
  - `size`: optional, default `20`

성공 응답 (`200`):

```json
{
  "success": true,
  "code": "S001",
  "message": "요청이 성공했습니다.",
  "data": {
    "items": [
      {
        "rewardItemId": 1,
        "title": "스타벅스 기프티콘",
        "imageUrl": "https://cdn.example.com/reward-item/1.png",
        "isActive": true,
        "createdAt": "2026-03-29T17:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

### 3. 보상 아이템 단건 조회

`GET /api/admin/reward-items/{rewardItemId}`

- 목적: 관리자 상세 조회

성공 응답 (`200`):

```json
{
  "success": true,
  "code": "S001",
  "message": "요청이 성공했습니다.",
  "data": {
    "rewardItemId": 1,
    "title": "스타벅스 기프티콘",
    "imageUrl": "https://cdn.example.com/reward-item/1.png",
    "isActive": true,
    "createdAt": "2026-03-29T17:00:00",
    "updatedAt": "2026-03-29T17:05:00"
  }
}
```

### 4. 보상 아이템 메타데이터 수정

`PATCH /api/admin/reward-items/{rewardItemId}`

- 목적: 제목/활성 상태 수정
- Content-Type: `application/json`

요청 예시:

```json
{
  "title": "배달의민족 상품권",
  "isActive": false
}
```

규칙:

- `title`, `isActive`는 둘 다 optional 이지만 둘 다 비어 있는 요청은 `400`
- 이미지 교체는 이 엔드포인트에서 처리하지 않는다.

### 5. 보상 아이템 이미지 교체

`PUT /api/admin/reward-items/{rewardItemId}/image`

- 목적: 이미지 파일만 교체
- Content-Type: `multipart/form-data`
- Parts:
  - `file`: image file, required

성공 응답 (`200`):

- `RewardItemResponse` 동일 구조 반환

## Response DTO Draft

### `RewardItemResponse`

- `rewardItemId: Long`
- `title: String`
- `imageUrl: String`
- `isActive: boolean`
- `createdAt: LocalDateTime`
- `updatedAt: LocalDateTime?`

### `RewardItemListResponse`

- `items: List<RewardItemResponse>`
- `page: int`
- `size: int`
- `totalElements: long`
- `totalPages: int`
- `hasNext: boolean`

### `UpdateRewardItemRequest`

- `title: String?`
- `isActive: Boolean?`

## Storage / URL Handling

- 업로드는 기존 `S3FileService`와 `CloudFrontSignedUrlService` 패턴을 재사용한다.
- 저장 키 prefix 권장안: `reward-item/`
- DB 의 `image_url`에는 object key 또는 저장된 URL 문자열을 보관한다.
- 응답 DTO 변환 시에는 항상 signed/public URL로 정규화한다.
- create/update 이미지 업로드 성공 후에만 엔터티의 `imageUrl`을 갱신한다.

## Validation Rules

- `title`
  - 공백 불가
  - 길이 1~100자
- `file`
  - create/image update 에서 필수
  - 빈 파일 금지
- soft deleted 리소스는 조회/수정 대상에서 제외한다.

아래는 권장 기본값이며 구현 중 환경에 맞춰 세부 조정 가능하다.

- 허용 이미지 MIME: `image/png`, `image/jpeg`, `image/webp`
- 최대 용량: 5MB

## Out of Scope

- reward pool / 확률표 / 티어 / 재고 정책
- 사용자용 공개 reward item 관리 API
- 일반화된 파일 메타데이터 테이블 도입
- 완전한 role 기반 관리자 권한 체계 재설계

## Follow-up Notes

- 가챠 실행 결과와 인벤토리 응답은 이 카탈로그의 `title`, `imageUrl`을 참조하도록 연결한다.
- 이후 확률/운영 설계에서는 `reward_items`와 별도의 reward pool 매핑 구조를 두는 방향이 자연스럽다.
- 본 문서는 보상 아이템 카탈로그 자체의 초안이며, 가챠 draw 결과 저장 모델까지 포함하지 않는다.
