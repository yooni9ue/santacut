# 프론트엔드 Vercel 배포

프론트는 Vercel, 백엔드는 AWS에 배포한다. AWS 서버·도메인·권한은 [santacut-infra](https://github.com/yooni9ue/santacut-infra)가 관리하며, 이 프론트 파이프라인은 백엔드 서버에 SSH로 접속하거나 컨테이너·Caddy 설정을 바꾸지 않는다.

## CI/CD 흐름

| 시점 | 동작 |
| --- | --- |
| frontend 관련 PR | 고정 Node.js 24·pnpm 설치 → 린트·타입·단위 테스트 → 프로덕션 빌드 → 모바일 Chromium/WebKit E2E |
| main 반영 | 위 검증 통과 후, 배포 활성화 상태면 Vercel production 설정으로 빌드·배포 |
| 수동 실행 | main에서 deploy=true를 선택하면 동일 검증·배포. 활성화 변수도 true여야 함 |

배포는 GitHub Actions가 담당한다. `frontend/vercel.json`의 `git.deploymentEnabled=false`는 Vercel Git 자동 배포를 꺼서 CI 전에 배포되거나 이중 배포되는 것을 방지한다. PR preview 배포는 현재 파이프라인에 포함하지 않는다.

## 최초 Vercel 설정

1. Vercel에서 이 저장소의 Next.js 프로젝트를 생성한다.
2. 프로젝트 **Root Directory를 frontend**로 지정하고 Node.js 24를 선택한다. CLI는 저장소 루트에서 연결된 프로젝트의 Root Directory 설정을 사용한다.
3. Production 환경 변수 `BACKEND_ORIGIN`에 AWS 백엔드의 공개 HTTPS origin을 넣는다. 예시: `https://api.example.com`. 경로·쿼리·사용자 인증 정보를 넣지 않는다. Preview를 별도로 만들 경우 해당 환경에도 맞는 AWS 주소가 필요하다.
4. GitHub의 `vercel-production` Environment에 아래 비밀값을 등록한다. 운영 승인자와 main 브랜치 제한을 설정할 수 있다.
5. GitHub 저장소 Variable `VERCEL_DEPLOY_ENABLED=true`로 배포를 활성화한다. 활성화 전에는 CI만 실행한다.
6. main의 수동 실행으로 최초 배포를 검증한다. 이후 main push는 검증 성공 후 자동 배포한다.

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

- 배포 단계는 `vercel pull` → `vercel build --prod` → `vercel deploy --prebuilt --prod` 순서다.
- `vercel-production` Environment와 동일 배포 concurrency 그룹을 사용해 production 배포가 겹치지 않도록 한다.
- 실패한 배포는 Vercel에서 로그를 확인하고, 서비스 복구가 필요하면 이전 정상 deployment로 rollback한다.
- AWS 백엔드 파이프라인·Terraform 설정·DB는 프론트 배포에서 변경하지 않는다.
- 실제 배포에는 Vercel 프로젝트·토큰·AWS HTTPS 백엔드가 필요하다. 이 PR에서 실제 계정 설정이나 배포는 수행하지 않았다.

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

[공식 Vercel GitHub Actions 가이드](https://vercel.com/kb/guide/how-can-i-use-github-actions-with-vercel)와 [외부 rewrite 문서](https://vercel.com/docs/routing/rewrites)를 따른다.
