# 실제 파일 분석 흐름

## 사용자 흐름

1. `/start`에서 카카오톡 또는 인스타그램 출처를 선택한다.
2. `/upload`에서 파일을 브라우저에서 검증하고, 대화 속 본인 화자를 선택한다.
3. `/join`에서 참가 정보를 등록하거나 기존 세션을 확인한다.
4. `/review`에서 참가자·출처·파일·화자를 확인하고, 두 가지 전송 동의에 체크한다.
5. 동의 후에만 `POST /v1/analyses/files`를 한 번 호출한다.
6. `/analysis`에서 상태 SSE를 구독한다. SSE가 끊기면 연결 종료를 안내하고 사용자가 같은 작업의 SSE를 다시 연결한다. 주기적인 상태 폴링은 하지 않는다.

진입 시 작업 확인용 GET 이후 상태 변경은 SSE로 받는다. Context에 작업 ID가 없으면 최근 작업을 조회하고 ID가 생기면서 상태 조회가 추가 실행될 수 있다. 재연결은 새 분석을 제출하지 않는다. 완료·실패 상태 및 화면 이탈 시 스트림을 닫는다.

## 개인정보와 재시도

- 파일 원문은 브라우저 메모리에만 보관하며 새로고침하면 다시 선택해야 한다.
- 서버 전송 전에는 파일 내용을 전송하지 않는다.
- 업로드 요청에는 `Idempotency-Key`를 사용한다. 네트워크 오류 때 자동으로 새 제출을 만들지 않는다.
- 화자를 바꾸거나 파일을 다시 선택하면 새 키를 만든다.
- 분석이 실패하면 같은 파일을 자동 재전송하지 않고, 사용자가 파일을 다시 선택한다.

## 상태 표시

`VALIDATING` → `QUEUED` → `ANALYZING` → `RANKING_PENDING` → `SUCCEEDED` 또는 `FAILED` 상태를 그대로 표시한다. `INSUFFICIENT_EVIDENCE`, `SPEAKER_NOT_IDENTIFIED`, `UNSUPPORTED_FILE_FORMAT` 등 서버 오류 코드는 사용자 행동 안내로 변환한다.

`/result`는 실제 결과 API, `/ranking`은 실제 랭킹 API와 연결되어 있다. 단, 현재 실제 `/analysis`의 성공 상태에는 결과 이동 버튼이나 자동 이동이 없고 준비 중 문구가 남아 있다. 결과 페이지 직접 진입은 가능하지만 정상 완료 동선 연결은 후속 코드 작업이다.

원본 파일 분석은 본인 메시지를 채점하되 상대 메시지도 맥락으로 Gemini에 전송한다. 프론트 동의 문구에는 이 범위가 아직 충분히 반영되지 않았다. 현재 문구만으로 본인 말만 전송된다고 해석하면 안 된다. 정확한 범위는 [API 가이드 §6.1](../../docs/api/frontend-api-guide.md#61-원본-파일-업로드-post-v1analysesfiles)를 따른다.

## 검증 명령

```bash
cd frontend
NEXT_TELEMETRY_DISABLED=1 pnpm lint
NEXT_TELEMETRY_DISABLED=1 pnpm typecheck
pnpm test -- --runInBand
pnpm test:e2e
```
