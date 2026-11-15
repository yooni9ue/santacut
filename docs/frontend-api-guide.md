# 프론트엔드 API 연동 가이드

산타컷 백엔드 API를 화면에서 호출할 때 알아야 할 흐름을 정리한 문서다. 요청·응답 필드의 정확한 정의는 Swagger가 기준이고, 이 문서는 호출 순서와 규칙을 설명한다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs` (타입 생성 도구 입력으로 사용)
- 백엔드 실행: `backend/`에서 `./gradlew.bat bootRun` (JDK 25)

## 1. 서비스 흐름

1. 닉네임과 지역(서울 25개 구)을 골라 참가 등록한다.
2. 카카오톡·인스타그램에서 추출한 대화·활동을 정규화한 이벤트 목록으로 분석을 요청한다.
3. 분석 진행 상황을 폴링으로 확인하고, 끝나면 결과를 조회한다.
4. 내 점수·순위·선물 도착 예정 시각과 전체 랭킹을 본다.
5. 결과 공유 링크를 만들거나 폐기한다.

점수와 도착 시각은 재미를 위한 시뮬레이션이다. 원본 내보내기 파일(카카오톡 txt, 인스타그램 JSON)을 직접 올리는 API는 아직 없고, 화면에서 이벤트 목록으로 바꿔 보내야 한다.

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
2. 쓰기 요청(`POST`, `DELETE`) 직전에 `GET /v1/csrf`를 호출한다. 응답 `{ headerName, token }`의 `token`을 `headerName`(기본 `X-XSRF-TOKEN`) 헤더로 보낸다. 같은 응답이 `XSRF-TOKEN` 쿠키도 설정하므로 쿠키를 지우지 않는다.
3. `POST /v1/participants`로 등록하면 세션이 HttpOnly 쿠키(`sc_session`, 운영은 `__Host-sc_session`)로 설정된다. 세션은 30일간 유효하며 JSON 응답에는 세션 값이 없다.
4. 이후 요청은 쿠키가 자동으로 실려 인증된다. 세션이 없거나 만료되면 `401`이다.

## 4. API 목록

| 메서드 | 경로 | 인증 | CSRF | 설명 |
| --- | --- | --- | --- | --- |
| GET | `/v1/csrf` | - | - | CSRF 토큰 발급 |
| GET | `/v1/regions` | - | - | 등록 가능한 지역 목록 |
| POST | `/v1/participants` | - | 필요 | 참가 등록 (`201`) |
| GET | `/v1/me` | 필요 | - | 내 참가 정보 |
| POST | `/v1/analyses` | 필요 | 필요 | 분석 요청 (`202`) |
| GET | `/v1/analyses/{id}` | 필요 | - | 분석 작업 진행 상태 |
| GET | `/v1/me/analysis` | 필요 | - | 내 최근 분석 작업 상태 |
| GET | `/v1/me/result` | 필요 | - | 내 최신 점수·순위·도착 시각 |
| GET | `/v1/rankings?offset=0` | 필요 | - | 랭킹 50명씩 |
| POST | `/v1/shares` | 필요 | 필요 | 공유 링크 생성·재사용 |
| DELETE | `/v1/shares/{token}` | 필요 | 필요 | 공유 링크 폐기 (`204`) |
| GET | `/s/{token}` | - | - | 공개 공유 HTML 페이지 |
| GET | `/s/{token}/image.png` | - | - | 공개 공유 이미지(1200×630) |

## 5. 참가 등록

- 닉네임: 앞뒤 공백 제거 후 NFKC 정규화, 한글·영문·숫자·밑줄 2~16자. 대소문자 구분 없이 중복을 막는다(`409`).
- `regionCode`: `GET /v1/regions`가 준 `code` 값만 쓸 수 있다.

## 6. 분석 요청과 상태 확인

`POST /v1/analyses` 요청 규칙:

- `Idempotency-Key` 헤더 필수: 영문·숫자·`_`·`-` 16~100자. 같은 요청을 다시 보낼 때(네트워크 재시도)만 같은 키를 쓴다. 같은 키에 다른 본문을 보내면 `409`.
- 본문: `source`(`KAKAO` 또는 `INSTAGRAM`), `events` 20~2,000개. 각 이벤트는 `occurredAt`(ISO-8601 UTC)과 `text`(1~2,000자).
- 전체 글자 수 20만 자 이하, 이벤트 시각은 최근 90일 안이고 현재보다 5분 넘게 미래가 아니어야 한다(`400`).
- 한 사람당 24시간에 3번까지(`429`). 12월 25일 0시(한국 시간) 이후에는 시즌 마감(`409`).

응답 `202 { id, status }`의 `id`로 `GET /v1/analyses/{id}`를 몇 초(예: 3초) 간격으로 다시 조회한다. 응답의 `status`가 진행 단계다. 202 응답의 `status`도 같은 값을 쓴다.

| `status` | 화면 문구(기획 §4.7) | 다음 행동 |
| --- | --- | --- |
| `QUEUED` | 산타 심사를 기다리고 있어요 | 계속 조회 |
| `ANALYZING` | 산타가 기록을 읽고 있어요 | 계속 조회 |
| `RANKING_PENDING` | 심사가 끝났어요. 순위와 도착일을 계산하고 있어요 | 계속 조회 |
| `SUCCEEDED` | - | 조회를 멈추고 `GET /v1/me/result`로 이동 |
| `FAILED` | `error_code`로 구분 | 조회를 멈추고 실패 안내 |
| `VALIDATING` | 파일 형식을 확인하고 있어요 | 원본 파일 검사용 값. 지금 서버는 보내지 않는다 |

- `FAILED`의 `error_code`: `INSUFFICIENT_EVIDENCE`(근거 부족), `EXPIRED`(24시간 대기 만료), `RETRY_EXHAUSTED`·`ANALYSIS_FAILED`(AI 오류), `SEASON_FROZEN`(시즌 마감).
- 점수·순위는 상태 응답에 없다. 결과는 `GET /v1/me/result` 하나로 읽는다.
- 창을 닫아도 심사는 계속된다. 재방문하면 `GET /v1/me/analysis`로 가장 최근 작업 상태를 읽는다. 진행 중이면 심사 화면, `SUCCEEDED`면 결과 화면으로 보낸다. 낸 작업이 없으면 `404`(`NOT_FOUND`)다.
- 오래 걸려도 새 분석을 자동으로 만들지 않는다. 같은 `id`를 다시 조회한다.

## 7. 표시 규칙

- `score_hundredths`는 정수다. 100으로 나눠 소수 둘째 자리까지 표시한다(`8245` → `82.45점`).
- `eta`, `computed_at` 등 시각은 ISO-8601 UTC 문자열이다. 화면에는 `Asia/Seoul`로 바꿔 보여준다.
- 순위는 동점이면 같은 순위를 공유한다. 배송 순번(`delivery_sequence`)은 항상 서로 다르다.
- 랭킹은 `offset`(0~10,000)부터 50명씩 온다.

## 8. 오류 응답

오류 응답 본문 형식은 Swagger의 `ApiErrorResponse` 스키마를 따른다. 엔드포인트별로 발생하는 상태 코드와 의미는 Swagger 각 API 설명에 있다. 공통으로 `401`은 세션 없음·만료, `403`은 CSRF 토큰 누락·불일치다.
