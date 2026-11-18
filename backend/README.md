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

## 문서 기준

현재 API 계약은 DTO·OpenAPI, 런타임 기본값은 `application.yml`, 배포는 워크플로·Compose를 기준으로 확인한다. 아래 그림의 시험 수·성능·비용·시즌 카운트다운은 작성 시점 기록이며 실시간 지표가 아니다. 그림에 남은 폴링 대안도 과거 설명으로, 현재 프론트는 SSE와 수동 재연결을 사용한다. 문서와 코드의 미완료 연결은 [전체 점검 기록](../docs/development/implementation-audit.md)을 참고한다.

## 한눈에 보기

<p align="center"><img src="docs/images/stats.svg" alt="핵심 숫자: HTTP 엔드포인트 18개(dev 전용 3개 별도), SQLite 테이블 13개, 자동 테스트 575개, 오류 코드 39개, 지역 271개, 참가자당 24시간 3회 제출(AI 비용은 앱 안의 토큰 가드가 막음). 검증 작업자 1초, 분석 작업자 3초, 작업 임대 9분, 대기 작업 만료 24시간. 설계 선택 6가지." width="100%"></p>

## 할 수 있는 일

- 닉네임으로 참가한다. 세션은 HttpOnly 쿠키다.
- 지역은 두 단계로 고른다. 시·도 16개, 시·군·구 255개.
- 카카오톡 TXT나 인스타그램 JSON 원본을 그대로 올린다. 20MB, 100만 줄(인스타그램은 메시지 객체 100만 개)까지 받는다.
- 서버가 원본을 파싱해 참여자 이름을 `나`·`상대`로 바꾼 두 화자 대화로 정규화하고, 대화 전체를 Gemini에 보낸다. 점수는 본인 말만 매긴다.
- 상대 말이 담긴 대화 내용은 작업이 끝나면(성공·실패 모두) 지운다. 원본 파일은 검증이 끝나면 바로 지운다.
- AI가 네 항목(공감 30%, 응원 25%, 존중 25%, 협력 20%)을 채점한다. 순위와 선물 도착 시각은 서버가 계산한다.
- 결과에는 크리스마스 유형 10종, 배지, 본인 한마디(`quote`), 항목 막대 4개, 가장 약한 항목, 상위 %, 같은 유형 비율이 함께 나온다.
- 진행 상태 SSE와 단건 조회 API를 제공한다. 현재 프론트는 SSE 연결 종료 시 수동 재연결하며 폴링하지 않는다. 최근 작업 조회 API는 있지만 프론트 새로고침 복귀 동선은 보완이 필요하다.
- 순위표는 공개다. 50줄씩 cursor로 넘기고, 동점은 `공동 N위`로 보인다. 남에게는 도착 날짜(`eta_date`)만 보이고, 정확한 도착 시각은 본인 결과에만 있다.
- 공유 링크는 그 순간의 결과를 1200×630 카드와 HTML로 고정한다. 한마디는 본인이 고를 때만(`include_quote`) 싣고, 다시 공유하면 같은 링크에서 넣고 뺄 수 있다. 상대 말은 싣지 않는다. 취소한 링크는 안내 페이지로 바뀐다.
- 시즌 마감 전에는 지역을 24시간 2번, 시즌 5번까지 바꿀 수 있다. 내 데이터는 언제든 지운다.

## 시즌 일정

산타컷은 크리스마스 시즌에만 연다. 2026-11-17 기준으로 마감까지 D-38이다.

| 시각(KST) | 일어나는 일 |
| --- | --- |
| 지금 ~ 2026-12-25 0시 | 참가, 분석, 지역 변경을 받는다. 순위는 미리보기(`PREVIEW`)다 |
| 2026-12-25 0시 | 시즌 마감. 순위와 배송 계획이 확정(`FINAL`)되고, 새 분석과 지역 변경은 `409 SEASON_FROZEN`이다 |
| 12/25부터 매일 0~6시 | 산타가 순위대로 배송한다. 시속 100km, 한 명에 180초. 6시까지 못 끝내면 다음 날 0시에 잇는다 |
| 2026-12-31 6시 | 배송 마감. 참가자가 많아도 모든 도착이 이 안에 든다([순위와 도착 시각](#순위와-도착-시각)) |

시즌 연도는 `app.season-year`(기본 `2026`)로 정한다.

## 아키텍처

<p align="center"><img src="docs/images/architecture.svg" alt="요청 흐름: 클라이언트가 HTTPS(Caddy, Let's Encrypt 인증서, HSTS)를 거쳐 Lightsail micro_3_0(1GB) 위 Spring Boot 앱의 4계층으로 들어간다. application은 흐름만 쓰는 facade와 실행 기법을 맡는 runner로 나뉜다. 진행 상황은 SSE 스트림이나 폴링으로 받는다. 작업자 셋이 원본 검증(1초), AI 채점(3초, 동시 3개), 순위 모아 계산(30초)을 하고, SQLite에 저장하고, Gemini 3.5 Flash-Lite를 부른다. SQLite는 매시 KMS로 암호화한 S3로 백업한다" width="100%"></p>

- 분석은 큐에 넣는다. 화면은 SSE(`/v1/analyses/{id}/events`)로 상태를 받는다. GET은 초기 확인·복구용이며 주기적인 브라우저 폴링은 하지 않는다.
- 작업자는 셋이다. 원본 검증은 1초, AI 채점은 3초 간격으로 돌고, 채점은 작업 3개를 동시에 한다(`ANALYSIS_WORKER_CONCURRENCY`). 순위는 30초마다 모아서 계산한다.
- AI 채점은 Spring AI와 Google Gemini 3.5 Flash-Lite(Gemini Developer API)다.
- 채점 프롬프트는 DB(`prompts`)에 판으로 쌓인다. AI를 부를 때마다 최신 판을 읽는다.
- 같은 입력을 다시 내면 AI를 부르지 않고 지난 결과를 쓴다.
- 입구에서 먼저 거른다. 원본 접수는 서버 전체에서 동시에 2개까지다(넘으면 `503 UPLOAD_BUSY`). 같은 IP는 참가 등록 1시간 10회, 분석 제출 24시간 20회까지다(넘으면 `429 CLIENT_RATE_LIMITED`). 파일이 아닌 요청 본문은 2MB까지 받는다.
- 운영 프로필은 `SESSION_SECURE=true`가 아니면 뜨지 않고 Swagger를 끈다. HTTPS에서는 CSRF 쿠키 이름이 `__Host-XSRF-TOKEN`이다.
- 서버는 환경마다 Lightsail `micro_3_0`(1GB) 한 대다. Caddy가 Let's Encrypt 인증서로 HTTPS를 맡고, HTTP는 HTTPS로 넘기며 HSTS를 붙인다.
- 브라우저는 Vercel 화면의 rewrite로 같은 출처처럼 API를 부른다. API를 바로 부르는 출처는 CORS 허용 목록으로 정한다(dev는 `dev.santacut.me`와 `localhost:3000` 포함).

### AI 채점

| 단계 | 하는 일 |
| --- | --- |
| 정규화 | 올해 1월 1일(한국 시각)부터 본인 2,000개, 상대 포함 6,000개, 200,000자까지 달마다 고르게 쓴다. 같은 사람이 3시간 안에 이어 보낸 메시지는 2,000자까지 한 턴으로 묶고, 보낼 대화는 최대 52,000자다 |
| 구간 나누기 | 약 6,700자씩, 최대 8구간. 구간 경계는 4턴씩 겹치되 겹친 줄은 맥락으로만 쓴다 |
| 채점 | 구간마다 Gemini 3.5 Flash-Lite에서 응답 스키마(`responseJsonSchema`)로 강제한 JSON 점수와 근거 줄 번호를 받는다. 한 구간짜리 대화는 세 번 따로 물어 항목별 중앙값을 낸다(틀린 답은 버리고 과반이면 나머지로) |
| 검증 | 서버가 점수 범위와 근거 번호를 확인한다. 본인 메시지가 8개 또는 300자보다 적으면 AI를 부르지 않고 `INSUFFICIENT_EVIDENCE`다 |
| 수축 | 항목마다 행동을 보일 기회가 적을수록 점수를 중립 점수 쪽으로 당긴다(사전 무게 0.25). 본인 턴이 5개보다 적은 대화는 더 당긴다. 근거가 없는 항목은 사전값(중립 55와 근거 있는 다른 항목 평균의 평균)을 주고 `low_confidence_dimensions`에 적는다 |
| 꾸밈 | 유형 10종, 배지, 20자 이하 한마디를 고른다. 순위에는 영향이 없다 |

- 점수 규칙은 `Score.RULE_VERSION` 7판이고, 채점 프롬프트는 DB `prompts`의 `score` 7판이다. 규칙 판과 반복 횟수는 재사용 해시에 들어가서 판이 바뀌면 다시 채점한다.

## 계층 구조

<p align="center"><img src="docs/images/layers.svg" alt="4계층(presentation, application, domain, infrastructure)과 6도메인(participant, session, analysis, ranking, share, prompt) 및 global 격자. application에는 흐름만 쓰는 facade와 실행 기법을 맡는 runner가 있다. 의존은 아래로만 흐르고, facade 간·service 간 주입 금지, runner는 facade·다른 runner를 주입받지 않음, 순회는 일급 컬렉션만, Port·Adapter·UseCase 이름 금지" width="100%"></p>

- 패키지는 계층 → 도메인 → 역할 순서로 나눈다.
- facade는 유스케이스 흐름만 위에서 아래로 읽히게 쓴다. 스레드·슬롯·잠금·리스·트랜잭션 같은 실행 기법은 runner(`@Runner`)가 맡는다. `TransactionRunner`, `AnalysisWorkerRunner`(채점 자리 3개와 스레드), `HeldJobRunner`(작업 잡기·리스·결과 기록), `AiBudgetRunner`(비용 예약과 공유 멈춤)가 있다. 점수 계산과 실패 분류 같은 업무 규칙은 도메인으로 내린다.
- facade에 의존성이 6개 이상이거나 private 함수가 10개 이상이면 나눌 신호다. 그래서 진행 상태 조회는 `AnalysisStatusFacade`로 따로 나눴다. ArchUnit이 runner의 위치·이름과 주입 금지를 검사한다.
- 자세한 규칙은 [코드 컨벤션](docs/backend-conventions.md)에 있다.

## 데이터 모델

<p align="center"><img src="docs/images/erd.svg" alt="ERD: regions, participants, sessions, analysis_jobs, analysis_uploads, assessments, result_snapshots, result_populations, shares, prompts, ai_usage_days, ranking_refresh, region_changes 13개 테이블의 모든 컬럼과 키. result_populations, prompts, ai_usage_days, ranking_refresh는 FK가 없는 독립 테이블이다" width="100%"></p>

- 기준은 JPA 엔티티다. 테이블·컬럼·외래 키·인덱스는 `ddl-auto: update`가 엔티티대로 만들고, SQLite 방언이 만들지 않는 유일 인덱스와 처음 넣을 행은 [`seed.sql`](src/main/resources/seed.sql)이 넣는다. 규칙은 [컨벤션 §12](docs/backend-conventions.md#12-jpa-엔티티와-스키마)에 있다.
- 원본 파일은 `analysis_uploads`에 잠깐 머물다가 검증이 끝나면 지워진다.
- 지역은 `parent_code`로 시·도와 시·군·구를 잇는다.
- `assessments.prompt_version`에는 채점에 쓴 프롬프트 판 번호가 남는다. 프롬프트를 DB로 옮기기 전 평가는 `NULL`이다.
- `analysis_jobs.digest_hash`는 같은 채점 입력의 재제출을 알아보는 해시다. `ai_usage_days`는 날짜별 AI 토큰 합계다. `result_populations`는 순위를 계산할 때 다시 세는 유형별 인원이다.
- `ai_usage_days.cached_input_tokens`는 암묵적 캐시에서 읽은 입력 토큰 수다. `ranking_refresh`는 순위 계산 요청 판과 마지막으로 계산한 판을 한 줄에 둔다. `region_changes`는 지역 변경 한도를 세는 기록이다.

## 분석 흐름

<p align="center"><img src="docs/images/analysis-flow.svg" alt="분석 흐름: 1 분석 제출, 2 접수 검사(참가자·IP 한도, 동시 접수 2개), 3 저장하고 202, 4 원본 꺼내기, 5 파싱과 나·상대 두 화자 정규화, 6 대기열로, 7 작업 잡기(동시 3개, 시즌 마감 확인), 8 최신 프롬프트 판과 Gemini 3.5 Flash-Lite로 구간별 채점, 9 평가를 저장하고 대화 내용을 지운 뒤 순위 계산 요청만 표시한다. 순위는 30초마다 모아 계산하고 그때 SUCCEEDED가 된다. 클라이언트는 VALIDATING부터 SUCCEEDED까지 SSE 스트림이나 폴링으로 본다" width="100%"></p>

- 원본은 접수할 때 한 번 보고, 검증 작업자가 한 번 더 본다.
- 빈 파일, zip, 바이너리는 바로 `422`다. 20MB를 넘으면 `413`이다. 카카오톡 TXT는 한 줄씩 읽고, 100만 줄을 넘으면 `UNSUPPORTED_FILE_FORMAT`이다.
- 평가 저장과 대화 내용 삭제는 한 트랜잭션이다. 순위는 계산 요청만 표시해 두고 30초마다 모아서 계산한다. 그동안 상태는 `RANKING_PENDING`이고, 계산이 끝나면 `SUCCEEDED`가 된다.
- 24시간 넘게 기다린 작업은 만료된다. AI가 꺼져 있어도 마찬가지다.
- Gemini가 호출을 막으면(선불 크레딧 소진 `429` 포함) 작업은 실패하지 않는다. `QUEUED`로 돌아가고 작업자가 15분 쉰 뒤 다시 잡는다. API 키 오류(401·403·404, 키 오류가 담긴 400)도 설정 오류로 보고 똑같이 쉰다.
- 앞 구간이 과금된 뒤 막힌 시도는 시도 횟수로 센다. 작업 하나의 최악 과금은 3번분이고, 다 쓰면 `RETRY_EXHAUSTED`다.
- 임대가 끝난 채 `ANALYZING`에 멈춘 작업은 AI가 꺼져 있어도 시도를 센 채 대기열 원래 자리로 돌아간다.
- 응답 형식·근거·점수 범위 오류처럼 입력 탓인 실패는 그 작업만 `ANALYSIS_FAILED`로 끝낸다. 연속 실패로 세지 않아 다른 참가자의 채점을 멈추지 않는다.
- 작업을 잡을 때 시즌 마감부터 본다. 마감 뒤에 밀린 작업은 AI를 부르지 않고 `SEASON_FROZEN`으로 끝낸다.

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
    participant R as 순위 계산
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
        W->>DB: 작업 임대(동시 3개) · 시즌 확인 · 최신 프롬프트 읽기
    end
    W->>AI: 구간별 채점 요청 (최대 8구간, 한 구간이면 3번)
    AI-->>W: 구조화 점수와 근거 줄 번호
    W->>DB: 평가 저장 + 대화 내용 삭제 + 순위 요청 표시 (한 트랜잭션)
    loop 30초마다
        R->>DB: 요청을 모아 순위·도착 시각 계산
    end
    C->>API: GET /v1/analyses/{id}/events (SSE)
    API-->>C: event: status (VALIDATING → QUEUED → ANALYZING → RANKING_PENDING → SUCCEEDED)
    Note over C,API: 스트림 종료 안내 후 같은 작업의 SSE를 수동 재연결
```

</details>

## 순위와 도착 시각

| 규칙 | 내용 |
| --- | --- |
| 계산 시점 | 심사 성공, 지역 변경, 삭제는 `ranking_refresh`의 요청 판만 올린다. 30초마다(`RANKING_REBUILD_DELAY_MS`) 모아서 한 번 계산하고, 12/25 0시와 앱 시작 때도 남은 요청을 처리한다 |
| 동점 | 점수 → 공감·존중·응원·협력 항목 점수 → 시즌 첫 평가 시각 → 참가자 ID 순으로 가른다. `공동 N위`는 점수가 모두 같을 때만이다 |
| 도착 시각 | 순위대로 시속 100km, 한 명에 180초, 매일 0~6시에 배송한다. 누적 작업 시간이 7일 × 6시간을 넘으면 모든 작업 시간을 같은 비율로 줄여 12/25~12/31 안에 끝낸다. 순서는 그대로다 |
| 공개 범위 | 순위표와 공유 카드에는 서울 날짜(`eta_date`)만 싣고, 정확한 `eta`는 본인 결과에만 준다. 순위표 행의 `eta`는 늘 `null`이다 |
| 지역 변경 | 24시간 2번, 시즌 5번까지다(`429 REGION_CHANGE_LIMITED`, `429 REGION_CHANGE_SEASON_LIMIT`). 같은 지역은 세지 않는다. 도착 시각 차이로 남의 지역을 좁혀 가지 못하게 막는다 |
| 순위표 페이지 | 50줄씩 넘긴다. cursor에 계산 시각이 들어 있어, 페이지 사이에 다시 계산되면 `409 RANKING_SNAPSHOT_CHANGED`다. 첫 페이지는 30초 캐시한다 |
| 표본 통계 | `top_percent`(1~100)는 순위 계산 참가자가 100명 이상일 때만, `archetype_share_percent`는 점수를 받은 참가자가 30명 이상일 때만 나온다 |

## 토큰 최적화 / AI 입력 설계

숫자는 모두 `./gradlew eval`(합성 대화로 운영 채점 경로를 그대로 부르는 수동 평가)로 쟀다. 기록은 [백엔드 설계 §14](docs/backend-design.md)에 있다.

<p align="center"><img src="docs/images/ai-pipeline.svg" alt="원본 대화 내보내기에서 점수까지: 올해 1월 1일부터 본인 2,000개·전체 6,000개를 달마다 고르게 고르고, 이름을 나·상대로 바꾸고 간격 줄·링크·웃음·이모지·긴 글을 줄인 대화문을 약 6,700자씩 최대 8구간으로 나눠 Gemini 3.5 Flash-Lite에 보낸다. 답은 250토큰 이하 JSON이고, 코드가 3번 중앙값·구간 무게·수축으로 합쳐 점수·유형·배지를 만든다" width="100%"></p>

정규화는 뜻을 잃지 않는 것만 줄인다. 모델은 점수만 매기고, 합산과 보정은 코드가 한다.

<p align="center"><img src="docs/images/ai-size-cost.svg" alt="이벤트 2,000개 긴 대화: 대화문 1,866줄, 시스템 프롬프트 포함 53,132자가 한 번 호출 37,838 입력 토큰(실측)이었고, 6구간은 약 46,100 토큰이었다. 지금 8구간은 올해 대화문 기준 약 56,000 토큰으로 추정한다. 분석 한 번 비용은 짧은 대화 0.085센트(1번)에서 0.254센트(3번 중앙값), 응답 스키마 뒤 0.281센트, 긴 대화 약 1.40센트(4구간)에서 약 1.56센트(6구간)를 거쳐 약 1.9센트(8구간, 추정)이고, 6구간 때 캐시 읽기가 걸리면 0.82~1.28센트였다" width="100%"></p>

정확도를 위해 토큰을 더 쓴 곳(3번 중앙값, 8구간, 응답 스키마, 1년치 대화문)을 다 합쳐도 분석 한 번에 2센트를 넘지 않을 것으로 본다. 8구간의 약 1.9센트는 글자 수로 어림한 추정이고, Gemini에서 잰 값이 아니다. 운영 사용량 로그로 확인한다. 캐시에서 읽은 입력 토큰(`cachedContentTokenCount`)은 따로 기록하고 100만 토큰당 0.03달러로 센다.

<p align="center"><img src="docs/images/ai-evolution.svg" alt="개선 단계: 본인 말 표본에서 대화 전체로 바꿔 짝 대화 구분 0/8에서 8/8, 근거 부족 16/40에서 0/40, MAE 8.96에서 5.50. 5판 프롬프트로 MAE 5.92에서 3.71. 구간 채점으로 긴 대화 위치 차이 42.3에서 6.1, 6구간으로 15.42에서 3.75. 3번 중앙값으로 표준편차 2.05에서 1.07, 유형 바뀜 2/5에서 0/5. 본인 수준 사전값으로 반 길이 차이 5.20에서 3.57. JSON 응답 스키마 강제로 MAE 5.82에서 4.76, 재채점 표준편차 1.34에서 0.59" width="100%"></p>

단계마다 바로 앞 상태와 비교했다. MAE는 양쪽 대화 전문을 넣은 평이한 프롬프트의 점수와의 차이다. 6단계(응답 스키마)는 출력이 호출당 약 94토큰에서 125토큰으로 늘었지만 상한 250 안이다.

모델끼리 비교한 MAE를 넘어 사람 판단과 맞는지는 골든셋으로 잰다. 사람 두 명 이상이 30~50개 대화에 따로 점수를 매기고, `EVAL_GOLDEN_DIR`을 가리켜 `./gradlew eval`을 돌리면 스피어만 순위 상관(목표 0.7 이상)과 항목 MAE가 나온다. 절차는 [설계 §4.6](docs/backend-design.md)에 있다. 라벨은 아직 모으지 않았다.

<p align="center"><img src="docs/images/ai-rejected.svg" alt="채택하지 않은 안: 지시 줄을 대화문 뒤로(MAE 5.92에서 6.46), tone 필드(250토큰 상한 2/40, MAE 3.71에서 5.04), 다섯 단계 눈금(MAE 7.72), 4구간 10,000자(위치 차이 15.42), 컨텍스트 캐시, Batch API, Flex, LLMLingua식 압축, 요약 후 채점" width="100%"></p>

"측정" 카드는 평가에서 지표가 나빠져 되돌렸다. "판단" 카드는 재지 않고 설계 검토에서 뺐다.

<p align="center"><img src="docs/images/ai-cost-guard.svg" alt="AI 비용 가드 흐름: 작업을 잡으면 모든 구간과 반복 호출의 비용을 미리 어림해 진행 중인 작업의 예약분과 함께 더하고, 한 달 9달러를 넘으면 부르지 않고 작업을 QUEUED로 두고 15분 쉰다. 부르면 토큰(캐시 읽기 따로)을 날짜별로 기록한다. Gemini가 429(크레딧 소진 포함)·5xx·시간 초과로 막거나 API 키 오류가 나도 작업은 실패하지 않고 QUEUED로 돌아간다. 과금 뒤 막힌 시도는 3번까지 센다. 24시간이 지나면 EXPIRED" width="100%"></p>

호출 하나가 한도를 넘겨 쓰지 않게 미리 어림한다. 채점 스레드 셋이 동시에 확인을 통과해 한도를 넘지 않도록, 진행 중인 작업의 어림 비용은 예약해 함께 센다. 크레딧이 떨어져도 작업은 사라지지 않는다.

### 올해 대화를 달마다 고르게

분석 기간은 올해 1월 1일 0시(한국 시각)부터 지금까지다. 예전에는 최근 90일을 최신순으로 2,000개까지 썼다. 연말 산타 명단이니 올해 대화를 봐야 하는데, 많이 쓰는 대화방은 2,000개가 최근 몇 주 만에 다 차서 그 앞이 모두 빠졌다.

<p align="center"><img src="docs/images/ai-year-spread.svg" alt="분석 기간과 대화문 고르기: 전에는 최근 90일을 최신순으로 2,000개까지 써서, 많이 쓰는 대화는 최근 몇 주만 남았다. 지금은 올해 1월 1일부터 달마다 같은 몫을 고르고, 모자란 달의 남는 몫은 다른 달로 넘긴다. 상대 말은 150자, 내 말은 300자까지 보낸다. 본인 2,000개, 전체 6,000개, 200,000자로 고른 뒤 AI 대화문은 52,000자, 최대 8구간이다" width="100%"></p>

업로드한 대화는 세 번에 걸쳐 고른다. 본인 메시지를 2,000개까지, 상대 메시지를 더해 전체 6,000개까지, 끝으로 글자 수를 200,000자까지 줄인다. 단계마다 달마다 같은 몫을 주고, 대화가 적어 몫을 다 못 쓴 달의 남는 몫은 다른 달로 넘긴다. 한 달 안에서는 가장 최근의 이어진 대화를 남겨 맥락이 끊기지 않게 한다. AI에 보내는 대화문도 52,000자를 넘으면 같은 방식으로 달마다 고르게 줄을 뺀다. 상대 말은 점수에 들어가지 않고 맥락만 주므로 150자에서 자르고, 본인 말은 지금처럼 300자까지 둔다.

1월 1일~11월 17일 합성 대화(하루 5·20·60·200개)로 재 보니 대화문은 약 51,300~51,700자, 1,385~1,424줄이었고 11개월이 모두 들어갔으며 8번 호출했다. 대화문에서 본인 말은 약 55~63%였다. 긴 대화 한 번은 약 56,000 입력 토큰, 약 1.9센트로 추정한다. 6구간 때(약 46,100토큰, 약 1.56센트)보다 약 20% 많다. Gemini에서 잰 값이 아니라 운영 사용량 로그로 확인한다. 짧은 대화는 그대로다.

## API

<p align="center"><img src="docs/images/api.svg" alt="API 지도: 엔드포인트 18개와 dev 전용 프롬프트 관리 3개를 도메인별 카드로 보여 준다. 카드마다 메서드, 하는 일, 경로, 성공 코드, 공개·세션·CSRF·멱등키 표시가 있다. 프롬프트 관리 API는 dev에서만 켜지고 X-Admin-Token 관리 토큰이 있어야 하며 운영은 404다. 화면은 이 API를 쓰지 않는다" width="100%"></p>

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
| GET | `/v1/analyses/{id}/events` | 분석 진행 상황 스트림(SSE) | 세션 |
| GET | `/v1/me/analysis` | 내 최근 분석 상태 | 세션 |
| GET | `/v1/me/result` | 내 점수·순위·도착 시각 | 세션 |
| GET | `/v1/rankings` | 랭킹 50명씩(cursor) | 공개 |
| POST | `/v1/shares` | 공유 링크 생성·재사용 | 세션 · CSRF |
| DELETE | `/v1/shares/{token}` | 공유 링크 폐기 (`204`) | 세션 · CSRF |
| GET | `/s/{token}` | 공개 공유 HTML | 공개 |
| GET | `/s/{token}/image.png` | 공개 공유 이미지 | 공개 |

dev 전용이다. `PROMPT_ADMIN_ENABLED=true`이고 `PROMPT_ADMIN_TOKEN`이 있을 때만 뜨고, 운영은 404다. 토큰이 없거나 틀리면 `401`이다. 운영 도구라 화면은 쓰지 않는다.

| 메서드 | 경로 | 하는 일 | 인증 |
| --- | --- | --- | --- |
| POST | `/v1/admin/prompts/{name}` | 프롬프트 다음 판 저장 (`201`) | `X-Admin-Token` · CSRF |
| GET | `/v1/admin/prompts/{name}` | 최신 판 | `X-Admin-Token` |
| GET | `/v1/admin/prompts/{name}/versions` | 판 목록(본문 없음) | `X-Admin-Token` |

프롬프트 이름은 영문 소문자, 숫자, 하이픈으로 된 1~40자다. 어기면 `400 INVALID_REQUEST`다.

</details>

## 품질 게이트

<p align="center"><img src="docs/images/quality.svg" alt="품질 게이트: 1 gradlew check(테스트 575개, bootJar), 2 ArchUnit과 파일당 타입 1개 구조 테스트, 3 컨벤션 확인 명령, 4 9인 리뷰 패널" width="100%"></p>

- 자동 검사를 통과한 뒤 리뷰어 9명이 판정한다.

## 부하와 메모리

<p align="center"><img src="docs/images/capacity.svg" alt="부하와 메모리 전후: dev 1GB 서버에 짧은 대화를 동시에 올린 시험에서 결과까지 p95가 5명 22~28초에서 9.6초, 15명 59초에서 16.2초, 30명은 메타스페이스 OOM으로 재시작해 측정 실패에서 33.3초가 됐다. 시간당 처리 약 850건에서 약 3,200건, 앱 재시작 2회에서 0회. JVM AOT 캐시 뒤 30명 시험 후 메타스페이스 133MB에서 32MB, 기동 26초에서 12초, 유휴 컨테이너 372MiB에서 346MiB, 스왑 269MB에서 194MB" width="100%"></p>

- 작업자 하나가 작업을 하나씩 채점하던 때는 동시 15명부터 앱이 메타스페이스 OOM으로 죽었다. 채점 자리를 3개로 늘리고 작업 잡기와 비용 확인을 잠금으로 묶은 뒤에는 30명이 몰려도 p95 33초, 오류 0건이다.
- JVM은 이미지를 빌드할 때 AOT 캐시(`/app/app.aot`)를 만들어 띄운다. 힙은 `-Xms64m -Xmx256m -Xmn24m`, 메타스페이스 상한은 160MB, `MALLOC_ARENA_MAX=2`이고 앱 컨테이너 `mem_limit`은 512MB다. 캐시가 어긋나도 캐시 없이 뜬다.
- 측정 방법과 NMT 표는 [배포 운영서](docs/deployment.md)에 있다.

## 배포

- `develop` 푸시: 테스트 → GHCR 이미지 게시 → dev 자동 배포. GitHub OIDC로 AWS 배포 역할을 맡아 러너 IP에만 SSH를 잠깐 열고, 검증한 이미지 digest로 바꾼 뒤 닫는다. 늦게 끝난 옛 실행이 새 배포를 덮지 않도록, develop 머리에 더 새 백엔드 변경이 있거나 서버 릴리스가 이미 이 커밋을 품으면 배포를 건너뛴다.
- `main` 푸시: 테스트 → 이미지 게시까지만 한다. 운영 배포는 수동 실행(`workflow_dispatch`, `deploy=true`)이다.
- 앱 설정(`GEMINI_API_KEY`, 도메인, CORS 등)은 KMS로 암호화해 S3 `config/app.env`에 두고, 서버가 부팅 때 tmpfs로 받는다. 같은 파일의 `GHCR_USER`, `GHCR_TOKEN`으로 이미지를 받는다.
- SQLite는 매시 S3에 백업하고(SSE-KMS, 최대 21일), Lightsail 자동 스냅숏을 매일 뜬다. `.backup` 사본은 64MB tmpfs `/tmp`가 아니라 데이터 볼륨(`/app/data/backup-tmp`)에 떴다가 바로 지운다.
- 자세한 절차는 [배포 운영서](docs/deployment.md)에 있다.

## 비용

| 항목 | 월 비용(USD) | 메모 |
| --- | ---: | --- |
| Lightsail `micro_3_0`(dev) | 7 | 1GB, 고정 IP·전송량 포함 |
| 스냅숏, S3, KMS 키 2개, Route 53 | 약 3.5 | 인프라 레포 기준 |
| Gemini 3.5 Flash-Lite | 최대 9 | 입력 100만 토큰 0.3달러(캐시 읽기 0.03달러), 출력 2.5달러로 어림한다 |

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
- `bootRun`은 프롬프트 관리 API(`/v1/admin/prompts`) 스위치를 켠다. 실제로 뜨려면 환경 변수 `PROMPT_ADMIN_TOKEN`도 넣어야 하고, 요청은 `X-Admin-Token` 헤더에 그 값을 싣는다.

## 데이터 출처

전국 시·도·시·군·구 지역과 대표 좌표([`regions.sql`](src/main/resources/regions.sql))는 2026-10-01 기준 행정동 경계로 만들었다. 생성 스크립트는 [`scripts/regions/generate_regions.py`](scripts/regions/generate_regions.py)다.

> 본 데이터는 통계청 통계지리정보서비스(SGIS, https://sgis.kostat.go.kr)에서 공공누리 제1유형으로 개방한 행정동 경계를 가공한 것이며(가공: vuski/admdongkor, https://github.com/vuski/admdongkor), CC BY 4.0으로 배포됩니다.

## 더 읽기

[시스템 아키텍처](docs/system-architecture.md) · [백엔드 설계](docs/backend-design.md) · [코드 컨벤션](docs/backend-conventions.md) · [배포](docs/deployment.md) · [API 연동 가이드](../docs/api/frontend-api-guide.md) · [작업 지침](CLAUDE.md)

<p align="center"><img src="docs/images/footer-season.svg" alt="2026-12-25 0시(KST) 시즌 마감. 순위와 선물 도착 시각이 확정되고, 그 뒤 새 분석과 지역 변경은 409 SEASON_FROZEN이다" width="100%"></p>
