# PR: [Chore(infra)] OMO 리뷰 파이프라인 설정과 문서 정리

## Summary

- 저장소에 OMO-native review pipeline 명령/스킬 세트를 추가했습니다.
- 리뷰 파이프라인이 참조할 `docs/code-convention.yaml`, `docs/adr.yaml`, `docs/omo-review-pipeline.md`를 추가하고, 관련 설계/구현 계획 문서를 정리했습니다.
- `.gitignore`를 조정해 `.opencode`의 의도된 자산을 추적 대상으로 열었고, `.gitlab-ci.yml`에는 현재 브랜치의 dev/test 경로 검증용 예외를 추가했습니다.

## Changes

### 1. 리뷰 파이프라인 자산 추가
- `.opencode/commands/review-pipeline-*.md`
- `.opencode/skills/review-pipeline-*/SKILL.md`
- `.opencode/oh-my-openagent.jsonc`

### 2. 기준 문서와 사용 문서 추가
- `docs/code-convention.yaml`
- `docs/adr.yaml`
- `docs/omo-review-pipeline.md`
- `docs/plans/2026-03-31-codereview-harness-omo-port-*.md`

### 3. 공통 설정 조정
- `.gitignore`: `.opencode` 하위의 선택 자산만 추적하도록 예외 규칙 추가
- `.gitlab-ci.yml`: `refactor-infra/cicd` 브랜치도 dev/test 이미지·배포 경로를 검증할 수 있도록 예외 변수/규칙 추가

### 4. 문서 정리
- 기존 reward 관련 계획 문서 다수를 제거해 현재 브랜치의 review-tooling 방향과 문서 베이스를 정리함
- `ui-ux-pro-max` 스킬과 데이터 자산을 함께 추가함

## Breaking Changes

없음

## Test Plan

- `git diff dev...HEAD --stat` 로 변경 표면을 확인함
- `git status --short --branch` 로 커밋 후 워킹트리를 확인함
- 문서/설정/명령 파일은 readback 위주로 구조 정합성을 확인함
- 앱 모듈(`web/mobile/was/watch`) 빌드나 테스트는 이번 diff가 해당 소유 모듈 코드를 직접 변경하지 않아 실행하지 않음

## Related

- base branch: `dev`
- 현재 브랜치는 shared tooling / docs / CI 범위의 변경이므로 리뷰도 제품 기능이 아니라 shared infra/process 관점에서 봐야 함
- `reflect-review` 단계 전까지는 소스 코드를 수정하지 않는 파이프라인 설계를 따름
