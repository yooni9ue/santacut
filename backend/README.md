<p align="center">
  <img src="docs/images/banner.svg" alt="SantaCut Backend: 대화 기록으로 산타 순위를 매기는 시즌 API. 12/25 시즌 마감까지 D-38(2026-11-17 기준)" width="100%">
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
  <img alt="Google Gemini" src="https://img.shields.io/badge/Google%20Gemini-3.5%20Flash--Lite-1E3A5C">
  <img alt="시즌 마감 2026-12-25" src="https://img.shields.io/badge/season-2026--12--25-8A6410">
</p>

인프라는 [`santacut-infra`](https://github.com/yooni9ue/santacut-infra)에 있다.

| 환경 | API | 화면(Vercel) | 상태 |
| --- | --- | --- | --- |
| dev | `https://api-dev.santacut.me` | `https://dev.santacut.me` | `develop`이 자동 배포된다 |
| 운영 | `https://api.santacut.me` | `https://santacut.me`, `https://www.santacut.me` | 아직 켜지 않았다 |

Swagger는 [`api-dev.santacut.me/swagger-ui.html`](https://api-dev.santacut.me/swagger-ui.html)에서 본다.

<p align="center"><img src="docs/images/divider-snow.svg" alt="" width="100%"></p>

## 한눈에 보기

<p align="center"><img src="docs/images/stats.svg" alt="핵심 숫자: HTTP 엔드포인트 18개(dev 전용 3개 별도), SQLite 테이블 11개, 자동 테스트 498개, 오류 코드 33개, 지역 271개, 참가자당 24시간 3회 제출(AI 비용은 앱 안의 토큰 가드가 막음). 검증 작업자 1초, 분석 작업자 3초, 작업 임대 3분, 대기 작업 만료 24시간. 설계 선택 6가지." width="100%"></p>

## 할 수 있는 일

- 닉네임으로 참가한다. 세션은 HttpOnly 쿠키다.
- 지역은 두 단계로 고른다. 시·도 16개, 시·군·구 255개.
- 카카오톡 TXT나 인스타그램 JSON 원본을 그대로 올린다. 20MB까지 받는다.
- 서버가 원본을 파싱해 참여자 이름을 `나`·`상대`로 바꾼 두 화자 대화로 정규화하고, 대화 전체를 Gemini에 보낸다. 점수는 본인 말만 매긴다.
- 상대 말이 담긴 대화 내용은 작업이 끝나면(성공·실패 모두) 지운다. 원본 파일은 검증이 끝나면 바로 지운다.
- AI가 네 항목(공감 30%, 응원 25%, 존중 25%, 협력 20%)을 채점한다. 순위와 선물 도착 시각은 서버가 계산한다.
- 결과에는 크리스마스 유형 10종, 배지, 본인 한마디(`quote`), 항목 막대 4개, 가장 약한 항목, 상위 %, 같은 유형 비율이 함께 나온다.
- 진행 상태는 SSE 스트림으로 받고, 안 되면 폴링으로 본다. 창을 닫았다가 다시 와도 이어서 본다.
- 순위표는 공개다. 50줄씩 cursor로 넘기고, 동점은 `공동 N위`로 보인다.
- 공유 링크는 그 순간의 결과를 1200×630 카드와 HTML로 고정한다. 한마디는 본인이 고를 때만(`include_quote`) 싣고, 상대 말은 싣지 않는다. 취소한 링크는 안내 페이지로 바뀐다.
- 시즌 마감 전에는 지역을 바꿀 수 있다. 내 데이터는 언제든 지운다.

## 시즌 일정

산타컷은 크리스마스 시즌에만 연다. 2026-11-17 기준으로 마감까지 D-38이다.

| 시각(KST) | 일어나는 일 |
| --- | --- |
| 지금 ~ 2026-12-25 0시 | 참가, 분석, 지역 변경을 받는다. 순위는 미리보기(`PREVIEW`)다 |
| 2026-12-25 0시 | 시즌 마감. 순위와 배송 계획이 확정(`FINAL`)되고, 새 분석과 지역 변경은 `409 SEASON_FROZEN`이다 |
| 12/25부터 매일 0~6시 | 산타가 순위대로 배송한다. 시속 100km, 한 명에 180초. 6시까지 못 끝내면 다음 날 0시에 잇는다 |

시즌 연도는 `app.season-year`(기본 `2026`)로 정한다.

## 아키텍처

<p align="center"><img src="docs/images/architecture.svg" alt="요청 흐름: 클라이언트가 HTTPS(Caddy, Let's Encrypt 인증서, HSTS)를 거쳐 Lightsail micro_3_0(1GB) 위 Spring Boot 앱의 4계층으로 들어간다. 진행 상황은 SSE 스트림이나 폴링으로 받는다. 작업자 둘이 원본 검증과 AI 채점을 하고, SQLite에 저장하고, Gemini 3.5 Flash-Lite를 부른다. SQLite는 매시 KMS로 암호화한 S3로 백업한다" width="100%"></p>

- 분석은 큐에 넣는다. 화면은 진행 상태를 SSE 스트림(`/v1/analyses/{id}/events`)으로 받거나 폴링한다.
- 작업자는 둘이다. 원본 검증은 1초, AI 채점은 3초 간격으로 돈다.
- AI 채점은 Spring AI와 Google Gemini 3.5 Flash-Lite(Gemini Developer API)다.
- 채점 프롬프트는 DB(`prompts`)에 판으로 쌓인다. AI를 부를 때마다 최신 판을 읽는다.
- 같은 입력을 다시 내면 AI를 부르지 않고 지난 결과를 쓴다.
- 서버는 환경마다 Lightsail `micro_3_0`(1GB) 한 대다. Caddy가 Let's Encrypt 인증서로 HTTPS를 맡고, HTTP는 HTTPS로 넘기며 HSTS를 붙인다.
- 브라우저는 Vercel 화면의 rewrite로 같은 출처처럼 API를 부른다. API를 바로 부르는 출처는 CORS 허용 목록으로 정한다(dev는 `dev.santacut.me`와 `localhost:3000` 포함).

### AI 채점

| 단계 | 하는 일 |
| --- | --- |
| 정규화 | 최근 90일, 2,000개(본인 최신 1,000개 우선), 200,000자까지 쓴다. 같은 사람이 3시간 안에 이어 보낸 메시지는 2,000자까지 한 턴으로 묶고, 보낼 대화는 최대 40,000자다 |
| 구간 나누기 | 약 6,700자씩, 최대 6구간. 구간 경계는 4턴씩 겹치되 겹친 줄은 맥락으로만 쓴다 |
| 채점 | 구간마다 Gemini 3.5 Flash-Lite에 JSON 점수와 근거 줄 번호를 받는다. 한 구간짜리 대화는 세 번 따로 물어 항목별 중앙값을 낸다(틀린 답은 버리고 과반이면 나머지로) |
| 검증 | 서버가 점수 범위와 근거 번호를 확인한다. 본인 메시지가 8개 또는 300자보다 적으면 AI를 부르지 않고 `INSUFFICIENT_EVIDENCE`다 |
| 수축 | 항목마다 행동을 보일 기회가 적을수록 점수를 중립 점수 쪽으로 당긴다(사전 무게 0.25). 본인 턴이 5개보다 적은 대화는 더 당긴다. 근거가 없는 항목은 사전값(중립 55와 근거 있는 다른 항목 평균의 평균)을 주고 `low_confidence_dimensions`에 적는다 |
| 꾸밈 | 유형 10종, 배지, 20자 이하 한마디를 고른다. 순위에는 영향이 없다 |

- 점수 규칙은 `Score.RULE_VERSION` 7판이고, 채점 프롬프트는 DB `prompts`의 `score` 7판이다. 규칙 판과 반복 횟수는 재사용 해시에 들어가서 판이 바뀌면 다시 채점한다.
- 공개 통계는 표본이 작을 때 숨긴다. `top_percent`는 순위 계산 참가자가 100명 이상일 때만, `archetype_share_percent`는 점수를 받은 참가자가 30명 이상일 때만 나온다.

## 계층 구조

<p align="center"><img src="docs/images/layers.svg" alt="4계층(presentation, application, domain, infrastructure)과 6도메인(participant, session, analysis, ranking, share, prompt) 및 global 격자. 의존은 아래로만 흐르고, facade 간·service 간 주입 금지, 순회는 일급 컬렉션만, Port·Adapter·UseCase 이름 금지" width="100%"></p>

- 패키지는 계층 → 도메인 → 역할 순서로 나눈다.
- 자세한 규칙은 [코드 컨벤션](docs/backend-conventions.md)에 있다.

## 데이터 모델

<p align="center"><img src="docs/images/erd.svg" alt="ERD: regions, participants, sessions, analysis_jobs, analysis_uploads, assessments, result_snapshots, result_populations, shares, prompts, ai_usage_days 11개 테이블의 모든 컬럼과 키. result_populations, prompts, ai_usage_days는 FK가 없는 독립 테이블이다" width="100%"></p>

- 기준은 [`schema.sql`](src/main/resources/schema.sql)과 JPA 엔티티다. 나중에 더한 컬럼(유형·배지·한마디 등)은 `ddl-auto: update`가 만든다.
- 원본 파일은 `analysis_uploads`에 잠깐 머물다가 검증이 끝나면 지워진다.
- 지역은 `parent_code`로 시·도와 시·군·구를 잇는다.
- `assessments.prompt_version`에는 채점에 쓴 프롬프트 판 번호가 남는다. 프롬프트를 DB로 옮기기 전 평가는 `NULL`이다.
- `analysis_jobs.digest_hash`는 같은 채점 입력의 재제출을 알아보는 해시다. `ai_usage_days`는 날짜별 AI 토큰 합계다. `result_populations`는 순위를 계산할 때 다시 세는 유형별 인원이다.

## 분석 흐름

<p align="center"><img src="docs/images/analysis-flow.svg" alt="분석 흐름: 1 분석 제출, 2 접수 검사, 3 저장하고 202, 4 원본 꺼내기, 5 파싱과 나·상대 두 화자 정규화, 6 대기열로, 7 작업 임대, 8 최신 프롬프트 판과 Gemini 3.5 Flash-Lite로 구간별 채점, 9 평가 저장과 순위 재계산을 한 트랜잭션으로 하고 대화 내용은 지운다. 클라이언트는 VALIDATING부터 SUCCEEDED까지 SSE 스트림이나 폴링으로 본다" width="100%"></p>

- 원본은 접수할 때 한 번 보고, 검증 작업자가 한 번 더 본다.
- 빈 파일, zip, 바이너리는 바로 `422`다. 20MB를 넘으면 `413`이다.
- 평가 저장과 순위 계산은 한 트랜잭션이다.
- 24시간 넘게 기다린 작업은 만료된다. AI가 꺼져 있어도 마찬가지다.
- Gemini가 호출을 막으면(선불 크레딧 소진 `429` 포함) 작업은 실패하지 않는다. `QUEUED`로 돌아가고 작업자가 15분 쉰 뒤 다시 잡는다.

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
    participant AI as Gemini 3.5 Flash-Lite
    C->>API: POST /v1/analyses/files (multipart, Idempotency-Key)
    API->>DB: 원본 + 작업 저장 (VALIDATING)
    API-->>C: 202 Accepted
    loop 1초마다
        V->>DB: 원본 꺼내기
    end
    V->>V: 파싱 · 이름을 나·상대로
    V->>DB: 작업 QUEUED · 원본 삭제
    loop 3초마다
        W->>DB: 작업 임대 · 최신 프롬프트 읽기
    end
    W->>AI: 구간별 채점 요청 (최대 6구간, 한 구간이면 3번)
    AI-->>W: 구조화 점수와 근거 줄 번호
    W->>DB: 평가 저장 + 순위 재계산 + 대화 내용 삭제 (한 트랜잭션)
    C->>API: GET /v1/analyses/{id}/events (SSE)
    API-->>C: event: status (VALIDATING → QUEUED → ANALYZING → RANKING_PENDING → SUCCEEDED)
    Note over C,API: 스트림이 안 되면 GET /v1/analyses/{id}를 2~3초마다 폴링
```

</details>

## 토큰 최적화 / AI 입력 설계

숫자는 모두 `./gradlew eval`(합성 대화로 운영 채점 경로를 그대로 부르는 수동 평가)로 쟀다. 기록은 [백엔드 설계 §14](docs/backend-design.md)에 있다.

<p align="center"><img src="docs/images/ai-pipeline.svg" alt="원본 대화 내보내기에서 점수까지: 최근 90일·최신 2,000개로 자르고, 이름을 나·상대로 바꾸고 간격 줄·링크·웃음·이모지·긴 글을 줄인 대화문을 약 6,700자씩 최대 6구간으로 나눠 Gemini 3.5 Flash-Lite에 보낸다. 답은 250토큰 이하 JSON이고, 코드가 3번 중앙값·구간 무게·수축으로 합쳐 점수·유형·배지를 만든다" width="100%"></p>

정규화는 뜻을 잃지 않는 것만 줄인다. 모델은 점수만 매기고, 합산과 보정은 코드가 한다.

<p align="center"><img src="docs/images/ai-size-cost.svg" alt="이벤트 2,000개 긴 대화: 대화문 1,866줄, 시스템 프롬프트 포함 53,132자가 한 번 호출 37,838 입력 토큰(실측)이었고, 지금 6구간은 약 46,100 토큰이다. 분석 한 번 비용은 짧은 대화 0.085센트에서 0.254센트, 긴 대화 약 1.40센트에서 약 1.56센트다" width="100%"></p>

정확도를 위해 토큰을 더 쓴 두 곳(3번 중앙값, 6구간)도 분석 한 번에 2센트를 넘지 않는다.

<p align="center"><img src="docs/images/ai-evolution.svg" alt="개선 단계: 본인 말 표본에서 대화 전체로 바꿔 짝 대화 구분 0/8에서 8/8, 근거 부족 16/40에서 0/40, MAE 8.96에서 5.50. 5판 프롬프트로 MAE 5.92에서 3.71. 구간 채점으로 긴 대화 위치 차이 42.3에서 6.1, 6구간으로 15.42에서 3.75. 3번 중앙값으로 표준편차 2.05에서 1.07, 유형 바뀜 2/5에서 0/5. 본인 수준 사전값으로 반 길이 차이 5.20에서 3.57" width="100%"></p>

단계마다 바로 앞 상태와 비교했다. MAE는 양쪽 대화 전문을 넣은 평이한 프롬프트의 점수와의 차이다.

<p align="center"><img src="docs/images/ai-rejected.svg" alt="채택하지 않은 안: 지시 줄을 대화문 뒤로(MAE 5.92에서 6.46), tone 필드(250토큰 상한 2/40, MAE 3.71에서 5.04), 다섯 단계 눈금(MAE 7.72), 4구간 10,000자(위치 차이 15.42), 컨텍스트 캐시, Batch API, Flex, LLMLingua식 압축, 요약 후 채점" width="100%"></p>

"측정" 카드는 평가에서 지표가 나빠져 되돌렸다. "판단" 카드는 재지 않고 설계 검토에서 뺐다.

<p align="center"><img src="docs/images/ai-cost-guard.svg" alt="AI 비용 가드 흐름: 작업을 잡으면 모든 구간과 반복 호출의 비용을 미리 어림하고, 한 달 9달러를 넘으면 부르지 않고 작업을 QUEUED로 두고 15분 쉰다. 부르면 토큰을 날짜별로 기록한다. Gemini가 429(크레딧 소진 포함)·403·5xx·시간 초과로 막아도 작업은 실패하지 않고 QUEUED로 돌아간다. 24시간이 지나면 EXPIRED" width="100%"></p>

호출 하나가 한도를 넘겨 쓰지 않게 미리 어림한다. 크레딧이 떨어져도 작업은 사라지지 않는다.

## API

<p align="center"><img src="docs/images/api.svg" alt="API 지도: 엔드포인트 18개와 dev 전용 프롬프트 관리 3개를 도메인별 카드로 보여 준다. 카드마다 메서드, 하는 일, 경로, 성공 코드, 공개·세션·CSRF·멱등키 표시가 있다. 프롬프트 관리 API는 dev에서만 켜지고 운영은 404다" width="100%"></p>

- 필드 정의는 Swagger(`/swagger-ui.html`)가 기준이다.
- 호출 순서는 [API 연동 가이드](../docs/api/frontend-api-guide.md)에 있다.

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

<p align="center"><img src="docs/images/quality.svg" alt="품질 게이트: 1 gradlew check(테스트 498개, bootJar), 2 ArchUnit과 파일당 타입 1개 구조 테스트, 3 컨벤션 확인 명령, 4 9인 리뷰 패널" width="100%"></p>

- 자동 검사를 통과한 뒤 리뷰어 9명이 판정한다.

## 배포

- `develop` 푸시: 테스트 → GHCR 이미지 게시 → dev 자동 배포. GitHub OIDC로 AWS 배포 역할을 맡아 러너 IP에만 SSH를 잠깐 열고, 검증한 이미지 digest로 바꾼 뒤 닫는다.
- `main` 푸시: 테스트 → 이미지 게시까지만 한다. 운영 배포는 수동 실행(`workflow_dispatch`, `deploy=true`)이다.
- 앱 설정(`GEMINI_API_KEY`, 도메인, CORS 등)은 KMS로 암호화해 S3 `config/app.env`에 두고, 서버가 부팅 때 tmpfs로 받는다. 같은 파일의 `GHCR_USER`, `GHCR_TOKEN`으로 이미지를 받는다.
- SQLite는 매시 S3에 백업하고(SSE-KMS, 최대 21일), Lightsail 자동 스냅숏을 매일 뜬다.
- 자세한 절차는 [배포 운영서](docs/deployment.md)에 있다.

## 비용

| 항목 | 월 비용(USD) | 메모 |
| --- | ---: | --- |
| Lightsail `micro_3_0`(dev) | 7 | 1GB, 고정 IP·전송량 포함 |
| 스냅숏, S3, KMS 키 2개, Route 53 | 약 3.5 | 인프라 레포 기준 |
| Gemini 3.5 Flash-Lite | 최대 9 | 입력 100만 토큰 0.3달러, 출력 2.5달러로 어림한다 |

- 앱 안의 비용 가드가 날짜별 토큰 합계로 한 달 9달러(`AI_BUDGET_MONTHLY_USD`)를 넘지 않게 막고, 호출 전에 그 호출의 어림 비용도 더해 본다.
- 하루 한도는 기본으로 없다(`AI_BUDGET_DAILY_USD=0`). Gemini는 선불 크레딧으로 써서 크레딧이 실제 상한이다.
- 참가자당 24시간 3회 제출 한도는 비용 장치가 아니라 점수를 거듭 다시 뽑지 못하게 하는 제품 규칙이다.

## 기술 스택

<p align="center"><img src="docs/images/tech-stack.svg" alt="기술 스택: Kotlin 2.3.21, Spring Boot 4.1.2, Java 25, SQLite 3.51.1, Spring AI 2.0.0, Springdoc 3.2.0, ArchUnit 1.5.1" width="100%"></p>

## 실행

JDK 25가 필요하다. 환경 변수 `GEMINI_API_KEY`에 Gemini API 키를 넣는다. 키가 틀리면 Gemini가 호출을 막아 분석 작업이 `QUEUED`에 머물고(15분 쉬고 다시 시도, 24시간 뒤 만료), 나머지 API는 그대로 돈다.

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

[시스템 아키텍처](docs/system-architecture.md) · [백엔드 설계](docs/backend-design.md) · [코드 컨벤션](docs/backend-conventions.md) · [배포](docs/deployment.md) · [API 연동 가이드](../docs/api/frontend-api-guide.md) · [작업 지침](CLAUDE.md)

<p align="center"><img src="docs/images/footer-season.svg" alt="2026-12-25 0시(KST) 시즌 마감. 순위와 선물 도착 시각이 확정되고, 그 뒤 새 분석과 지역 변경은 409 SEASON_FROZEN이다" width="100%"></p>
