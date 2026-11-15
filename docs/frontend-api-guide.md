# 프론트엔드 API 연동 가이드

산타컷 백엔드 API를 화면에서 호출할 때 알아야 할 흐름을 정리한 문서다. 요청·응답 필드의 정확한 정의는 Swagger가 기준이고, 이 문서는 호출 순서와 규칙을 설명한다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs` (타입 생성 도구 입력으로 사용)
- 백엔드 실행: `backend/`에서 `./gradlew.bat bootRun` (JDK 25)

## 1. 서비스 흐름

1. 닉네임과 지역(시·도 → 시·군·구)을 골라 참가 등록한다. 지역은 전국 시·도 16개와 그 아래 시·군·구 255개다.
2. 카카오톡·인스타그램 내보내기 원본 파일을 올리거나, 정규화한 이벤트 목록을 보내 분석을 요청한다.
3. 분석 진행 상황을 폴링으로 확인하고, 끝나면 결과를 조회한다.
4. 내 점수·순위·선물 도착 예정 시각과 전체 랭킹을 본다.
5. 결과 공유 링크를 만들거나 폐기한다.

점수와 도착 시각은 재미를 위한 시뮬레이션이다. 원본 내보내기 파일(카카오톡 TXT, 인스타그램 `message_1.json`)은 `POST /v1/analyses/files`로 그대로 올린다(§6.1).

## 2. 같은 출처로 호출하기

세션과 CSRF 토큰이 쿠키로 오가므로 화면과 API는 같은 출처(origin)여야 한다. 로컬 개발 서버에서는 아래 경로를 `http://localhost:8080`으로 프록시한다.

| 경로 | 용도 |
| --- | --- |
| `/v1/**` | API |
| `/s/**` | 공개 공유 페이지와 이미지 |
| `/swagger-ui/**`, `/swagger-ui.html`, `/v3/**` | API 문서 |

`fetch`에는 `credentials: 'include'`(같은 출처면 기본값 `same-origin`도 가능)를 쓴다. 별도 API 도메인과 CORS는 지원하지 않는다.

## 3. 인증과 CSRF

1. 페이지 로드 시 `GET /v1/me`를 호출한다. `200`이면 로그인 상태, `401`이면 미등록 상태다.
2. 쓰기 요청(`POST`, `PATCH`, `DELETE`) 직전에 `GET /v1/csrf`를 호출한다. 응답 `{ headerName, token }`의 `token`을 `headerName`(기본 `X-XSRF-TOKEN`) 헤더로 보낸다. 같은 응답이 `XSRF-TOKEN` 쿠키도 설정하므로 쿠키를 지우지 않는다.
3. `POST /v1/participants`로 등록하면 세션이 HttpOnly 쿠키(`sc_session`, 운영은 `__Host-sc_session`)로 설정된다. 세션은 30일간 유효하며 JSON 응답에는 세션 값이 없다.
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
| GET | `/v1/me/analysis` | 필요 | - | 내 최근 분석 작업 상태 |
| GET | `/v1/me/result` | 필요 | - | 내 최신 점수·순위·도착 시각·확정 상태 |
| GET | `/v1/rankings` | - | - | 랭킹 50명씩(cursor 페이지네이션) |
| POST | `/v1/shares` | 필요 | 필요 | 공유 링크 생성·재사용 |
| DELETE | `/v1/shares/{token}` | 필요 | 필요 | 공유 링크 폐기 (`204`) |
| GET | `/s/{token}` | - | - | 공개 공유 HTML 페이지 |
| GET | `/s/{token}/image.png` | - | - | 공개 공유 이미지(1200×630) |

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
- 성공하면 `200`과 `GET /v1/me`와 같은 참가 정보를 준다. 서버가 같은 요청 안에서 순위와 도착 예정 시각을 다시 계산한다.
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

응답 `202 { id, status }`의 `id`로 `GET /v1/analyses/{id}`를 몇 초(예: 3초) 간격으로 다시 조회한다. 응답의 `status`가 진행 단계다. 202 응답의 `status`도 같은 값을 쓴다.

| `status` | 화면 문구(기획 §4.7) | 다음 행동 |
| --- | --- | --- |
| `VALIDATING` | 파일 형식을 확인하고 있어요 | 계속 조회. 원본 파일 제출에서만 나온다 |
| `QUEUED` | 산타 심사를 기다리고 있어요 | 계속 조회 |
| `ANALYZING` | 산타가 기록을 읽고 있어요 | 계속 조회 |
| `RANKING_PENDING` | 심사가 끝났어요. 순위와 도착일을 계산하고 있어요 | 계속 조회 |
| `SUCCEEDED` | - | 조회를 멈추고 `GET /v1/me/result`로 이동 |
| `FAILED` | `error_code`로 구분 | 조회를 멈추고 실패 안내 |

- `FAILED`의 `error_code`: `INSUFFICIENT_EVIDENCE`(근거 부족), `EXPIRED`(24시간 대기 만료), `RETRY_EXHAUSTED`·`ANALYSIS_FAILED`(AI 오류), `SEASON_FROZEN`(시즌 마감).
- 원본 파일 제출에서만 나오는 `error_code`: `UNSUPPORTED_FILE_FORMAT`(형식 오류), `SPEAKER_NOT_IDENTIFIED`(본인 이름 없음), `INSUFFICIENT_EVENTS`(본인 메시지 20개 미만). `RETRY_EXHAUSTED`는 파일 검증을 세 번 넘게 시작했을 때도 나온다.
- 점수·순위는 상태 응답에 없다. 결과는 `GET /v1/me/result` 하나로 읽는다.
- 창을 닫아도 심사는 계속된다. 재방문하면 `GET /v1/me/analysis`로 가장 최근 작업 상태를 읽는다. 진행 중이면 심사 화면, `SUCCEEDED`면 결과 화면으로 보낸다. 낸 작업이 없으면 `404`(`NOT_FOUND`)다.
- 오래 걸려도 새 분석을 자동으로 만들지 않는다. 같은 `id`를 다시 조회한다.

### 6.1 원본 파일 업로드 `POST /v1/analyses/files`

지원 형식은 카카오톡 대화 TXT(PC·iOS·Android 내보내기)와 인스타그램 `messages/inbox/<대화방>/message_1.json`이다. zip, CSV, EUC-KR 파일은 받지 않는다. 인스타그램은 zip을 풀어 `message_1.json` 하나만 고른다.

흐름:

1. 파일을 고르면 브라우저에서 읽어 화자 목록을 뽑는다(아래 정규식). 등록 전에는 파일을 서버로 보내지 않는다(기획 §2·§4.4).
2. 화자 목록에서 본인을 고르게 한다. 카카오톡과 인스타그램 모두 고른다. 인스타그램 파일에는 소유자 표시가 없다.
3. 참가 등록과 동의를 마친 뒤 multipart로 올린다. 파트는 `file`(필수), `source`(`KAKAO`·`INSTAGRAM`), `self_speaker`(1~100자)다. 헤더는 `Idempotency-Key`와 `X-XSRF-TOKEN`이다.
4. 응답은 `202 { id, status: "VALIDATING" }`이다. 이후 폴링은 §6과 같다. 검증을 통과하면 `QUEUED`로 바뀐다.

규칙:

- 서버는 본인 메시지만 분석한다. 다른 참여자의 메시지는 버리고 AI에 보내지 않는다.
- 최근 90일 안의 메시지 중 최신 2,000개, 누적 200,000자까지만 쓴다. 메시지 하나는 2,000자에서 자른다.
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
- 순위는 동점이면 같은 순위를 공유한다. `tied`가 `true`면 `공동 N위`로 표시한다. 배송 순번(`delivery_sequence`)은 항상 서로 다르다.
- `stage`는 `PREVIEW`(시즌 마감 전 미리보기)나 `FINAL`(시즌 확정)이다. 계산 시각과 함께 보여준다.

### 내 결과 `GET /v1/me/result`

- 점수와 순위는 늘 같은 심사 것이다. 서로 다른 심사의 점수와 순위를 섞지 않는다.
- `ranking_pending`이 `true`면 새 심사가 아직 순위에 반영되지 않았다. 이전 스냅숏이 있으면 그 점수·순위를 그대로 보여주고 반영 중임을 따로 알린다.
- 첫 순위 계산 전이면 `rank`, `delivery_sequence`, `eta`, `computed_at`이 `null`이다. 점수·판정을 먼저 보여주고 순위·도착일은 `계산 중`으로 둔다.
- 점수가 나온 심사가 없으면 `404`(`RESULT_NOT_FOUND`)다.

### 랭킹 `GET /v1/rankings`

- 로그인 없이 볼 수 있다. 내 순위 카드는 `GET /v1/me/result`로 따로 읽는다.
- 응답: `{ season_year, participant_count, computed_at, stage, rows[], next_cursor }`. 각 행은 `nickname`, `rank`, `tied`, `score_hundredths`, `eta`다.
- 한 번에 50줄이 온다. `더 보기`는 `next_cursor`를 `cursor` 쿼리로 보낸다. `next_cursor`가 `null`이면 마지막 페이지다.
- `cursor`는 불투명 문자열이다. 직접 만들지 말고 받은 값만 보낸다. 잘못된 값은 `400`(`INVALID_CURSOR`)이다.
- 아직 순위가 없으면 `participant_count`는 `0`, `computed_at`은 `null`, `rows`는 빈 배열이다(빈 상태 화면).

### 공유 `POST /v1/shares`, `/s/{token}`

- 공유 링크는 만든 순간의 결과를 고정한다. 순위, 공동 순위, 계산 시각, 상태(`미리보기`·`시즌 확정`), 공유 시각을 그때 값으로 담는다.
- 카드 이미지와 공개 페이지는 공동 순위를 `공동 N위`로, 계산 시각과 공유 시각을 한국 시간으로 보여준다.
- 공개 페이지에는 `공유 당시 결과`와 공유 시각이 나온다. 주요 버튼은 `나도 산타컷 받아보기`다.
- 링크가 없거나 취소됐을 때 `Accept`에 `text/html`이 있는 요청은 `404`와 "더 이상 볼 수 없는 결과" HTML을 받는다. 시작 링크(`내 산타 점수 알아보기`)가 있다. 공개 페이지(`/s/{token}`)와 이미지(`/s/{token}/image.png`) 모두 같다. 브라우저 주소창에서 이미지 주소를 열 때도 `Accept`에 `text/html`이 들어가므로 이 HTML이 온다.
- 같은 경우 그 밖의 요청(`fetch`, `<img>` 등 `Accept`에 `text/html`이 없는 요청)은 지금처럼 `404` JSON(`SHARE_NOT_FOUND`)이다.
- 링크를 비활성화해도 이미 저장·전달된 이미지는 회수할 수 없다.

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
| 세션 만료·쿠키 분실 | `401` | `NOT_AUTHENTICATED` | 공개 링크 보기 / 새 참여 안내 |
| 시즌 동결 | `409` | `SEASON_FROZEN` | 분석 요청·지역 변경 모두. 결과 보기 |
| 공유 비활성·존재하지 않음 | `404` | `SHARE_NOT_FOUND` | `Accept`에 `text/html`이 있는 `/s/{token}`·`/s/{token}/image.png` 요청은 JSON 대신 안내 HTML |
| 결과 없음 | `404` | `RESULT_NOT_FOUND` | 점수가 나온 심사가 없음 |
| 공유할 순위 없음 | `409` | `NO_RANKED_RESULT` | `POST /v1/shares`. 첫 순위 계산 전이면 공유 버튼을 숨기거나 잠시 뒤 다시 |
| 분석 작업 없음 | `404` | `NOT_FOUND` | `GET /v1/analyses/{id}`(없거나 남의 작업), `GET /v1/me/analysis`(낸 작업 없음) |
| 세션의 참가자가 지워짐 | `404` | `PARTICIPANT_NOT_FOUND` | 새 참여 안내 |
| 잘못된 Idempotency-Key | `400` | `INVALID_IDEMPOTENCY_KEY` | 키 생성 코드 버그 |
| 같은 키로 다른 요청 | `409` | `IDEMPOTENCY_CONFLICT` | 새 제출에는 새 키를 만든다 |
| AI 모델 미설정 | `503` | `AI_NOT_CONFIGURED` | 지금은 분석을 받을 수 없다고 안내 |
| 랭킹 cursor 오류 | `400` | `INVALID_CURSOR` | 받은 `next_cursor`만 보낸다 |
