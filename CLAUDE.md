# BFF 프로젝트 컨텍스트

## 이 문서의 목적

Claude Code가 BFF 프로젝트의 아키텍처 원칙과 구현 의도를 이해하고,
일관된 방향으로 코드를 작성하기 위한 가이드 문서입니다.

---

## BFF(Backend For Frontend)란?

### 핵심 개념

BFF는 **프론트엔드를 위한 전용 백엔드 서버**입니다.

WAS(백엔드)는 원자 단위의 API를 제공하고, BFF는 이 API들을 조합해
**페이지/feature 단위의 API로 가공하여 클라이언트에게 전달**합니다.

```
[Web 클라이언트]
      ↓ 단일 요청
[BFF 서버]  ← 이 프로젝트
      ↓ 원자 단위 API 여러 개 호출
[WAS 서버]
      ↓
[Database]
```

### BFF의 역할

| 요청 종류               | BFF의 역할                                                |
| ----------------------- | --------------------------------------------------------- |
| Read (GET)              | 여러 WAS API 조합 → 데이터 정제 → 프론트 맞춤 형태로 응답 |
| Write (POST/PUT/DELETE) | 인증 처리 + 로깅 후 WAS로 패스스루(그대로 전달)           |

### BFF가 해결하는 문제

1. **API 호출 횟수 감소**: 프론트가 한 번 요청하면 BFF가 여러 API를 대신 호출
2. **데이터 정제**: WAS의 복잡한 응답에서 프론트에 필요한 데이터만 추출
3. **단일 창구**: 프론트는 BFF Swagger 하나만 참조하면 됨
4. **인증/보안 집중화**: 민감한 인증 정보가 브라우저에 노출되지 않음

---

## 아키텍처 원칙

### 이 BFF가 지켜야 할 원칙

```
✅ BFF가 하는 것
- WAS API 호출 및 응답 조합
- 프론트 맞춤 데이터 가공 및 정제
- 인증/인가 처리
- 요청/응답 로깅
- 에러 핸들링 및 표준화

❌ BFF가 하지 않는 것
- 자체 DB 보유 및 직접 접근
- 비즈니스 로직 처리 (WAS의 역할)
- 데이터 영속성 관리
```

### 현재 스프린트 범위 (read-only facade)

이번 구현은 **초기 인프라 세팅** 단계로, read-only 조회 기능만 포함합니다.
이후 스프린트에서 인증 패스스루, WebSocket, write 요청 처리가 순차적으로 추가됩니다.

---

## 기술 스택

| 항목          | 기술                             |
| ------------- | -------------------------------- |
| 런타임        | TypeScript + Node.js             |
| 프레임워크    | NestJS                           |
| HTTP 서버     | Fastify adapter                  |
| 환경변수 관리 | @nestjs/config + class-validator |
| 코드 품질     | ESLint + Prettier                |

### Fastify를 선택한 이유

NestJS 기본 HTTP 서버인 Express 대신 Fastify를 사용합니다.
BFF는 많은 요청을 중계하는 서버이므로 **성능이 중요**하기 때문입니다.

---

## 디렉토리 구조

```
bff/
├── .claude/
│   └── CLAUDE.md          # 이 파일
├── src/
│   ├── main.ts            # 앱 진입점, Fastify 부트스트랩
│   ├── app.module.ts      # 루트 모듈
│   ├── modules/
│   │   └── health/        # 헬스체크 모듈
│   │       ├── health.controller.ts
│   │       └── health.module.ts
│   ├── common/
│   │   ├── filters/
│   │   │   └── http-exception.filter.ts   # 전역 예외 처리
│   │   └── interceptors/
│   │       └── request-logging.interceptor.ts  # 요청 로깅
│   └── config/
│       ├── configuration.ts   # 환경변수 로드
│       ├── env.validation.ts  # 환경변수 검증 스키마
│       └── app.config.ts      # 앱 설정 타입
├── .env.example
├── package.json
└── tsconfig.json
```

### 디렉토리 역할

- `modules/`: 도메인 단위 기능 모듈 (앞으로 running/, course/ 등이 추가됨)
- `common/`: 전역 공통 로직 (필터, 인터셉터, 데코레이터 등)
- `config/`: 환경변수 로딩 및 검증

---

## 환경변수

```env
NODE_ENV=development          # 실행 환경
PORT=3000                     # BFF 서버 포트
WAS_BASE_URL=http://localhost:8080  # BFF가 호출할 WAS 주소
LOG_LEVEL=info                # 로그 레벨
REQUEST_TIMEOUT_MS=5000       # 외부 호출 타임아웃 (ms)
```

---

## API 엔드포인트

### GET /health

BFF 서버 기동 여부 및 설정 로드 상태 확인용 헬스체크

**Response**

```json
{
  "status": "ok",
  "service": "bff",
  "timestamp": "2026-03-10T00:00:00.000Z"
}
```

---

## 코딩 컨벤션

### TypeScript

- `strict: true` 활성화 (암묵적 any 금지, null 체크 강제)
- 모든 함수 파라미터와 반환 타입 명시

### NestJS 패턴

- 모듈은 `modules/` 하위에 도메인 단위로 생성
- 전역 필터/인터셉터는 `common/` 하위에 배치
- `main.ts`에서 전역 필터/인터셉터 등록

### 명명 규칙

```
파일명: kebab-case (health.controller.ts)
클래스명: PascalCase (HealthController)
변수/함수명: camelCase (getHealth)
환경변수: UPPER_SNAKE_CASE (WAS_BASE_URL)
```

---

## 확장 계획 (앞으로 추가될 것들)

```
현재 (이번 스프린트)
└── 기본 인프라 + 헬스체크

다음 스프린트
├── 인증 패스스루 모듈
├── WAS HTTP 클라이언트 모듈 (@nestjs/axios)
└── Running 조회 API

이후
├── WebSocket 모듈
├── Course 추천 API
└── Write 요청 패스스루 처리
```
