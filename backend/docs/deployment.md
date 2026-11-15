# 초저예산 서버 배포 운영서

## 배포 구조와 전제

도메인은 가비아에서 별도 구매하고 서버는 월 20,000원 이내를 목표로 한다. `backend/ci-cd/deploy/compose.yml`은 Caddy, Spring Boot 앱, SQLite 파일 저장소로 구성된다. PostgreSQL 컨테이너는 사용하지 않는다. SQLite 파일은 Docker named volume에 보관한다.

앱은 Spring AI를 통해 Ollama에 연결한다. 2GB급 저가 서버에서 앱과 `qwen2.5:3b`를 동시에 운영하기에는 메모리가 부족하므로, 운영 전용 사설 Ollama 주소를 준비해 `OLLAMA_BASE_URL`에 넣거나 서버 예산을 다시 산정해야 한다. Ollama 포트를 인터넷에 직접 공개하지 않는다. 로컬 개발은 `http://localhost:11434` 기본값을 사용한다.

SQLite 단일 파일은 작은 단일 인스턴스 서비스에 적합하다. 여러 앱 인스턴스를 동시에 띄우거나 여러 서버에서 같은 파일을 공유하지 않는다. DB 파일과 세션을 외부에 공개하지 않는다.

## 서버 준비

Ubuntu LTS x86_64, Docker Engine/Compose, 고정 공인 IPv4를 준비한다. 보안 그룹은 80/443을 공개하고 SSH 접근은 운영자 IP로 제한한다. `deploy` 사용자는 배포 전용으로 만들고 Docker 그룹의 root 권한 범위를 인지한다.

```bash
sudo apt-get update
sudo apt-get install -y curl age rclone
sudo adduser --disabled-password --gecos '' deploy
sudo usermod -aG docker deploy
sudo install -d -o deploy -g deploy -m 750 /opt/santacut /opt/santacut/releases
```

가비아 DNS의 `@` A 레코드를 서버 IPv4로 지정한다. 전파 후 80/443 연결을 확인한다. `APP_PUBLIC_BASE_URL`은 실제 HTTPS 도메인으로 설정된다.

## 환경 설정과 배포

`backend/ci-cd/deploy/.env.example`을 `/opt/santacut/.env`로 복사해 도메인, 이메일, Ollama 사설 주소, 모델을 설정하고 권한을 제한한다.

```bash
chmod 600 /opt/santacut/.env
```

GitHub Actions는 PR과 main에서 검증하고, main 커밋 이미지를 GHCR에 게시한다. 운영 배포는 `workflow_dispatch`에서 `deploy=true`를 선택한다. GitHub production environment에 `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS`를 등록해야 한다. 배포 스크립트는 검증한 image digest를 사용하고 readiness를 확인한다.

현재 기본 세션 쿠키 보안은 로컬 HTTP 실행에 맞춰 꺼져 있다. Compose는 HTTPS 도메인 사용을 전제로 `APP_SESSION_SECURE=true`로 설정한다.

### 운영 설정 키

| 키(환경 변수) | 기본값 | 뜻 |
| --- | --- | --- |
| `app.public-base-url` (`APP_PUBLIC_BASE_URL`) | `http://localhost:8080` | 공유 링크·Open Graph에 쓰는 공개 출처. 경로 없는 URL이어야 한다 |
| `app.session-secure` (`APP_SESSION_SECURE`) | `false` | 세션 쿠키 Secure·`__Host-` 이름 사용 |
| `app.season-year` (`APP_SEASON_YEAR`) | `2026` | 시즌 연도. 그해 12월 25일 0시(KST)에 마감한다 |
| `app.origin-lat`, `app.origin-lon` (`SANTA_ORIGIN_LAT`, `SANTA_ORIGIN_LON`) | 여의도 좌표 | 산타 출발 좌표. 응답에 내보내지 않는다 |
| `santacut.ai.daily-call-limit` (`SANTACUT_AI_DAILY_CALL_LIMIT`) | `100` | 서울 날짜 기준 하루 AI 호출 상한 |
| `santacut.ai.poll-delay-ms` | `3000` | 분석 작업자가 다음 작업을 찾기 전에 쉬는 시간 |
| `santacut.upload.poll-delay-ms` | `1000` | 원본 파일 검증 작업자가 다음 원본을 찾기 전에 쉬는 시간 |
| `OLLAMA_BASE_URL`, `OLLAMA_MODEL` | `http://localhost:11434`, `qwen2.5:3b` | Spring AI Ollama 연결 |

`poll-delay-ms` 두 키는 Compose에 넣지 않았다. 바꿀 때는 `SANTACUT_AI_POLL_DELAY_MS`, `SANTACUT_UPLOAD_POLL_DELAY_MS` 환경 변수(Spring 완화 바인딩)를 앱 서비스에 더한다.

### 업로드 크기 제한

원본 파일은 20MB까지 받는다. 앞단과 앱이 함께 막는다.

| 위치 | 설정 | 값 |
| --- | --- | --- |
| `ci-cd/deploy/Caddyfile` | `request_body max_size` | 21MB |
| `application.yml` | `spring.servlet.multipart.max-file-size` / `max-request-size` | 20MB / 21MB |
| `application.yml` | `server.tomcat.max-swallow-size` | 21MB |

Caddy 한도는 multipart 경계와 다른 파트를 감안해 파일 한도보다 1MB 크다. Tomcat `max-swallow-size`는 업로드를 거절한 뒤에도 남은 본문을 읽어, 연결을 끊지 않고 `413 FILE_TOO_LARGE` JSON을 보내게 한다. Caddy가 먼저 막으면 `code` 없는 `413`이 간다. 한도를 바꿀 때는 세 곳을 함께 바꾼다. 업로드 원본은 `analysis_uploads` 테이블에 잠깐 머물다 검증이 끝나면 지워지므로 별도 볼륨은 필요 없다.

### 지역 시드

지역 데이터는 앱이 뜰 때마다 들어간다. `schema.sql`이 테이블과 서울 26행을 만들고, 이어서 `spring.sql.init.data-locations: classpath:regions.sql`이 나머지 시·도와 시·군·구를 넣는다(모두 271개). 둘 다 `INSERT OR IGNORE`라 기존 DB에는 새 지역만 들어간다. `regions.parent_code`는 앱 시작 때 `RegionFacade`가 채운다. 별도 마이그레이션 단계는 없다. 이미 있는 지역의 이름·좌표를 바꾸려면 시드만으로는 안 되고 DB를 직접 고쳐야 한다.

## SQLite 백업과 복구

`backend/ci-cd/scripts/backup.sh`는 실행 중인 앱 컨테이너의 `sqlite3 .backup`으로 일관된 파일 복사본을 만들고, age로 암호화한 다음 rclone remote에 전송한다. `/opt/santacut/backup.env`에 `BACKUP_AGE_RECIPIENT`와 `BACKUP_REMOTE`를 설정한다. 이 파일은 git에 넣지 않는다. 서버 밖 백업이 확인되기 전에는 백업 성공으로 처리하지 않는다.

복구는 운영 볼륨에 바로 덮어쓰지 않고 격리된 디렉터리에서 수행한다.

1. 암호화 파일을 서버 밖에서 내려받고 checksum을 확인한다.
2. age 개인키로 복호화하고 `sqlite3 restored.db 'PRAGMA integrity_check;'`를 실행한다.
3. 임시 앱에 복원해 readiness, 참가자 수, 랭킹 및 공유 페이지를 확인한다.
4. 검증 후 유지보수 시간에 볼륨을 교체하고 앱을 다시 확인한다.

`ddl-auto: update`는 스키마를 자동 되돌리지 않는다. 배포 전 파일 백업을 확인하고, 컬럼 삭제·변경을 포함한 릴리스는 이전 앱 바이너리만으로 롤백할 수 있다고 가정하지 않는다. 최소 목표 복구 시점은 24시간, 복구 시간은 2시간으로 잡되 복구 훈련 전에는 보장하지 않는다.

## 아직 운영 전에 필요한 것

- 실제 도메인·서버와 DNS 설정
- 사설 네트워크를 통한 Ollama 주소 또는 예산에 맞는 AI 제공 방식
- 배포용 GitHub secrets와 production environment
- 암호화 백업 remote 및 복구 리허설
- 카카오톡·인스타그램 실제 내보내기 샘플로 원본 파서 검증(지금 파서는 공개 예시로 만들었다)
