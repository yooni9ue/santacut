<p align="center">
  <img src="docs/images/banner.svg" alt="SantaCut Backend: 대화·활동을 AI가 채점하고 순위를 매기는 시즌 API" width="100%">
</p>

<p align="center">
  <b>대화 기록으로 산타 순위를 매기는 API.</b><br>
  인프라는 <a href="https://github.com/yooni9ue/santacut-infra"><code>santacut-infra</code></a>에 있다.
</p>

## 한눈에 보기

<p align="center"><img src="docs/images/stats.svg" alt="핵심 숫자: 엔드포인트 17개, 테이블 9개, 자동 테스트 265개, 오류 코드 29개, 지역 271개, 참가자당 하루 3회. 검증 작업자 1초, 분석 작업자 3초, 작업 임대 3분, 대기 작업 만료 24시간. 설계 선택 6가지." width="100%"></p>

## 할 수 있는 일

- 닉네임으로 참가한다. 세션은 HttpOnly 쿠키다.
- 지역은 두 단계로 고른다. 시·도 16개, 시·군·구 255개.
- 카카오톡 TXT나 인스타그램 JSON 원본을 그대로 올린다. 20MB까지.
- 서버가 원본을 파싱한다. 본인 메시지만 AI에 보낸다.
- AI가 점수를 매긴다. 순위와 선물 도착 시각은 서버가 계산한다.
- 진행 상태는 폴링으로 본다. 창을 닫아도 다시 이어 본다.
- 순위표는 공개다. 50줄씩 cursor로 넘긴다. 동점은 `공동 N위`.
- 공유 링크는 그 순간의 결과를 고정한다. 취소한 링크는 안내 페이지로 바뀐다.
- 시즌 마감 전에는 지역을 바꿀 수 있다. 내 데이터는 언제든 지운다.

## 아키텍처

<p align="center"><img src="docs/images/architecture.svg" alt="요청 흐름: 클라이언트, Caddy, Spring Boot 4계층, 분석 워커, SQLite, AI 채점, 공유 페이지" width="100%"></p>

- 분석은 큐에 넣고, 화면은 진행 상태를 폴링으로 조회한다.
- 작업자는 둘이다. 원본 검증 1초, AI 채점 3초 간격.
- 지금 코드는 Spring AI + Ollama(`qwen2.5:3b`)다. 운영은 Bedrock을 계획한다.

## 계층 구조

<p align="center"><img src="docs/images/layers.svg" alt="4계층과 5도메인 격자, 의존 방향, facade와 service 규칙" width="100%"></p>

- 패키지는 계층 → 도메인 → 역할 순서다.
- 자세한 규칙은 [코드 컨벤션](docs/backend-conventions.md)에 있다.

## 데이터 모델

<p align="center"><img src="docs/images/erd.svg" alt="ERD: regions, participants, sessions, analysis_jobs, analysis_uploads, assessments, ai_daily_budget, result_snapshots, shares 9개 테이블과 외래 키 관계" width="100%"></p>

- 기준은 [`schema.sql`](src/main/resources/schema.sql)이다.
- 원본 파일은 `analysis_uploads`에 잠깐 머문다. 검증이 끝나면 지운다.
- 지역은 `parent_code`로 시·도와 시·군·구를 잇는다.

## 분석 흐름

<p align="center"><img src="docs/images/analysis-flow.svg" alt="분석 흐름: 원본 업로드, 접수 검사, 저장, 원본 검증·정규화, 대기열, 작업 임대, AI 채점, 한 트랜잭션 완료, VALIDATING부터 SUCCEEDED까지 상태 폴링" width="100%"></p>

- 원본은 접수 때 한 번, 검증 작업자가 한 번 더 본다.
- 빈 파일·zip·바이너리는 바로 `422`. 20MB를 넘으면 `413`.
- 평가 저장과 순위 계산은 한 트랜잭션이다.
- 하루 예산을 넘으면 워커가 작업을 꺼내지 않는다.
- 24시간 넘게 기다린 작업은 만료된다. AI가 꺼져 있어도.

<details>
<summary>시퀀스 다이어그램</summary>

```mermaid
sequenceDiagram
    autonumber
    actor C as 클라이언트
    participant API as AnalysisController
    participant DB as SQLite
    participant V as 검증 워커
    participant W as 분석 워커
    participant AI as AI 모델
    C->>API: POST /v1/analyses/files (multipart, Idempotency-Key)
    API->>DB: 원본 + 작업 저장 (VALIDATING)
    API-->>C: 202 Accepted
    loop 1초마다
        V->>DB: 원본 꺼내기
    end
    V->>V: 파싱 · 본인 메시지만
    V->>DB: 작업 QUEUED · 원본 삭제
    loop 3초마다
        W->>DB: 예산 확인 · 작업 임대
    end
    W->>AI: 채점 요청
    AI-->>W: 구조화 점수
    W->>DB: 평가 저장 + 순위 재계산 (한 트랜잭션)
    loop 화면이 몇 초마다
        C->>API: GET /v1/analyses/{id}
        API->>DB: 상태 조회
        API-->>C: 진행 단계 (VALIDATING → QUEUED → ANALYZING → SUCCEEDED)
    end
```

</details>

## API

<p align="center"><img src="docs/images/api.svg" alt="API 지도: participant, session, analysis, ranking, share 도메인별 엔드포인트 17개 전부와 메서드, 공개·세션·CSRF·멱등키 표시" width="100%"></p>

- 필드 정의는 Swagger(`/swagger-ui.html`)가 기준이다.
- 호출 순서는 [API 연동 가이드](../docs/frontend-api-guide.md)에 있다.

<details>
<summary>엔드포인트 표 (텍스트)</summary>

| 메서드 | 경로 | 하는 일 | 인증 |
| --- | --- | --- | --- |
| GET | `/v1/csrf` | CSRF 토큰 발급 | 공개 |
| GET | `/v1/regions` | 지역 목록(`?parent=`로 하위 지역) | 공개 |
| GET | `/v1/nicknames/availability` | 닉네임 가용성 확인 | 공개 |
| POST | `/v1/participants` | 참가 등록 (`201`) | 공개 · CSRF |
| GET | `/v1/me` | 내 참가 정보 | 세션 |
| PATCH | `/v1/me/region` | 내 지역 변경 (시즌 마감 전) | 세션 · CSRF |
| DELETE | `/v1/me` | 내 데이터 삭제 (`204`) | 세션 · CSRF |
| POST | `/v1/analyses` | 이벤트 JSON으로 분석 요청 (`202`) | 세션 · CSRF · `Idempotency-Key` |
| POST | `/v1/analyses/files` | 원본 파일로 분석 요청 (`202`, 20MB) | 세션 · CSRF · `Idempotency-Key` |
| GET | `/v1/analyses/{id}` | 분석 진행 상태 | 세션 |
| GET | `/v1/me/analysis` | 내 최근 분석 상태 | 세션 |
| GET | `/v1/me/result` | 내 점수·순위·도착 시각 | 세션 |
| GET | `/v1/rankings` | 랭킹 50명씩(cursor) | 공개 |
| POST | `/v1/shares` | 공유 링크 생성·재사용 | 세션 · CSRF |
| DELETE | `/v1/shares/{token}` | 공유 링크 폐기 (`204`) | 세션 · CSRF |
| GET | `/s/{token}` | 공개 공유 HTML | 공개 |
| GET | `/s/{token}/image.png` | 공개 공유 이미지 | 공개 |

</details>

## 품질 게이트

<p align="center"><img src="docs/images/quality.svg" alt="품질 게이트: gradlew check, 구조 테스트, 컨벤션 확인 명령, 9인 리뷰 패널" width="100%"></p>

- 자동 검사를 통과한 뒤 리뷰어 9명이 판정한다.

## 기술 스택

<p align="center"><img src="docs/images/tech-stack.svg" alt="기술 스택: Kotlin 2.3.21, Spring Boot 4.1.2, Java 25, SQLite 3.51.1, Spring AI 2.0.0, Springdoc 3.2.0, ArchUnit 1.5.1" width="100%"></p>

## 실행

JDK 25가 필요하다. AI 채점에는 Ollama(`localhost:11434`)가 필요하다.

```powershell
cd backend
./gradlew.bat bootRun                      # 로컬 실행 (:8080)
./gradlew.bat --no-daemon check bootJar    # 커밋 전 검증
```

- 로컬 DB는 `./data/santacut.db`, 헬스 체크는 `/actuator/health`다.

## 데이터 출처

전국 시·도·시·군·구 지역과 대표 좌표([`regions.sql`](src/main/resources/regions.sql))는 2026-10-01 기준 행정동 경계로 만들었다. 생성 스크립트는 [`scripts/regions/generate_regions.py`](scripts/regions/generate_regions.py)다.

> 본 데이터는 통계청 통계지리정보서비스(SGIS, https://sgis.kostat.go.kr)에서 공공누리 제1유형으로 개방한 행정동 경계를 가공한 것이며(가공: vuski/admdongkor, https://github.com/vuski/admdongkor), CC BY 4.0으로 배포됩니다.

## 더 읽기

[시스템 아키텍처](docs/system-architecture.md) · [백엔드 설계](docs/backend-design.md) · [코드 컨벤션](docs/backend-conventions.md) · [배포](docs/deployment.md) · [API 연동 가이드](../docs/frontend-api-guide.md) · [작업 지침](CLAUDE.md)
