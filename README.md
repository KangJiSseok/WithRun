## **1) 브랜치 전략**

### **기본 브랜치**

- `dev`: 통합 개발 브랜치 (항상 최신 개발 상태)
- `main`: 배포/릴리즈 브랜치 (안정 버전만)

### **기능 브랜치 생성 규칙**

- **기준 브랜치:** 항상 `dev`에서 분기
- **단위:** “기능 단위 / 이슈 단위”로 생성 (너무 크게 잡지 않기)
- **브랜치명 규칙(추천)**
    - `feat/<scope>/<short-desc>`
    - `fix/<scope>/<short-desc>`
    - `hotfix/<scope>/<short-desc>`
    - 예시
        - `feat/mobile/login-screen`
        - `fix/wear/hr-stream-null`
        - `refactor/web/api-layer-cleanup`

> scope는 `web | wear | mobile | server | common | infra` 등 팀에서 쓰는 루트 디렉토리 기준으로.
> 

---

## **2) PR & 머지 정책**

### **PR 생성 규칙**

- PR은 **항상 기능 브랜치 → dev**
- **라인수 제한:** 변경 라인 기준 500줄 이하 권장 (초과 시 PR 분리)
- PR에는 최소 아래 항목 포함(템플릿 권장)
    - 작업 내용 요약
    - 변경 범위(scope)
    - 테스트 방법/결과
    - 스크린샷/영상(UI 변경 시)
    - 관련 이슈 링크

### **Approve 규칙**

- **최소 1명 Approve 필수**
- 본인 Approve 후 머지 불가(셀프 머지 금지 권장)
- 리뷰어는 가능한 한 **해당 도메인(scope) 담당 1명 포함** 추천

### **최종 Merge 책임**

- **PR 올린 사람이 머지한다**
- 단, 아래 조건일 때는 예외(추천)
    - 긴급 hotfix → 담당자/리드가 바로 머지 가능
    - 작성자가 부재/장기 미응답 → 팀 합의 후 대리 머지

---

## **3) 머지 전략: Rebase Merge**

### **원칙**

- `dev`는 **Rebase merge로 깔끔한 히스토리 유지**
- squash는 기본 비권장(원하면 “작은 커밋 난립”일 때만 팀 합의)

### **Rebase Merge 흐름(권장 절차)**

1. 기능 브랜치에서 작업 후 push
2. PR 생성
3. 리뷰 반영 후 `dev` 최신 상태로 rebase
    - `git fetch origin`
    - `git rebase origin/dev`
4. 충돌 해결
5. 테스트/빌드 확인
6. PR rebase merge

---

## **4) Conflict 해결 규칙**

- **원칙:** “늦게 merge 하는 사람이 해결한다”
    - 이유: 현재 dev의 최신 상태를 반영하며 들어오는 쪽이 조정하는 게 자연스러움
- 단, 충돌이 특정 기능 소유권/의도가 강하게 걸려있으면
    - **관련 기능 작성자(또는 담당자)에게 확인 후 해결**
    - “무단 로직 변경” 방지용

---

## **5) 커밋 메시지 규칙**

### **포맷**

- `type(scope) : message`
- scope는 **변경된 루트 디렉토리 기준**으로 표기
    - 단일: `web`, `wear`, `mobile`
    - 복수 변경: `web,wear` 처럼 콤마로 나열
    - 공통/전역: `common`, `infra` 등 허용

### **예시**

- `feat(mobile) : add login flow`
- `fix(wear) : prevent hr stream crash on null sensor`
- `refactor(web) : simplify api client`
- `chore(infra) : bump gradle and node versions`
- `docs(common) : update branch policy`

### **type 정의**

- `feat` : 기능 개발
- `hotfix` : 치명적인 오류(긴급)
- `fix` : 일반 수정사항(함수명/클래스명/필드 수정 포함)
- `style` : 포맷팅(세미콜론/정렬 등) + 로직 변경 없음
- `docs` : 문서
- `test` : 테스트 코드/테스트 추가
- `refactor` : 기능 변화 없이 구조 개선(클린코드/성능)
- `chore` : 빌드/패키지/설정/잡무
- `cicd` : CI/CD 관련 작업
- `rename` : 파일/폴더명 변경(가능하면 단독 커밋)
- `remove` : 파일/폴더 삭제(가능하면 단독 커밋)

---

## **6) PR 크기 관리(500줄 정책 운영 방법)**

- 500줄 초과가 불가피하면 아래 방식으로 **PR을 쪼갠다**
    1. **기반 PR:** 리팩토링/공통 작업(동작 변화 없음)
    2. **기능 PR:** 실제 기능 변경
    3. **UI/리소스 PR:** 이미지/스타일/문구 등
- 리뷰 효율이 가장 중요: “한 PR에 한 주제” 원칙

---

## **7) MR 제목 규칙**

- [Feat/Chore/…(작업 디렉토리(WAS/WEB/…))] 작업 사항 핵심 1줄 요약
    - ex) `[Feat(WAS)] 사용자 인증/인가를 위한 JWT 설정`

```md
<!-- MR 작성 후 Jira 상태 관리 잊지말기 -->
## Motivation
<!-- 작성 배경 -->
-

## Problem Solving
<!-- 해결 방법 -->
-

## To Reviewer
<!-- 리뷰어에게 말하고 싶은 것, 유의 깊게 봐주었으면 하는 것, 의문점 등 -->
-
```
