<p align="center">
  <img src="docs/images/banner.svg" alt="SantaCut Backend: 대화 기록으로 산타 순위를 매기는 시즌 API. 12/25 시즌 마감까지 D-39(2026-11-16 기준)" width="100%">
</p>

<p align="center">
  <b>대화 기록으로 산타 순위를 매기는 API.</b><br>
  원본을 받아 AI가 채점하고, 순위와 선물 도착 시각은 서버가 계산한다.
</p>

<p align="center">
  <img alt="Kotlin 2.3.21" src="https://img.shields.io/badge/Kotlin-2.3.21-134236?logo=kotlin&logoColor=white">
  <img alt="Spring Boot 4.1.2" src="https://img.shields.io/badge/Spring%20Boot-4.1.2-B5123E?logo=springboot&logoColor=white">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-12263F">
  <img alt="SQLite" src="https://img.shields.io/badge/SQLite-3.51.1-0C1B2E?logo=sqlite&logoColor=white">
  <img alt="Amazon Bedrock" src="https://img.shields.io/badge/Amazon%20Bedrock-Claude%20Haiku%204.5-1E3A5C">
  <img alt="시즌 마감 2026-12-25" src="https://img.shields.io/badge/season-2026--12--25-8A6410">
</p>

인프라는 [`santacut-infra`](https://github.com/yooni9ue/santacut-infra)에 있다.

<p align="center"><img src="docs/images/divider-snow.svg" alt="" width="100%"></p>

## 한눈에 보기

<p align="center"><img src="docs/images/stats.svg" alt="핵심 숫자: HTTP 엔드포인트 17개(dev 전용 3개 별도), SQLite 테이블 9개, 자동 테스트 321개, 오류 코드 30개, 지역 271개, 참가자당 24시간 3회 제출(앱 AI 호출 한도 없음). 검증 작업자 1초, 분석 작업자 3초, 작업 임대 3분, 대기 작업 만료 24시간. 설계 선택 6가지." width="100%"></p>

## 할 수 있는 일

- 닉네임으로 참가한다. 세션은 HttpOnly 쿠키다.
- 지역은 두 단계로 고른다. 시·도 16개, 시·군·구 255개.
- 카카오톡 TXT나 인스타그램 JSON 원본을 그대로 올린다. 20MB까지 받는다.
- 서버가 원본을 파싱하고, 본인 메시지만 AI에 보낸다.
- AI가 점수를 매긴다. 순위와 선물 도착 시각은 서버가 계산한다.
- 진행 상태는 폴링으로 본다. 창을 닫았다가 다시 와도 이어서 본다.
- 순위표는 공개다. 50줄씩 cursor로 넘기고, 동점은 `공동 N위`로 보인다.
- 공유 링크는 그 순간의 결과를 고정한다. 취소한 링크는 안내 페이지로 바뀐다.
- 시즌 마감 전에는 지역을 바꿀 수 있다. 내 데이터는 언제든 지운다.

## 시즌 일정

산타컷은 크리스마스 시즌에만 연다. 2026-11-16 기준으로 마감까지 D-39다.

| 시각(KST) | 일어나는 일 |
| --- | --- |
| 지금 ~ 2026-12-25 0시 | 참가, 분석, 지역 변경을 받는다. 순위는 미리보기(`PREVIEW`)다 |
| 2026-12-25 0시 | 시즌 마감. 순위와 배송 계획이 확정(`FINAL`)되고, 새 분석과 지역 변경은 `409 SEASON_FROZEN`이다 |
| 12/25부터 매일 0~6시 | 산타가 순위대로 배송한다. 시속 100km, 한 명에 180초. 6시까지 못 끝내면 다음 날 0시에 잇는다 |

시즌 연도는 `app.season-year`(기본 `2026`)로 정한다.

## 아키텍처

<p align="center"><img src="docs/images/architecture.svg" alt="요청 흐름: 클라이언트가 Caddy(지금 HTTP 80, 도메인을 산 뒤 TLS 443)를 거쳐 Spring Boot 앱의 4계층으로 들어간다. 작업자 둘이 원본 검증과 AI 채점을 하고, SQLite에 저장하고, Amazon Bedrock Claude Haiku 4.5를 부른다. SQLite는 매시 S3로 백업한다" width="100%"></p>

- 분석은 큐에 넣는다. 화면은 진행 상태를 폴링한다.
- 작업자는 둘이다. 원본 검증은 1초, AI 채점은 3초 간격으로 돈다.
- AI 채점은 Spring AI와 Amazon Bedrock(Claude Haiku 4.5, Converse API)이다.
- 채점 프롬프트는 DB(`prompts`)에 판으로 쌓인다. AI를 부를 때마다 최신 판을 읽는다.
- 서버는 Lightsail `nano_3_0`(512MB) 한 대다. 지금은 도메인 없이 HTTP로 띄운다.

## 계층 구조

<p align="center"><img src="docs/images/layers.svg" alt="4계층(presentation, application, domain, infrastructure)과 6도메인(participant, session, analysis, ranking, share, prompt) 및 global 격자. 의존은 아래로만 흐르고, facade 간·service 간 주입 금지, 순회는 일급 컬렉션만, Port·Adapter·UseCase 이름 금지" width="100%"></p>

- 패키지는 계층 → 도메인 → 역할 순서로 나눈다.
- 자세한 규칙은 [코드 컨벤션](docs/backend-conventions.md)에 있다.

## 데이터 모델

<p align="center"><img src="docs/images/erd.svg" alt="ERD: regions, participants, sessions, analysis_jobs, analysis_uploads, assessments, result_snapshots, shares, prompts 9개 테이블의 모든 컬럼과 키. assessments.prompt_version은 채점에 쓴 프롬프트 판 번호이고 prompts는 FK가 없는 독립 테이블이다" width="100%"></p>

- 기준은 [`schema.sql`](src/main/resources/schema.sql)이다.
- 원본 파일은 `analysis_uploads`에 잠깐 머물다가 검증이 끝나면 지워진다.
- 지역은 `parent_code`로 시·도와 시·군·구를 잇는다.
- `assessments.prompt_version`에는 채점에 쓴 프롬프트 판 번호가 남는다. 프롬프트를 DB로 옮기기 전 평가는 `NULL`이다.

## 분석 흐름

<p align="center"><img src="docs/images/analysis-flow.svg" alt="분석 흐름: 1 분석 제출, 2 접수 검사, 3 저장하고 202, 4 원본 꺼내기, 5 파싱과 정규화, 6 대기열로, 7 작업 임대, 8 최신 프롬프트 판으로 Bedrock Claude Haiku 4.5 채점, 9 평가 저장과 순위 재계산을 한 트랜잭션으로. 클라이언트는 VALIDATING부터 SUCCEEDED까지 폴링한다" width="100%"></p>

- 원본은 접수할 때 한 번 보고, 검증 작업자가 한 번 더 본다.
- 빈 파일, zip, 바이너리는 바로 `422`다. 20MB를 넘으면 `413`이다.
- 평가 저장과 순위 계산은 한 트랜잭션이다.
- 앱에는 AI 호출 수 한도가 없다. AI 비용은 AWS Bedrock 예산 차단(인프라)이 막는다.
- 24시간 넘게 기다린 작업은 만료된다. AI가 꺼져 있어도 마찬가지다.

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
    participant AI as Amazon Bedrock
    C->>API: POST /v1/analyses/files (multipart, Idempotency-Key)
    API->>DB: 원본 + 작업 저장 (VALIDATING)
    API-->>C: 202 Accepted
    loop 1초마다
        V->>DB: 원본 꺼내기
    end
    V->>V: 파싱 · 본인 메시지만
    V->>DB: 작업 QUEUED · 원본 삭제
    loop 3초마다
        W->>DB: 작업 임대 · 최신 프롬프트 읽기
    end
    W->>AI: 채점 요청 (Converse, Claude Haiku 4.5)
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

<p align="center"><img src="docs/images/api.svg" alt="API 지도: 엔드포인트 17개와 dev 전용 프롬프트 관리 3개를 도메인별 카드로 보여 준다. 카드마다 메서드, 하는 일, 경로, 성공 코드, 공개·세션·CSRF·멱등키 표시가 있다. 프롬프트 관리 API는 dev에서만 켜지고 운영은 404다" width="100%"></p>

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

dev 전용(`PROMPT_ADMIN_ENABLED=true`일 때만 있고, 운영은 404):

| 메서드 | 경로 | 하는 일 | 인증 |
| --- | --- | --- | --- |
| POST | `/v1/admin/prompts/{name}` | 프롬프트 다음 판 저장 (`201`) | 공개 · CSRF |
| GET | `/v1/admin/prompts/{name}` | 최신 판 | 공개 |
| GET | `/v1/admin/prompts/{name}/versions` | 판 목록(본문 없음) | 공개 |

프롬프트 이름은 영문 소문자, 숫자, 하이픈으로 된 1~40자다. 어기면 `400 INVALID_REQUEST`다.

</details>

## 품질 게이트

<p align="center"><img src="docs/images/quality.svg" alt="품질 게이트: 1 gradlew check(테스트 321개, bootJar), 2 ArchUnit과 파일당 타입 1개 구조 테스트, 3 컨벤션 확인 명령, 4 9인 리뷰 패널" width="100%"></p>

- 자동 검사를 통과한 뒤 리뷰어 9명이 판정한다.

## 기술 스택

<p align="center"><img src="docs/images/tech-stack.svg" alt="기술 스택: Kotlin 2.3.21, Spring Boot 4.1.2, Java 25, SQLite 3.51.1, Spring AI 2.0.0, Springdoc 3.2.0, ArchUnit 1.5.1" width="100%"></p>

## 실행

JDK 25가 필요하다. AI 채점에는 Bedrock을 부를 AWS 자격 증명(기본 체인)이 필요하다. 자격 증명이 없으면 분석 작업은 실패로 끝나고, 나머지 API는 그대로 돈다.

```powershell
cd backend
./gradlew.bat bootRun                      # 로컬 실행 (:8080)
./gradlew.bat --no-daemon check bootJar    # 커밋 전 검증
```

- 로컬 DB는 `./data/santacut.db`, 헬스 체크는 `/actuator/health`다.
- `bootRun`은 dev처럼 프롬프트 관리 API(`/v1/admin/prompts`)를 켠다.

## 데이터 출처

전국 시·도·시·군·구 지역과 대표 좌표([`regions.sql`](src/main/resources/regions.sql))는 2026-10-01 기준 행정동 경계로 만들었다. 생성 스크립트는 [`scripts/regions/generate_regions.py`](scripts/regions/generate_regions.py)다.

> 본 데이터는 통계청 통계지리정보서비스(SGIS, https://sgis.kostat.go.kr)에서 공공누리 제1유형으로 개방한 행정동 경계를 가공한 것이며(가공: vuski/admdongkor, https://github.com/vuski/admdongkor), CC BY 4.0으로 배포됩니다.

## 더 읽기

[시스템 아키텍처](docs/system-architecture.md) · [백엔드 설계](docs/backend-design.md) · [코드 컨벤션](docs/backend-conventions.md) · [배포](docs/deployment.md) · [API 연동 가이드](../docs/frontend-api-guide.md) · [작업 지침](CLAUDE.md)

<p align="center"><img src="docs/images/footer-season.svg" alt="2026-12-25 0시(KST) 시즌 마감. 순위와 선물 도착 시각이 확정되고, 그 뒤 새 분석과 지역 변경은 409 SEASON_FROZEN이다" width="100%"></p>
