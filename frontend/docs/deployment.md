# 프론트엔드 CI/CD와 같은 서버 배포

프론트 빌드·이미지 배포는 이 앱 저장소에서 관리한다. AWS 서버·도메인·OIDC 역할·방화벽은 [santacut-infra](https://github.com/yooni9ue/santacut-infra)가 관리한다. 이 PR은 인프라 생성이나 실제 서버 배포를 실행하지 않는다.

## 실행 흐름

| 시점 | 동작 |
| --- | --- |
| frontend 관련 PR | Node.js 24·pnpm 고정 설치 → 린트·타입·단위 테스트 → standalone 빌드 → 모바일 Chromium/WebKit E2E → Docker 이미지 빌드·상태 확인 |
| main 반영 | 위 검증 성공 후 GHCR에 frontend 이미지를 게시 |
| 수동 실행, deploy=true | 선택한 dev/main 환경 승인·OIDC 인증 → 러너 IP의 임시 SSH 허용 → 테스트된 이미지 digest 배포 → SSH 허용 규칙 제거 |

운영 배포는 자동으로 실행되지 않는다. GitHub Actions `프론트엔드 CI/CD`에서 **main 브랜치**와 대상 환경을 고른다. 환경 기본값은 `dev`다. 운영(main) Environment에는 승인자를 설정한다.

## 요청 경로

```text
브라우저 → 같은 HTTPS 도메인의 Caddy
             ├─ /v1/**, /s/**, Swagger → app:8080 (Spring Boot)
             ├─ /actuator/** → 404
             └─ 그 외 → frontend:3000 (Next.js)
```

프론트 컨테이너는 기존 `santacut_default` 네트워크에 참여한다. 호스트 3000번은 127.0.0.1에만 바인딩한다. Caddy는 컨테이너 네트워크로 직접 접근한다. 같은 origin을 유지하므로 세션 쿠키와 CSRF 요청에 별도 CORS 설정이 필요 없다.

프론트 배포 전에는 `FRONTEND_UPSTREAM=app:8080`으로 기존 백엔드 전용 동작을 유지한다. 프론트가 정상일 때만 배포 스크립트가 `/opt/santacut/.env`의 값을 `frontend:3000`으로 변경하고 Caddy를 재생성한다. 다른 백엔드 설정은 유지한다.

## 최초 준비 순서

1. 인프라 저장소의 `docs/operations.md`에 따라 dev 서버, 도메인, Docker, GitHub OIDC 역할을 준비한다.
2. **이 PR의 Caddy 경로 분기와 FRONTEND_UPSTREAM 설정을 포함한 백엔드 릴리스를 먼저 배포한다.** 기존 백엔드만 설치된 상태면 프론트 배포 스크립트가 중단한다. `/opt/santacut/current`, `current-image`, `.env`와 `santacut_default` 네트워크가 있어야 한다.
3. 아래 환경 변수·비밀값과 GHCR 이미지 pull 권한을 준비한다. GHCR 이미지가 비공개면 서버에서 별도의 읽기 전용 인증이 필요하다.
4. `dev`에 먼저 프론트를 배포하고 루트 화면, `/v1/regions`, 세션·CSRF, 공유 경로를 확인한다.
5. 임시 SSH 포트 규칙을 열고 닫은 후 운영자 CIDR 규칙이 보존되는지 확인한다. 이 확인은 인프라 운영서의 최초 배포 체크 항목이다.
6. 같은 순서로 `main` 환경에 배포한다.

### GitHub Environment 설정

앱 저장소의 Environment 이름을 인프라와 동일한 **dev**, **main**으로 만든다. 배포 브랜치를 main으로 제한하고 운영(main)에 승인자를 둔다.

| 종류 | 이름 | 용도 |
| --- | --- | --- |
| Variable | AWS_DEPLOY_ROLE_ARN | 인프라 bootstrap의 환경별 앱 배포 OIDC 역할 |
| Secret | DEPLOY_HOST | 해당 환경 Lightsail의 고정 IP 또는 SSH 호스트 |
| Secret | DEPLOY_USER | 서버 SSH 계정(인프라 기본값은 ubuntu) |
| Secret | DEPLOY_SSH_KEY | 해당 환경 배포 SSH 개인키 |
| Secret | DEPLOY_KNOWN_HOSTS | 신뢰할 수 있는 경로로 확인한 SSH 호스트 키 |

이전 백엔드 워크플로는 `production` Environment·직접 SSH·Ollama 설정을 사용한다. 인프라 저장소는 `dev/main`·OIDC·Bedrock 구성을 전제로 하므로, **백엔드 운영 배포 설정의 별도 정합성 작업이 남아 있다.** 프론트 워크플로가 백엔드 모델·DB·백업 설정을 자동 변환하지 않는다.

## 상태 확인과 복구

- 이미지: `ghcr.io/yooni9ue/santacut-frontend:<commit>`와 불변 digest를 사용한다.
- 서버 릴리스: `/opt/santacut/frontend/releases/<commit>`.
- 활성 상태: `/opt/santacut/frontend/current`, `/opt/santacut/frontend/current-image`.
- 배포는 백엔드와 같은 `/opt/santacut/deploy.lock`을 잡는다. 동시에 배포하면 한쪽이 중단되어 Caddy·환경 파일 갱신이 겹치지 않는다.
- 새 컨테이너 상태, 로컬 HTTP, 공개 HTTPS를 확인한 뒤 활성 릴리스 정보를 기록한다.
- 실패하면 기존 프론트 이미지를 다시 띄우고 이전 Caddy 환경 설정으로 복구를 시도한다. 첫 배포 실패면 새 프론트 컨테이너를 내리고 기존 백엔드 라우팅을 복구한다.
- SQLite·백엔드 앱 이미지는 프론트 배포 중 교체하지 않는다. Caddy 재생성 중 짧은 연결 끊김이 발생할 수 있어 무중단 배포가 아니다.
- 프론트 컨테이너 메모리 상한은 384MB다. 2GB 서버에서 백엔드·Caddy와 함께 운영하므로 실제 사용량을 관찰한다.
- 이전 프론트 릴리스 정리는 운영자가 현재 릴리스·롤백 버전을 보존하며 수행한다. 인프라의 기존 정리 cron은 백엔드 릴리스 디렉터리만 처리한다.

## 로컬 검증

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm test
pnpm build
CI=true pnpm test:e2e
bash -n ci-cd/scripts/*.sh
bash ci-cd/scripts/test-deploy.sh
```

배포 스크립트 테스트는 가짜 Docker·HTTP 응답으로 첫 배포 실패, 정상 배포, 실패한 업그레이드의 복구를 검증한다. 실제 Docker 실행·SSH·TLS·AWS 권한 검증을 대체하지 않는다. GitHub Actions에서는 Linux Docker 이미지 빌드와 컨테이너 상태 검사까지 수행한다.
