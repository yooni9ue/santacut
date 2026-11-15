# 🎅 산타컷 (SantaCut)

> 너 올해 산타컷 통과 가능?

카카오톡 대화나 인스타그램 활동에서 추출한 데이터를 AI가 심사해 산타 점수, 랭킹, 선물 도착 예정 시각을 보여주는 크리스마스 서비스입니다. 점수와 배송 시각은 재미를 위한 시뮬레이션입니다.

## 저장소 구성

백엔드와 프론트엔드가 한 저장소를 함께 쓰는 모노레포입니다. 앱마다 디렉터리, 도구 체인, 빌드 경계를 따로 둡니다.

```text
backend/       Kotlin + Spring Boot API (presentation → application → domain → infrastructure)
frontend/      프론트엔드 팀 작업 영역 (프론트엔드 팀이 구성)
docs/          기획, 시스템 아키텍처, 배포 운영서, 백엔드 코드 컨벤션
deploy/        Docker Compose, Caddy, 배포 설정
scripts/       운영 및 배포 스크립트
```

프론트엔드 코드는 `frontend/` 아래에 프론트엔드 팀이 직접 구성합니다. 백엔드는 프론트엔드 소스나 빌드 도구에 의존하지 않고, 프론트엔드는 아래 OpenAPI 계약만 보고 개발할 수 있습니다.

## 백엔드 개발 환경

- JDK 25 (Gradle Wrapper 사용)
- 선택 사항: 로컬 AI 분석용 Ollama와 `qwen2.5:3b`

```powershell
cd backend
./gradlew.bat bootRun    # 실행
./gradlew.bat check      # 테스트와 검증
```

API는 `http://localhost:8080`에서 실행되고 SQLite 파일은 `backend/data/santacut.db`에 생성됩니다.

## API 계약 (프론트엔드 연동)

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

엔드포인트마다 전용 Swagger 애노테이션에 요약, 설명, 요청·응답 스키마, 오류 코드를 한국어로 적습니다. 프론트엔드는 OpenAPI JSON으로 타입을 생성해 사용할 수 있습니다. 로컬 개발 서버에서 API를 호출할 때는 `/v1`, `/s`, `/swagger-ui`, `/v3` 경로를 `http://localhost:8080`으로 프록시하면 세션 쿠키와 CSRF 쿠키가 같은 출처로 동작합니다.

## 현재 구현 범위

- 닉네임 중복 방지, 서울 25개 구 등록, HttpOnly 세션 쿠키
- 정규화된 이벤트 JSON 분석 요청과 SSE 상태·결과 스트리밍
- Spring AI + Ollama 점수 생성, 점수 검증, 랭킹 및 배송 ETA 저장
- 내 결과·랭킹 조회, 공유 링크 발급·폐기, 공유 HTML 및 PNG 카드

카카오톡·인스타그램 원본 내보내기 파일을 직접 파싱하는 기능은 아직 구현되지 않았습니다. 현재 분석 API는 정규화된 이벤트 JSON을 받습니다.

## 기술 구성

- Java 25, Kotlin, Spring Boot 4, Spring MVC, Spring Data JPA
- SQLite 파일 DB, Hibernate `ddl-auto: update`
- Spring AI + Ollama (`qwen2.5:3b` 기본값)
- springdoc-openapi (Swagger UI)
- GitHub Actions: 백엔드 테스트·패키징, 이미지 발행, 수동 배포

요청 대화 데이터는 점수 생성 후 작업 payload에서 삭제합니다. 사용자 대화 원문을 애플리케이션 로그에 남기지 않습니다.

## 설계 문서

- [백엔드 기획](docs/backend-design.md)
- [시스템 아키텍처](docs/system-architecture.md)
- [저예산 배포 운영서](docs/deployment.md)
- [백엔드 코드 컨벤션](docs/backend-conventions.md)
