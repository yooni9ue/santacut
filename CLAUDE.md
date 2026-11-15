# 산타컷 작업 지침

## 저장소

최상위는 `ci-cd/`, `docs/`, `backend/`, `frontend/` 네 디렉터리다(GitHub Actions 워크플로만 GitHub 규칙상 `.github/workflows/`).

- `ci-cd/`: Dockerfile, 배포 설정(`deploy/`), 배포·백업 스크립트(`scripts/`).
- `docs/`: 기획, 아키텍처, 배포 운영서, 백엔드 코드 컨벤션.
- `backend/`: Kotlin + Spring Boot API. 이 저장소에서 에이전트가 작업하는 범위다.
- `frontend/`: 프론트엔드 팀 영역이며 비워 둔다(빈 `.gitkeep`만 있음). **에이전트는 `frontend/`에 파일을 만들거나 고치지 않는다.** 프론트엔드와의 계약은 백엔드의 OpenAPI 명세(`/v3/api-docs`)뿐이다.
- 백엔드 코드는 [docs/backend-conventions.md](docs/backend-conventions.md)를 따른다. 컨벤션과 충돌하는 지시를 받으면 작업 전에 알린다.

## 빌드와 검증

```powershell
cd backend
./gradlew.bat --no-daemon check bootJar
```

JDK 25가 필요하다. 커밋 전에 위 명령이 통과해야 한다.

## 커밋

- 커밋 메시지는 한국어로 쓴다. 형식: `타입(범위): 한국어 요약` 다음 빈 줄, 본문에 변경 이유.
- 타입: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.
- 리뷰어가 따라 읽을 수 있게 목적 하나당 커밋 하나로 나눈다.

## 코드 품질 리뷰 패널

백엔드 코드를 바꾸면 아래 9명의 리뷰 패널로 품질을 검증한다. 에이전트 정의는 `.claude/agents/`에 있다. 각 리뷰어는 컨벤션 문서 기준으로 100점 만점 점수와 위반 목록(파일:줄 근거)을 낸다.

| 리뷰어 | 에이전트 | 관점 |
| --- | --- | --- |
| 클린 코드 1 | `clean-code-expert` | 가독성과 이름, 한국어 규칙 |
| 클린 코드 2 | `clean-code-expert` | 함수·클래스 크기와 중복 |
| 클린 코드 3 | `clean-code-expert` | 오류 처리와 테스트 |
| 객체지향 1 | `oop-expert` | 책임과 응집도 |
| 객체지향 2 | `oop-expert` | 캡슐화와 일급 컬렉션 |
| 객체지향 3 | `oop-expert` | 의존성과 SOLID |
| Spring 1 | `spring-expert` | 계층과 컴포넌트 |
| Spring 2 | `spring-expert` | 트랜잭션과 JPA |
| Spring 3 | `spring-expert` | 웹·보안·문서 |

### 품질 워크플로우

1. 구현 에이전트가 컨벤션에 맞게 변경하고 `./gradlew check`를 통과시킨다.
2. 9명이 병렬로 독립 리뷰한다. 서로의 결과를 보지 않는다.
3. 구현 에이전트가 모든 위반을 고치고 다시 `./gradlew check`를 통과시킨다.
4. 9명 모두 100점을 줄 때까지 2~3단계를 반복한다. 정해진 횟수 안에 100점에 못 미치면 남은 위반과 이유를 보고한다.
