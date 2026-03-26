# Local Valhalla Bootstrap

`WIT-146` 범위의 로컬 전용 Valhalla 실행 진입점입니다.
운영 배포용 compose는 건드리지 않고, 개발자가 `docker compose`만으로 Valhalla를 띄운 뒤 WAS에서 `VALHALLA_BASE_URL`로 연결할 수 있게 만드는 목적입니다.

## Files

- `docker-compose.yml`: 로컬 Valhalla 실행 정의
- `.env.example`: 기본 이미지, 포트, 샘플 PBF URL 예시
- `custom_files/`: PBF 다운로드 결과, 타일, 설정 등 Valhalla 런타임 산출물 저장 디렉터리

## Prerequisites

- Docker Engine
- Docker Compose v2
- `curl` (smoke test용)

## Quick Start

```bash
cp .env.example .env
docker compose up -d
docker compose ps
```

첫 기동 시에는 `VALHALLA_TILE_URL`에 지정한 PBF를 다운로드하고 타일을 생성하므로 시간이 걸릴 수 있습니다.
기본값은 한국 Geofabrik PBF이며, 국가 단위 extract 라서 초기 타일 생성 시간이 샘플 PBF보다 더 오래 걸릴 수 있습니다.
로컬 기본값은 Windows/Docker Desktop 환경에서의 안정성을 위해 `server_threads=1`, `build_tar=False`, `use_default_speeds_config=False`로 보수적으로 잡아뒀습니다.

## Readiness Check

Valhalla readiness는 `/status`로 확인합니다.

```bash
curl -fsS "http://localhost:${VALHALLA_PORT:-8002}/status"
```

정상 기동 시 HTTP 200과 함께 `version`, `tileset_last_modified` 같은 필드가 응답됩니다.

## Smoke Test

아래 요청은 기본 한국 PBF 기준의 최소 route 확인용 요청입니다.

```bash
curl -fsS -X POST "http://localhost:${VALHALLA_PORT:-8002}/route" \
  -H "Content-Type: application/json" \
  --data '{"locations":[{"lat":37.566295,"lon":126.977945},{"lat":37.565889,"lon":126.983063}],"costing":"auto"}'
```

응답 본문에 `trip` 객체가 내려오면 기본 라우팅은 정상입니다.

## WAS Connection Point

WAS는 `was/src/main/resources/application.yml`에서 아래 환경변수를 읽도록 연결점을 열어뒀습니다.

```env
VALHALLA_BASE_URL=http://localhost:8002
```

Spring property key는 `valhalla.base-url`이며, 로컬 기본값도 `http://localhost:8002`입니다.
실제 WAS 실행 방식에 따라 IDE Run Configuration, shell 환경변수, 또는 별도 `.env` 주입 방식으로 넣으면 됩니다.

## Common Operations

```bash
docker compose logs -f valhalla
docker compose down
```

다른 PBF로 다시 구성하려면:

1. `.env`의 `VALHALLA_TILE_URL`을 변경합니다.
2. `custom_files/` 내부 산출물을 비웁니다.
3. `docker compose up -d`로 다시 기동합니다.

## Stability Knobs

- `VALHALLA_USE_TILES_IGNORE_PBF=False`: 실패한 partial tile 재사용을 막고 PBF 기준으로 다시 판단합니다.
- `VALHALLA_BUILD_TAR=False`: 디버깅 중에는 tar extract 생성을 끄고 디렉터리 타일만 사용합니다.
- `VALHALLA_SERVER_THREADS=1`: 대용량 지역 빌드 시 스레드 수를 줄여 안정성을 높입니다.
- `VALHALLA_USE_DEFAULT_SPEEDS_CONFIG=False`: enhance 단계의 default speeds 주입을 끕니다.

## Notes

- 이 디렉터리는 local-only bootstrap 용도입니다.
- `deploy/compose/docker-compose.prod.yml`, `deploy/compose/docker-compose.test.yml`, `.gitlab-ci.yml`는 수정하지 않습니다.
- `WIT-141`의 worker/domain 생성 로직은 이 범위에 포함되지 않습니다.
