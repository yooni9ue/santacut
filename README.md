# 🎅 산타컷 (SantaCut)

> 너 올해 산타컷 통과 가능?

카카오톡 대화나 인스타그램 활동에서 추출한 데이터를 AI가 심사해 산타 점수, 랭킹, 선물 도착 예정 시각을 보여주는 크리스마스 서비스입니다. 점수와 배송 시각은 재미를 위한 시뮬레이션입니다.

## 저장소 구성

이 저장소는 백엔드와 웹 프론트엔드를 함께 관리하는 모노레포입니다. 각 앱은 독립된 도구 체인과 배포 산출물을 유지하고, 프론트엔드는 백엔드의 OpenAPI 계약으로 타입을 맞춥니다.

```text
backend/       Kotlin + Spring Boot API, JPA, SQLite, SSE, Swagger
frontend/      React + TypeScript + Vite 웹 앱
docs/          백엔드 기획, 시스템 아키텍처, 배포 운영서
deploy/        Docker Compose, Caddy, 배포 설정
scripts/       운영 및 배포 스크립트
```

## 개발 환경

- 백엔드: JDK 25 (Gradle Wrapper 사용)
- 프론트엔드: Node.js 24 및 npm
- 선택 사항: 로컬 AI 분석용 Ollama와 `qwen2.5:3b`

### 실행

터미널 1에서 백엔드를 실행합니다.

```powershell
cd backend
./gradlew.bat bootRun
```

터미널 2에서 프론트엔드 의존성을 설치하고 개발 서버를 시작합니다.

```powershell
npm ci
npm run dev
```

프론트엔드는 `http://localhost:5173`, API는 `http://localhost:8080`에서 실행됩니다. Vite 개발 서버는 API 경로를 백엔드로 프록시합니다. 프록시 주소를 바꾸려면 `frontend/.env.local`에 `VITE_API_PROXY_TARGET=http://localhost:8080`을 설정합니다.

백엔드 단독 실행은 `backend/`에서 `./gradlew.bat bootRun`을 사용합니다. 프론트엔드 단독 빌드는 루트에서 `npm run check`와 `npm run build`를 실행합니다.

## OpenAPI와 프론트엔드 타입

Swagger UI는 `http://localhost:8080/swagger-ui.html`, OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 확인할 수 있습니다. 프론트엔드의 API 클라이언트는 `frontend/src/shared/api/schema.d.ts`의 생성 타입을 사용합니다.

백엔드를 실행한 상태에서 계약 타입을 새로 생성합니다.

```powershell
npm run api:types
```

엔드포인트별 Swagger 설명은 백엔드의 전용 메타 애노테이션에 작성합니다. API 계약을 수정한 PR에는 생성된 타입 변경도 함께 포함합니다.

## 현재 구현 범위

- 닉네임 중복 방지, 서울 25개 구 등록, HttpOnly 세션 쿠키
- 정규화된 이벤트 JSON 분석 요청과 SSE 상태·결과 스트리밍
- Spring AI + Ollama 점수 생성, 점수 검증, 랭킹 및 배송 ETA 저장
- 내 결과·랭킹 조회, 공유 링크 발급·폐기, 공유 HTML 및 PNG 카드
- 참가자 등록, 분석, 랭킹, 공유를 연결하는 React 웹 화면

카카오톡·인스타그램 원본 내보내기 파일을 직접 파싱하는 기능은 아직 구현되지 않았습니다. 현재 분석 API는 서버 문서에 정의된 정규화 이벤트 JSON을 받습니다.

## 기술 구성

- Java 25, Kotlin, Spring Boot 4, Spring MVC, Spring Data JPA
- SQLite 파일 DB, Hibernate `ddl-auto: update`
- Spring AI + Ollama (`qwen2.5:3b` 기본값)
- React 19, TypeScript, Vite, OpenAPI 기반 API 타입
- GitHub Actions: 백엔드 테스트·패키징, 프론트엔드 타입 검사·빌드, OpenAPI 생성 타입 불일치 검사

요청 대화 데이터는 점수 생성 후 작업 payload에서 삭제합니다. 사용자 대화 원문을 애플리케이션 로그에 남기지 않습니다.

## 설계 문서

- [백엔드 기획](docs/backend-design.md)
- [시스템 아키텍처](docs/system-architecture.md)
- [저예산 배포 운영서](docs/deployment.md)
- [프론트엔드 구조와 개발 규칙](frontend/README.md)
