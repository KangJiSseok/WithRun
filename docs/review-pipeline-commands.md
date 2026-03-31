# Review Pipeline Commands Overview

`.opencode/commands` 아래의 review-pipeline 커맨드들은 구현이 끝난 feature branch를 기준으로, 리뷰 준비부터 리뷰 반영까지를 일관된 흐름으로 묶어주는 리뷰 하네스다. 핵심 목적은 `git diff`만 보고 즉흥적으로 리뷰하는 대신, **설계 의도**, **평가기준**, **PR 설명**을 먼저 고정한 뒤 그 기준에 맞는 근거 기반 리뷰를 수행하게 만드는 것이다.

## 전체 흐름

리뷰 파이프라인의 진입점은 `review-pipeline-auto.md`다. 이 커맨드는 현재 브랜치 상태를 점검한 다음, 아래 순서대로 산출물을 만들고 마지막에 리뷰 반영까지 이어준다.

1. 브랜치 상태 확인 및 산출물 디렉토리 준비
2. 설계 의도 문서 작성
3. 작업별 평가기준 문서 작성
4. PR 본문 작성
5. 근거 기반 코드리뷰 실행
6. 리뷰 코멘트 반영 및 QA 수행

즉, 이 묶음은 단순한 “리뷰 실행 커맨드 모음”이 아니라, **리뷰를 가능하게 하는 문맥을 먼저 구축하는 파이프라인**이라고 보는 편이 정확하다.

## 커맨드별 역할

### `review-pipeline-auto.md`

전체 파이프라인 오케스트레이터다. 현재 브랜치와 base branch를 점검하고, `.review-artifacts/{branch-name}/` 아래에 산출물을 모은다. 개별 커맨드를 순서대로 연결하는 허브 역할을 한다.

### `review-pipeline-write-intent.md`

현재 대화와 작업 맥락을 바탕으로 `design-intent.md`를 만든다. 이번 변경이 무엇을 해결하는지, 어떤 선택을 했는지, 무엇을 의도적으로 제외했는지를 정리해서 이후 리뷰가 작성자의 의도를 오해하지 않도록 돕는다.

### `review-pipeline-gen-criteria.md`

`docs/code-convention.yaml`, `docs/adr.yaml`, AGENTS 규칙을 바탕으로 작업 맞춤형 `code-quality-guide.md`를 생성한다. 모든 팀 규칙을 그대로 들이미는 대신, **이번 작업에 실제로 적용되는 기준만 추려서** 리뷰 기준 문서로 만드는 단계다.

### `review-pipeline-create-pr-body.md`

`git diff dev...HEAD`를 기반으로 `pr-body.md`를 만든다. 무엇이 바뀌었는지, 어떤 범위에 영향이 있는지, 테스트 계획과 관련 문맥이 무엇인지를 정리해 구현 설명을 명시적으로 남긴다.

### `review-pipeline-review.md`

실제 자동 리뷰를 수행하는 단계다. `design-intent.md`, `code-quality-guide.md`, `pr-body.md`, 그리고 `git diff`를 함께 읽고 리뷰 코멘트를 생성한다. 이 커맨드의 핵심은 취향 리뷰를 줄이고, **기준 문서에 근거한 지적만 남기도록 강제한다**는 데 있다.

### `review-pipeline-reflect-review.md`

생성된 리뷰 코멘트를 읽고 항목별로 수용/거부를 판단한 뒤, 승인된 변경을 반영하고 QA까지 수행한다. 특히 `[p1]`~`[p4]` 우선순위에 따라 대응 강도를 다르게 가져가도록 설계되어 있다.

### `review-pipeline-update-docs.md`

이 파일은 core review loop 자체라기보다, 하네스를 떠받치는 규칙 문서를 유지보수하는 보조 커맨드에 가깝다. `docs/code-convention.yaml`과 `docs/adr.yaml`을 작업 맥락에 맞게 갱신해 이후 파이프라인이 더 정확한 기준 문서를 만들 수 있도록 돕는다.

## 이 하네스의 공통 설계 패턴

이 커맨드들은 몇 가지 일관된 설계 원칙을 공유한다.

- **산출물 중심 흐름**: 중간 판단을 대화에만 남기지 않고 `.review-artifacts/{branch-name}/` 아래 문서로 저장한다.
- **문맥 선행**: 리뷰 전에 설계 의도, 평가기준, PR 설명을 먼저 만든다.
- **사용자 확인 지점 분리**: 의도/기준/PR 본문은 사용자 피드백을 반영해 확정하고, 자동 리뷰 단계는 사용자 확인 없이 진행한다.
- **worktree(fork) 활용**: 문서 생성이나 반영 단계는 격리된 컨텍스트에서 수행하도록 전제한다.
- **sub-agent 활용**: ADR 추출이나 자동 리뷰처럼 독립적으로 위임 가능한 작업은 sub-agent에게 맡긴다.
- **base branch diff 고정**: 여러 단계가 `git diff dev...HEAD`를 기준으로 동작하므로, 파이프라인 전체가 같은 비교 기준을 공유한다.

## 생성되는 주요 산출물

기본 산출물 경로는 아래와 같다.

```text
.review-artifacts/{branch-name}/
├── design-intent.md
├── code-quality-guide.md
├── pr-body.md
└── review-comments.md
```

이 구조 덕분에 리뷰 준비 과정과 최종 리뷰 결과가 한 폴더에 모이고, 나중에 “왜 이런 리뷰가 나왔는가”를 역추적하기 쉬워진다.

## 이해 포인트

이 review harness의 핵심 가치는 자동화 그 자체보다 **리뷰 품질의 기준화**에 있다. 설계 의도와 팀 규칙, 작업별 맥락을 먼저 정리한 뒤 리뷰를 수행하게 만들기 때문에, 구현자와 리뷰어가 같은 전제를 공유하도록 돕는 장치로 보는 것이 가장 적절하다.
