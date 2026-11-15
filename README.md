# 🎅 산타컷 (SantaCut)

> 너 올해 산타컷 통과 가능?

카카오톡 대화나 인스타그램 활동에서 추출한 데이터를 AI가 심사해 산타 점수, 랭킹, 선물 도착 예정 시각을 보여주는 크리스마스 서비스입니다. 점수와 배송 시각은 재미를 위한 시뮬레이션입니다.

## 저장소 구성

```text
backend/     Kotlin + Spring Boot API와 백엔드 문서·CI/CD·설정 (backend/CLAUDE.md 참고)
client/      Next.js 모바일 웹과 프론트엔드 설정
frontend/    기존 프론트엔드 작업 영역 (비어 있음)
docs/        프론트엔드가 참고하는 문서
```

GitHub Actions 워크플로는 GitHub 규칙상 `.github/workflows/`에 있으며, 백엔드 워크플로(`backend.yml`)는 `backend/` 변경에만 실행됩니다. 프론트엔드 팀은 자체 워크플로 파일을 추가하면 됩니다.

## 프론트엔드

- Node.js 24 LTS (`client/.nvmrc`)
- Next.js App Router, React, TypeScript, Tailwind CSS, ESLint
- `client/src/` 구조 및 `@/*` 경로 별칭
- [API 연동 가이드](docs/frontend-api-guide.md)
- Swagger UI: `http://localhost:8080/swagger-ui.html`, OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Node.js 24를 설치한 뒤 실행합니다. nvm을 사용한다면 `client/`에서 `nvm install`과 `nvm use`를 먼저 실행하세요.

```bash
cd client
npm ci
npm run dev
```

[http://localhost:3000](http://localhost:3000)에서 확인할 수 있습니다. 현재 기본 화면에는 환경 변수가 필요하지 않습니다. 프론트엔드와 백엔드 연동은 아직 구현되지 않았습니다.

`client/` 디렉터리에서 검사·빌드를 실행합니다.

```bash
npm run lint
npm run typecheck
npm run build
npm start
```

`npm start`는 빌드 결과를 실행합니다.

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
