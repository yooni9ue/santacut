# 프론트엔드 API 연동 가이드

산타컷 백엔드 API를 화면에서 부를 때 알아야 할 흐름을 정리한 문서다. 요청·응답 필드의 정확한 정의는 OpenAPI(Swagger)가 기준이고, 이 문서는 호출 순서와 규칙을 설명한다. 이 문서의 엔드포인트와 필드는 2026-11-17 dev 서버의 `/v3/api-docs`와 맞췄다.

## 프론트 반영 체크리스트 (2026-11-17 백엔드 변경분, #47~#60)

백엔드가 바뀌면서 화면에서 손봐야 할 일을 모았다. 자세한 규칙은 각 절에 있다.

- [ ] **API 주소**: Vercel Preview·Development에 `https://api-dev.santacut.me`, Production은 운영 서버를 켠 뒤 `https://api.santacut.me`(0절).
- [ ] **도메인 연결**: Vercel Settings → Domains에 `santacut.me`, `www.santacut.me`(main), `dev.santacut.me`(develop)를 추가한다. DNS는 백엔드가 Route 53에 걸어 두었다.
- [ ] **쿠키·CSRF**: 모든 `fetch`에 `credentials: 'include'`, SSE는 `{ withCredentials: true }`. 쓰기 요청 전 `GET /v1/csrf` 본문의 토큰을 `X-XSRF-TOKEN` 헤더로 보낸다. 쿠키는 다른 출처라 읽을 수 없다(2·3절).
- [ ] **분석 제출**: `Idempotency-Key` 헤더 필수. 업로드가 몰리면 `503 UPLOAD_BUSY`(`Retry-After` 10초)가 오니 같은 키로 잠시 뒤 다시 보낸다. 같은 IP에서 너무 많이 내면 `429 CLIENT_RATE_LIMITED`(`retry_at`, `Retry-After`)다(8·9절).
- [ ] **동의 문구**: 상대 메시지를 포함한 최근 대화가 Google Gemini로 전송되고 채점 뒤 삭제된다는 고지를 업로드 동의 화면에 넣는다(6.1절, 법률 검토 필요).
- [ ] **내 결과 화면** `GET /v1/me/result`: 새 필드 `archetype`, `dimension_scores`, `weakest_dimension`, `top_percent`(100명 미만이면 null), `archetype_share_percent`(30명 미만이면 null), `badges`, `quote`(null 가능), `low_confidence_dimensions`(근거 부족 항목에 "근거 적음" 표시). 정확한 도착 시각 `eta`는 본인 결과에만 있다(7절).
- [ ] **랭킹 화면** `GET /v1/rankings`: 행의 도착 정보는 `eta_date`(서울 날짜)를 쓴다. `eta`는 항상 null이다. 페이지 사이에 순위가 다시 계산되면 `409 RANKING_SNAPSHOT_CHANGED`가 오니 목록을 비우고 첫 페이지부터 다시 읽는다. 예전 숫자 cursor는 `400`이다(7절).
- [ ] **순위 반영 대기**: 분석이 끝난 뒤 순위는 최대 30초 안에 반영된다. SSE는 `RANKING_PENDING` 다음 `SUCCEEDED` 순서로 오고, 결과의 `ranking_pending`이 true인 동안은 "순위 계산 중"을 보여 준다(6.0절).
- [ ] **지역 변경**: 24시간 2번, 시즌 5번까지. 넘으면 `429 REGION_CHANGE_LIMITED`(`retry_at` 있음) 또는 `REGION_CHANGE_SEASON_LIMIT`(없음)(5절).
- [ ] **공유**: `POST /v1/shares` 본문 `include_quote`(기본 false)로 본인 한마디를 카드에 넣을지 고른다. 이미 만든 링크가 있으면 같은 링크에 바뀐 값이 반영되니 켜기·끄기 토글로 만든다. 공유 카드에는 날짜만 보이고 상대 대화는 절대 나오지 않는다(7절 공유 카드 구성).
- [ ] **참가 등록 한도**: 같은 IP에서 1시간에 10번 넘게 등록하면 `429 CLIENT_RATE_LIMITED`다. `retry_at`으로 다시 시도할 시각을 보여 준다(9절).
- [ ] **큰 요청 본문**: 파일 업로드가 아닌 요청 본문이 2MB를 넘으면 `413 REQUEST_TOO_LARGE`다(8절).
- [ ] **관리 API 제외**: `/v1/admin/**`는 dev 전용 운영 도구다. `X-Admin-Token`이 있어야 하고 운영은 404다. dev OpenAPI에 보이더라도 화면과 타입 생성 대상에서 뺀다(4절).

## 0. 환경별 주소

| 환경 | 화면(Vercel) | API | 상태 |
| --- | --- | --- | --- |
| 로컬 | `http://localhost:3000` | `http://localhost:8080` | `backend/`에서 `./gradlew.bat bootRun`(JDK 25) |
| dev | `https://dev.santacut.me` | `https://api-dev.santacut.me` | `develop` 브랜치가 자동 배포된다 |
| 운영 | `https://santacut.me`, `https://www.santacut.me` | `https://api.santacut.me` | 아직 켜지 않았다 |

- Swagger UI: `https://api-dev.santacut.me/swagger-ui.html`(로컬은 `http://localhost:8080/swagger-ui.html`)
- OpenAPI JSON: `https://api-dev.santacut.me/v3/api-docs`. 타입 생성 도구 입력으로 쓴다.
- API는 HTTPS만 받는다. Caddy가 Let's Encrypt 인증서로 TLS를 끝내고, `http://` 요청은 `308`로 HTTPS로 넘긴다. 응답에는 HSTS(`Strict-Transport-Security: max-age=31536000`)가 붙는다.

## 1. 서비스 흐름

1. 닉네임과 지역(시·도 → 시·군·구)을 골라 참가 등록한다. 지역은 전국 시·도 16개와 그 아래 시·군·구 255개다.
2. 카카오톡·인스타그램 내보내기 원본 파일을 올리거나, 정규화한 이벤트 목록을 보내 분석을 요청한다.
3. 분석 진행 상황을 SSE 스트림으로 받거나 폴링으로 확인하고, 끝나면 결과를 조회한다.
4. 내 점수·순위·선물 도착 예정 시각, 산타 유형·배지·항목 막대와 전체 랭킹을 본다.
5. 결과 공유 링크를 만들거나 폐기한다.

점수와 도착 시각은 재미를 위한 시뮬레이션이다. 원본 내보내기 파일(카카오톡 TXT, 인스타그램 `message_1.json`)은 `POST /v1/analyses/files`로 그대로 올린다(§6.1).

## 2. 호출 경로와 CORS

### 권장: 같은 출처 프록시

프론트엔드는 Next.js `rewrites`(`frontend/next.config.ts`)로 아래 경로를 `BACKEND_ORIGIN`(dev는 `https://api-dev.santacut.me`)에 넘긴다. 브라우저는 화면과 같은 출처(`https://dev.santacut.me/v1/...`)를 부르므로 CORS도, 서드파티 쿠키 차단도 신경 쓸 일이 없다. 로컬에서는 `BACKEND_ORIGIN`을 비우면 `http://127.0.0.1:8080`으로 간다.

| 경로 | 용도 |
| --- | --- |
| `/v1/**` | API |
| `/s/**` | 공개 공유 페이지와 이미지 |
| `/swagger-ui/**`, `/swagger-ui.html`, `/v3/**` | API 문서 |

분석 제출에 쓰는 `Idempotency-Key` 헤더도 CORS 허용 헤더에 있어서 API 주소로 바로 보내도 된다. 그래도 서드파티 쿠키 차단을 피하려면 이 프록시를 쓰는 편이 낫다.

### API 주소로 바로 부를 때

- 서버는 `APP_CORS_ALLOWED_ORIGINS`에 적은 출처만 허용한다. 코드 기본값은 `https://www.santacut.me,https://santacut.me`다. dev 서버는 여기에 `https://dev.santacut.me`와 `http://localhost:3000`을 더해 둔다. 다른 출처(예: Vercel 미리보기 주소)에서 부르려면 백엔드에 그 출처를 넣어 달라고 한다. 허용하지 않은 출처의 사전 요청(preflight)은 `403`이다.
- 허용 메서드는 `GET`, `POST`, `PATCH`, `DELETE`이고, 허용 요청 헤더는 `Content-Type`, `X-XSRF-TOKEN`, `Last-Event-ID`, `Idempotency-Key`다. 응답 헤더 가운데 `Retry-After`를 읽을 수 있다. 사전 요청 결과는 3,600초 캐시된다.
- 모든 `fetch`에 `credentials: 'include'`를 넣는다. 빠뜨리면 세션 쿠키도 CSRF 쿠키도 가지 않아 `401`이나 `403`이 난다. `EventSource`는 `{ withCredentials: true }`로 연다.
- CSRF 쿠키(HTTPS인 dev·운영은 `__Host-XSRF-TOKEN`, 로컬 HTTP는 `XSRF-TOKEN`)는 API 호스트의 HttpOnly 쿠키라 화면의 `document.cookie`로는 읽을 수 없다. 토큰은 쿠키가 아니라 `GET /v1/csrf` 응답 본문에서 꺼낸다(§3).
- dev 서버는 쿠키를 `SameSite=None; Secure`로 내린다(`APP_SESSION_SECURE=true`, `APP_SESSION_SAME_SITE=None`). 그래서 `http://localhost:3000`에서 `https://api-dev.santacut.me`를 바로 불러도 쿠키가 실린다. 다만 브라우저가 서드파티 쿠키를 막으면(Safari 기본값, Chrome 시크릿 창 등) 쿠키가 빠져 `401`이나 `403`이 나므로, 이때는 프록시를 쓴다.
- 운영은 화면과 API가 같은 사이트(`santacut.me`)라 `SameSite=Lax`(기본값)로도 쿠키가 간다.

## 3. 인증과 CSRF

1. 페이지 로드 시 `GET /v1/me`를 호출한다. `200`이면 로그인 상태, `401`이면 미등록 상태다.
2. 쓰기 요청(`POST`, `PATCH`, `DELETE`) 직전에 `GET /v1/csrf`를 호출한다. 응답 `{ headerName, token }`의 `token`을 `headerName`(기본 `X-XSRF-TOKEN`) 헤더로 보낸다. 같은 응답이 CSRF 쿠키(`__Host-XSRF-TOKEN`, 로컬 HTTP는 `XSRF-TOKEN`)도 설정하므로 쿠키를 지우지 않는다. 화면은 쿠키 이름에 기대지 않는다.
3. `POST /v1/participants`로 등록하면 세션이 HttpOnly 쿠키로 설정된다. 쿠키 이름은 HTTPS로 띄운 dev·운영(`APP_SESSION_SECURE=true`)에서 `__Host-sc_session`, 로컬 HTTP(`APP_SESSION_SECURE=false`)에서 `sc_session`이다. 프론트엔드는 HttpOnly라 쿠키를 직접 읽지 않으므로 이름에 기대지 않는다. 세션은 30일간 유효하며 JSON 응답에는 세션 값이 없다.
4. 이후 요청은 쿠키가 자동으로 실려 인증된다. 세션이 없거나 만료되면 `401`이다.

## 4. API 목록

| 메서드 | 경로 | 인증 | CSRF | 설명 |
| --- | --- | --- | --- | --- |
| GET | `/v1/csrf` | - | - | CSRF 토큰 발급 |
| GET | `/v1/regions` | - | - | 지역 목록(`?parent=`로 하위 지역) |
| GET | `/v1/nicknames/availability` | - | - | 닉네임 가용성 확인 |
| POST | `/v1/participants` | - | 필요 | 참가 등록 (`201`) |
| GET | `/v1/me` | 필요 | - | 내 참가 정보 |
| PATCH | `/v1/me/region` | 필요 | 필요 | 내 지역 변경(시즌 마감 전) |
| DELETE | `/v1/me` | 필요 | 필요 | 내 데이터 삭제 (`204`) |
| POST | `/v1/analyses` | 필요 | 필요 | 이벤트 목록으로 분석 요청 (`202`) |
| POST | `/v1/analyses/files` | 필요 | 필요 | 원본 파일로 분석 요청 (`202`, multipart) |
| GET | `/v1/analyses/{id}` | 필요 | - | 분석 작업 진행 상태 |
| GET | `/v1/analyses/{id}/events` | 필요 | - | 분석 작업 진행 상황 스트림(SSE, `text/event-stream`) |
| GET | `/v1/me/analysis` | 필요 | - | 내 최근 분석 작업 상태 |
| GET | `/v1/me/result` | 필요 | - | 내 최신 점수·순위·도착 시각·확정 상태 |
| GET | `/v1/rankings` | - | - | 랭킹 50명씩(cursor 페이지네이션) |
| POST | `/v1/shares` | 필요 | 필요 | 공유 링크 생성·재사용 |
| DELETE | `/v1/shares/{token}` | 필요 | 필요 | 공유 링크 폐기 (`204`) |
| GET | `/s/{token}` | - | - | 공개 공유 HTML 페이지 |
| GET | `/s/{token}/image.png` | - | - | 공개 공유 이미지(1200×630) |

`/v1/admin/prompts/**` 세 개는 dev 전용 운영 도구다. `X-Admin-Token` 헤더로만 열리고(없거나 틀리면 `401`), 운영 서버에는 없다(`404`). 화면은 부르지 않으며, dev OpenAPI로 타입을 만들 때도 뺀다.

## 5. 참가 등록

- 닉네임: 앞뒤 공백 제거 후 NFKC 정규화, 한글·영문·숫자·밑줄 2~16자. 대소문자 구분 없이 중복을 막는다(`409`).
- 입력을 멈추면 `GET /v1/nicknames/availability?nickname=`로 확인한다. 응답은 `{ available }`이다. 로그인은 필요 없다.
- 가용성 확인은 예약이 아니다. `available`이 `true`여도 등록 때 `NICKNAME_TAKEN`이 날 수 있다. 화면에 "예약됨"으로 쓰지 않는다.
- 형식이 틀리면 `400`(`INVALID_NICKNAME`), `nickname` 쿼리가 없으면 `400`(`INVALID_REQUEST`)이다.
- 지역은 두 단계로 고른다. `GET /v1/regions`로 시·도를 받고, `GET /v1/regions?parent={시·도 code}`로 그 아래 지역을 받는다.
- 각 지역은 `{ code, name, parent_code, selectable }`이다. 최상위 지역은 `parent_code`가 `null`이다.
- `regionCode`에는 `selectable`이 `true`인 `code`만 쓴다. 구가 없는 시·도는 그 자체로 `selectable`이 `true`다.
- 하위 지역이 있는 시·도나 없는 코드를 보내면 `400`(`UNSUPPORTED_REGION`)이다. 시·도를 바꾸면 하위 선택을 비운다.

### 지역 변경 `PATCH /v1/me/region`

- 본문은 `{ regionCode }`이다. 규칙은 등록과 같다(`selectable`인 코드만).
- 성공하면 `200`과 `GET /v1/me`와 같은 참가 정보를 준다. 순위와 도착 예정 시각은 서버가 30초쯤 안에 모아서 다시 계산한다. 그동안 내 결과의 `eta`는 이전 지역 기준이다.
- 지금과 같은 지역을 보내면 아무것도 바꾸지 않고 `200`을 주며, 변경 횟수도 세지 않는다.
- 지역은 24시간에 2번, 한 시즌에 5번까지 바꿀 수 있다(서버 설정). 24시간 한도를 넘으면 `429`(`REGION_CHANGE_LIMITED`)이고 본문 `retry_at`과 `Retry-After` 헤더가 다시 바꿀 수 있는 시점이다. 시즌 한도를 넘으면 `429`(`REGION_CHANGE_SEASON_LIMIT`)이고 `retry_at`이 없다. 도착 예정 시각 차이로 다른 사람의 지역을 좁혀 가지 못하게 하려는 한도다.
- 시즌 마감(12월 25일 0시, 한국 시간) 뒤에는 `409`(`SEASON_FROZEN`)다. 마감 뒤에는 변경 버튼 대신 안내를 보여준다.
- 이미 만든 공유 링크는 만든 당시 결과를 그대로 보여준다.

### 내 데이터 삭제 `DELETE /v1/me`

- 지우기 전에 삭제 범위와 복구 불가를 확인받는다(기획 §5).
- 참가 정보, 세션, 분석 작업과 아직 남은 원본 파일, 평가, 순위 기록, 공유 링크가 모두 지워진다. 되돌릴 수 없다.
- 성공하면 `204`이고 세션 쿠키가 만료된다(`Max-Age=0`). 이후 `GET /v1/me`는 `401`이다. 완료 화면을 보여준다.
- 시즌 마감 전이면 남은 참가자로 순위를 다시 계산한다. 마감 뒤에는 다시 계산하지 않는다.
- 이미 저장되거나 전달된 공유 이미지는 회수할 수 없다. 화면에 회수된다고 쓰지 않는다.

## 6. 분석 요청과 상태 확인

`POST /v1/analyses` 요청 규칙:

- `Idempotency-Key` 헤더 필수: 영문·숫자·`_`·`-` 16~100자(아니면 `400` `INVALID_IDEMPOTENCY_KEY`, 헤더가 없으면 `400` `INVALID_REQUEST`). 같은 요청을 다시 보낼 때(네트워크 재시도)만 같은 키를 쓴다. 같은 키에 다른 본문을 보내면 `409`(`IDEMPOTENCY_CONFLICT`).
- 서버에 AI 모델이 설정되지 않았으면 `503`(`AI_NOT_CONFIGURED`)이다. 두 제출 API 모두 같다.
- 본문: `source`(`KAKAO` 또는 `INSTAGRAM`), `events` 20~2,000개. 각 이벤트는 `occurredAt`(ISO-8601 UTC)과 `text`(1~2,000자).
- 이벤트 수가 20개 미만이면 `INSUFFICIENT_EVENTS`, 2,000개 초과면 `TOO_MANY_EVENTS`(`400`).
- 전체 글자 수 20만 자 이하, 이벤트 시각은 최근 90일 안이고 현재보다 5분 넘게 미래가 아니어야 한다(`400`, `INVALID_EVENT_RANGE`).
- 한 사람당 24시간에 3번까지(`429`, `DAILY_USER_LIMIT`). 응답의 `retry_at`과 `Retry-After` 헤더가 다시 낼 수 있는 시점이다. 12월 25일 0시(한국 시간) 이후에는 시즌 마감(`409`).

응답 `202 { id, status }`의 `id`로 진행 단계를 따라간다. 먼저 SSE 스트림(§6.0)을 열고, 안 되면 `GET /v1/analyses/{id}`를 2~3초 간격으로 다시 조회한다. 응답의 `status`가 진행 단계다. 202 응답의 `status`도 같은 값을 쓴다.

| `status` | 화면 문구(기획 §4.7) | 다음 행동 |
| --- | --- | --- |
| `VALIDATING` | 파일 형식을 확인하고 있어요 | 계속 조회. 원본 파일 제출에서만 나온다 |
| `QUEUED` | 산타 심사를 기다리고 있어요 | 계속 조회 |
| `ANALYZING` | 산타가 기록을 읽고 있어요 | 계속 조회 |
| `RANKING_PENDING` | 심사가 끝났어요. 순위와 도착일을 계산하고 있어요 | 계속 조회. 서버가 순위를 30초쯤마다 모아서 계산하므로 보통 1분 안에 `SUCCEEDED`가 된다 |
| `SUCCEEDED` | - | 조회를 멈추고 `GET /v1/me/result`로 이동 |
| `FAILED` | `error_code`로 구분 | 조회를 멈추고 실패 안내 |

- `FAILED`의 `error_code`: `INSUFFICIENT_EVIDENCE`(근거 부족), `EXPIRED`(24시간 대기 만료), `RETRY_EXHAUSTED`·`ANALYSIS_FAILED`(AI 오류), `SEASON_FROZEN`(시즌 마감).
- 원본 파일 제출에서만 나오는 `error_code`: `UNSUPPORTED_FILE_FORMAT`(형식 오류), `SPEAKER_NOT_IDENTIFIED`(본인 이름 없음), `INSUFFICIENT_EVENTS`(본인 메시지 20개 미만). `RETRY_EXHAUSTED`는 파일 검증을 세 번 넘게 시작했을 때도 나온다.
- 점수·순위는 상태 응답에 없다. 결과는 `GET /v1/me/result` 하나로 읽는다.
- 창을 닫아도 심사는 계속된다. 재방문하면 `GET /v1/me/analysis`로 가장 최근 작업 상태를 읽는다. 진행 중이면 심사 화면, `SUCCEEDED`면 결과 화면으로 보낸다. 낸 작업이 없으면 `404`(`NOT_FOUND`)다.
- 오래 걸려도 새 분석을 자동으로 만들지 않는다. 같은 `id`를 다시 조회한다.
- 서버의 한 달 AI 비용 한도에 닿았거나, Gemini가 호출을 막았거나(선불 크레딧 소진 `429` 포함), 연달아 실패하면 작업자가 15분 쉬었다가 다시 잡는다. 이때 작업은 실패하지 않고 `QUEUED`에 머문다. 하루 비용 한도는 기본으로 없다. 24시간이 지나도록 못 잡으면 `EXPIRED`로 끝난다.
- 본인 메시지가 8개 또는 300자보다 적으면 AI를 부르지 않고 `INSUFFICIENT_EVIDENCE`로 끝난다. 이 밖의 근거 부족 실패는 본인 말이 거의 없어 어느 항목도 판단할 수 없을 때만 난다. 한두 항목만 근거가 약하면 실패하지 않고 그 항목을 사전값(중립 점수와 본인의 다른 항목 평균)으로 매긴다(§7 `low_confidence_dimensions`).
- 같은 참가자가 같은 내용으로 다시 내면 AI를 다시 부르지 않고 지난번과 같은 결과(점수 또는 근거 부족)로 끝난다.

### 6.0 진행 상황 스트림 `GET /v1/analyses/{id}/events`

폴링 대신 SSE로 진행 단계를 받을 수 있다. 폴링은 그대로 있으며, 스트림이 안 되면 폴링으로 바꾼다.

- 연결하자마자 현재 단계를 한 번 받고, 단계가 바뀔 때마다 하나씩 받는다.
- 이벤트 이름은 `status`, `data`는 `GET /v1/analyses/{id}`와 같은 JSON이다.
- `id`는 진행 단계 순번이다(`VALIDATING` 0, `QUEUED` 1, `ANALYZING` 2, `RANKING_PENDING` 3, `SUCCEEDED` 4, `FAILED` 5). AI 제공자가 잠시 막아 대기열로 돌아가면 작아질 수 있다.
- `SUCCEEDED`나 `FAILED`를 보낸 뒤 서버가 닫는다. 이미 끝난 작업에 연결하면 끝난 단계를 받고 바로 닫힌다.
- 15초마다 주석 줄(`: heartbeat`)이 온다. `EventSource`는 이 줄을 이벤트로 넘기지 않는다.
- 5분이 지나면 서버가 닫는다. 브라우저 `EventSource`는 `Last-Event-ID`를 붙여 다시 연결하고, 서버는 현재 단계를 처음부터 다시 보낸다.
- 인증과 소유 규칙은 상태 조회와 같다. 세션이 없으면 `401`, 다른 참가자의 작업이면 `404`다.
- 참가자 한 명은 스트림을 하나만 쓴다. 새로 열면 이전 스트림이 닫힌다(탭을 여러 개 열면 마지막 탭만 받는다).
- 서버 전체 스트림 수가 상한에 닿으면 `503`(`ANALYSIS_STREAM_LIMIT`)과 `Retry-After`가 온다. 이때는 폴링으로 바꾼다.

`EventSource`는 응답 코드와 본문을 알려 주지 않는다. 그래서 처음 연결이 실패하면(`503` 포함) 이유를 따지지 않고 폴링으로 바꾼다.

```ts
const POLL_INTERVAL_MS = 3000; // 폴링은 2~3초 간격을 권장한다

function watchAnalysis(id: string, onStatus: (status: AnalysisStatus) => void) {
  let opened = false;
  const source = new EventSource(`/v1/analyses/${id}/events`, { withCredentials: true });

  source.addEventListener("open", () => { opened = true; });
  source.addEventListener("status", (event) => {
    const status = JSON.parse((event as MessageEvent).data) as AnalysisStatus;
    onStatus(status);
    if (status.status === "SUCCEEDED" || status.status === "FAILED") source.close();
  });
  source.addEventListener("error", () => {
    // 한 번도 열리지 않았으면(401, 404, 503 등) 스트림을 접고 폴링한다.
    // 열린 뒤 끊긴 것은 EventSource가 Last-Event-ID를 붙여 다시 연결하게 둔다.
    if (!opened || source.readyState === EventSource.CLOSED) {
      source.close();
      startPolling(id, onStatus, POLL_INTERVAL_MS);
    }
  });
  return () => source.close();
}
```

- 예시는 §2의 같은 출처 프록시를 쓴다고 보고 상대 경로로 연다. API 주소로 바로 열 때는 앞에 API 주소를 붙이고, 그때는 `withCredentials: true`가 있어야 세션 쿠키가 함께 간다.
- 스트림을 쓰는 동안에는 폴링을 멈춘다. 화면을 떠나면 `close()`로 닫는다.

### 6.1 원본 파일 업로드 `POST /v1/analyses/files`

지원 형식은 카카오톡 대화 TXT(PC·iOS·Android 내보내기)와 인스타그램 `messages/inbox/<대화방>/message_1.json`이다. zip, CSV, EUC-KR 파일은 받지 않는다. 인스타그램은 zip을 풀어 `message_1.json` 하나만 고른다.

흐름:

1. 파일을 고르면 브라우저에서 읽어 화자 목록을 뽑는다(아래 정규식). 등록 전에는 파일을 서버로 보내지 않는다(기획 §2·§4.4).
2. 화자 목록에서 본인을 고르게 한다. 카카오톡과 인스타그램 모두 고른다. 인스타그램 파일에는 소유자 표시가 없다.
3. 참가 등록과 동의를 마친 뒤 multipart로 올린다. 파트는 `file`(필수), `source`(`KAKAO`·`INSTAGRAM`), `self_speaker`(1~100자)다. 헤더는 `Idempotency-Key`와 `X-XSRF-TOKEN`이다.
4. 응답은 `202 { id, status: "VALIDATING" }`이다. 이후 폴링은 §6과 같다. 검증을 통과하면 `QUEUED`로 바뀐다.

규칙:

- 서버는 최근 대화 전체를 채점에 쓴다. 점수는 본인 행동만 매기지만, 본인 말의 뜻을 읽으려고 상대(다른 참여자) 메시지도 함께 Google Gemini에 보낸다. 참여자 이름은 `나`·`상대`(단체 대화는 `상대1`, `상대2`)로 바꿔 보내고 저장하지 않는다.
- 상대 메시지를 포함한 대화 내용은 채점 작업에만 쓰고 작업이 끝나면(성공·실패 모두) 지운다.
- 동의 화면 문구 제안(출시 전 법률 검토 필요): "고른 대화방의 최근 90일 대화(내 메시지와 상대방 메시지)를 점수 산정을 위해 Google Gemini로 보냅니다. 상대방 이름은 보내지 않고 '상대'로 바꿉니다. 점수는 내 말만 평가하며, 대화 내용은 채점이 끝나면 지웁니다. 상대방 메시지가 포함되므로 상대방이 원하지 않을 만한 대화는 올리지 마세요."
- 최근 90일 안의 메시지 중 2,000개(본인 최신 1,000개를 먼저 남기고 나머지는 최신순), 누적 200,000자까지만 쓴다. 카카오톡 TXT가 100만 줄을, 인스타그램 JSON이 객체 100만 개를 넘으면 `UNSUPPORTED_FILE_FORMAT`이다. 메시지 하나는 2,000자에서 자른다.
- 원본과 본인 이름은 검증이 끝나면 지운다. 늦어도 24시간 뒤에는 지운다. 파일 이름은 저장하지 않는다.
- 하루 3번 한도는 `POST /v1/analyses`와 함께 센다.
- 같은 키로 같은 파일·출처·본인 이름을 다시 보내면 처음 작업을 준다. 하나라도 다르면 `409`(`IDEMPOTENCY_CONFLICT`)다. 같은 키를 두 엔드포인트에 같이 써도 `409`다.
- 빈 파일, zip, 바이너리 파일은 바로 `422`(`UNSUPPORTED_FILE_FORMAT`)다. 그 밖의 형식 오류는 상태 조회의 `error_code`로 온다.
- 서버는 파일 이름, 확장자, Content-Type을 보지 않는다. 내용만 본다.

브라우저 화자 추출(카카오톡). 파일을 UTF-8로 읽고 맨 앞 BOM을 뗀다. 줄마다 아래 정규식을 `^…$`로 맞춘다. 처음 맞은 메시지 줄의 형식(PC 또는 모바일)으로 정하고, 그 뒤로는 같은 형식 줄만 메시지로 본다. 이름은 PC 패턴의 1번 그룹, 모바일 패턴의 7번 그룹이다.

| 줄 | 정규식 |
| --- | --- |
| PC 날짜 구분선 | `^-{5,} (\d{4})년 (\d{1,2})월 (\d{1,2})일 \S요일 -{5,}$` |
| PC 메시지 | `^\[(.{1,100}?)\] \[(오전\|오후) (\d{1,2}):(\d{2})\] (.*)$` |
| iOS 메시지 | `^(\d{4})\. (\d{1,2})\. (\d{1,2})\. (?:(오전\|오후) )?(\d{1,2}):(\d{2}), (.{1,100}?) : (.*)$` |
| Android 메시지 | `^(\d{4})년 (\d{1,2})월 (\d{1,2})일 (오전\|오후) (\d{1,2}):(\d{2}), (.{1,100}?) : (.*)$` |

- 표 안의 `\|`는 마크다운 표 때문에 붙인 것이다. 정규식에서는 `|`다.
- 모바일 날짜 구분선(`\d{4}년 \d{1,2}월 \d{1,2}일 \S요일`, `\d{4}년 \d{1,2}월 \d{1,2}일 (오전|오후) \d{1,2}:\d{2}`)과 시스템 줄(시각 접두어는 있지만 `, 이름 : `가 없는 줄)은 화자가 아니다.
- 인스타그램은 JSON의 `messages[].sender_name`에서 뽑는다. 모든 글자가 U+00FF 이하이면 ISO-8859-1 바이트로 보고 UTF-8로 다시 읽는다. 다시 읽은 결과에 U+FFFD가 생기면 원래 값을 쓴다.
- 화자 비교 규칙: 앞뒤 공백을 떼고(`trim`) NFC로 정규화한 값이 완전히 같아야 같은 사람이다. `self_speaker`에는 목록에서 고른 값을 그대로 보낸다.
- 정규식은 공개된 내보내기 예시로 만들었다. 실제 샘플로는 아직 검증하지 않았다.

파일 크기:

- 20MB 이하만 받는다. 고를 때 브라우저에서 먼저 크기를 확인한다.
- 넘으면 `413`이다. 서버는 `code: FILE_TOO_LARGE`를 준다. 앞단 프록시가 막으면 `code` 없는 `413`이 올 수 있다. 화면은 `413`이면 `code`가 없어도 `FILE_TOO_LARGE`로 다룬다.

## 7. 표시 규칙

- `score_hundredths`는 정수다. 100으로 나눠 소수 둘째 자리까지 표시한다(`8245` → `82.45점`).
- `eta`, `computed_at` 등 시각은 ISO-8601 UTC 문자열이다. 화면에는 `Asia/Seoul`로 바꿔 보여준다.
- 정확한 도착 예정 시각(`eta`)은 본인 결과(`GET /v1/me/result`)에만 있다. 랭킹처럼 다른 사람이 보는 곳에는 날짜(`eta_date`, `Asia/Seoul`)만 온다. 공유 카드도 날짜만 싣는다.
- 도착 예정일은 참가자가 아무리 많아도 12월 25일부터 12월 31일(하루 0~6시) 안에 들어온다. 참가자가 많으면 산타가 그만큼 빨라진다고 보고 모든 작업 시간을 같은 비율로 줄인다.
- 순위는 동점이면 같은 순위를 공유한다. `tied`가 `true`면 `공동 N위`로 표시한다. 배송 순번(`delivery_sequence`)은 항상 서로 다르다.
- `stage`는 `PREVIEW`(시즌 마감 전 미리보기)나 `FINAL`(시즌 확정)이다. 계산 시각과 함께 보여준다.

### 내 결과 `GET /v1/me/result`

- 점수와 순위는 늘 같은 심사 것이다. 서로 다른 심사의 점수와 순위를 섞지 않는다.
- `ranking_pending`이 `true`면 새 심사가 아직 순위에 반영되지 않았다. 이전 스냅숏이 있으면 그 점수·순위를 그대로 보여주고 반영 중임을 따로 알린다.
- 첫 순위 계산 전이면 `rank`, `delivery_sequence`, `eta`, `computed_at`이 `null`이다. 점수·판정을 먼저 보여주고 순위·도착일은 `계산 중`으로 둔다.
- 점수가 나온 심사가 없으면 `404`(`RESULT_NOT_FOUND`)다.
- `low_confidence_dimensions`는 근거가 부족해 사전값(중립 점수 55와 근거 있는 다른 항목 평균들의 평균)을 준 채점 항목 키 배열이다(`care_empathy`, `positivity_support`, `respectful_communication`, `cooperation_sharing`). 없으면 빈 배열이다. 화면은 이 항목에 "대화에서 판단할 근거가 적었어요"처럼 신뢰도가 낮다는 표시를 붙인다. 이 필드가 생기기 전 평가는 빈 배열이다.
- 아래 필드는 2026-11-17에 더했다. 기존 필드는 그대로다. 모두 재미용 꾸밈이고 점수·순위에는 영향이 없다.
  - `archetype`: `{ code, name }`. 산타 유형 10가지 가운데 하나다(예: `{"code":"HOT_COCOA_COURIER","name":"핫초코 배달부"}`). `code`로 일러스트를 고른다. 코드 목록은 OpenAPI 스키마에 있다.
  - `dimension_scores`: 항목 막대 4개 배열. 각 원소는 `{ key, label, score }`이고 순서는 공감·응원·존중·협력이다. `score`는 0~100, 소수 둘째 자리까지다. 이 필드가 생기기 전 평가도 저장한 항목 점수로 채운다.
  - `weakest_dimension`: 점수가 가장 낮은 항목 하나(`{ key, label, score }`).
  - `top_percent`: 상위 몇 %(1~100, 올림)다. 순위 계산 참가자가 100명보다 적거나 첫 순위 계산 전이면 `null`이다. `null`이면 숨긴다.
  - `archetype_share_percent`: 같은 유형인 참가자 비율(%)이다. "참가자 중 8%만 이 유형" 같은 희귀도 표시에 쓴다. 점수를 받은 참가자가 30명보다 적으면 `null`이고, 이때는 숨긴다.
  - 유형은 위 두 항목 점수 차이로 고른다. 차이가 3점보다 작으면 균형형, 7점 이상이면 가장 높은 항목의 유형이고, 그 사이는 수축 전 평균의 차이(6점 미만이면 균형형)와 순서로 가른다. 같은 대화를 다시 채점할 때 생기는 작은 점수 흔들림으로 유형이 바뀌는 일을 줄이려는 규칙이다.
  - `badges`: 본인 말에서 낱말 횟수로 받은 배지 라벨 배열이다(예: `고마워 12회 · 감사 자판기`, `미안 0회 · 사과 품절`). 없으면 빈 배열이다. 앞 원소가 우선이다.
  - `quote`: 본인 근거 줄에서 고른 20자 이하 한마디다. 상대 말은 절대 들어가지 않는다. 없으면 `null`이다. 내 결과 화면에만 보여 주고, 공유 카드에는 사용자가 고를 때만 싣는다(아래 공유).

### 랭킹 `GET /v1/rankings`

- 로그인 없이 볼 수 있다. 내 순위 카드는 `GET /v1/me/result`로 따로 읽는다.
- 응답: `{ season_year, participant_count, computed_at, stage, rows[], next_cursor }`. 각 행은 `nickname`, `rank`, `tied`, `score_hundredths`, `eta_date`, `eta`다.
- `eta_date`는 `2026-12-26` 같은 `Asia/Seoul` 날짜다. `eta`는 더 쓰지 않는 필드로 늘 `null`이다(2026-11-17까지는 정확한 시각이었다). 행의 도착일은 `eta_date`로 보여준다.
- 한 번에 50줄이 온다. `더 보기`는 `next_cursor`를 `cursor` 쿼리로 보낸다. `next_cursor`가 `null`이면 마지막 페이지다.
- `cursor`는 불투명 문자열이다. 직접 만들지 말고 받은 값만 보낸다. 잘못된 값이나 2026-11-17 이전 형식(숫자)은 `400`(`INVALID_CURSOR`)이다.
- `cursor`에는 그 페이지의 순위 계산 시각이 들어 있다. 페이지 사이에 순위가 다시 계산되면 `409`(`RANKING_SNAPSHOT_CHANGED`)다. 이때는 목록을 비우고 `cursor` 없이 첫 페이지부터 다시 읽는다. 줄이 겹치거나 빠지지 않게 하려는 규칙이다.
- 순위는 서버가 변경을 모아 30초쯤마다 다시 계산하고, 첫 페이지는 30초쯤 캐시한다. 새로고침해도 바로 바뀌지 않을 수 있다.
- 아직 순위가 없으면 `participant_count`는 `0`, `computed_at`은 `null`, `rows`는 빈 배열이다(빈 상태 화면).

### 공유 `POST /v1/shares`, `/s/{token}`

- 공유 링크는 만든 순간의 결과를 고정한다. 순위, 공동 순위, 계산 시각, 상태(`미리보기`·`시즌 확정`), 공유 시각을 그때 값으로 담는다.
- 카드 이미지와 공개 페이지는 공동 순위를 `공동 N위`로, 계산 시각과 공유 시각을 한국 시간으로 보여준다.
- 공개 페이지에는 `공유 당시 결과`와 공유 시각이 나온다. 주요 버튼은 `나도 산타컷 받아보기`다.
- 링크가 없거나 취소됐을 때 `Accept`에 `text/html`이 있는 요청은 `404`와 "더 이상 볼 수 없는 결과" HTML을 받는다. 시작 링크(`내 산타 점수 알아보기`)가 있다. 공개 페이지(`/s/{token}`)와 이미지(`/s/{token}/image.png`) 모두 같다. 브라우저 주소창에서 이미지 주소를 열 때도 `Accept`에 `text/html`이 들어가므로 이 HTML이 온다.
- 같은 경우 그 밖의 요청(`fetch`, `<img>` 등 `Accept`에 `text/html`이 없는 요청)은 지금처럼 `404` JSON(`SHARE_NOT_FOUND`)이다.
- 링크를 비활성화해도 이미 저장·전달된 이미지는 회수할 수 없다.
- 요청 본문은 생략할 수 있다. `{"include_quote": true}`를 보내면 내 한마디(`quote`)를 카드에 싣는다. 기본은 `false`다. 같은 결과로 이미 만든 링크가 있으면 그 링크(같은 토큰)를 돌려주되, 보낸 `include_quote`를 반영해 한마디를 넣거나 뺀다.
- 서버가 그리는 카드(PNG, 공개 페이지)에는 유형, 점수, 판정, 순위, 상위 %, 도착 예정일, 항목 막대 4개 요약, 배지 1개, 가장 약한 항목이 들어간다. 상대 말은 어떤 경우에도 들어가지 않는다.

#### 공유 카드 구성 안내

앱 안에서 카드를 직접 그릴 때도 같은 구성을 쓴다. 값은 `GET /v1/me/result`에서 읽는다.

| 자리 | 필드 | 비고 |
| --- | --- | --- |
| 유형 이름과 일러스트 자리 | `archetype.name`, `archetype.code` | 일러스트는 `code`별로 준비한다. 희귀도는 `archetype_share_percent` |
| 점수 | `score_hundredths` | 100으로 나눠 소수 둘째 자리 |
| 판정 | `verdict` | |
| 순위 | `rank`, `tied` | `공동 N위` |
| 상위 % | `top_percent` | `null`이면 숨김 |
| 도착 예정일 | `eta` | `Asia/Seoul` 날짜만(예: `12월 26일`). 남이 보는 카드라 시각은 넣지 않는다 |
| 항목 막대 4개 | `dimension_scores` | `label`과 `score` |
| 배지 1개 | `badges[0]` | 없으면 숨김 |
| 가장 약한 항목 | `weakest_dimension.label` | "아쉬운 항목" 같은 가벼운 문구 |
| 한마디(선택) | `quote` | 사용자가 켰을 때만. 기본은 끔 |

- 카드에는 상대 말을 넣지 않는다. 대화 원문은 `quote`(본인 말, 사용자 선택)만 쓴다.

## 8. 오류 응답

오류 응답 본문 형식은 Swagger의 `ApiErrorResponse` 스키마를 따른다. 엔드포인트별로 발생하는 상태 코드와 의미는 Swagger 각 API 설명에 있다. 화면 분기는 HTTP 상태가 아니라 `code`로 한다. `403`(CSRF 토큰 누락·불일치)과 없는 경로 같은 프레임워크 오류에만 `code`가 없다. 예외로 `413`은 프록시가 막으면 `code`가 없으므로 상태만 보고 `FILE_TOO_LARGE`로 다룬다.

| 기획 §5 상황 | 상태 | `code` | 비고 |
| --- | --- | --- | --- |
| 요청 형식 오류(본문 파싱·필드 검증 실패, 필수 헤더 누락, 잘못된 ID 형식) | `400` | `INVALID_REQUEST` | 화면 버그로 보고 입력을 유지한다 |
| 최근 기록·근거 부족(보낼 이벤트가 적음) | `400` | `INSUFFICIENT_EVENTS` | 20개 미만 |
| 한 번에 너무 많은 이벤트 | `400` | `TOO_MANY_EVENTS` | 2,000개 초과 |
| 이벤트 시각·글자 수 범위 밖 | `400` | `INVALID_EVENT_RANGE` | |
| 최근 기록·근거 부족(AI 판정) | - | 상태 조회의 `error_code` `INSUFFICIENT_EVIDENCE` | 작업은 `FAILED`로 끝난다 |
| 파일이 너무 큼 | `413` | `FILE_TOO_LARGE` | 20MB 초과. `code`가 없어도 같은 뜻 |
| 지원하지 않는 파일 형식(빈 파일·zip·바이너리) | `422` | `UNSUPPORTED_FILE_FORMAT` | 업로드 즉시 거절. 파일 다시 고르기 |
| 지원하지 않는 파일 형식(검증 중 발견) | - | 상태 조회의 `error_code` `UNSUPPORTED_FILE_FORMAT` | 인코딩·스키마 오류, 메시지 0개 |
| 본인 이름을 대화에서 못 찾음 | - | 상태 조회의 `error_code` `SPEAKER_NOT_IDENTIFIED` | 본인 다시 고르기 |
| 본인 메시지 부족 | - | 상태 조회의 `error_code` `INSUFFICIENT_EVENTS` | 최근 90일 본인 메시지 20개 미만 |
| AI 일시 오류 | - | 상태 조회의 `error_code` `ANALYSIS_FAILED`, `RETRY_EXHAUSTED` | 같은 작업 상태를 다시 확인한다 |
| 닉네임 형식 오류 | `400` | `INVALID_NICKNAME` | 등록·가용성 확인 공통. 닉네임 입력란에 표시 |
| 닉네임 동시 등록 충돌 | `409` | `NICKNAME_TAKEN` | 닉네임만 수정 |
| 지역 선택 오류 | `400` | `UNSUPPORTED_REGION` | 없는 코드이거나 하위 지역을 골라야 하는 시·도 |
| 요청 한도 초과 | `429` | `DAILY_USER_LIMIT` | 본문 `retry_at`(UTC)과 `Retry-After` 헤더(초)로 다시 가능한 시점을 안내 |
| 같은 네트워크(IP)의 요청이 너무 많음 | `429` | `CLIENT_RATE_LIMITED` | 참가 등록·분석 제출. 본문 `retry_at`과 `Retry-After`로 안내 |
| 요청 본문이 너무 큼 | `413` | `REQUEST_TOO_LARGE` | 파일 업로드가 아닌 본문 2MB 초과. 프록시가 막으면 `code`가 없다 |
| 원본 파일 접수가 몰림 | `503` | `UPLOAD_BUSY` | 같은 `Idempotency-Key`로 `Retry-After` 뒤 다시 올린다 |
| 세션 만료·쿠키 분실 | `401` | `NOT_AUTHENTICATED` | 공개 링크 보기 / 새 참여 안내 |
| 시즌 동결 | `409` | `SEASON_FROZEN` | 분석 요청·지역 변경 모두. 결과 보기 |
| 공유 비활성·존재하지 않음 | `404` | `SHARE_NOT_FOUND` | `Accept`에 `text/html`이 있는 `/s/{token}`·`/s/{token}/image.png` 요청은 JSON 대신 안내 HTML |
| 결과 없음 | `404` | `RESULT_NOT_FOUND` | 점수가 나온 심사가 없음 |
| 공유할 순위 없음 | `409` | `NO_RANKED_RESULT` | `POST /v1/shares`. 첫 순위 계산 전이면 공유 버튼을 숨기거나 잠시 뒤 다시 |
| 분석 작업 없음 | `404` | `NOT_FOUND` | `GET /v1/analyses/{id}`(없거나 남의 작업), `GET /v1/me/analysis`(낸 작업 없음) |
| 세션의 참가자가 지워짐 | `404` | `PARTICIPANT_NOT_FOUND` | 새 참여 안내 |
| 잘못된 Idempotency-Key | `400` | `INVALID_IDEMPOTENCY_KEY` | 키 생성 코드 버그 |
| 같은 키로 다른 요청 | `409` | `IDEMPOTENCY_CONFLICT` | 새 제출에는 새 키를 만든다 |
| 진행 상황 스트림이 꽉 참 | `503` | `ANALYSIS_STREAM_LIMIT` | 폴링으로 바꾼다. `Retry-After` 뒤에 다시 열어 봐도 된다 |
| AI 모델 미설정 | `503` | `AI_NOT_CONFIGURED` | 지금은 분석을 받을 수 없다고 안내 |
| 랭킹 cursor 오류 | `400` | `INVALID_CURSOR` | 받은 `next_cursor`만 보낸다 |
| 페이지 사이에 순위가 다시 계산됨 | `409` | `RANKING_SNAPSHOT_CHANGED` | 목록을 비우고 `cursor` 없이 첫 페이지부터 다시 읽는다 |
| 지역을 너무 자주 바꿈 | `429` | `REGION_CHANGE_LIMITED` | 본문 `retry_at`과 `Retry-After`로 다시 가능한 시점을 안내 |
| 시즌 지역 변경 횟수 소진 | `429` | `REGION_CHANGE_SEASON_LIMIT` | 이번 시즌에는 더 바꿀 수 없다고 안내(`retry_at` 없음) |

## 9. 요청 한도

| 대상 | 한도 | 넘으면 |
| --- | --- | --- |
| 분석 제출(`POST /v1/analyses`·`/files` 합산) | 참가자당 최근 24시간 3회 | `429` `DAILY_USER_LIMIT`. 본문 `retry_at`과 `Retry-After`(초) |
| 참가 등록(`POST /v1/participants`) | IP당 1시간 10회 | `429` `CLIENT_RATE_LIMITED`. 본문 `retry_at`과 `Retry-After`(초) |
| 분석 제출(IP 단위, `POST /v1/analyses`·`/files` 합산) | IP당 24시간 20회 | `429` `CLIENT_RATE_LIMITED`. 본문 `retry_at`과 `Retry-After`(초) |
| 지역 변경(`PATCH /v1/me/region`) | 참가자당 최근 24시간 2회, 시즌 5회(같은 지역은 세지 않음) | `429` `REGION_CHANGE_LIMITED`(본문 `retry_at`과 `Retry-After`) 또는 `REGION_CHANGE_SEASON_LIMIT`(`retry_at` 없음) |
| 원본 파일 | 20MB | `413` `FILE_TOO_LARGE` |
| 원본 파일 동시 접수 | 서버 전체 2개 | `503` `UPLOAD_BUSY`. `Retry-After` 10초 |
| 파일이 아닌 요청 본문 | 2MB | `413` `REQUEST_TOO_LARGE` |
| 이벤트 JSON | 20~2,000개, 이벤트당 1~2,000자, 합계 200,000자 | `400` |
| 진행 상황 스트림 | 참가자당 1개(새로 열면 이전 것이 닫힘), 서버 전체 200개, 연결당 5분 | `503` `ANALYSIS_STREAM_LIMIT`. `Retry-After` 30초 |
| 순위표 | 한 페이지 50줄 | `next_cursor`로 다음 페이지 |

IP 한도는 서버 메모리에서 센다. 같은 키로 다시 보낸 제출(`Idempotency-Key` 재전송)은 한도를 새로 쓰지 않고 처음 작업을 돌려준다. 원본 파일은 한도를 먼저 확인하고 나서 내용을 읽으므로, 한도에 걸린 요청은 파일 검사(`422`)보다 `429`가 먼저 온다.
