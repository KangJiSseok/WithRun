# Code Review

## Summary

- 이번 브랜치는 product module 변경이 아니라 shared infra/tooling/docs 변경이다.
- `docs/adr.yaml`에는 직접적으로 맞물리는 ADR이 없어서, 코멘트는 `code-quality-guide.md`의 공통 기준과 `design-intent.md`의 의도/주의사항을 근거로 작성했다.
- 우선순위는 공용 CI 안전성과 추적 범위의 명확성에 맞췄다.

## Comments

### [p1]

#### 1. 공유 CI 설정에 현재 기능 브랜치 이름이 하드코딩되어 있습니다
- **파일/라인:** `.gitlab-ci.yml:17`, `.gitlab-ci.yml:54`, `.gitlab-ci.yml:82`
- **근거:** `code-quality-guide.md`의 “공용 파이프라인에 임시 운영 규칙이 장기 설정으로 남지 않는지 확인” 기준, `design-intent.md`의 “현재 브랜치를 한시적으로 통과시킨다” 결정과 주의사항
- **문제:** `DEV_TEST_BRANCH: "refactor-infra/cicd"` 와 그에 연결된 deploy/image rule 이 그대로 머지되면, 앞으로도 이 특정 기능 브랜치가 shared dev/test 배포 경로를 계속 탈 수 있다. 이는 테스트 편의를 위해 넣은 임시 예외를 장기 infra 규칙으로 굳히는 형태라 shared pipeline의 예측 가능성을 떨어뜨린다.
- **제안 변경:** 머지 전에 이 예외를 제거하거나, 브랜치명을 고정 문자열로 두지 말고 명시적 CI 변수/수동 잡/환경 토글 기반으로 일시 검증 경로를 열어라.
- **부작용:** 현재 브랜치에서 push만으로 dev/test 경로를 검증하는 편의는 줄어든다.
- **그래도 할 가치:** 공용 배포 규칙은 일회성 검증 편의보다 안정성과 유지보수성이 우선이다. 임시 브랜치명이 파이프라인에 남으면 나중에 이유를 모르는 운영 리스크가 된다.
- **판정: REJECT** — 사용자 요청으로 `DEV_TEST_BRANCH` 변수와 feature-branch 예외 조건을 유지한다. 이 브랜치에서는 테스트 배포 경로 검증 편의를 우선하기로 했다.

### [p2]

#### 2. `.opencode` 추적 범위를 열었지만 Python cache 생성물은 여전히 무시되지 않습니다
- **파일/라인:** `.gitignore:41-59`
- **근거:** `code-quality-guide.md`의 “의도된 소스 자산만 추적” 기준, `design-intent.md`의 “생성 산출물이 섞이지 않도록 ignore 경계를 엄격히 유지해야 한다” 주의사항
- **문제:** `.gitignore`는 `.opencode` 하위의 commands/skills/config를 추적 대상으로 열었지만, Python cache (`__pycache__/`, `*.pyc`)에 대한 제외 규칙은 없다. 실제로 현재 워킹트리에도 `.opencode/skills/ui-ux-pro-max/scripts/__pycache__/` 가 untracked 로 남아 있어, 이후 로컬 실행 결과가 쉽게 잡음 diff로 유입될 수 있다.
- **제안 변경:** root `.gitignore`에 `__pycache__/`, `*.py[cod]` 또는 최소 `.opencode/**/__pycache__/`, `.opencode/**/*.pyc` 규칙을 추가해 생성 cache 가 버전 관리 범위 밖에 머물게 하라.
- **부작용:** 없음에 가깝다. 로컬 Python 실행 산출물만 계속 무시된다.
- **그래도 할 가치:** 이번 브랜치의 목적은 공유할 도구 자산만 추적하는 것이지, 실행 캐시를 공유하는 것이 아니다. ignore 경계를 닫아야 이후 리뷰 잡음과 실수 커밋을 줄일 수 있다.
- **판정: ACCEPT** — root `.gitignore`에 `__pycache__/` 와 `*.py[cod]` 를 추가해 `.opencode` 포함 repo 전역 Python cache 생성물을 무시하도록 했다.

## Re-review after accepted fixes

- `.gitlab-ci.yml` 관련 p1 코멘트는 사용자 요청에 따라 reject 처리했다. 따라서 feature-branch 예외는 현재 의도된 상태로 유지되며, 이번 reflect-review에서는 추가 수정하지 않았다.
- `.gitignore` 재검토 결과, Python cache 생성물이 ignore 대상에 포함되어 `.opencode/skills/ui-ux-pro-max/scripts/__pycache__/` 가 더 이상 일반 untracked 항목으로 나타나지 않는다.
- 별도 참고: YAML 진단 기준으로 `.gitlab-ci.yml:21-22` 의 multiline rule 문자열에는 기존 구문 문제가 남아 있다. 이는 이번 reflect-review에서 수정한 라인이 아니라, 커밋된 원본에도 존재하던 항목이라 이번 판정 반영 범위에는 포함하지 않았다.
