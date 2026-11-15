# 프론트엔드 API 연동 가이드

산타컷 백엔드 API를 화면에서 호출할 때 알아야 할 흐름을 정리한 문서다. 요청·응답 필드의 정확한 정의는 Swagger가 기준이고, 이 문서는 호출 순서와 규칙을 설명한다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs` (타입 생성 도구 입력으로 사용)
- 백엔드 실행: `backend/`에서 `./gradlew.bat bootRun` (JDK 25)

## 1. 서비스 흐름

1. 닉네임과 지역(서울 25개 구)을 골라 참가 등록한다.
2. 카카오톡·인스타그램에서 추출한 대화·활동을 정규화한 이벤트 목록으로 분석을 요청한다.
3. 분석 진행 상황과 결과를 실시간(SSE)으로 받는다.
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
| GET | `/v1/analyses/{id}/events` | 필요 | - | 분석 진행·결과 SSE |
| GET | `/v1/me/result` | 필요 | - | 내 최신 점수·순위·도착 시각 |
| GET | `/v1/rankings?offset=0` | 필요 | - | 랭킹 50명씩 |
| POST | `/v1/shares` | 필요 | 필요 | 공유 링크 생성·재사용 |
| DELETE | `/v1/shares/{token}` | 필요 | 필요 | 공유 링크 폐기 (`204`) |
| GET | `/s/{token}` | - | - | 공개 공유 HTML 페이지 |
| GET | `/s/{token}/image.png` | - | - | 공개 공유 이미지(1200×630) |

## 5. 참가 등록

- 닉네임: 앞뒤 공백 제거 후 NFKC 정규화, 한글·영문·숫자·밑줄 2~16자. 대소문자 구분 없이 중복을 막는다(`409`).
- `regionCode`: `GET /v1/regions`가 준 `code` 값만 쓸 수 있다.

## 6. 분석 요청과 SSE

`POST /v1/analyses` 요청 규칙:

- `Idempotency-Key` 헤더 필수: 영문·숫자·`_`·`-` 16~100자. 같은 요청을 다시 보낼 때(네트워크 재시도)만 같은 키를 쓴다. 같은 키에 다른 본문을 보내면 `409`.
- 본문: `source`(`KAKAO` 또는 `INSTAGRAM`), `events` 20~2,000개. 각 이벤트는 `occurredAt`(ISO-8601 UTC)과 `text`(1~2,000자).
- 전체 글자 수 20만 자 이하, 이벤트 시각은 최근 90일 안이고 현재보다 5분 넘게 미래가 아니어야 한다(`400`).
- 한 사람당 24시간에 3번까지(`429`). 12월 25일 0시(한국 시간) 이후에는 시즌 마감(`409`).

응답 `202 { id, status }`의 `id`로 `GET /v1/analyses/{id}/events`를 `EventSource`로 연다.

| 이벤트 이름 | 데이터 | 의미 |
| --- | --- | --- |
| `analysis` | 작업 상태(`status`, `error_code` 등) | 1초마다 현재 상태 |
| `result` | 점수·판정·코멘트·순위·배송 순번·도착 시각 | 성공. 스트림 종료 |
| `error` | 작업 상태(`error_code` 포함) | 실패 또는 근거 부족. 스트림 종료 |

- 상태 값: `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `INSUFFICIENT_DATA`.
- 서버 이름의 `error` 이벤트와 연결 오류(`onerror`)는 브라우저에서 둘 다 발생할 수 있다. 데이터가 있는 `error` 이벤트를 먼저 처리하고, 연결 오류는 작업이 끝나지 않았을 때만 재연결한다.
- 스트림은 최대 150초 열려 있다. 그 안에 끝나지 않으면 다시 연결한다.

## 7. 표시 규칙

- `score_hundredths`는 정수다. 100으로 나눠 소수 둘째 자리까지 표시한다(`8245` → `82.45점`).
- `eta`, `computed_at` 등 시각은 ISO-8601 UTC 문자열이다. 화면에는 `Asia/Seoul`로 바꿔 보여준다.
- 순위는 동점이면 같은 순위를 공유한다. 배송 순번(`delivery_sequence`)은 항상 서로 다르다.
- 랭킹은 `offset`(0~10,000)부터 50명씩 온다.

## 8. 오류 응답

오류 응답 본문 형식은 Swagger의 `ApiErrorResponse` 스키마를 따른다. 엔드포인트별로 발생하는 상태 코드와 의미는 Swagger 각 API 설명에 있다. 공통으로 `401`은 세션 없음·만료, `403`은 CSRF 토큰 누락·불일치다.
