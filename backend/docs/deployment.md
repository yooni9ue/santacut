# 초저예산 서버 배포 운영서

## 배포 구조와 전제

서버와 AWS 자원은 [`santacut-infra`](https://github.com/yooni9ue/santacut-infra)가 만든다. 서버 쪽 약속(비밀값 파일, 백업 버킷, 사용자)은 그 레포 `docs/operations.md` 6·7절이 기준이다. 이 문서는 앱 레포가 그 약속에 맞춘 부분을 적는다.

- dev 서버는 Lightsail `micro_3_0`(1GB, 2 vCPU, 40GB)이고 스왑 1GB가 있다. 2026-11-16에 `nano_3_0`(512MB)에서 올렸다(기동 212초 → 26초). 운영(`main`)은 아직 켜지 않았다.
- 주소는 dev `https://api-dev.santacut.me`, 운영 `https://api.santacut.me`다. DNS는 Route 53 호스팅 영역(`santacut.me`)이 맡고, 가비아에서 네임서버를 위임했다. 화면은 Vercel(`dev.santacut.me`, `santacut.me`, `www.santacut.me`)에 있다.
- `backend/ci-cd/deploy/compose.yml`은 Caddy와 Spring Boot 앱 두 컨테이너다. SQLite 파일은 Docker named volume에 둔다.
- AI는 Google Gemini 3.5 Flash-Lite(Gemini Developer API)다. API 키(`GEMINI_API_KEY`)로 부른다.
- SQLite 한 파일이라 앱 인스턴스는 하나만 띄운다. 여러 서버에서 같은 파일을 쓰지 않는다.

## 메모리 예산

| 대상 | 설정 |
| --- | --- |
| 앱 컨테이너 | `mem_limit: 512m` |
| JVM | `-Xms128m -Xmx256m -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError` |
| Caddy | `mem_limit: 64m` |

- 힙이 모자라면 JVM이 바로 죽고 `restart: unless-stopped`가 다시 띄운다.
- 1GB 번들 기준 값이다. 처음에는 512MB 번들에 맞춰 메타스페이스 128MB, 컨테이너 400m로 두었다.
- 2026-11-17 dev 부하 시험에서 동시 15명부터 `OutOfMemoryError: Metaspace`로 앱이 두 번 다시 떴다. 워밍업이 끝난 앱은 GC 뒤에도 메타스페이스를 126~128MB 썼다(`jcmd 1 VM.metaspace`). 192MB로 올린 뒤 같은 시험에서는 133MB를 쓰고 다시 뜨지 않았다. 컨테이너 메모리는 최대 409MiB였다.
- 스왑은 비상용이다. 자주 죽거나 느리면 번들을 올린다(인프라 레포 9절).

## HTTPS와 도메인

Caddy 사이트 주소는 `DOMAIN`을 따른다. Caddyfile은 `{$SITE_ADDRESS::80}` 하나로 두 경우를 모두 처리한다.

| 경우 | `DOMAIN` | `ACME_EMAIL` | `PUBLIC_BASE_URL` | `SESSION_SECURE` | Caddy |
| --- | --- | --- | --- | --- | --- |
| dev(지금) | `api-dev.santacut.me` | 운영자 이메일 | `https://api-dev.santacut.me` | `true` | Let's Encrypt 인증서 자동 발급, 443 |
| 운영 | `api.santacut.me` | 운영자 이메일 | `https://api.santacut.me` | `true` | 같음 |
| 도메인 없음(로컬 점검용) | 비움 | 비움 | `http://<고정 IP>` | `false` | `:80` 평문 HTTP |

- Caddy는 80번으로 온 요청을 HTTPS로 넘기고(`308`), 모든 응답에 HSTS(`max-age=31536000`), `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options`를 붙인다. `/actuator`는 밖에서 `404`다.
- 진행 상황 스트림(`/v1/analyses/*/events`)은 압축하지 않고 버퍼 없이 바로 흘려보낸다.
- 운영 프로필은 Caddy가 붙인 `X-Forwarded-*` 헤더를 믿는다(`server.forward-headers-strategy: native`, 내부 프록시 대역만). 그래서 앱은 원래 요청이 HTTPS였다는 것을 안다.
- HTTP에서 `SESSION_SECURE=true`면 브라우저가 `__Host-` 세션 쿠키를 보내지 않는다.
- 서버의 앱은 운영 프로필(`SPRING_PROFILES_ACTIVE=production`)로 뜨고, 이 프로필은 `SESSION_SECURE=true`가 아니면 뜨지 않는다. Compose도 `SESSION_SECURE`가 비어 있으면 멈춘다. 그래서 위 표의 "도메인 없음" 평문 HTTP 점검은 서버 배포가 아니라 운영 프로필 없이 띄운 로컬 앱에서만 한다.
- Secure일 때 CSRF 쿠키 이름은 `__Host-XSRF-TOKEN`이다(로컬 HTTP는 `XSRF-TOKEN`). 화면은 쿠키를 읽지 않고 `GET /v1/csrf` 본문의 토큰을 쓰므로 이름이 바뀌어도 영향이 없다.

## 환경 설정과 배포

앱 설정은 서버 디스크에 두지 않는다. 운영자가 인프라 레포 `scripts/put-secrets.sh`로 KMS 암호화해 S3에 올리고, 서버는 부팅 때 `/run/santacut/app.env`(tmpfs, 600)로 받는다. 키 목록은 `backend/ci-cd/deploy/.env.example`에 있다.

- Compose는 이 파일을 두 번 쓴다. 앱 컨테이너의 `env_file`이고, `deploy.sh`·`backup.sh`의 `--env-file`(보간용)이다.
- 앱은 AWS 기본 자격 증명 체인으로 환경 변수의 키를 읽는다. 키를 Spring 설정에 따로 적지 않는다.
- CI는 `APP_ENV_FILE=.env.example`로 예시 파일을 넣어 Compose 구문만 검사한다.

GitHub Actions는 PR, `develop`, `main`에서 검증한다. `develop`과 `main`에 푸시된 커밋 이미지는 GHCR에 게시한다(`ghcr.io/yooni9ue/santacut:<sha>`, `develop`은 `:develop` 태그도 붙인다). `develop`은 이어서 dev 서버에 자동 배포하고(아래), `main`은 이미지 게시까지만 한다. 운영 배포는 `workflow_dispatch`에서 `deploy=true`를 고른다.

- 서버가 이미지를 받을 때 쓰는 GHCR 자격 증명은 `app.env`의 `GHCR_USER`, `GHCR_TOKEN`(`read:packages`만)이다. 인프라의 `fetch-secrets.sh`가 이 두 키로 `docker login ghcr.io`를 하고, 앱 컨테이너에 넘기는 파일에서는 뺀다. 릴리스 묶음에는 `compose.yml`, `Caddyfile`, `deploy.sh`, `backup.sh`가 들어간다. 배포 스크립트는 검증한 image digest를 쓰고 readiness를 확인한다.

### dev 자동 배포

`backend/**`를 바꾼 커밋이 `develop`에 푸시되면 `deploy-dev` 잡이 `test` → `publish` 다음에 dev 서버(`https://api-dev.santacut.me`)로 배포한다. 동시에 하나만 돈다(`concurrency: dev-deploy`).

1. GitHub Environment `dev`(배포 브랜치는 `develop`만)에서 OIDC로 `santacut-app-deploy-dev` 역할을 맡는다.
2. 러너 IP `/32`에 Lightsail 22번을 연다.
3. 릴리스 묶음을 `scp`로 올리고 `deploy.sh <image@digest> /opt/santacut/releases/<sha>`를 돌린다. 이어서 `/opt/santacut/current-image`가 방금 올린 digest와 같은지 대조한다.
4. `https://api-dev.santacut.me/v3/api-docs`를 호출해 응답을 확인한다.
5. 성공하든 실패하든(`if: always()`) 러너 CIDR만 닫는다. 닫은 뒤 22번에 러너 CIDR이 남아 있으면 잡을 실패시킨다. 운영자 CIDR이 빠졌으면 다시 연다.

Lightsail의 열기는 기존 CIDR에 더하는 방식이고, 닫기는 지정한 CIDR만 뺀다(2026-11-17 dev에서 확인). 그래서 운영자 CIDR은 배포 뒤에도 그대로 남는다.

`dev` Environment 설정:

| 종류 | 이름 | 값 |
| --- | --- | --- |
| 변수 | `AWS_DEPLOY_ROLE_ARN` | 인프라 출력 `app_deploy_role_arns.dev` |
| 변수 | `DEV_DEPLOY_HOST`, `DEV_DEPLOY_USER` | `54.180.194.120`, `ubuntu` |
| 변수 | `DEV_OPERATOR_SSH_CIDR` | 운영자 SSH CIDR(`SSH_ALLOWED_CIDRS`와 같은 값) |
| 시크릿 | `DEV_DEPLOY_SSH_KEY` | 배포용 SSH 개인 키 |
| 시크릿 | `DEV_DEPLOY_KNOWN_HOSTS` | `ssh-keyscan 54.180.194.120` 결과 |

### 운영 설정 키

| 키(환경 변수) | 기본값 | 뜻 |
| --- | --- | --- |
| `app.public-base-url` (`APP_PUBLIC_BASE_URL`, Compose에서는 `PUBLIC_BASE_URL`) | `http://localhost:8080` | 공유 링크·Open Graph에 쓰는 공개 출처. 경로 없는 URL이어야 한다 |
| `app.session-secure` (`APP_SESSION_SECURE`, Compose에서는 `SESSION_SECURE`) | `false`(운영 프로필 `true`) | 세션·CSRF 쿠키 Secure·`__Host-` 이름 사용. 운영 프로필은 `app.require-secure-session=true`라 `false`면 앱이 뜨지 않고, Compose는 `SESSION_SECURE`를 꼭 넣어야 한다 |
| `app.cors.allowed-origins` (`APP_CORS_ALLOWED_ORIGINS`) | `https://www.santacut.me,https://santacut.me` | 쿠키를 실어 API를 부를 수 있는 프론트엔드 출처. 쉼표로 나눈다. dev는 `https://dev.santacut.me`와 `http://localhost:3000`을 더한다. 자격 증명을 허용하므로 `*`는 쓸 수 없고, 넣으면 앱이 뜨지 않는다 |
| `app.session-same-site` (`APP_SESSION_SAME_SITE`) | `Lax` | 세션·CSRF 쿠키의 SameSite(`Strict`, `Lax`, `None`). `None`은 `APP_SESSION_SECURE=true`일 때만 쓸 수 있고, 아니면 앱이 뜨지 않는다. dev만 `None`으로 둬 `http://localhost:3000`에서도 쿠키가 실리게 한다 |
| `app.season-year` (`APP_SEASON_YEAR`) | `2026` | 시즌 연도. 그해 12월 25일 0시(KST)에 마감한다 |
| `app.origin-lat`, `app.origin-lon` (`SANTA_ORIGIN_LAT`, `SANTA_ORIGIN_LON`) | 여의도 좌표 | 산타 출발 좌표. 응답에 내보내지 않는다 |
| `santacut.ai.poll-delay-ms` | `3000` | 분석 작업자가 다음 작업을 찾기 전에 쉬는 시간 |
| `santacut.upload.poll-delay-ms` | `1000` | 원본 파일 검증 작업자가 다음 원본을 찾기 전에 쉬는 시간 |
| `santacut.ai.worker.concurrency` (`ANALYSIS_WORKER_CONCURRENCY`) | `3` | 동시에 채점하는 분석 작업 수. 작업 하나가 Gemini를 보통 3번 동시에 부르므로 동시 호출은 이 값의 3~6배다 |
| `spring.ai.google.genai.api-key` (`GEMINI_API_KEY`) | 없음 | Gemini Developer API 키. 없으면 앱이 뜨지 않는다 |
| `spring.ai.google.genai.chat.model` (`GEMINI_MODEL`) | `gemini-3.5-flash-lite` | 채점 모델. 비우면 AI 분석을 끈다 |
| `santacut.prompt-admin.enabled` (`PROMPT_ADMIN_ENABLED`) | `false` | dev 전용 프롬프트 관리 API(`/v1/admin/prompts`). dev의 `app.env`에만 `true`를 둔다. 운영은 두지 않아 404다 |
| `santacut.prompt-admin.token` (`PROMPT_ADMIN_TOKEN`) | 없음 | 관리 API 토큰. 요청은 `X-Admin-Token` 헤더에 이 값을 싣고, 앱은 시간 차이가 나지 않게 비교한다. 없거나 틀리면 `401`이다. 비어 있으면 `PROMPT_ADMIN_ENABLED=true`여도 API가 뜨지 않는다(`404`). dev `app.env`(KMS 암호화)에만 둔다. 예: `openssl rand -hex 32` |
| `springdoc.api-docs.enabled`, `springdoc.swagger-ui.enabled` (`SWAGGER_ENABLED`) | `true`(운영 프로필 `false`) | OpenAPI 명세(`/v3/api-docs`)와 Swagger UI. dev와 운영 모두 운영 프로필로 뜨므로 dev `app.env`에만 `SWAGGER_ENABLED=true`를 두고, 운영은 두지 않아 `404`다 |
| `santacut.rate-limit.registrations-per-client`, `registration-window` | `10`, `1h` | 클라이언트 IP 하나가 창 동안 만들 수 있는 참가자 수. 넘으면 `429 CLIENT_RATE_LIMITED`와 `retry_at` |
| `santacut.rate-limit.submissions-per-client`, `submission-window` | `20`, `24h` | 클라이언트 IP 하나가 창 동안 낼 수 있는 분석 제출 수(JSON·원본 파일 합산). 참가자별 24시간 3회 한도와 함께 본다 |
| `santacut.upload-intake.max-concurrent-uploads`, `retry-after` | `2`, `10s` | 동시에 메모리로 읽어 저장하는 원본 파일 수. 넘으면 기다리지 않고 `503 UPLOAD_BUSY`와 `Retry-After` |
| `santacut.request-body.max-size` | `2MB` | 파일 업로드가 아닌 요청 본문 상한. 넘으면 `413 REQUEST_TOO_LARGE` |
| `santacut.ai.budget.daily-usd` (`AI_BUDGET_DAILY_USD`) | `0`(끔) | 앱 안의 AI 비용 가드 하루 한도(달러). 0이면 하루 한도가 없다. Gemini를 선불 크레딧으로 쓰므로 기본으로 끈다 |
| `santacut.ai.budget.monthly-usd` (`AI_BUDGET_MONTHLY_USD`) | `9` | 앱 안의 AI 비용 가드 한 달 한도(달러). 호출 전 어림 비용을 더해 넘을 것 같아도 부르지 않는다. 넣지 않아도 된다 |

- 단가(입력·출력 100만 토큰당 0.3·2.5달러), 멈춤 시간(15분), 연속 실패 기준(3번)은 `application.yml`의 `santacut.ai.budget`에 있다.
- 출력 토큰 상한(`max-output-tokens`)은 250이고 `application.yml`에 있다. 3.5 Flash-Lite는 생각을 끌 수 없어 `thinking-level: MINIMAL`로 둔다. 생각 토큰은 출력 단가로 청구되고 이 상한을 함께 쓴다. `thinking-budget`을 넣으면 Gemini가 요청을 거절하므로 두지 않는다.
- Gemini 호출 하나의 시간 상한은 60초이고 SDK 재시도는 하지 않는다. `GeminiClientConfiguration`에 있다.
- 선불 크레딧이 떨어지면 Gemini가 `429 RESOURCE_EXHAUSTED`로 막는다. 작업은 실패하지 않고 `QUEUED`에 남으며 작업자는 15분씩 멈췄다 다시 시도한다. AI Studio에서 크레딧을 채우면 이어서 처리한다.

IP 한도는 앱 메모리에서 센다. 앱이 하나뿐이라 충분하지만 앱이 다시 뜨면 처음부터 센다. IP는 운영 프로필에서 Tomcat이 믿는 프록시(Caddy)가 붙인 `X-Forwarded-For`로 바꾼 값이다. Caddy는 밖에서 온 `X-Forwarded-For`를 믿지 않고 실제 접속 IP로 덮어쓰므로 클라이언트가 IP를 꾸밀 수 없다. 통신사 NAT처럼 여러 사람이 IP 하나를 함께 쓰면 한도에 먼저 닿을 수 있으니, 문의가 오면 한도를 올린다. 사람인지 확인(Turnstile 같은 캡차)은 후속 과제다.

`poll-delay-ms` 두 키는 Compose에 넣지 않았다. 바꿀 때는 `SANTACUT_AI_POLL_DELAY_MS`, `SANTACUT_UPLOAD_POLL_DELAY_MS` 환경 변수(Spring 완화 바인딩)를 앱 서비스에 더한다.

### 분석 처리량

2026-11-17에 dev(1GB, 2vCPU)에서 동시 5·15·30명이 짧은 대화(89줄)를 한꺼번에 올리게 했다. 참가자는 사람마다 따로 등록하고 끝나면 지웠다. 결과까지 걸린 시간은 업로드부터 `SUCCEEDED`를 확인할 때까지다(1초 간격 폴링).

| 동시 업로드 | 변경 전(작업자 1개) p50 / p95 | 변경 후(동시 3개) p50 / p95 / 최대 |
| --- | --- | --- |
| 5명 | 12~19초 / 22~28초 | 7.5초 / 9.6초 / 9.6초 |
| 15명 | 34초 / 59초 | 11.5초 / 16.2초 / 18.6초 |
| 30명 | 앱이 Metaspace OOM으로 다시 떠 측정 실패 | 17.3초 / 33.3초 / 33.4초 |

- 변경 후 시험에서는 오류가 없었고 Gemini 429와 작업자 멈춤도 없었다. 업로드 응답은 p95 0.8초 안이다. 앱 CPU는 평균 25%, 순간 최대 111%였다.
- 작업 하나의 Gemini 호출은 1~2초다. 변경 전에는 작업이 끝날 때마다 3초를 쉬고, 같은 스레드에서 원본 검증도 번갈아 해 작업당 약 4.2초가 걸렸다(시간당 약 850건).
- 지금은 채점 스레드 3개가 쉬지 않고 이어서 채점한다. 30명이 몰려도 33초 안에 끝나 시간당 약 3,200건을 처리한다. 이때 병목은 원본 파일 검증 작업자다(한 번에 하나, 1초 간격). 이벤트 제출(`POST /v1/analyses`)은 검증을 거치지 않는다.
- 시간당 300건(12초에 1건)이면 대기열이 거의 쌓이지 않는다. 결과까지는 5~10초다. 검증 대기 1초, 작업자 폴링 3초, 채점 2초 안팎을 더한 값이다.
- 동시 작업 수를 더 올리기 전에 Gemini 요청 한도(429)와 순위 재계산 시간을 본다. 순위 재계산은 작업마다 하나뿐인 DB 커넥션을 쥐고 줄을 선다. 참가자가 많아지면 이 부분이 먼저 느려진다.

### 업로드 크기 제한

원본 파일은 20MB까지 받는다. 앞단과 앱이 함께 막는다. 원본 파일 업로드(`/v1/analyses/files`)가 아닌 요청 본문은 2MB까지만 받는다.

| 위치 | 설정 | 값 |
| --- | --- | --- |
| `ci-cd/deploy/Caddyfile` | `request_body @upload max_size` | 21MB |
| `ci-cd/deploy/Caddyfile` | `request_body @notUpload max_size` | 2MB |
| `application.yml` | `santacut.request-body.max-size`(multipart가 아닌 요청의 Content-Length) | 2MB |
| `application.yml` | `spring.servlet.multipart.max-file-size` / `max-request-size` | 20MB / 21MB |
| `application.yml` | `server.tomcat.max-swallow-size` | 21MB |

Caddy 한도는 multipart 경계와 다른 파트를 감안해 파일 한도보다 1MB 크다. Tomcat `max-swallow-size`는 업로드를 거절한 뒤에도 남은 본문을 읽어, 연결을 끊지 않고 `413 FILE_TOO_LARGE` JSON을 보내게 한다. Caddy가 먼저 막으면 `code` 없는 `413`이 간다. 한도를 바꿀 때는 세 곳을 함께 바꾼다.

256MB 힙을 지키려고 원본 파일 접수는 이 순서로 한다. 시즌·참가자·같은 `Idempotency-Key`의 작업·하루 제출 한도·IP 한도를 먼저 보고, 통과한 요청만 파일 내용을 메모리로 한 번 읽는다. 요청 해시는 내용을 이어 붙이지 않고 해시에 바로 흘려 넣으며, 저장할 때도 복사하지 않는다. 내용을 읽고 저장하는 일은 동시에 2개(`santacut.upload-intake.max-concurrent-uploads`)까지만 하고, 넘으면 `503 UPLOAD_BUSY`로 바로 돌려보낸다. 업로드 원본은 `analysis_uploads` 테이블에 잠깐 머물다 검증이 끝나면 지워지므로 별도 볼륨은 필요 없다.

### 지역 시드

지역 데이터는 앱이 뜰 때마다 들어간다. `schema.sql`이 테이블과 서울 26행을 만들고, 이어서 `spring.sql.init.data-locations: classpath:regions.sql,classpath:prompts.sql`이 돈다. `regions.sql`은 나머지 시·도와 시·군·구를 넣고(모두 271개), `prompts.sql`은 채점 프롬프트(`score`) 1판을 넣는다. 셋 다 `INSERT OR IGNORE`라 기존 DB에는 새 지역만 들어가고, 이미 있는 프롬프트 판은 건드리지 않는다. `regions.parent_code`는 앱 시작 때 `RegionFacade`가 채운다. 별도 마이그레이션 단계는 없다. 이미 있는 지역의 이름·좌표를 바꾸려면 시드만으로는 안 되고 DB를 직접 고쳐야 한다.

## SQLite 백업과 복구

`backend/ci-cd/scripts/backup.sh`가 한 시간마다 돈다(cron 설치는 인프라 레포 7절).

1. 실행 중인 앱 컨테이너에서 `sqlite3 .backup`으로 일관된 복사본을 뜬다. 복사본은 데이터 볼륨의 `/app/data/backup-tmp/`에 뜨고, 서버로 옮긴 뒤 바로 지운다. 컨테이너 `/tmp`는 64MB tmpfs라 DB가 60MB를 넘으면 `.backup`이 실패해 배포까지 막혔기 때문이다. 그래서 볼륨에는 DB 크기만큼 빈 공간이 더 있어야 한다.
2. 서버에서 `PRAGMA integrity_check`로 확인하고 `/opt/santacut/backups/santacut-<시각>.sqlite`(600)로 둔다.
3. rclone으로 `$BACKUP_REMOTE/santacut-<시각>.sqlite`(버킷의 `backups/sqlite/`)에 올린다. 암호화는 S3가 환경 KMS 키로 한다.
4. 올린 파일을 내려받아 바이트 단위로 대조한다(`rclone check --download`). 이게 통과해야 성공이다.
5. 서버의 로컬 복사본은 2일만 남긴다.

`VACUUM`은 자동으로 돌리지 않는다. DB 크기만큼 공간을 더 쓰고 도는 동안 쓰기를 모두 막아, 연결이 하나뿐인 앱이 그동안 멈추기 때문이다. 순위 스냅숏은 계산할 때마다 지우고 다시 쓰므로 빈 페이지는 SQLite가 다시 쓴다. 파일이 지나치게 커졌으면 점검 시간에 앱을 멈추고 `sqlite3 santacut.db 'VACUUM;'`을 한 번 돌린다.

`/opt/santacut/backup.env`(600)에 `BACKUP_REMOTE`를 둔다. 예시는 `backend/ci-cd/deploy/backup.env.example`이다. 비밀값은 없다.

- 버킷 정책이 SSE-KMS와 환경 키 ARN을 둘 다 요구한다. 그래서 remote에 `server_side_encryption='aws:kms'`, `sse_kms_key_id='<키 ARN>'`이 꼭 있어야 한다.
- `upload_cutoff=5G`로 멀티파트 업로드를 피한다.
- 앱 사용자는 `backups/*`에만 쓸 수 있다.

age 암호화는 뺐다. 2026-11 전에 age로 만든 `sqlite/*.age` 백업만 PC에서 `age -d -i <개인키 파일>`로 푼다.

복구는 인프라 레포 `docs/operations.md` 9절 "S3 백업에서 SQLite 복원"을 그대로 따른다. 2026-11-17 dev에서 그 절차로 리허설했다(앱 중단 37초, 전체 56초, 복원 전후 지문 일치). 리허설에서 드러나 그 문서에 반영한 점은 다음과 같다.

- 서버에서 `docker compose`를 직접 부를 때는 `APP_IMAGE=$(cat /opt/santacut/current-image)`를 먼저 export한다. `compose.yml`이 이 값을 요구해서, 없으면 `stop`부터 보간 오류로 실패한다.
- 운영 볼륨에 바로 덮어쓰지 않는다. 내려받은 파일의 `PRAGMA integrity_check`가 `ok`인지부터 본다.
- 앱과 Caddy를 함께 멈춘다. 다시 띄울 때는 앱이 healthy가 된 뒤 Caddy를 띄운다.
- 바꾸기 전 DB는 `/opt/santacut/backups/pre-restore-<시각>.sqlite`(600)로 남긴다. 볼륨의 새 DB는 UID 10001 소유, 600으로 둔다.
- 오래된 백업은 운영 DB가 아니라 임시 디렉터리에 받아 점검한다. 3시간 전 객체와 가장 오래된 객체 모두 KMS로 풀렸고 `ok`였다.

`ddl-auto: update`는 스키마를 자동으로 되돌리지 않는다. 컬럼 삭제·변경이 든 릴리스는 이전 앱 이미지만으로 롤백된다고 가정하지 않는다. 배포 전 백업은 `deploy.sh`가 두 번째 배포부터 자동으로 뜬다.

## 아직 운영 전에 필요한 것

- 운영(`main`) 인프라 켜기와 `api.santacut.me` 레코드, 운영용 `app.env`(인프라 레포 12절)
- 운영 배포용 GitHub secrets와 production environment
- 카카오톡·인스타그램 실제 내보내기 샘플로 원본 파서 검증(지금 파서는 공개 예시로 만들었다)
