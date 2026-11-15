# 초저예산 서버 배포 운영서

## 배포 구조와 전제

도메인은 가비아에서 별도 구매하고 서버는 월 20,000원 이내를 목표로 한다. `deploy/compose.yml`은 Caddy, Spring Boot 앱, SQLite 파일 저장소로 구성된다. PostgreSQL 컨테이너는 사용하지 않는다. SQLite 파일은 Docker named volume에 보관한다.

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

`deploy/.env.example`을 `/opt/santacut/.env`로 복사해 도메인, 이메일, Ollama 사설 주소, 모델을 설정하고 권한을 제한한다.

```bash
chmod 600 /opt/santacut/.env
```

GitHub Actions는 PR과 main에서 검증하고, main 커밋 이미지를 GHCR에 게시한다. 운영 배포는 `workflow_dispatch`에서 `deploy=true`를 선택한다. GitHub production environment에 `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS`를 등록해야 한다. 배포 스크립트는 검증한 image digest를 사용하고 readiness를 확인한다.

현재 기본 세션 쿠키 보안은 로컬 HTTP 실행에 맞춰 꺼져 있다. Compose는 HTTPS 도메인 사용을 전제로 `APP_SESSION_SECURE=true`로 설정한다.

## SQLite 백업과 복구

`scripts/backup.sh`는 실행 중인 앱 컨테이너의 `sqlite3 .backup`으로 일관된 파일 복사본을 만들고, age로 암호화한 다음 rclone remote에 전송한다. `/opt/santacut/backup.env`에 `BACKUP_AGE_RECIPIENT`와 `BACKUP_REMOTE`를 설정한다. 이 파일은 git에 넣지 않는다. 서버 밖 백업이 확인되기 전에는 백업 성공으로 처리하지 않는다.

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
- 전국 시·군·구 좌표와 카카오톡·인스타그램 내보내기 원본 파서
