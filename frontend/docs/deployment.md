# 프론트엔드 Vercel 배포

프론트는 Vercel, 백엔드는 AWS에 배포한다. AWS 서버·도메인·권한은 [santacut-infra](https://github.com/yooni9ue/santacut-infra)가 관리하며, 이 프론트 파이프라인은 백엔드 서버에 SSH로 접속하거나 컨테이너·Caddy 설정을 바꾸지 않는다.

## CI/CD 흐름

| 시점 | 동작 |
| --- | --- |
| develop/main 대상 frontend 관련 PR | 고정 Node.js 24·pnpm 설치 → 린트·타입·단위 테스트 → 프로덕션 빌드 → 모바일 Chromium/WebKit E2E |
| develop 반영 | 위 검증 통과 후, 배포 활성화 상태면 Vercel Preview 환경으로 개발 배포 |
| main 반영 | 위 검증 통과 후, 배포 활성화 상태면 Vercel production 설정으로 빌드·배포 |
| 수동 실행 | develop 또는 main에서 deploy=true를 선택하면 동일 검증·배포. 활성화 변수도 true여야 함 |

배포는 GitHub Actions가 담당한다. `frontend/vercel.json`의 `git.deploymentEnabled=false`는 Vercel Git 자동 배포를 꺼서 CI 전에 배포되거나 이중 배포되는 것을 방지한다. 기능 브랜치·PR은 CI만 실행한다. 개발 서버는 develop의 Preview 환경을 사용하며 Vercel의 로컬 Development 환경과는 다르다.

개발은 `feature/*`, `chore/*`, `fix/*`, `refactor/*` 등 → PR → `develop` → 릴리스 PR → `main` 순서다. 백엔드 AWS 배포 파이프라인은 이 변경에 포함하지 않는다.

## 최초 Vercel 설정

1. Vercel에서 이 저장소의 Next.js 프로젝트를 생성한다.
2. 프로젝트 **Root Directory를 frontend**로 지정하고 Node.js 24를 선택한다. CLI는 저장소 루트에서 연결된 프로젝트의 Root Directory 설정을 사용한다.
3. Production 환경 변수 `BACKEND_ORIGIN`에 AWS 백엔드의 공개 HTTPS origin을 넣는다. 예시: `https://api.example.com`. 경로·쿼리·사용자 인증 정보를 넣지 않는다. Preview 환경의 develop 브랜치 전용 변수에는 AWS 개발 백엔드 HTTPS origin을 넣는다. 개발·운영 데이터 분리가 필요하면 별도 AWS API/DB가 필요하며, 이 워크플로가 백엔드를 생성하지는 않는다. Vercel Production Branch는 main으로 설정한다.
4. GitHub의 `vercel-development`, `vercel-production` Environment 각각에 아래 비밀값을 등록한다. 배포 허용 브랜치를 각각 develop, main으로 제한한다. 같은 Vercel 프로젝트를 사용할 수 있고 필요하면 운영 승인자를 설정한다.
5. GitHub 저장소 Variable `VERCEL_DEPLOY_ENABLED=true`로 배포를 활성화한다. 활성화 전에는 CI만 실행한다.
6. 고정 개발 주소가 필요하면 Vercel 프로젝트에 도메인·DNS를 등록하고 GitHub vercel-development Environment Variable `VERCEL_DEVELOPMENT_DOMAIN`에 호스트명만 넣는다(예: `dev.example.com`). 개발 배포 후 CLI alias로 갱신한다. 비워두면 실행 Summary의 고유 Preview URL을 사용한다. 운영 도메인을 넣지 않는다.
7. develop/main 각각 수동 실행에서 deploy=true로 최초 배포를 검증한다. 이후 관련 파일 push는 검증 성공 후 자동 배포한다. Git 자동 배포가 꺼져 있으므로 개발 도메인도 위 CLI alias로 연결한다.
8. develop/main의 PR·필수 CI 브랜치 보호를 설정한다. 경로 필터 때문에 백엔드 전용 PR에는 프론트 CI가 실행되지 않으므로 모든 PR에 프론트 검사를 필수로 지정하면 대기 상태가 될 수 있다.

| Secret | 값 |
| --- | --- |
| VERCEL_TOKEN | 해당 프로젝트 배포 권한이 있는 Vercel 토큰 |
| VERCEL_ORG_ID | Vercel 팀·계정 ID |
| VERCEL_PROJECT_ID | Root Directory가 frontend인 프로젝트 ID |

토큰과 실제 환경 파일은 커밋하지 않는다. `.vercel/`은 Git에서 제외한다. 프로젝트 ID는 Vercel 설정 또는 로컬 `vercel link`의 `.vercel/project.json`에서 확인한다.

## API·쿠키·CSRF

```text
브라우저 → Vercel 프론트 도메인
             ├─ 화면 → Next.js
             └─ /v1/**, /s/**, Swagger
                  → Next.js 외부 rewrite → AWS HTTPS 백엔드
```

Axios는 상대 경로 `/v1`을 사용한다. 브라우저가 AWS API 도메인으로 직접 요청하지 않으므로 기존 쿠키·CSRF 흐름을 유지할 수 있다. 백엔드의 세션 쿠키는 Domain을 고정하지 않는 현재 설정을 유지하고, HTTPS 운영에서는 Secure를 사용한다.

`BACKEND_ORIGIN`은 서버 빌드 설정이며 `NEXT_PUBLIC_` 접두사를 사용하지 않는다. 변경하면 재배포한다. Vercel 빌드에서 값이 없거나 HTTPS가 아니면 빌드를 중단한다. 로컬 개발·백엔드 없는 CI 테스트에서는 `http://127.0.0.1:8080`이 기본값이다.

공유 링크의 대표 공개 도메인을 프론트 도메인으로 사용할 경우 백엔드의 `APP_PUBLIC_BASE_URL`도 그 도메인에 맞춘다. Vercel production 배포 후 실제 AWS와 참가 등록·세션 유지·CSRF 쓰기·공유 페이지·분석 SSE 재연결을 확인한다. 현재 모의 단위 테스트와 UI E2E는 이 실배포 통합 검증을 대체하지 않는다.

## 운영

- 개발: `vercel pull --environment=preview --git-branch=develop` → `vercel build` → `vercel deploy --prebuilt --environment=preview` → 선택한 개발 도메인 alias 갱신.
- 운영: `vercel pull --environment=production` → `vercel build --prod` → `vercel deploy --prebuilt --prod`.
- 브랜치별 Environment와 배포 concurrency 그룹을 사용한다. 개발과 운영은 독립적으로 실행하며 진행 중인 배포는 취소하지 않는다.
- 자동 실행 경로는 frontend/**와 워크플로 파일이다. 공통 문서만 변경되면 배포하지 않으며 필요하면 수동 실행한다.
- 실패한 배포는 Vercel에서 로그를 확인하고, 서비스 복구가 필요하면 이전 정상 deployment로 rollback한다.
- AWS 백엔드 파이프라인·Terraform 설정·DB는 프론트 배포에서 변경하지 않는다.
- 프로젝트 `santacut`의 개발 배포 및 `dev.santacut.me` alias는 설정되어 있다. 운영 프론트와 운영 API 준비 여부는 별개이며, 운영 API 연결 완료를 프론트 배포 성공만으로 판단하지 않는다.

## 현재 환경 구분

| 환경 | 프론트 | 백엔드 설정 |
| --- | --- | --- |
| 개발(develop / Preview) | `https://dev.santacut.me` | `BACKEND_ORIGIN=https://api-dev.santacut.me` |
| 로컬 | `http://localhost:3000` | 미설정 시 `http://127.0.0.1:8080` |
| 운영(main / Production) | `https://santacut.me` | 운영 API 준비 후 `https://api.santacut.me` 연결·통합 확인 필요 |

GA 환경 변수와 재배포 조건은 [GA 운영 안내](analytics.md)를 참고한다. 문서도 `frontend/**`에 있으면 위 CI/CD 경로 필터에 포함된다.

## 검증

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm test
pnpm build
CI=true pnpm test:e2e
```

[Preview 환경 설정](https://vercel.com/docs/cli/pull)·[Preview 도메인 연결](https://vercel.com/kb/guide/how-to-alias-a-preview-deployment-using-the-cli)과 [공식 Vercel GitHub Actions 가이드](https://vercel.com/kb/guide/how-can-i-use-github-actions-with-vercel)와 [외부 rewrite 문서](https://vercel.com/docs/routing/rewrites)를 따른다.
