# 평가기준

## 공통 기준 (Code Convention)

- **GEN-003**: 한 변경은 한 책임에 집중한다. bugfix나 운영 변경은 직접 관련 없는 정리 작업과 과도하게 섞이지 않아야 한다.
- **GEN-004**: 사용자나 호출자가 관찰하는 동작이 바뀌는 경우 변경 근거와 검증 방법을 함께 남긴다. 이번 브랜치에서는 특히 CI 배포 조건 변경이 이에 해당한다.
- **MONO-002**: 변경 검증은 영향을 받는 소유 모듈 단위로 수행한다. 이번 diff는 앱 모듈이 아니라 root/common/tooling 영역이므로, 불필요한 전체 앱 빌드보다 설정/문서/파이프라인 정합성 검토가 우선이다.

## 저장소 로컬 규칙

- `README.md` 기준 PR은 `dev`를 대상으로 하고, 리뷰 효율을 위해 **한 PR에 한 주제**, **500줄 이하 권장** 원칙을 따른다. 대규모 mixed-scope 변경은 리뷰 가능성과 롤백 가능성을 떨어뜨리는지 본다.
- root `AGENTS.md`는 `.gitlab-ci.yml`을 공유 infra 경계로 본다. 따라서 브랜치 전용 예외나 임시 운영 규칙이 장기 설정으로 남지 않는지 확인해야 한다.
- 이번 브랜치의 `.gitignore` 변경 의도는 `.opencode`의 **의도된 소스 자산만** 추적하는 것이다. 생성 캐시나 로컬 산출물은 버전 관리에 들어오지 않아야 한다.

## 이 작업에 적용되는 ADR

강하게 직접 적용되는 ADR은 없음.

- `docs/adr.yaml`의 `ADR-001`~`ADR-005`는 product UI ownership, bridge contract, backend domain layering, watch ownership, web styling에 대한 결정이다.
- 이번 브랜치는 `.gitlab-ci.yml`, `.gitignore`, `.opencode/**`, `docs/**` 중심의 process/tooling 변경이므로, ADR보다는 위 공통 기준과 설계의도를 주된 grounding source로 사용한다.

## 이 브랜치에 대한 적용 메모

- CI 관련 변경은 **임시 검증 편의**와 **공용 파이프라인 안정성** 사이의 trade-off를 분명히 설명해야 한다.
- `.opencode` 버전 관리 개방은 필요한 자산만 열어야 하며, Python cache·dependency 폴더 같은 생성물 유입이 없는지 실제 `git status`로 확인해야 한다.
- 리뷰 코멘트는 취향 문제가 아니라, 공유 infra 안전성·추적 범위의 명확성·리뷰 가능성에 초점을 맞춘다.
