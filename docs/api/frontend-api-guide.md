# 프론트엔드 API 연동 가이드

산타컷 백엔드 API를 화면에서 부를 때 알아야 할 흐름을 정리한 문서다. 요청·응답 필드의 정확한 정의는 OpenAPI(Swagger)가 기준이고, 이 문서는 호출 순서와 규칙을 설명한다. 이 문서의 엔드포인트와 필드는 2026-11-17 dev 서버의 `/v3/api-docs`와 맞췄다.

## 프론트 반영 체크리스트 (백엔드 계약 요구사항)

백엔드 계약을 화면에 적용할 때의 요구사항이다. 아래 체크박스는 통합 검증 완료 표시가 아니다. 2026-11-18 현재 실제 연결·미반영 항목은 [화면별 구현 범위](../../frontend/docs/screens.md), [전체 점검 기록](../development/implementation-audit.md)에 구분한다.

- [ ] **[긴급] 심사 완료 뒤 결과 화면 이동**: 지금 분석 화면(`LiveAnalysisScreen`, `frontend/src/components/flow/screens.tsx`)은 상태가 `SUCCEEDED`가 되어도 "심사가 끝났어요"만 보여 주고 `/result`로 가지 않는다. 링크나 버튼도 없어서 사용자는 결과를 볼 수 없다(2026-11-20 dev에서 확인, 서버 채점과 SSE는 정상). `SUCCEEDED`를 받으면 `/result`로 이동(`router.replace`)하고, 이동이 안 될 때를 위해 "결과 보기" 버튼도 둔다. `RANKING_PENDING` 동안은 대기 화면을 유지한다(6.0절).
- [ ] **API 주소**: Vercel Preview·Development에 `https://api-dev.santacut.me`, Production은 운영 서버를 켠 뒤 `https://api.santacut.me`(0절).
- [ ] **도메인 연결**: Vercel Settings → Domains에 `santacut.me`, `www.santacut.me`(main), `dev.santacut.me`(develop)를 추가한다. DNS는 백엔드가 Route 53에 걸어 두었다.
- [ ] **쿠키·CSRF**: 모든 `fetch`에 `credentials: 'include'`, SSE는 `{ withCredentials: true }`. 쓰기 요청 전 `GET /v1/csrf` 본문의 토큰을 `X-XSRF-TOKEN` 헤더로 보낸다. 쿠키는 다른 출처라 읽을 수 없다(2·3절).
- [ ] **분석 제출**: `Idempotency-Key` 헤더 필수. 업로드가 몰리면 `503 UPLOAD_BUSY`(`Retry-After` 10초)가 오니 같은 키로 잠시 뒤 다시 보낸다. 같은 IP에서 너무 많이 내면 `429 CLIENT_RATE_LIMITED`(`retry_at`, `Retry-After`)다(8·9절).
- [ ] **동의 문구**: 상대 메시지를 포함한 올해 대화가 Google Gemini로 전송되고 채점 뒤 삭제된다는 고지를 업로드 동의 화면에 넣는다(6.1절, 법률 검토 필요).
- [ ] **분석 범위 표기**: 업로드 화면과 동의 문구의 분석 범위를 "최근 90일"에서 "올해 1월 1일부터"로 바꾼다. 상한 표기도 "최대 2,000개"에서 "본인 메시지 2,000개(상대 포함 6,000개)"로 함께 고친다. 이벤트 JSON 제출은 그대로 2,000개까지다(6·6.1절).
- [ ] **내 결과 화면** `GET /v1/me/result`: 새 필드 `archetype`, `dimension_scores`, `weakest_dimension`, `top_percent`(100명 미만이면 null), `archetype_share_percent`(30명 미만이면 null), `badges`, `quote`(null 가능), `low_confidence_dimensions`(근거 부족 항목에 "근거 적음" 표시). 정확한 도착 시각 `eta`는 본인 결과에만 있다(7절).
- [ ] **산타 조서 화면** `GET /v1/me/dossier`(2026-11-20 추가): 결과 화면을 `scenes` 배열 순서대로 한 장씩 넘기는 세로 스냅 스크롤로 그린다. 장면마다 저장(공유 카드) 버튼을 두되 `shareable`이 true인 장면에만 둔다. 카드에는 `shareable`이 true인 줄만 싣는다(삐 처리·하위 판정·느린 답장 줄은 화면에만). `404 DOSSIER_NOT_FOUND`(또는 결과의 `dossier_available`이 false)면 조서 대신, 시즌 마감 전(`stage`가 마감 전)이면 "대화를 다시 올리면 새 조서를 받는다"는 재업로드 안내를, 마감 뒤에는 "이 결과에는 조서가 없다"는 안내만 띄운다(마감 뒤에는 재업로드할 수 없다). `tone` 값은 화면 문구에만 쓰고 분석 이벤트·로그·에러 리포트에 싣지 않는다. 기존 점수·유형 화면은 "자세히 보기"로 접어 남긴다(7절 산타 조서).
- [ ] **팩폭 산타 리캡 카드 덱** `GET /v1/me/recap`(2026-11-20 추가): 업로드 → 작업 완료(`SUCCEEDED`) → `GET /v1/me/result`의 `recap_available` 확인 → `GET /v1/me/recap` → 덱 → 공유 순서로 붙인다. `cards`를 받은 순서대로 9:16(1080×1920) 카드 한 장씩 그리고, 모든 카드 상단에 `score_text`를 크게 찍는다. 문구·칩·숫자는 말줄임이나 접기 없이 다 보이게 두고, 가리기 토글은 두지 않는다. 공유 문구는 `share_text`를 그대로 쓴다. 숫자가 든 문장은 화면에서 만들지 않는다. 석탄 농담을 끄면 `stamp_mild`·`verdict_mild`로 바꾼다. `404 RECAP_NOT_FOUND`(또는 `recap_available: false`)면 기존 결과 화면과 시즌 마감 전 재업로드 안내를 보여 준다. 필드 표와 실제 응답 예시는 [7절 리캡 카드 덱](#리캡-카드-덱-get-v1merecap)에 있다.
- [ ] **랭킹 화면** `GET /v1/rankings`: 행의 도착 정보는 `eta_date`(서울 날짜)를 쓴다. `eta`는 항상 null이다. 페이지 사이에 순위가 다시 계산되면 `409 RANKING_SNAPSHOT_CHANGED`가 오니 목록을 비우고 첫 페이지부터 다시 읽는다. 예전 숫자 cursor는 `400`이다(7절).
- [ ] **리캡 문구 v3**(2026-11-20 바뀜): 등급 이름·도장·공유 문구가 등급별 6종으로 바뀌고 최상위에 `share_text_mild`가 생겼다. `flip`은 이제 `"근데 "`로 시작하니 화면에서 "근데" 라벨을 붙이지 않는다. 칩 이름은 `chip.label`을 그대로 쓰고, 화면 고정 문구(eyebrow·제목·영역 표시명·버튼)와 석탄 스킨은 "화면 고정 문구"와 "석탄 스킨" 절을 따른다. 유형 이름 두 개(`WORKSHOP_ELF`, `RUDOLPH_DRIVER`)도 바뀌었다.
- [ ] **순위 반영 대기와 리캡 먼저 보여 주기**(2026-11-20 바뀜): 서버가 작업 성공 직후 순위를 다시 계산하므로 `RANKING_PENDING`은 보통 1~2초, 작업이 몰리면 최대 약 6초 만에 `SUCCEEDED`로 바뀐다(전에는 0~30초). SSE는 `RANKING_PENDING` 다음 `SUCCEEDED` 순서로 온다. `RANKING_PENDING`을 받으면 대기 화면에 머물지 말고 바로 `GET /v1/me/recap`으로 덱을 그리기 시작한다. 순위·도착 시각 자리는 "순위 계산 중" 라벨로 두고, 같은 SSE에서 `SUCCEEDED`가 오면 덱을 한 번 더 불러 채운다(6.0절, 7절 리캡 카드 덱의 전체 흐름). 진행 상황은 SSE로 받기를 권한다.
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
3. 분석 진행 상황은 SSE로 받고, 끊기면 같은 작업의 SSE를 수동 재연결한다. 상태 GET은 초기 확인·복구용으로 제공하며 현재 프론트는 주기적으로 폴링하지 않는다.
4. 내 점수·순위·선물 도착 예정 시각, 산타 유형·배지·항목 막대와 전체 랭킹을 본다. 2026-11-20부터는 같은 결과를 「팩폭 산타」 리캡 카드 덱(`GET /v1/me/recap`)과 산타 조서(`GET /v1/me/dossier`)로도 본다.
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
| GET | `/v1/me/dossier` | 필요 | - | 내 결과의 「2026 산타 조서」 장면 목록 |
| GET | `/v1/me/recap` | 필요 | - | 내 최신 결과의 「팩폭 산타」 리캡 카드 덱(9:16 카드 목록) |
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
- 성공하면 `200`과 `GET /v1/me`와 같은 참가 정보를 준다. 순위와 도착 예정 시각은 서버가 몇 초 안에 다시 계산한다. 그동안 내 결과의 `eta`는 이전 지역 기준이다.
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
- 전체 글자 수 20만 자 이하, 이벤트 시각은 올해 1월 1일 0시(한국 시각) 이후이고 현재보다 5분 넘게 미래가 아니어야 한다(`400`, `INVALID_EVENT_RANGE`).
- 한 사람당 24시간에 3번까지(`429`, `DAILY_USER_LIMIT`). 응답의 `retry_at`과 `Retry-After` 헤더가 다시 낼 수 있는 시점이다. 12월 25일 0시(한국 시간) 이후에는 시즌 마감(`409`).

응답 `202 { id, status }`의 `id`로 진행 단계를 따라간다. 초기 상태 GET 이후 SSE(§6.0)로 변경을 받는다. 끊기면 연결을 닫고 재연결 버튼을 제공한다. 새 분석 제출이나 주기적인 GET 폴링으로 전환하지 않는다. 202 응답의 `status`도 상태 조회와 같은 값을 쓴다.

| `status` | 화면 문구(기획 §4.7) | 다음 행동 |
| --- | --- | --- |
| `VALIDATING` | 파일 형식을 확인하고 있어요 | 계속 조회. 원본 파일 제출에서만 나온다 |
| `QUEUED` | 산타 심사를 기다리고 있어요 | 계속 조회 |
| `ANALYZING` | 산타가 기록을 읽고 있어요 | 계속 조회 |
| `RANKING_PENDING` | 심사가 끝났어요. 순위와 도착일을 계산하고 있어요 | SSE를 그대로 열어 두고 리캡 덱(`GET /v1/me/recap`)을 먼저 보여 준다. 서버가 작업 성공 직후 순위를 다시 계산하므로 보통 1~2초(몰리면 최대 약 6초) 안에 `SUCCEEDED`가 온다 |
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

현재 프론트 정책은 SSE 단독 구독이다. GET 상태 API가 남아 있는 것은 초기 확인·복구를 위한 것으로 폴링 fallback을 뜻하지 않는다. 구현 기준은 `frontend/src/lib/analyses.ts`의 `watchAnalysis`와 `screens.tsx`의 `LiveAnalysisScreen`이다.

- 진행 상황은 SSE로 받기를 권한다. 상태가 바뀌는 순간 서버가 보내므로, 순위 반영(`RANKING_PENDING` → `SUCCEEDED`)처럼 1~2초 만에 끝나는 단계도 놓치지 않는다.
- 연결하자마자 현재 단계를 한 번 받고, 단계가 바뀔 때마다 하나씩 받는다.
- 이벤트 이름은 `status`, `data`는 `GET /v1/analyses/{id}`와 같은 JSON이다.
- `id`는 진행 단계 순번이다(`VALIDATING` 0, `QUEUED` 1, `ANALYZING` 2, `RANKING_PENDING` 3, `SUCCEEDED` 4, `FAILED` 5). AI 제공자가 잠시 막아 대기열로 돌아가면 작아질 수 있다.
- `SUCCEEDED`나 `FAILED`를 보낸 뒤 서버가 닫는다. 이미 끝난 작업에 연결하면 끝난 단계를 받고 바로 닫힌다.
- 15초마다 주석 줄(`: heartbeat`)이 온다. `EventSource`는 이 줄을 이벤트로 넘기지 않는다.
- 5분이 지나면 서버가 닫는다. 기본 EventSource에는 자동 재연결 기능이 있지만 현재 프론트는 오류 시 `close()`하고 수동 재연결 안내를 한다. 새 연결에서도 서버가 현재 상태를 보내므로 과거 이벤트 재생에 의존하지 않는다.
- 인증과 소유 규칙은 상태 조회와 같다. 세션이 없으면 `401`, 다른 참가자의 작업이면 `404`다.
- 참가자 한 명은 스트림을 하나만 쓴다. 새로 열면 이전 스트림이 닫힌다(탭을 여러 개 열면 마지막 탭만 받는다).
- 서버 전체 스트림 수가 상한에 닿으면 `503`(`ANALYSIS_STREAM_LIMIT`)과 `Retry-After`가 온다. 잠시 뒤 같은 SSE를 다시 연다.

`EventSource`는 응답 코드와 본문을 알려 주지 않는다. 연결 실패 사유를 확정하지 말고 연결 종료 안내와 재연결 버튼을 보여준다. 아래는 호출 형태이며 검증·중복 종료 방지는 실제 유틸에서 수행한다.

```ts
function watchAnalysis(id: string, onStatus: (status: AnalysisStatus) => void, onDisconnect: () => void) {
  const source = new EventSource(`/v1/analyses/${id}/events`, { withCredentials: true });

  source.addEventListener("status", (event) => {
    const status = JSON.parse((event as MessageEvent).data) as AnalysisStatus;
    onStatus(status);
    if (status.status === "SUCCEEDED" || status.status === "FAILED") source.close();
  });
  source.addEventListener("error", () => {
    source.close();
    onDisconnect(); // UI의 재연결 버튼에서 같은 작업을 다시 구독한다.
  });
  return () => source.close();
}
```

- 예시는 §2의 같은 출처 프록시를 쓴다고 보고 상대 경로로 연다. API 주소로 바로 열 때는 앞에 API 주소를 붙이고, 그때는 `withCredentials: true`가 있어야 세션 쿠키가 함께 간다.
- 화면을 떠나면 `close()`로 닫는다. 동시에 여러 스트림을 열지 않는다.

### 6.1 원본 파일 업로드 `POST /v1/analyses/files`

지원 형식은 카카오톡 대화 TXT(PC·iOS·Android 내보내기)와 인스타그램 `messages/inbox/<대화방>/message_1.json`이다. zip, CSV, EUC-KR 파일은 받지 않는다. 인스타그램은 zip을 풀어 `message_1.json` 하나만 고른다.

흐름:

1. 파일을 고르면 브라우저에서 읽어 화자 목록을 뽑는다(아래 정규식). 등록 전에는 파일을 서버로 보내지 않는다(기획 §2·§4.4).
2. 화자 목록에서 본인을 고르게 한다. 카카오톡과 인스타그램 모두 고른다. 인스타그램 파일에는 소유자 표시가 없다.
3. 참가 등록과 동의를 마친 뒤 multipart로 올린다. 파트는 `file`(필수), `source`(`KAKAO`·`INSTAGRAM`), `self_speaker`(1~100자)다. 헤더는 `Idempotency-Key`와 `X-XSRF-TOKEN`이다.
4. 응답은 `202 { id, status: "VALIDATING" }`이다. 이후 상태 확인은 §6의 SSE 흐름을 따른다. 검증을 통과하면 `QUEUED`로 바뀐다. 같은 키 재전송은 기존 작업의 현재 상태를 반환할 수 있다.

규칙:

- 서버는 올해 대화를 달마다 고르게 골라 채점에 쓴다. 점수는 본인 행동만 매기지만, 본인 말의 뜻을 읽으려고 상대(다른 참여자) 메시지도 함께 Google Gemini에 보낸다. 참여자 이름은 `나`·`상대`(단체 대화는 `상대1`, `상대2`)로 바꿔 보낸다. 원본 파일 제출은 팩폭 문장에서 이름을 가리려고 참여자 이름을 작업이 끝날 때까지만 서버에 두고 지운다. 이름은 AI에 보내지 않는다.
- 상대 메시지를 포함한 대화 내용은 채점 작업에만 쓰고 작업이 끝나면(성공·실패 모두) 지운다.
- 동의 화면 문구 제안(출시 전 법률 검토 필요): "고른 대화방의 올해(1월 1일부터) 대화(내 메시지와 상대방 메시지)를 점수 산정을 위해 Google Gemini로 보냅니다. 상대방 이름은 보내지 않고 '상대'로 바꿉니다. 점수는 내 말만 평가하며, 대화 내용은 채점이 끝나면 지웁니다. 상대방 메시지가 포함되므로 상대방이 원하지 않을 만한 대화는 올리지 마세요."
- 올해 1월 1일 0시(한국 시각)부터의 메시지 중 본인 메시지 2,000개, 상대 포함 6,000개, 누적 200,000자까지만 쓴다. 넘으면 오류 없이 달마다 고르게 골라 줄인다. 카카오톡 TXT가 100만 줄을, 인스타그램 JSON이 객체 100만 개를 넘으면 `UNSUPPORTED_FILE_FORMAT`이다. 메시지 하나는 2,000자에서 자른다.
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
- `recap_available`(2026-11-20 추가): 최신 결과에 리캡 카드 덱(`GET /v1/me/recap`)이 있으면 `true`다. `ranking_pending`이 true여도 최신 평가 기준이다. 이 기능 전에 받은 결과면 `false`이고, 이때는 리캡 대신 이 결과 화면을 보여 주고 시즌 마감 전이면 재업로드 안내를 둔다(7절 리캡 카드 덱의 전체 흐름).
- `dossier_available`(2026-11-20 추가): 최신 결과에 산타 조서가 있으면 `true`다. `ranking_pending`이 true(재업로드 직후 순위 계산 전)여도 최신 평가 기준이다. 이 기능 전에 받은 결과면 `false`이고, 조서 대신 시즌 마감 전에만 재업로드 안내를 띄운다.

### 산타 조서 `GET /v1/me/dossier`

- 최신 결과(평가)의 조서다. 평소에는 `GET /v1/me/result`와 같은 평가라 점수·순위·판정이 같다. 재업로드 직후 순위 다시 계산(보통 1~2초) 전(`ranking_pending`이 true)에는 결과 화면이 지난 평가를 보여 주는 동안에도 조서는 새 평가의 것을 주고, 그동안 판결 장의 순위(`big_value`)는 `null`이다. 순위가 반영되면 둘이 다시 같아진다. 결과가 없으면 `404 RESULT_NOT_FOUND`, 최신 결과에 조서가 없으면(이 기능 전 결과, 예전 집계 규칙으로 센 조서) `404 DOSSIER_NOT_FOUND`다.
- 응답: `{ title, tone, scenes[] }`. `title`은 칭호, `tone`은 `normal`이나 `mild`(죄목과 놀림 문구를 끈 담백 톤)다. `tone`은 문구를 고르는 데만 쓰고 분석 이벤트·로그에 싣지 않는다.
- 장면: `{ id, kind, speaker, headline, big_value, lines[], memo, chart, shareable }`. 줄은 `{ id, text, shareable }`, 그래프는 `{ type, values }`(`bar24`는 0~23시 24칸, `bar12`는 1~12월 12칸)다. `headline`, `big_value`, `memo`, `chart`는 `null`일 수 있다.
- 장면 순서는 `teaser`(가린 숫자 티저, 공유 불가) → `hook`(올해의 한마디) → `ledger`(입버릇 장부) → `extreme_1`~`extreme_3`(가장 튀는 지표) → `acts`(올해의 너 3막) → `dossier`(칭호·죄목·공로·총평) → `verdict`(판결, 순위, 지역·도착 예정) → `summons`(공유 도발 문구)다. 받은 순서 그대로 그린다.
- 본인 메시지가 100개 미만이면 `ledger`·`acts`·`dossier`가 빠지고 극값은 2장이며, 티저 첫 줄에 "더 시끄러운 방을 올리라"는 안내가 붙는다. 100~299개면 장부 3줄, 극값 2장이다.
- `speaker`는 `elf`(계수 요정), `rudolph`(루돌프), `lawyer`(변호 요정), `santa`(산타) 중 하나다. 장면마다 화자 일러스트를 바꾼다.
- 숫자는 모두 서버가 단위까지 붙여 준다(`1,234번`, `23%`, `2시간 40분대`, `새벽 1시대`). 화면에서 다시 포맷하지 않는다.
- 문구는 결과 ID로 고정돼 다시 열어도 같다. 원문은 저장하지 않으므로 대화 내용 인용은 나오지 않는다.

```json
{
  "title": "웃음 과다 야간 근무자",
  "tone": "normal",
  "scenes": [
    {"id": "hook", "kind": "hook", "speaker": "elf", "headline": "네 엄지가 올해 제일 편애한 말", "big_value": "킹받네",
     "lines": [{"id": "l1", "text": "312번", "shareable": true}], "memo": "산타 사전에 없는 말이라 요정이 검색해 봄.", "chart": null, "shareable": true}
  ]
}
```

### 리캡 카드 덱 `GET /v1/me/recap`

「팩폭 산타」 결과를 9:16 카드 여러 장으로 보여 주는 덱이다. 이 절만 읽고 화면을 붙일 수 있게 흐름, 응답 예시, 필드 전체, 그리는 규칙을 모았다. 필드 정의의 기준은 OpenAPI의 `getMyRecap`이고, 아래 표는 2026-11-20 `develop` 코드(`presentation/roast/**`, `application/roast/**`)와 맞춰 봤다.

#### 숫자는 서버, 문장은 AI

- 카드의 숫자(점수, 컷 거리, 칩 값, 순위, 도착 시각, 막대 점수)는 모두 서버가 정한다. 문장(칭호, 판결, 놀림, 뒤집기, 쪽지, 줄거리)은 분석할 때 AI 작가가 쓰고 서버가 검증해 저장한 것이다. 검증에 떨어진 필드에는 서버가 준비한 대체 문장이 들어간다. 같은 결과는 다시 열어도 같은 문장이다.
- 문장 필드에는 숫자가 없다. 숫자는 `score_text`, `cut_distance`, `chip.value`, `rank`, `delivery_eta`, `bars[].score` 같은 숫자 필드로만 온다.
- **프론트는 숫자가 들어간 문장을 만들지 않는다.** "72점으로 선물 확정!", "컷보다 7.18점 높아요" 같은 문장을 화면에서 조립하지 않는다. 숫자는 받은 필드를 그 자리에 그대로 찍고, 문장은 받은 문장만 쓴다. 숫자 없는 고정 UI 라벨("점수", "컷 65", "순위")과 표시 형식 변환(시각의 KST 변환)은 괜찮다.

#### 전체 흐름

1. **업로드**: `POST /v1/analyses/files`(6.1절)로 원본 파일을 올린다. 응답 `202 { id, status: "VALIDATING" }`의 `id`를 잡아 둔다.
2. **작업 따라가기**: 처음 한 번 `GET /v1/analyses/{id}`로 상태를 읽고, 그다음은 SSE `GET /v1/analyses/{id}/events`(6.0절)로 바뀌는 상태를 받는다. 주기적으로 GET을 다시 부르는 폴링은 하지 않는다. 끊기면 같은 작업의 SSE에 다시 붙는다. `RANKING_PENDING`을 받으면 SSE는 열어 둔 채 바로 4번(덱 받기)으로 가서 덱을 먼저 보여 준다. 이때 덱은 새 평가 기준이고 순위·도착 시각만 비어 있다. 같은 SSE에서 `SUCCEEDED`가 오면(보통 1~2초 뒤) 덱을 한 번 더 불러 순위를 채운다. `RANKING_PENDING` 없이 바로 `SUCCEEDED`를 받았으면 그대로 다음 단계로 간다. `FAILED`면 `error_code`로 실패 안내를 띄운다.
3. **결과 확인**: `GET /v1/me/result`를 부르고 `recap_available`을 본다.
   - `true`면 리캡 덱으로 간다.
   - `false`면 이 결과는 리캡 기능(2026-11-20) 전에 받은 것이다. 덱 대신 기존 결과 화면을 보여 주고, 시즌 마감 전이면 "대화를 다시 올리면 팩폭 산타 카드를 받을 수 있다"는 재업로드 안내를 함께 둔다. 마감(12월 25일 0시 KST) 뒤에는 재업로드가 안 되므로 안내 없이 결과 화면만 보여 준다. 재업로드도 하루 3번 한도(9절)에 들어간다.
4. **덱 받기**: `GET /v1/me/recap`을 부른다(세션 쿠키 필요, CSRF 없음).
   - `200`: 아래 응답대로 덱을 그린다.
   - `404 RESULT_NOT_FOUND`: 점수가 나온 심사가 없다. 업로드 화면으로 보낸다.
   - `404 RECAP_NOT_FOUND`: 최신 결과에 카드 문장이 없다(기능 전 결과). 3번의 `recap_available: false`와 똑같이 처리한다.
   - `401`: 세션이 없다. 참가 안내로 보낸다.
5. **덱 그리기**: `cards`를 받은 순서대로 한 장씩 넘긴다(아래 그리는 규칙).
6. **공유**: 카드마다 이미지 저장·공유 버튼을 둔다. 지금 카드를 1080×1920 PNG로 그려 Web Share API(`navigator.share({ files, text })`)에 넘기고, 공유 문구 `text`는 응답의 `share_text`를 그대로 쓴다. 파일 공유를 못 하는 브라우저는 이미지 내려받기와 `share_text` 복사로 대신한다.

`ranking_pending`이 `true`일 때(작업 성공 직후 순위 다시 계산 전, 보통 1~2초, 몰리면 최대 약 6초):

- 덱은 새 평가 기준이라 그대로 그린다. 이때 판정 카드의 `rank`와 `delivery_eta`는 `null`이다.
- 순위·도착 시각 자리에는 숫자 없는 고정 라벨("순위 계산 중")을 두고, SSE로 `SUCCEEDED`를 받으면 `GET /v1/me/recap`을 한 번 더 불러 채운다. SSE가 끊겼으면 몇 초 뒤 한 번 더 부른다. 결과 화면(`GET /v1/me/result`)은 그동안 지난 평가의 순위를 보여 줄 수 있으니 두 화면의 순위를 섞지 않는다.
- 업로드 직후 흐름(2번)에서는 `RANKING_PENDING`에서 덱을 먼저 그리므로 처음 받는 덱은 대개 `ranking_pending`이 `true`다.

#### 응답 예시

문구 v3 기준의 예시다. 필드와 순서는 실제 응답과 같고, 문장은 카피 덱의 리라이트 예시를 넣었다(사람 이름 없음). `assessment_id`는 형식 예시다.

```json
{
  "assessment_id": "3d6f2c1a-8b4e-4f7a-9c2d-1e5b7a9c0f13",
  "skin": "GIFT",
  "score": 70.55,
  "score_text": "70.55",
  "share_text": "산타가 내 단톡방 다 읽음. 70.55점, 굴뚝에 걸쳐서 겨우 통과. 넌 넘냐?",
  "share_text_mild": "산타가 내 단톡방 다 읽음. 70.55점, 굴뚝에 걸쳐서 겨우 통과. 넌 넘냐?",
  "cards": [
    {"index": 0, "kind": "VERDICT", "width": 1080, "height": 1920, "score_text": "70.55",
     "score": 70.55, "cut": 65, "cut_distance": "+5.55", "cut_distance_value": 5.55, "passed": true,
     "stamp": "겨우 실었다", "stamp_mild": "겨우 실었다", "grade": "굴뚝에 걸쳐서 통과", "grade_code": "SCRAPED_THROUGH",
     "rank": null, "ranking_pending": false, "delivery_eta": "2026-12-24T18:30:02Z",
     "title": "단톡방 공식 'ㄱ?' 자판기",
     "verdict": "'ㄱ?'만 치고 선물까지 타 가네. 겨우 실었다, 분하지만.",
     "verdict_mild": "올해 이 방 약속은 다 네 손에서 나왔더라. 선물 실어 뒀다."},
    {"index": 1, "kind": "ROAST", "width": 1080, "height": 1920, "score_text": "70.55",
     "chip": {"label": "물음표 살포율", "value": "34%"},
     "roast": "말끝마다 물음표. 단톡방이 아니라 청문회인 줄 알았다.",
     "flip": "근데 그 물음표 없었으면 이 방은 진작 눈 덮였다."},
    {"index": 2, "kind": "ROAST", "width": 1080, "height": 1920, "score_text": "70.55",
     "chip": {"label": "'ㄱㄱ' 발사 횟수", "value": "18번"},
     "roast": "어디 가는지 듣기도 전에 'ㄱㄱ'부터 친다. 손이 먼저다.",
     "flip": "근데 그 'ㄱㄱ' 없었으면 글램핑은 아직도 투표 중이다."},
    {"index": 3, "kind": "ROAST", "width": 1080, "height": 1920, "score_text": "70.55",
     "chip": {"label": "새벽 출몰률", "value": "1%"},
     "roast": "밤만 되면 증발한다. 산타보다 먼저 퇴근하는 사람.",
     "flip": "근데 덕분에 이불 찰 새벽 흑역사가 없다. 장부가 깨끗하다."},
    {"index": 4, "kind": "LETTER", "width": 1080, "height": 1920, "score_text": "70.55",
     "letter": "서산 글램핑, 인천 락페, 배낚시까지. 네가 'ㄱ?' 안 쳤으면 이 방은 올해 방구석에서 귤만 깠다. 솔직히 좀 부러웠다. 북극엔 나더러 가자는 애가 없거든. 내년에도 먼저 던져라. 썰매는 내가 몰마."},
    {"index": 5, "kind": "STORY", "width": 1080, "height": 1920, "score_text": "70.55",
     "acts": [
       {"act": 1, "months": "1~4월", "line": "봄. 서산 글램핑 가자고 방을 들쑤셨다. 보드게임은 덤."},
       {"act": 2, "months": "5~8월", "line": "여름. 인천 락페에 물놀이까지. 방 전체가 끌려 나갔다."},
       {"act": 3, "months": "9~12월", "line": "가을. 안양 에어비앤비로 또 소집. 이쯤 되면 직업이다."}
     ]},
    {"index": 6, "kind": "ARCHETYPE", "width": 1080, "height": 1920, "score_text": "70.55",
     "archetype": {"code": "WORKSHOP_ELF", "name": "일 벌이는 공방 엘프"}},
    {"index": 7, "kind": "DIMENSIONS", "width": 1080, "height": 1920, "score_text": "70.55",
     "bars": [
       {"key": "care_empathy", "label": "공감", "score": 68.33},
       {"key": "positivity_support", "label": "응원", "score": 72.95},
       {"key": "respectful_communication", "label": "존중", "score": 70.55},
       {"key": "cooperation_sharing", "label": "협력", "score": 79.02}
     ]}
  ]
}
```

#### 최상위 필드

| 필드 | 타입 | null | 뜻 |
| --- | --- | --- | --- |
| `assessment_id` | string | 아님 | 카드를 만든 결과(평가) ID. 늘 최신 평가다. 덱을 다시 받았을 때 바뀌었는지 보는 데 쓴다 |
| `skin` | `"GIFT"` \| `"COAL"` | 아님 | 총점 65점 이상이면 `GIFT`(선물), 아래면 `COAL`(석탄). 배경·색을 고른다. 재심은 없다 |
| `score` | number | 아님 | 총점(소수 둘째 자리). 계산용이다 |
| `score_text` | string | 아님 | 총점 표시 문자열(`"72.18"`). 화면에는 이 값을 그대로 찍는다 |
| `share_text` | string | 아님 | 서버가 고정한 공유 문구. 등급마다 다르고 서버가 `score_text`를 넣어 준다(아래 등급 표). 화면에서 고치지 않는다 |
| `share_text_mild` | string | 아님 | 석탄 농담을 끈 화면의 공유 문구(2026-11-20 추가). 통과 등급이면 `share_text`와 같고, 탈락 세 등급이면 점수 없이 `"올해 선물은 내년으로 미뤄짐. 너는 받았는지 까봐."`다 |
| `cards` | array | 아님 | 카드 목록. 순서 고정(아래). 7장 또는 8장 |

리캡의 선물·석탄은 결과 응답의 `verdict`(기존 점수 판정)가 아니라 컷 65점 선물 판정이다. 둘이 다를 수 있으니 리캡 화면에서는 `skin`·`passed`·`stamp`만 쓴다.

#### 카드 공통 필드

모든 카드에 있다.

| 필드 | 타입 | null | 뜻 |
| --- | --- | --- | --- |
| `index` | number | 아님 | 카드 순서. 0부터 하나씩 는다 |
| `kind` | string | 아님 | `VERDICT`, `ROAST`, `LETTER`, `STORY`, `ARCHETYPE`, `DIMENSIONS` 중 하나. 이 값으로 카드 모양을 고른다 |
| `width` | number | 아님 | 늘 `1080` |
| `height` | number | 아님 | 늘 `1920`(9:16) |
| `score_text` | string | 아님 | 최상위 `score_text`와 같다. 모든 카드 상단에 크게 찍는다 |

#### 카드 순서

순서는 서버가 고정한다. 받은 배열 순서대로 그리고, 화면에서 다시 정렬하거나 카드를 빼지 않는다.

| 순서 | `kind` | 장 수 | 언제 |
| --- | --- | --- | --- |
| 1 | `VERDICT` | 1 | 늘 |
| 2 | `ROAST` | 3 | 늘 |
| 3 | `LETTER` | 1 | 늘 |
| 4 | `STORY` | 0~1 | 저장한 줄거리가 있을 때만. 없으면 카드째 빠지고 뒤 카드의 `index`가 하나씩 당겨진다 |
| 5 | `ARCHETYPE` | 1 | 늘 |
| 6 | `DIMENSIONS` | 1 | 늘 |

`STORY`가 빠질 수 있으니 카드 위치를 `index` 숫자로 짐작하지 말고 `kind`로 고른다. 지금은 빠진 막을 대체 줄로 채워 저장하므로 거의 늘 세 막이 온다. 그래도 줄거리 없이 저장된 결과가 있을 수 있다.

#### `VERDICT` 판정 카드

| 필드 | 타입 | null | 뜻 |
| --- | --- | --- | --- |
| `score` | number | 아님 | 총점(소수 둘째 자리) |
| `cut` | number | 아님 | 선물 컷. 늘 `65` |
| `cut_distance` | string | 아님 | 컷 거리 표시 문자열. 부호를 늘 붙이고 소수 둘째 자리까지(`"+7.18"`, `"-3.20"`, 딱 컷이면 `"+0.00"`). **화면에는 이 값을 찍는다** |
| `cut_distance_value` | number | 아님 | 같은 컷 거리의 숫자(`7.18`, `-3.20`). 컷 아래면 음수다. 막대 길이·색처럼 계산할 때만 쓰고, 화면에 직접 포맷해 찍지 않는다 |
| `passed` | boolean | 아님 | 65점 이상이면 `true`. `skin == "GIFT"`와 같다 |
| `stamp` | string | 아님 | 도장 문구. 등급마다 다르다(아래 표) |
| `stamp_mild` | string | 아님 | 석탄 농담을 끈 화면의 도장 문구. 통과 등급이면 `stamp`와 같고, 탈락 세 등급이면 `"내년 명단 예약"` |
| `grade` | string | 아님 | 등급 이름(아래 표). 화면에 찍는다 |
| `grade_code` | string | 아님 | 등급 코드(아래 표). 스타일 분기에 쓴다 |
| `rank` | number | 가능 | 전국 순위. 순위 계산 참가자가 100명보다 적거나 `ranking_pending`이 `true`면 `null`이다 |
| `ranking_pending` | boolean | 아님 | 최신 평가가 아직 순위에 반영되지 않았으면 `true`(위 흐름 참고) |
| `delivery_eta` | string(ISO-8601 UTC) | 가능 | 선물 도착 예정 시각. 석탄이거나 순위 계산 전이면 `null`이다 |
| `title` | string | 아님 | 칭호(AI 문장) |
| `verdict` | string | 아님 | 판결 한 줄(AI 문장, 농담) |
| `verdict_mild` | string | 아님 | 놀림 없는 판결(AI 문장). 석탄 농담을 끈 화면에 쓴다 |

`grade_code` 값(총점 기준):

| `grade_code` | `grade` | `stamp` | 총점 | `skin` |
| --- | --- | --- | --- | --- |
| `PREMIUM` | 굴뚝 프리패스 | 산타 최애 | 90 이상 | `GIFT` |
| `GIFT_DEPARTED` | 썰매 탑승 완료 | 선물 실었다 | 75 이상 90 미만 | `GIFT` |
| `SCRAPED_THROUGH` | 굴뚝에 걸쳐서 통과 | 겨우 실었다 | 65 이상 75 미만 | `GIFT` |
| `ONE_STEP_SHORT` | 굴뚝 앞 유턴 | 아깝다 진짜 | 55 이상 65 미만 | `COAL` |
| `OFF_LIST` | 명단에 없음 | 산타 읽씹 | 50 이상 55 미만 | `COAL` |
| `COAL` | 석탄 당첨 | 반품 불가 | 50 미만 | `COAL` |

등급별 `share_text`(`{점수}` 자리에 서버가 `score_text`를 넣는다):

| `grade_code` | `share_text` |
| --- | --- |
| `PREMIUM` | 산타가 내 단톡방 다 읽고도 {점수}점 줌. 이게 인성이다. 너도 까봐. |
| `GIFT_DEPARTED` | 산타가 내 단톡방 다 읽음. {점수}점, 선물 실림. 넌 읽히면 석탄 각. |
| `SCRAPED_THROUGH` | 산타가 내 단톡방 다 읽음. {점수}점, 굴뚝에 걸쳐서 겨우 통과. 넌 넘냐? |
| `ONE_STEP_SHORT` | {점수}점. 굴뚝 앞에서 썰매가 유턴함. 억울해서 올린다. 너는 넘나 보자. |
| `OFF_LIST` | 산타한테 읽씹당함. {점수}점. 나만 당할 수 없다. 너도 까봐. |
| `COAL` | 산타가 내 단톡방 읽고 석탄 보냄. 반품도 안 된대. 혼자 받긴 억울하다. 너도 까봐. |

`stamp`와 `stamp_mild`, `verdict`와 `verdict_mild`:

- 석탄 문구는 기본이 농담이다. 화면은 `stamp`와 `verdict`를 기본으로 보여 준다.
- 사용자가 석탄 농담을 끄면(기획의 "석탄 농담 끄기"·[담백하게 보기] 토글) `stamp` 대신 `stamp_mild`, `verdict` 대신 `verdict_mild`, 공유할 때 `share_text` 대신 `share_text_mild`를 쓴다. 다시 부를 필요 없이 같은 응답 안에서 바꾼다. 담백 화면에서 `grade_code`가 `COAL`이면 등급 이름("석탄 당첨")은 숨긴다(조서 판결 장과 같은 규칙).
- 이 토글은 서버에 저장하지 않는다. 요청 파라미터도, 응답의 톤 필드도 없다. 켜고 끈 상태는 화면이 기억한다(필요하면 기기 로컬 저장소). 조서의 `tone`(서버가 정하는 담백 톤)과는 다른 장치다.
- 선물이면 `stamp_mild`는 `stamp`와, `share_text_mild`는 `share_text`와 같고, `verdict_mild`는 놀림 없이 선물이 간다는 문장이다. 그래서 토글은 석탄 화면에만 두어도 된다. 선물 화면에 두더라도 같은 규칙으로 두 필드를 바꾸면 된다.
- 바꾸는 것은 이 세 쌍뿐이다. 칭호, 놀림, 뒤집기, 숫자는 토글과 상관없이 그대로 보인다.

`rank`:

- `null`이 되는 경우는 둘이다. 순위 계산 참가자가 100명보다 적을 때(상위 비율 표본 기준과 같은 100명)와 `ranking_pending`이 `true`일 때다.
- 순위 자리에는 숫자 없는 고정 라벨을 둔다. `ranking_pending`이 `true`면 "순위 계산 중", 아니면 "순위 집계 전"처럼 쓴다. 100명 미만이라도 `delivery_eta`는 올 수 있다(위 예시처럼 `rank: null`인데 도착 시각은 있다).

`delivery_eta`:

- ISO-8601 UTC 문자열이다(`"2026-12-24T18:30:02Z"`, 초 아래 자리가 붙을 수도 있다). `new Date(...)`로 읽어 `Asia/Seoul`로 바꿔 보여 준다. 위 예시는 한국 시각 12월 25일 오전 3시 30분이다.

```ts
new Intl.DateTimeFormat("ko-KR", { timeZone: "Asia/Seoul", month: "long", day: "numeric", hour: "numeric", minute: "2-digit" })
  .format(new Date(card.delivery_eta)); // "12월 25일 오전 3:30"
```

- 석탄(`passed: false`)이거나 순위 계산 전이면 `null`이다. 석탄이면 도착 시각 자리를 두지 않고, 순위 계산 전이면 "도착 시각 계산 중" 같은 고정 라벨을 둔다.
- 리캡 카드는 오너 결정에 따라 도착 시각을 분 단위까지 싣는다. 7절의 "남이 보는 곳에는 날짜만" 규칙은 랭킹과 서버가 그리는 공유 카드(`/s/{token}`) 기준이다.

#### `ROAST` 팩폭 카드(3장)

| 필드 | 타입 | null | 뜻 |
| --- | --- | --- | --- |
| `chip` | object | 가능 | 서버가 센 숫자 칩 `{ label, value }`. 그 짝이 사실 없이 쓴 대체 문장이면 `null`이다 |
| `chip.label` | string | 아님 | 칩 이름(`"새벽 출몰률"`, `"물음표 살포율"`, `"'ㄱㄱ' 발사 횟수"`). 화면은 이 값을 그대로 찍고 자체 문구("물음표로 끝난 말풍선" 같은 것)로 바꾸지 않는다 |
| `chip.value` | string | 아님 | 칩 값. 서버가 단위까지 붙인 문자열(`"1%"`, `"124자"`, `"18번"`). 다시 포맷하지 않는다 |
| `roast` | string | 아님 | 말버릇 놀림(AI 문장) |
| `flip` | string | 아님 | 같은 버릇을 뒤집어 칭찬하는 문장(AI 문장). 문구 v3부터 문장이 `"근데 "`로 시작한다(서버가 검증한다). 화면은 "근데" 라벨을 따로 붙이지 않는다 |

- `chip`이 `null`이면 칩 자리를 비우고 `roast`·`flip`만 그린다. 대체 숫자나 문구를 만들어 넣지 않는다.
- 한 카드 안에서 칩 → 놀림 → 뒤집기 순서로 읽히게 둔다. "근데"는 `flip` 문장 안에 있으니 별도 라벨로 겹쳐 찍지 않는다. 문장 앞 "근데"만 작게 꾸미려면 `flip`이 `"근데 "`로 시작하는지 먼저 확인한다. 나머지는 받은 문장 그대로 둔다.
- 문구 v3 전에 저장된 결과는 `flip`이 "근데" 없이 시작한다. 이때도 라벨을 덧붙이지 않고 받은 문장 그대로 보인다.

#### `LETTER` 산타 쪽지 카드

| 필드 | 타입 | null | 뜻 |
| --- | --- | --- | --- |
| `letter` | string | 아님 | 산타 쪽지(AI 문장, 60~120자, 놀림 없음). 화면 폭에 맞춰 줄을 바꾼다 |

#### `STORY` 세 막 줄거리 카드(있을 때만)

| 필드 | 타입 | null | 뜻 |
| --- | --- | --- | --- |
| `acts` | array | 아님 | 막 목록. 1막·2막·3막 순서다 |
| `acts[].act` | number | 아님 | 막 번호(1~3) |
| `acts[].months` | string | 아님 | 막의 달 범위. 1막 `"1~4월"`, 2막 `"5~8월"`, 3막 `"9~12월"` |
| `acts[].line` | string | 아님 | 그 막의 한 줄(AI 문장). 지명·가게·행사 이름은 나올 수 있지만 사람 이름은 나오지 않는다 |

#### `ARCHETYPE` 산타 유형 카드

| 필드 | 타입 | null | 뜻 |
| --- | --- | --- | --- |
| `archetype` | object | 아님 | 최신 평가의 산타 유형 `{ code, name }` |
| `archetype.code` | string | 아님 | 유형 코드(10가지, 예: `RUDOLPH_DRIVER`). 일러스트를 고른다. 목록은 OpenAPI 스키마와 7절 `archetype`과 같다 |
| `archetype.name` | string | 아님 | 유형 이름(`"맨 앞에서 끄는 루돌프"`). 2026-11-20에 `WORKSHOP_ELF`는 "일 벌이는 공방 엘프", `RUDOLPH_DRIVER`는 "맨 앞에서 끄는 루돌프"로 이름이 바뀌었다. 이름은 하드코딩하지 말고 이 값을 쓴다 |

#### `DIMENSIONS` 항목 막대 카드

| 필드 | 타입 | null | 뜻 |
| --- | --- | --- | --- |
| `bars` | array | 아님 | 항목 막대 4개. 공감·응원·존중·협력 순서 |
| `bars[].key` | string | 아님 | `care_empathy`, `positivity_support`, `respectful_communication`, `cooperation_sharing` |
| `bars[].label` | string | 아님 | `공감`, `응원`, `존중`, `협력` |
| `bars[].score` | number | 아님 | 0~100, 소수 둘째 자리. 막대 길이와 옆 숫자로 쓴다 |

#### 타입 예시(TypeScript)

```ts
type RecapCardBase = { index: number; width: 1080; height: 1920; score_text: string };

type RecapCard =
  | (RecapCardBase & {
      kind: "VERDICT";
      score: number; cut: number; cut_distance: string; cut_distance_value: number; passed: boolean;
      stamp: string; stamp_mild: string; grade: string;
      grade_code: "PREMIUM" | "GIFT_DEPARTED" | "SCRAPED_THROUGH" | "ONE_STEP_SHORT" | "OFF_LIST" | "COAL";
      rank: number | null; ranking_pending: boolean; delivery_eta: string | null;
      title: string; verdict: string; verdict_mild: string;
    })
  | (RecapCardBase & { kind: "ROAST"; chip: { label: string; value: string } | null; roast: string; flip: string })
  | (RecapCardBase & { kind: "LETTER"; letter: string })
  | (RecapCardBase & { kind: "STORY"; acts: { act: number; months: string; line: string }[] })
  | (RecapCardBase & { kind: "ARCHETYPE"; archetype: { code: string; name: string } })
  | (RecapCardBase & { kind: "DIMENSIONS"; bars: { key: string; label: string; score: number }[] });

type RecapResponse = {
  assessment_id: string;
  skin: "GIFT" | "COAL";
  score: number;
  score_text: string;
  share_text: string;
  share_text_mild: string;
  cards: RecapCard[];
};
```

모르는 `kind`가 오면(나중에 카드 종류가 늘 때) 그 카드는 건너뛰고 나머지를 그린다.

#### 그리는 규칙

- **9:16 한 장씩**: 카드 한 장이 화면 하나다(1080×1920 비율). 좌우 넘김이나 세로 스냅으로 한 장씩 넘긴다.
- **모든 필드를 보인다**: 받은 문장·칩·숫자는 잘리거나 접히지 않게 다 보인다. 말줄임, "더 보기", 접기를 쓰지 않는다. 긴 문장은 글자 크기를 줄여 카드 안에 맞춘다.
- **점수는 크게**: 모든 카드 상단에 `score_text`를 가장 크게 찍는다.
- **가리기 토글 없음**: 점수, 순위, 막대, 칩을 가리는 토글은 두지 않는다(오너 결정). 바꿀 수 있는 것은 석탄 농담 끄기(`*_mild`로 바꾸기) 하나뿐이다.
- **스킨**: `skin`으로 배경과 색을 고른다(`GIFT` 선물, `COAL` 석탄).
- **공유 문구는 서버 것**: `share_text`(담백 화면이면 `share_text_mild`)를 그대로 쓴다. 화면에서 숫자나 닉네임을 덧붙인 공유 문구를 만들지 않는다. 공유 직전에 문구 미리보기를 보여 주고, 사용자가 담백 문구를 고를 수 있게 한다.
- **이미지 저장**: 저장 이미지는 화면에 보이는 카드와 같은 1080×1920으로 그린다. 상대 대화나 원문은 카드에 없으므로 따로 지울 것은 없다.

#### 화면 고정 문구(프론트 전용, 문구 v3)

아래 문구는 서버 응답에 없어서 화면이 고정으로 찍는다. AI 문장, 서버 문구와 겹치지 않게 이 표의 문구만 쓴다.

| 카드 | 자리 | 문구 |
| --- | --- | --- |
| 판정 | eyebrow | 산타가 네 단톡방을 끝까지 읽었다 |
| 판정 | 컷 칩 | 통과: `선물 컷 65 · {cut_distance}`(예: `선물 컷 65 · +5.55`) / 탈락: `선물 컷 65 · {cut_distance} 모자람`(예: `선물 컷 65 · -17.00 모자람`). 숫자는 서버 `cut_distance`를 그대로 넣는다 |
| 판정 | 도착 문구 | `굴뚝 도착 예정 {delivery_eta를 서울 시각 MM/DD HH:mm} · 자는 척 필수` |
| 판정 | 하단 고정 | 재심 없음. 산타 퇴근함. |
| 팩폭 | 번호 라벨 | 산타가 밑줄 친 말버릇 01(02, 03) |
| 팩폭 | 뒤집기 앞 라벨 | 없음. "근데"는 `flip` 문장에 들어 있다 |
| 편지 | eyebrow / 제목 | 산타가 굳이 남긴 쪽지 / 놀려서 미안, 근데 진심이다 |
| 편지 | 서명 | — 북극에서, 야근 중인 산타 |
| 줄거리 | eyebrow / 제목 | 산타가 몰래 본 2026 / 올해 너, 세 줄 요약 |
| 줄거리 | 막 라벨 | `months`(1~4월 등)를 그대로 둔다. AI 문장이 계절 낱말로 시작하니 "봄 편" 같은 라벨을 겹쳐 달지 않는다 |
| 유형 | eyebrow | 북극 인사팀 배치 결과 |
| 채점표 | eyebrow / 제목 | 산타 채점표 원본 / 어디서 벌고 어디서 까였나 |
| 채점표 | 영역 표시명 | `care_empathy` 맞장구, `positivity_support` 기 살려주기, `respectful_communication` 선 지키기, `cooperation_sharing` 판 깔기 |
| 채점표 | 지목 영역 제목 | 다음 굴뚝은 누구냐 |
| 채점표 | 버튼 | 단톡방에 던지기(카카오, 1순위) / 스토리에 박제하기(2순위) |

- 영역 표시명은 화면에서만 바꾼다. 서버 `bars[].label`("공감/응원/존중/협력")은 기존 공유 카드 요약이 쓰고 있어 그대로 둔다. 화면은 `bars[].key`로 위 표시명을 고른다.
- 칩 이름은 화면이 따로 쓰지 않고 `chip.label`을 그대로 쓴다.
- 고정 문구의 "세 줄 요약" 같은 셈말은 서버 문장 검증 대상이 아니다. AI 문장(`title`, `verdict`, `roast`, `flip`, `letter`, `acts[].line`)에만 숫자·셈말이 없다.

#### 석탄 스킨(`skin: "COAL"`) 연출

- 탈락 세 등급(`ONE_STEP_SHORT`, `OFF_LIST`, `COAL`)은 모두 `COAL` 스킨이다. 그중 `grade_code: "COAL"`(석탄 당첨, 도장 "반품 불가")은 한정판 카드처럼 검정 바탕에 금박 도장으로 가장 공들여 그린다. 경품 당첨 화면 느낌을 낸다.
- 석탄 카드의 도착 문구는 `굴뚝 도착 예정 12/25 03:30 · 석탄도 자는 척은 필수`처럼 고정 문구로 둔다. 석탄은 `delivery_eta`가 `null`이므로 시각은 화면 고정값이다.
- 담백 화면(석탄 농담 끄기)에서도 금박은 그대로 두고 문구만 `*_mild`로 바꾼다.
- 판정 카드는 점수를 가장 크게 두고, 점수 아래 얇은 트랙에 컷 65 지점을 굴뚝 아이콘으로 세운다. 마커가 컷 근처에서 잠깐 멈칫한 뒤 도장이 찍힌다. 첫 진입 때 한 번만 재생하고 가리기는 없다.
- 인스타용 이미지 하단에는 작은 워터마크 "산타컷에서 판정받음 · 재심 없음"을 넣는다.

### 랭킹 `GET /v1/rankings`

- 로그인 없이 볼 수 있다. 내 순위 카드는 `GET /v1/me/result`로 따로 읽는다.
- 응답: `{ season_year, participant_count, computed_at, stage, rows[], next_cursor }`. 각 행은 `nickname`, `rank`, `tied`, `score_hundredths`, `eta_date`, `eta`다.
- `eta_date`는 `2026-12-26` 같은 `Asia/Seoul` 날짜다. `eta`는 더 쓰지 않는 필드로 늘 `null`이다(2026-11-17까지는 정확한 시각이었다). 행의 도착일은 `eta_date`로 보여준다.
- 한 번에 50줄이 온다. `더 보기`는 `next_cursor`를 `cursor` 쿼리로 보낸다. `next_cursor`가 `null`이면 마지막 페이지다.
- `cursor`는 불투명 문자열이다. 직접 만들지 말고 받은 값만 보낸다. 잘못된 값이나 2026-11-17 이전 형식(숫자)은 `400`(`INVALID_CURSOR`)이다.
- `cursor`에는 그 페이지의 순위 계산 시각이 들어 있다. 페이지 사이에 순위가 다시 계산되면 `409`(`RANKING_SNAPSHOT_CHANGED`)다. 이때는 목록을 비우고 `cursor` 없이 첫 페이지부터 다시 읽는다. 줄이 겹치거나 빠지지 않게 하려는 규칙이다.
- 순위는 서버가 변경 직후(1초쯤 모아서) 다시 계산하고, 첫 페이지는 계산 시각이 바뀌기 전까지 30초쯤 캐시한다. 새로 계산하면 캐시를 바로 버린다.
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
| 한 번에 너무 많은 이벤트 | `400` | `TOO_MANY_EVENTS` | 이벤트 JSON 제출에서 2,000개 초과. 파일 업로드는 서버가 본인 2,000개·상대 포함 6,000개 안으로 골라 쓰므로 화면에서 다룰 일이 없다 |
| 이벤트 시각·글자 수 범위 밖 | `400` | `INVALID_EVENT_RANGE` | |
| 최근 기록·근거 부족(AI 판정) | - | 상태 조회의 `error_code` `INSUFFICIENT_EVIDENCE` | 작업은 `FAILED`로 끝난다 |
| 파일이 너무 큼 | `413` | `FILE_TOO_LARGE` | 20MB 초과. `code`가 없어도 같은 뜻 |
| 지원하지 않는 파일 형식(빈 파일·zip·바이너리) | `422` | `UNSUPPORTED_FILE_FORMAT` | 업로드 즉시 거절. 파일 다시 고르기 |
| 지원하지 않는 파일 형식(검증 중 발견) | - | 상태 조회의 `error_code` `UNSUPPORTED_FILE_FORMAT` | 인코딩·스키마 오류, 메시지 0개 |
| 본인 이름을 대화에서 못 찾음 | - | 상태 조회의 `error_code` `SPEAKER_NOT_IDENTIFIED` | 본인 다시 고르기 |
| 본인 메시지 부족 | - | 상태 조회의 `error_code` `INSUFFICIENT_EVENTS` | 올해(1월 1일부터) 본인 메시지 20개 미만 |
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
| 조서 없음 | `404` | `DOSSIER_NOT_FOUND` | `GET /v1/me/dossier`. 최신 결과에 조서가 없음. 시즌 마감 전이면 재업로드 안내, 마감 뒤에는 조서 없음 안내만 |
| 리캡 카드 없음 | `404` | `RECAP_NOT_FOUND` | `GET /v1/me/recap`. 최신 결과에 카드 문장이 없음(이 기능 전 결과). 덱 대신 기존 결과 화면, 시즌 마감 전이면 재업로드 안내 |
| 공유할 순위 없음 | `409` | `NO_RANKED_RESULT` | `POST /v1/shares`. 첫 순위 계산 전이면 공유 버튼을 숨기거나 잠시 뒤 다시 |
| 분석 작업 없음 | `404` | `NOT_FOUND` | `GET /v1/analyses/{id}`(없거나 남의 작업), `GET /v1/me/analysis`(낸 작업 없음) |
| 세션의 참가자가 지워짐 | `404` | `PARTICIPANT_NOT_FOUND` | 새 참여 안내 |
| 잘못된 Idempotency-Key | `400` | `INVALID_IDEMPOTENCY_KEY` | 키 생성 코드 버그 |
| 같은 키로 다른 요청 | `409` | `IDEMPOTENCY_CONFLICT` | 새 제출에는 새 키를 만든다 |
| 진행 상황 스트림이 꽉 참 | `503` | `ANALYSIS_STREAM_LIMIT` | 연결 종료 안내 후 같은 SSE 재연결. EventSource에서는 Retry-After 헤더를 직접 읽을 수 없음 |
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
