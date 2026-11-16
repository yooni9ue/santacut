# 🎅 산타컷 (SantaCut)

> 너 올해 산타컷 통과 가능?

카카오톡 대화나 인스타그램 활동에서 추출한 데이터를 AI가 심사해 산타 점수, 랭킹, 선물 도착 예정 시각을 보여주는 크리스마스 서비스입니다. 점수와 배송 시각은 재미를 위한 시뮬레이션입니다.

## 저장소 구성

```text
backend/     Kotlin + Spring Boot API와 백엔드 문서·CI/CD·설정 (backend/CLAUDE.md 참고)
frontend/    Next.js 모바일 웹·설정·프론트 전용 문서(docs/)
docs/        기획·디자인·API·개발 규칙과 공통 문서
```

GitHub Actions 워크플로는 GitHub 규칙상 `.github/workflows/`에 있으며, 백엔드 워크플로(`backend.yml`)는 `backend/` 변경에만 실행됩니다. 프론트엔드 CI/CD는 `.github/workflows/frontend.yml`에서 독립적으로 실행됩니다.

## 프론트엔드

- Node.js 24 LTS (`frontend/.nvmrc`)
- Next.js App Router, React, TypeScript, Tailwind CSS, ESLint
- pnpm 12.21.0 (`packageManager` 고정, `pnpm-lock.yaml`)
- TanStack Query + Axios
- Vitest + Testing Library, Playwright (모바일 Chromium/WebKit)
- `frontend/src/` 구조 및 `@/*` 경로 별칭
- [API 연동 가이드](docs/api/frontend-api-guide.md)
- Swagger UI: dev `https://api-dev.santacut.me/swagger-ui.html`, 로컬 `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: dev `https://api-dev.santacut.me/v3/api-docs`, 로컬 `http://localhost:8080/v3/api-docs`

Node.js 24와 pnpm을 설치한 뒤 실행합니다 (`npm install -g pnpm@12.21.0`). nvm을 사용한다면 `frontend/`에서 `nvm install`과 `nvm use`를 먼저 실행하세요.

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

[http://localhost:3000](http://localhost:3000)에서 확인할 수 있습니다. 현재 메인 디자인 미리보기에는 환경 변수가 필요하지 않습니다. 화면에서 실제 API를 호출하는 기능은 아직 구현되지 않았습니다.

`/v1`, `/s`, Swagger 경로는 기본적으로 `http://127.0.0.1:8080`으로 프록시합니다. 다른 서버를 사용하려면 `frontend/.env.local`에 `BACKEND_ORIGIN`을 설정한 뒤 개발 서버를 재시작하세요. 브라우저는 같은 출처의 상대 경로로 요청합니다.

`frontend/` 디렉터리에서 검사·빌드를 실행합니다.

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm start
```

`pnpm start`는 빌드 결과를 실행합니다.

## 프론트엔드 공통 코드와 테스트

- `frontend/src/app/providers.tsx`: TanStack Query Provider
- `frontend/src/lib/query-client.ts`: 서버 요청 간 캐시 분리, 브라우저 캐시 유지, 조회 재시도 정책
- `frontend/src/lib/api.ts`: 브라우저용 Axios 인스턴스, 쓰기 전 CSRF 발급, 공통 오류 메시지
- `api.get("/me")`처럼 `/v1` 이후 경로를 사용합니다. 세션 쿠키를 보내고 쓰기 요청은 자동으로 `/v1/csrf`에서 토큰을 받아 헤더에 넣습니다. 실패한 쓰기는 자동 재시도하지 않습니다.
- Server Component에서 사용자별 API를 호출할 때는 쿠키를 명시적으로 전달하는 별도 요청 단위 클라이언트가 필요합니다.

```bash
cd frontend
pnpm test                 # 단위·컴포넌트 테스트
pnpm test:watch           # 변경 감지
pnpm exec playwright install chromium webkit  # 브라우저 최초 설치
pnpm test:e2e             # 모바일 Chromium + WebKit
```

E2E는 3100번 포트에서 서버를 자동 실행합니다. 로컬은 개발 서버, CI는 Next.js 프로덕션 빌드를 사용합니다. CI 방식으로 로컬 검증하려면 `pnpm build` 후 `CI=true pnpm test:e2e`를 실행합니다. 현재 테스트는 실제 백엔드 없이 공통 API 코드와 메인 미리보기 동작을 검증합니다.

## 문서 찾아보기

[문서 목차](docs/README.md)에서 기획, 디자인, API, 개발 규칙을 목적별로 찾을 수 있습니다.

프론트엔드·백엔드 공통 개발 흐름은 [AI-DLC 안내](docs/ai-dlc/README.md)를 따릅니다. `AI-DLC로 <기능>을 구현해줘`처럼 요청하면 작업 크기에 맞춰 기획·설계·구현·검증·PR 인계를 진행합니다.

## UI·UX 기획

[UI·UX 기획서](docs/planning/ui-ux-plan.md)에서 화면 구성, 사용자 흐름, 상태별 안내, 공유 경험과 구현 우선순위를 확인할 수 있습니다. 현재 화면의 시각 기준은 [디자인 방향](docs/design/visual-direction.md)에 정리합니다.

## 백엔드

```powershell
cd backend
./gradlew.bat bootRun    # 실행 (JDK 25)
./gradlew.bat check      # 테스트와 검증
```

- [백엔드 기획](backend/docs/backend-design.md)
- [시스템 아키텍처](backend/docs/system-architecture.md)
- [저예산 배포 운영서](backend/docs/deployment.md)
- [백엔드 코드 컨벤션](backend/docs/backend-conventions.md)


## 디자인 미리보기

- `/`: 화이트 배경, 손글씨 메인 제목, 굴뚝 산타와 판정 예시
- `/classic`: 초기 우체국 시안
- `/original`: 순한맛 손그림 시안
- `/post-office`: 북극 심사국 시안

메인 시작 버튼은 준비 중 안내를 표시합니다. 산타 꺼내기와 판정 예시 전환은 브라우저에서 동작하며 실제 AI 분석을 수행하지 않습니다.


## 프론트엔드 CI/CD

- PR·main 변경: 린트, 타입 검사, 단위·컴포넌트 테스트, 모바일 Chromium/WebKit, 프로덕션 빌드
- 프론트엔드: Vercel / 백엔드: AWS
- Vercel 배포를 활성화하면 main 검증 성공 후 production에 배포
- 브라우저의 `/v1` 요청은 Next.js rewrite가 AWS의 `BACKEND_ORIGIN`으로 프록시
- [Vercel 배포 설정과 운영서](frontend/docs/deployment.md)

## PR 작성

[PR 작성 규칙](docs/development/pr-guide.md)과 [PR 양식](docs/pull_request_template.md)을 따릅니다. 새 프론트엔드 전용 문서는 `frontend/docs/`에 두고 기존 문서는 별도 요청 없이 이동하지 않습니다.
