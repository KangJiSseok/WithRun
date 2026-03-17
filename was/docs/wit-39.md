# [API] POST /api/running-sessions/{runningSessionId}/register-course - 러닝 기록을 코스로 등록

## 개요

완료된 러닝 기록을 커뮤니티 코스로 등록한다. 코스 마스터 정보와 다중 코스타입 매핑을 함께 생성하며, 원본 러닝 세션의 좌표 경로(`running_sessions.coordinates`)를 새 코스의 경로(`courses.coordinates`)로 반영한다.

## API 명세

* **Method**: POST
* **URI**: /api/running-sessions/{runningSessionId}/register-course
* **인증**: Bearer Token 필요
* **사용자 유형**: 유저

## Request

### Headers

| 헤더 | 값 | 필수 |
| -- | -- | -- |
| Authorization | Bearer {accessToken} | ✅ |
| Content-Type | application/json | ✅ |

### Path Variables

| 변수명 | 타입 | 설명 | 필수 |
| -- | -- | -- | -- |
| runningSessionId | number | 코스로 등록할 러닝 세션 ID | ✅ |

### Query Parameters

없음

### Request Body

```json
{
  "title": "한강 야간 러닝 10K",
  "difficulty": "MEDIUM",
  "courseTypes": ["RIVERSIDE", "URBAN"],
  "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
}
```

* `title` (string, 필수): 등록할 코스 제목
* `difficulty` (string, 필수): 코스 난이도. 가능한 값: `EASY`, `MEDIUM`, `HARD`
* `courseTypes` (array<string>, 필수): 코스타입 목록. 가능한 값: `RIVERSIDE`, `PARK`, `MOUNTAIN_TRAIL`, `TRACK`, `URBAN`, `OTHER`
* `snapshotImageUrl` (string, 선택): 코스 스냅샷 이미지 URL. 미지정 시 러닝 세션 값을 사용

## Response

### 성공 응답

* **Status**: 201 Created

```json
{
  "courseId": 1,
  "title": "한강 야간 러닝 10K",
  "status": "COMMUNITY",
  "difficulty": "MEDIUM",
  "distanceM": 10000,
  "elevationGainM": 120,
  "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png",
  "startLatitude": 37.566501,
  "startLongitude": 126.978001,
  "endLatitude": 37.574501,
  "endLongitude": 126.989001,
  "coordinates": [
    {"latitude": 37.566501, "longitude": 126.978001, "elevation": 0.0},
    {"latitude": 37.574501, "longitude": 126.989001, "elevation": 1.0}
  ]
}
```

* `courseId` (number): 생성된 코스 ID
* `title` (string): 생성된 코스 제목
* `status` (string): 생성된 코스 상태. 항상 `COMMUNITY`
* `difficulty` (string): 저장된 코스 난이도
* `distanceM` (number): 러닝 기록에서 반영된 코스 거리
* `elevationGainM` (number): 러닝 기록에서 반영된 누적 상승 고도
* `snapshotImageUrl` (string | null): 저장된 코스 스냅샷 이미지 URL
* `startLatitude` (number): 시작 지점 위도
* `startLongitude` (number): 시작 지점 경도
* `endLatitude` (number | null): 종료 지점 위도
* `endLongitude` (number | null): 종료 지점 경도
* `coordinates` (array): `running_sessions.coordinates`를 기반으로 저장된 코스 경로 좌표 직렬화 결과

### 에러 응답

| Status Code | Error Code | 설명 |
| -- | -- | -- |
| 400 | BAD_REQUEST | 유효하지 않은 요청 |
| 401 | UNAUTHORIZED | 인증 실패 |
| 404 | RUNNING_SESSION_NOT_FOUND | 존재하지 않는 러닝 세션 |
| 409 | RUNNING_SESSION_NOT_COMPLETED | 완료되지 않은 러닝 세션 |

## 관련 DB 테이블

* `running_sessions`: 코스로 전환할 원본 러닝 기록과 집계 정보, `coordinates` 경로 데이터
* `courses`: 커뮤니티 코스 마스터 데이터와 `coordinates` 경로를 저장
* `course_difficulties`: 코스의 난이도를 저장 (1:1 관계)
* `course_type_map`: 코스와 코스타입의 다대다 매핑을 저장

## 인수 조건 (Acceptance Criteria)

- [ ] 완료된 러닝 세션만 코스로 등록할 수 있어야 한다.
- [ ] 코스 생성 시 `courses` 테이블에 `status`가 `COMMUNITY`로 저장되어야 한다.
- [ ] `distanceM`, `elevationGainM`, 이미지 URL, 시작/종료 좌표 정보는 러닝 세션 데이터 또는 요청값을 기준으로 반영되어야 한다.
- [ ] `running_sessions.coordinates`가 `courses.coordinates`로 복사되어 새 코스 경로 데이터로 저장되어야 한다.
- [ ] `courseTypes`의 각 값에 대해 `course_type_map` 테이블에 레코드가 생성되어야 한다.
- [ ] `difficulty` 값에 대해 `course_difficulties` 테이블에 레코드가 생성되어야 한다.
- [ ] 생성된 코스의 `createdByUserId`는 인증 사용자로 저장되어야 한다.

