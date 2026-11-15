# 🎅 산타컷 (SantaCut)

> 너 올해 산타컷 통과 가능?

카카오톡 대화나 인스타그램 활동에서 추출한 데이터를 AI가 심사해 산타 점수, 랭킹, 선물 도착 예정 시각을 보여주는 크리스마스 서비스입니다. 점수와 배송 시각은 재미를 위한 시뮬레이션입니다.

## 현재 구현 범위

- 닉네임 중복 방지, 서울 25개 구 등록, 세션 쿠키 발급
- 정규화된 이벤트 JSON 분석 작업 제출과 SSE 상태·결과 스트리밍
- Spring AI를 통한 Ollama 로컬 모델 점수 요청
- 점수 검증, 순위와 배송 ETA 스냅샷 저장
- 결과 조회, 공유 토큰, 공유용 HTML 및 PNG 카드

현재 API는 카카오톡·인스타그램 원본 내보내기 파일을 직접 파싱하지 않습니다. 먼저 내보내기 방법을 안내하는 화면과 각 플랫폼 JSON 파서가 필요합니다. 서버에는 `AnalysisInput` 형식의 정규화 이벤트를 보내야 합니다.

## 기술 구성

- Java 25, Kotlin, Spring Boot 4, Spring MVC
- SQLite 파일 DB, Hibernate `ddl-auto: update`
- 영속 계층은 Spring Data JPA Repository로 통일 (참가자·세션·작업·예산·점수·랭킹·공유)
- Spring AI + Ollama (`qwen2.5:3b` 기본값)

요청 데이터는 점수 생성에만 사용하고 분석 완료 후 작업 payload를 지웁니다. 사용자 대화 원문을 로그로 남기지 않습니다.

## 로컬 실행

JDK 25와 Ollama가 필요합니다. Ollama를 시작한 뒤 모델을 준비합니다.

```powershell
ollama pull qwen2.5:3b
```

저장소 루트에서 애플리케이션을 실행합니다.

```powershell
./gradlew.bat bootRun
```

기본 주소는 `http://localhost:8080`이며 헬스체크는 `/actuator/health/readiness`입니다. SQLite 파일은 `data/santacut.db`에 생성됩니다. Ollama 주소와 모델은 `OLLAMA_BASE_URL`, `OLLAMA_MODEL`로 바꿀 수 있습니다.

## 설계 문서

- [백엔드 기획](docs/backend-design.md)
- [시스템 아키텍처](docs/system-architecture.md)
- [저예산 배포 운영서](docs/deployment.md)
