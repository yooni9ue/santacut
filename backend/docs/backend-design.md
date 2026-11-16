# 산타컷 백엔드 설계·기획

> 너 올해 산타컷 통과 가능?

문서 버전: 0.2 / 작성일: 2026-11-15 / 구현 반영: 2026-11-16

이 문서는 구현 전 설계에서 출발했다. 0.2부터는 현재 코드(`backend/src`)에 맞췄다. 구현과 달라진 설계는 지우지 않고 **[대체됨]** 표시를 붙여 남긴다. 그때 그렇게 정한 까닭도 기록으로 두기 위해서다. 현재 동작은 각 절의 "현재 구현"과 §9·§10·§14를 따르고, 필드 정의는 OpenAPI 명세(`/v3/api-docs`)를 따른다. 미확정 항목은 §12에 모았다.

## 1. 서비스 정의와 범위

사용자가 카카오톡 대화 기반 JSON 또는 인스타그램 활동 JSON을 제출하면 AI가 유쾌한 산타 점수와 심사평을 생성한다. 모든 사용자는 하나의 랭킹에 참여한다. 산타는 점수 순서대로 지역 중심지에 방문하며, 실제 지역 간 거리와 활동 시간을 반영해 선물 도착 예정 날짜·시간을 계산한다.

핵심 기능:

1. DB에서 중복을 막는 유니크 닉네임 등록.
2. 거주 지역 선택: 시·도 및 시·군·구. 구가 없는 지역도 선택 가능해야 한다.
3. 데이터 받기 안내와 카카오톡·인스타그램 선택.
4. 파일 업로드, 형식 검증, 정규화, AI 채점.
5. 100점 만점 소수점 둘째 자리 점수와 단일 통합 랭킹.
6. 순위별 선물 도착 예정 일시 계산.
7. 결과 페이지와 공유 카드.

친구 익명 투표와 친구별 리그는 현재 MVP 범위에 포함하지 않는다. 결과는 오락용 해석이며 실제 선물 배송을 제공하는 서비스는 아니다.

## 2. 사용자 흐름

닉네임·지역 등록 → 내 데이터 가져오기 → 출처 선택 → 데이터 내보내기 안내 → 파일 업로드 → AI 심사 대기 → 점수·랭킹·도착 예정일 확인 → 공유.

닉네임은 식별용이며 인증 수단이 아니다. MVP 기본안은 서버 발급 익명 사용자 ID와 HttpOnly/Secure 세션 쿠키로 소유권을 확인하는 방식이다. 닉네임만 알아서 타인의 결과를 덮어쓸 수 없어야 한다. 쿠키 분실 시 복구와 기기 간 연결은 별도 계정 기능 도입 전 지원 범위를 안내한다.

## 3. 데이터 받기 및 업로드

### 3.1 안내 페이지

공통 진입 버튼은 `내 데이터 가져오기` 하나다. 이후 카카오톡과 인스타그램 중 하나를 선택한다. 입력 경로만 다르고 채점·랭킹·공유 기능은 공통이다.

- 카카오톡: 기기별 공식 내보내기 경로를 안내한다. 원본이 TXT인 경우 검증된 파서로 내부 JSON에 변환한다. 사용자가 자신의 화자를 선택해야 한다.
- 결정(2026-11-16): 본인 메시지만 분석한다. 다른 참여자의 메시지는 맥락으로도 쓰지 않고 버리며 AI에 보내지 않는다. 인스타그램도 본인 이름을 고른다(`message_1.json`에 소유자 표시가 없다).
- 인스타그램: 데이터 형식 JSON, 기간, 필요한 항목을 설명하고 공식 계정 센터의 정보 내보내기로 연결한다. 외부 경로는 출시 전 실제 계정·모바일에서 확인한다.
- 안내 페이지에 `파일 준비됐어요 · 업로드하기`를 제공한다.
- 버튼 클릭만으로 파일이 즉시 다운로드된다고 약속하지 않는다. 외부 서비스의 생성 대기·로그인·내보내기 절차를 안내한다.
- ‘인스타 알고리즘 JSON’은 서비스 내 통칭이다. 추천 알고리즘 자체나 모든 추천 콘텐츠가 내보내기에 포함된다고 가정하지 않는다. 실제 샘플에서 지원 가능한 활동 필드를 먼저 확정한다.

### 3.2 검증·정규화 기본안

초기에는 JSON과 지원 확인된 카톡 TXT만 허용한다. ZIP을 지원할 경우 압축 해제 총량·파일 수·경로 탈출 검증을 별도로 구현한다.

- 원본 파일 최대 20MB, 분석 범위 최근 90일, 최대 2,000개 이벤트를 초기 제한값으로 제안한다.
- 파일 확장자뿐 아니라 실제 인코딩·내용·스키마·날짜를 검증한다.
- 본문 내 명령문은 분석 대상 데이터로 취급하며 AI 시스템 지시로 실행하지 않는다.
- 시간순·일자별 고정 샘플링으로 재실행 시 같은 이벤트를 선택한다. 토큰 제한 초과 시 같은 규칙으로 축소한다. 지금은 주별 고정 표본으로 구현했다(§4.4).
- 내부 공통 이벤트: source, occurred_at, actor_role, event_type, text, context. 출처별 원본 구조는 어댑터에서 변환한다.
- 관련 없는 개인정보를 줄이고 원문은 공개 응답·로그·공유 카드에 포함하지 않는다.
- 샘플 부족과 미지원 스키마는 명시적 오류로 돌려준다. 빈 데이터를 낮은 점수로 채점하지 않는다.
- 판정 가능한 최소 데이터 기준은 출처별 샘플 검증 후 확정한다.

현재 구현:

- 입력 경로는 둘이다. 원본 파일 업로드(`POST /v1/analyses/files`, multipart)와, 브라우저가 정규화한 이벤트 JSON(`POST /v1/analyses`)이다.
- 원본 파일은 카카오톡 TXT(PC 형식, iOS·Android 모바일 형식)와 인스타그램 `message_1.json`만 받는다. ZIP, CSV, EUC-KR은 받지 않는다. 파일 이름·확장자·Content-Type은 보지 않고 내용만 본다.
- 접수 단계에서는 빈 파일, ZIP(첫 4바이트 `PK\x03\x04`), 앞 4,096바이트에 NUL이 있는 바이너리만 바로 `422 UNSUPPORTED_FILE_FORMAT`으로 거절한다. 엄격한 UTF-8 디코딩과 파싱은 검증 작업자 몫이다.
- 크기 한도는 20MB다. Spring multipart `max-file-size: 20MB`·`max-request-size: 21MB`, Tomcat `max-swallow-size: 21MB`, Caddy `request_body max_size 21MB`가 함께 막는다. 서버가 막으면 `413 FILE_TOO_LARGE`, 프록시가 막으면 `code` 없는 `413`이 간다.
- 정규화하면 본인 메시지만 담은 이벤트(`occurredAt`, `text`) 목록이 나온다. 최근 90일, 최신 2,000개, 누적 200,000자까지 쓰고 메시지 하나는 2,000자에서 자른다. 20개가 안 되면 `INSUFFICIENT_EVENTS`다.
- 이 한도는 파싱과 저장에만 쓴다. AI에는 이벤트 전체가 아니라 항목별 낱말 신호와 작은 표본만 보낸다(§4.4).
- **[대체됨]** 위의 "내부 공통 이벤트: source, occurred_at, actor_role, event_type, text, context"는 구현하지 않았다. 다른 참여자 메시지를 버리기로 하면서(§3.1 결정) `actor_role`과 `context`가 쓸모없어졌다. `source`는 작업마다 한 번만 저장한다.

## 4. AI 점수와 유쾌한 결과

### 4.1 점수 기본안

점수는 0.00~100.00이고 DB에는 0~10000 정수(score_hundredths)로 저장한다. API와 화면은 항상 소수점 둘째 자리로 표시한다. 항목별 소수점을 가중 합산하고 최종 단계에서 한 번 반올림한다. 랜덤 소수점을 붙여 순위를 조작하지 않는다.

아래 항목·가중치는 구현 검증용 초안이며 최종 확정된 평가척도가 아니다.

| 내부 평가 항목 | 가중치 | 공개 표현 예시 |
| --- | ---: | --- |
| 배려·공감 | 30% | 천사력 |
| 긍정·응원 | 25% | 분위기 살리기력 |
| 존중하는 소통 | 25% | 급발진 브레이크력 |
| 나눔·협력 | 20% | 선물 받을 명분 |

각 항목은 0~100 범위의 구조화된 값과 근거 이벤트 ID를 반환한다. 점수 합산은 AI가 아닌 서버가 수행한다. AI는 판정명과 짧은 심사평도 생성하되 원문·개인정보를 직접 인용하지 않는다.

카톡·인스타를 동일한 모델 버전·척도에 넣되, 근거 유형은 다르게 해석한다. 근거가 없는 항목을 자동 0점으로 처리하지 않는다. 근거 부족 시 순위 등록을 보류한다. 단순 관심사나 콘텐츠 취향만으로 인성을 추정하지 않으며, 인스타 내보내기에 평가 가능한 증거가 부족하면 지원 필드·평가척도를 재설계한다.

카톡과 인스타에서 대표 샘플을 모아 점수 편향을 검증해야 한다. 데이터 출처만으로 유리해지지 않는지 평가한 뒤 통합 랭킹을 출시한다. 데이터 양·팔로워 수·파일 개수로 가산점을 주지 않는다.

### 4.2 결과 톤

정색한 도덕 성적표가 아닌 친구끼리 공유할 수 있는 장난스러운 산타 판정이다.

- 97.82점: `산타 친자 의혹` / `이 정도면 선물 말고 상속을 기다려라.`
- 61.09점: `굴뚝 앞 배송 보류` / `산타가 주소를 다시 확인 중.`
- 늦은 배송: `크리스마스 선물인데 새해 복 많이 받아라.`

예시는 실제 점수 구간 확정안이 아니다. 성격·민감한 속성·정신건강을 단정하지 않고 기록에서 드러난 가벼운 행동을 소재로 삼는다. 도착 시각은 AI가 만들지 않고 배송 엔진의 계산값을 사용한다.

### 4.3 재현성과 재업로드

모델, 프롬프트, 파서, 점수 규칙 버전을 저장한다. 낮은 temperature만으로 결정성을 보장하지 않으며 검증된 결과 캐시를 사용한다.

현재 구현: 평가(`assessments.prompt_version`)에는 채점 프롬프트 판 번호만 남긴다. 모델·파서·점수 규칙 버전과 결과 캐시는 아직 없다(§14.5).

캐시 키는 사용자 ID + 원본 해시 + 선택한 본인 화자 + 분석 범위 + 파서/모델/프롬프트/점수 버전이다. 원본이 같아도 화자가 다르면 같은 결과를 사용하지 않는다.

사용자·시즌당 활성 점수는 하나다. 최신 업로드 요청이 성공하면 기존 결과를 교체한다. 실패 시 이전 정상 결과를 유지한다. 오래 걸린 이전 작업이 나중에 끝나 최신 요청 결과를 덮어쓰지 못하도록 요청 순번을 비교한다. 두 출처 모두 제출했다고 보너스나 점수 합산을 하지 않는다.

### 4.4 AI 입력, 응답 계약, 재사용, 비용 가드

카카오톡·인스타그램 내보내기는 크다. 예전에는 이벤트를 하나씩 JSON 객체로 보냈다. 2,000개짜리 내보내기면 사용자 메시지만 17만~24만 자였다. 지금은 채점 작업자가 저장한 이벤트로 항목별 신호와 표본을 만들어 그 둘만 보낸다.

- 신호와 표본은 도메인 모델(`ScoringMessages`, `SampleCandidates`)이 만든다. AI 클라이언트는 그것을 글로 옮기기만 한다.
- 모두 본인 메시지에서만 만든다. 다른 참여자의 메시지는 정규화할 때 이미 버렸다.
- 날짜와 주는 한국 시각 기준이다. 주는 월요일에 시작한다.

사용자 메시지는 이렇게 생겼다.

```text
[signals]
care_empathy hit=41 weeks=9/13 shown=12
positivity_support hit=88 weeks=13/13 shown=30
respectful_communication hit=12 weeks=6/13 shown=5
cooperation_sharing hit=30 weeks=10/13 shown=11
[sample]
# 11-09주
17|내일 몇 시에 만나? 같이 저녁 먹자
```

- `hit`은 그 항목 낱말이 든 본인 메시지 수, `weeks=a/b`는 그런 메시지가 있는 주 수와 메시지가 있는 전체 주 수, `shown`은 표본에서 낱말이 든 줄 수다.
- 표본 줄은 `번호|본문`이다. 시각은 보내지 않는다. 채점 프롬프트가 활동 시간대를 점수에 쓰지 못하게 하므로 쓰이지 않는 토큰이다. 주가 바뀔 때만 `# 11-09주` 머리줄을 하나 넣는다.
- 메시지 수, 보낸 날 수, 시간대 분포, 연속 일수, 웃음·이모지 비율(`ScoringFeatures`)은 서버에서 계산하지만 AI에는 보내지 않는다. 지금은 사용량 로그에 메시지 수만 남긴다.

항목 낱말은 `ScoreTheme`에 상수로 둔다. 활용형도 걸리도록 어간으로 적는다.

| 항목 | 낱말 |
| --- | --- |
| `care_empathy` | 괜찮, 걱정, 힘들, 조심, 아프 |
| `positivity_support` | 고마, 고맙, 좋아, 사랑, 응원, 화이팅, 최고 |
| `respectful_communication` | 미안, 죄송, 감사, 부탁 |
| `cooperation_sharing` | 약속, 만나, 만날, 같이, 도와, 나눠 |

표본은 이렇게 고른다.

1. 공백, 문장 부호, 기호·이모지, ㅋ·ㅎ·ㅠ·ㅜ를 빼고 2자가 안 되는 메시지는 버린다.
2. 메시지 하나는 200자에서 자른다.
3. 달력 주로 나눈다. 주마다 항목 낱말이 든 메시지를 먼저, 그다음 이른 시각 순서로 줄 세운다.
4. 모든 주의 첫째, 모든 주의 둘째 순서(라운드 로빈)로 뽑는다. 120개나 본문 6,000자에 닿기 전에 멈춘다.
5. 뽑은 메시지를 시각순으로 놓고 1부터 번호를 붙인다. AI는 이 번호를 근거로 돌려준다.

- 무작위를 쓰지 않는다. 같은 제출이면 늘 같은 표본이 나온다. 그래서 작업 ID로 시드를 만들 필요가 없다.
- 표본이 8줄보다 적거나 본문이 300자보다 짧으면 AI를 부르지 않고 `INSUFFICIENT_EVIDENCE`로 끝낸다. 문턱은 일부러 낮게 잡았다. 사용량 로그가 쌓이면 다시 정한다.
- 채점 프롬프트 2판이 이 형식을 설명한다. 1판은 이벤트 JSON을 받던 때의 기록으로 남는다.

2,000개짜리 합성 내보내기로 잰 사용자 메시지 크기는 다음과 같다. `AnalysisScoringServiceTest`가 7,000자 상한을 지키는지 확인한다. 토큰 수는 실측이 아니라 한글 1자 1토큰, 그 밖의 3자 1토큰으로 어림한 값이다.

| 입력 | 예전 JSON | 지금 | 어림 토큰(예전 → 지금) |
| --- | ---: | ---: | ---: |
| 짧은 메시지 위주(본문 약 4만 3천 자) | 170,289자 | 4,043자 | 약 7만 4천 → 약 2천 7백 |
| 메시지마다 100자(본문 약 11만 자) | 238,670자 | 6,607자 | 약 13만 → 약 5천 |

시스템 프롬프트 2판은 1,402자(어림 약 900토큰)다.

#### 응답 계약과 근거 보정

- 응답은 `{"sufficient":true,"evidence":{...},"scores":{...}}`다. 근거는 항목마다 표본 번호 1~3개, 점수는 0~100 정수로 받는다. 서버는 소수 둘째 자리까지 받아들인다.
- 출력 토큰 상한(`max-tokens`)은 250이다. 정상 답은 100토큰 안팎이다.
- 범위 밖 번호와 항목 안에서 겹친 번호는 지운다. 지운 뒤에도 모든 항목에 근거가 남으면 통과시킨다. 다시 묻지 않는다. 지운 수는 사용량 로그에 남는다.
- 출력 한도에 걸려 잘린 JSON은 `AI_RESPONSE_INVALID`다.

#### 같은 입력 재사용

- 채점 입력 해시는 모델 ID, 프롬프트 판, AI 사용자 메시지를 이은 값의 SHA-256이다. `analysis_jobs.digest_hash`에 남긴다.
- 같은 참가자가 같은 해시로 다시 내면 AI를 부르지 않는다. 지난 작업이 성공했으면 그 점수를 새 평가로 옮겨 적고 순위를 다시 계산한다. 근거 부족이나 응답 오류(`AI_RESPONSE_*`, `AI_EVIDENCE_INVALID`, `AI_SCORE_OUT_OF_RANGE`)로 끝났으면 같은 이유로 끝낸다.
- 호출 오류, 시간 초과, 제공자 차단으로 끝난 작업에는 해시를 남기지 않는다. 다시 내면 다시 부른다.
- 참가자끼리는 결과를 나누지 않는다.

#### 사용량 기록과 앱 안의 비용 가드

- 호출마다 응답 메타데이터의 입력·출력 토큰과 종료 사유를 읽는다. 작업 ID, 프롬프트 판, 메시지 수, 표본 줄 수·글자 수, 근거 보정 수, 결과 코드와 함께 숫자만 로그에 남긴다.
- 토큰 수는 날짜(한국)별로 `ai_usage_days`에 더한다. 토큰 수에 설정 단가(입력 100만 토큰당 1달러, 출력 5달러)를 곱해 하루·한 달 비용을 어림한다.
- 하루 0.5달러나 한 달 9달러에 닿으면 작업을 잡지 않는다. 작업은 `QUEUED`로 남고, 24시간이 지나면 만료가 정리한다. 한도와 단가는 설정(`santacut.ai.budget`)에 있다.
- 제공자가 권한 거부·요청 한도·할당량·일시 중단으로 막으면 작업을 실패시키지 않는다. 리스를 풀어 `QUEUED`로 되돌리고 15분 동안 작업을 잡지 않는다. 근거 부족이 아닌 실패가 3번 연달아 나도 15분 멈춘다. 멈춤 상태는 앱 메모리에만 있다.
- Bedrock 호출 하나(SDK 재시도 포함)는 60초에서 끊는다. 작업 리스(3분)보다 짧아야 리스가 끝난 뒤 다른 시도가 같은 작업을 또 부르지 않는다.
- 단가는 설정값이다. global 추론 프로파일의 실제 청구 단가와 다를 수 있다. AWS Bedrock 예산 차단(santacut-infra)은 그대로 마지막 방어선이다.

## 5. 랭킹과 동점 처리

정렬 기준 초안:

1. 최종 표시 점수 내림차순.
2. 배려·공감 점수 내림차순.
3. 존중하는 소통 점수 내림차순.
4. 긍정·응원 점수 내림차순.
5. 나눔·협력 점수 내림차순.

모든 항목까지 같은 경우 공개 순위는 공동 순위로 표기한다. 예: 1, 2, 2, 4. 배송은 순차 처리해야 하므로 공동 순위 안에서는 시즌 최초 심사 성공 시각, 사용자 UUID 순으로 안정적인 배송 순번을 정한다. 공동 순위여도 도착 시간이 다를 수 있음을 결과 도움말에 안내한다.

공개 rank와 고유 delivery_sequence를 분리한다. 참가자가 늘어도 기존 참가자의 점수 자체를 백분위로 바꾸지 않는다. 랭킹은 cursor 페이지네이션으로 제공하고 별도로 내 순위를 조회한다.

현재 구현:

- `GET /v1/rankings`는 로그인 없이 보는 공개 API다. 응답은 `{ stage, rows[], season_year, participant_count, computed_at, next_cursor }` 봉투이고, 한 페이지에 50줄이 온다.
- `next_cursor`는 직전 페이지 마지막 줄의 `delivery_sequence`를 담은 불투명 문자열이다. 이전 응답이 준 값이 아니면 `400 INVALID_CURSOR`다.
- 같은 순위인 참가자가 또 있으면 행과 내 결과에 `tied: true`가 붙는다. 화면에는 `공동 N위`로 쓴다. 값은 `result_snapshots.tied`에 저장한다.
- `stage`는 `PREVIEW`(시즌 마감 전 미리보기)나 `FINAL`(마감 뒤 확정)이다.

## 6. 지역과 비공개 출발지

**운영 출발지는 서울 여의도 IFC몰이다.** 사용자 요청에 따라 이 문서에는 명시한다. 다만 서비스 화면·클라이언트 번들·공개 API·공유 카드에는 출발지와 정확한 좌표를 포함하지 않는다.

이 문서가 공개 저장소에 올라가면 저장소 독자는 출발지를 알 수 있다. 따라서 출발지는 ‘서비스 화면에서 비공개’이며, 공개 문서에 적으면서 실제 비밀로 보장할 수는 없다.

정확한 출발 좌표는 구현 시 검증하여 서버 운영 설정 SANTA_ORIGIN_LAT/SANTA_ORIGIN_LON으로 주입한다. 클라이언트 공개 환경변수 접두사를 사용하지 않는다. 운영 API 응답에는 설정을 직렬화하지 않는다. 도착 시간 자체로 위치를 추정할 가능성까지 차단한다고 보장하지 않는다.

지역은 자유 입력 대신 행정구역 코드로 선택한다. regions 테이블에는 시·도, 시·군·구, 대표 좌표, 좌표 출처·버전을 보관한다. 대표 좌표는 시·군·구청 소재지를 기본안으로 하고, 없는 지역의 대체 기준을 명시한다. 상세 집 주소는 받지 않는다.

**[대체됨]** 대표 좌표는 청사 소재지가 아니라 행정동 경계의 면적 중심이다. 좌표 출처·버전은 컬럼이 아니라 `regions.sql` 머리 주석에 적는다. 결정 이유는 §14.2에 있다.

현재 구현:

- `regions` 컬럼은 `code`, `name`, `latitude`, `longitude`, `parent_code`다. 시·도 16개와 시·군·구 255개, 합쳐서 271개다.
- 코드는 `KR-{시·도}`와 `KR-{시·도}-{시·군·구 뒤 3자리}`다. 서울 코드만 비공식 번호이고, 서울 26행은 `schema.sql`에 있다. 나머지는 `regions.sql`이 넣는다.
- 일반구는 시·도 바로 아래에 두고 `수원시 장안구`처럼 이름을 붙인다. 하위 지역이 없는 세종특별자치시(`KR-36`)는 시·도를 그대로 고른다.
- `GET /v1/regions`는 시·도를, `GET /v1/regions?parent={code}`는 그 하위 지역을 준다. 등록과 변경에는 `selectable: true`인 코드만 쓴다. 하위 지역이 있는 시·도를 보내면 `400 UNSUPPORTED_REGION`이다.
- 시즌 마감 전에는 `PATCH /v1/me/region`으로 지역을 바꿀 수 있다. 순위와 ETA는 같은 요청 안에서 다시 계산한다. 마감 뒤에는 `409 SEASON_FROZEN`이다.

## 7. 배송 시뮬레이션

### 7.1 MVP 운영 파라미터

마지막 논의의 저속 산타 설정을 초기 기본안으로 채택한다. 아래 수치는 실제 산타 연구 결과가 아니라 서비스 자체 설정이다.

| 설정 | 기본값 |
| --- | --- |
| 최초 출발 | 시즌 연도 12월 25일 00:00:00, Asia/Seoul |
| 출발 장소 | 여의도 IFC몰, 서버 좌표 설정 |
| 루돌프 비행 속도 | 100km/h |
| 1명당 배송 작업 | 180초 |
| 일일 활동 | 00:00~06:00 KST, 하루 6시간 |
| 휴식 | 06:00~다음 날 00:00, 18시간 |
| 이동 방식 | 지역 대표 좌표 간 대권거리, 도로 경로 사용 안 함 |
| 순서 | 배송 순번 오름차순, 거리 최적화하지 않음 |
| 다음 날 위치 | 전날 이동·배송을 중단한 지점에서 재개 |
| 수령 시각 | 해당 사용자의 배송 작업 완료 시각 |

하루 600km는 배송을 전혀 하지 않고 이동만 할 때의 상한이다. 하루 120명은 이동 시간이 전혀 없을 때의 상한이다. 두 상한을 동시에 보장하지 않는다.

### 7.2 거리·시간 계산

구면 Haversine 공식을 사용하고 지구 평균 반지름은 6371.0088km로 고정한다. 같은 지역의 연속 배송은 이동 거리 0이지만 인당 180초는 누적한다.

각 배송의 작업량:

    travel_seconds = distance_km / 100 * 3600
    work_seconds = travel_seconds + 180

첫 번째 거리는 출발 좌표→1위 지역, 이후에는 직전 수령자 지역→현재 수령자 지역이다. 서울→부산→서울 순위라면 그 순서대로 왕복한다. 낮은 순위의 가까운 지역을 먼저 방문하지 않는다.

이동·배송 작업은 휴식 시간에 멈추며 남은 작업을 다음 날 재개하는 선점형 모델이다. 날짜가 바뀌어도 귀환 이동은 추가하지 않는다. 순수 ETA 계산에는 누적 작업 시간만 필요하고, 지도상 실시간 위치를 제공하려면 별도의 경로 보간이 필요하다.

### 7.3 누적 작업 시간에서 도착 일시 계산

내부 누적 시간은 반올림하지 않은 값으로 유지하고, 최종 완료 시각만 초 단위 올림한다. API는 ISO 8601 시간대 포함 값을 반환한다. 화면은 분 단위 올림으로 표시하여 실제 계산보다 먼저 도착했다고 표시하지 않는다.

start = 시즌 12월 25일 00:00 KST
B = 21600초 (하루 활동 시간)
W = 현재 사용자 배송까지의 총 작업 시간을 초 단위 올림한 양의 정수

    day_index = floor((W - 1) / B)
    seconds_in_day = W - day_index * B
    eta = start + day_index calendar days + seconds_in_day seconds

W가 정확히 21600초면 첫날 06:00에 완료한다. 다음 작업은 다음 날 00:00부터 시작한다. 날짜 덧셈은 Asia/Seoul 달력 기준으로 수행하고 DB에는 UTC timestamptz로 저장한다. 연도 경계를 자동 처리한다.

### 7.4 검증용 예제

거리 값은 설명용 고정 입력이며 특정 실제 지역 좌표로 계산한 결과가 아니다.

| 순서 | 이전 지점부터 거리 | 누적 작업 시간 | 도착 시각 |
| --- | ---: | ---: | --- |
| 1 | 100km | 1시간 3분 | 12/25 01:03 |
| 2 | 500km | 6시간 6분 | 12/26 00:06 |
| 3 | 0km | 6시간 9분 | 12/26 00:09 |

출발지와 모든 배송지가 같다는 경계 조건에서는 120번째가 12/25 06:00, 121번째가 12/26 00:03에 받는다.

### 7.5 랭킹 갱신과 시즌 확정

초기 운영안은 12/25 00:00 이전에 새 심사·지역 변경이 생기면 미리보기 배송 일정을 재계산하고 출발 시점에 시즌을 동결하는 방식이다. 확정 시점 이전에 커밋된 정상 결과만 포함한다. 진행 중 심사는 동결된 배송 순서를 변경하지 않는다.

점수, 지역, 정렬 규칙, 좌표 버전, 배송 설정을 같은 스냅샷에 고정한다. 계산 작업은 중복 요청을 합쳐 처리하고, 완성된 스냅샷으로 한 번에 교체한다. 계산 도중 구버전 순위와 신버전 ETA를 섞어서 응답하지 않는다. 계산 중에는 이전 완료 스냅샷과 계산 시각을 보여준다.

출발 이후 가입자와 재심사 결과는 다음 시즌 대기 또는 랭킹 외 체험으로 처리하는 정책을 출시 전에 확정한다. 도착일이 다음 해로 넘어가도 기존 시즌의 배송 계획에 속한다.

## 8. 저장 구조

### 8.1 구현 전 제안 [대체됨]

아래는 구현 전에 낸 제안이다. 실제로는 SQLite 파일 DB 하나, DB 테이블 작업 큐, 같은 JVM 안의 작업자 둘로 줄였다(§8.2). 시즌·스냅숏 테이블은 따로 두지 않고 `result_snapshots.computed_at`으로 최신 계산을 고른다.

관계형 DB(PostgreSQL 권장), 비공개 객체 저장소, 비동기 작업 큐, AI 작업자, 배송 계산 작업자로 구성한다. 특정 웹 프레임워크·AI 공급자는 아직 확정하지 않는다.

| 테이블 | 주요 필드·제약 |
| --- | --- |
| users | id, nickname, nickname_key UNIQUE, region_id, created_at |
| sessions | user_id, token_hash, expires_at; 원본 세션 토큰 저장 금지 |
| regions | code PK, parent_code, name, latitude, longitude, coordinate_version |
| uploads | id, user_id, source, private_object_key, file_hash, status, expires_at |
| analysis_jobs | id, upload_id, request_sequence, status, attempts, error_code, version_set |
| assessments | id, user_id, season_id, score_hundredths CHECK 0..10000, component_scores, verdict, version_set |
| season_entries | season_id, user_id, active_assessment_id; UNIQUE(season_id,user_id) |
| seasons | id, starts_at, freezes_at, status, delivery_config_version |
| ranking_snapshots | id, season_id, computed_at, status, rule_versions |
| ranking_entries | snapshot_id, user_id, rank, delivery_sequence, score_hundredths, region_id, eta; UNIQUE(snapshot_id,user_id) |
| share_cards | public_token UNIQUE, user_id, snapshot_id, asset_key, created_at, revoked_at |

### 8.2 현재 구현

[`schema.sql`](../src/main/resources/schema.sql)과 JPA 엔티티가 기준이다. SQLite 테이블은 9개이고, Hibernate `ddl-auto: update`가 엔티티에 맞춰 컬럼을 더한다. 시각은 모두 UTC 텍스트로 저장한다.

| 테이블 | 필드·제약 |
| --- | --- |
| regions | code PK, name, latitude, longitude, parent_code FK→regions(code) NULL |
| participants | id PK, nickname, nickname_key UNIQUE, region_code FK→regions, created_at |
| sessions | token_hash PK, user_id FK→participants CASCADE, expires_at; 원본 세션 토큰은 저장하지 않음 |
| analysis_jobs | id PK, user_id FK CASCADE, idempotency_key, request_hash, source, payload NULL, status, attempts, lease_token, lease_until, error_code, assessment_id FK→assessments SET NULL, created_at, finished_at; UNIQUE(user_id, idempotency_key) |
| analysis_uploads | job_id PK FK→analysis_jobs CASCADE, source, self_speaker, content BLOB, created_at; created_at 색인 |
| assessments | id PK, user_id FK CASCADE, source CHECK(KAKAO, INSTAGRAM), score_hundredths CHECK 0..10000, verdict, comment, components, created_at, prompt_version NULL |
| result_snapshots | id PK, user_id FK CASCADE, assessment_id FK CASCADE, rank, delivery_sequence, eta, computed_at, tied |
| prompts | id PK, name, version CHECK ≥ 1, content, created_at; UNIQUE(name, version) |
| shares | token PK, user_id FK CASCADE, snapshot_id FK→result_snapshots CASCADE, nickname, score_hundredths, rank, eta, verdict, comment, computed_at, revoked_at NULL, tied, stage, created_at NULL; 활성 링크는 (user_id, snapshot_id)당 하나 |

- 원본은 객체 저장소가 아니라 `analysis_uploads`에 둔다. 상태 폴링이 `analysis_jobs` 행을 자주 읽으므로 원본을 작업 행에 넣지 않는다.
- `regions.parent_code`는 앱이 뜰 때 `RegionFacade`가 코드 접두어를 보고 채운다. 기존 DB에서는 Hibernate가 `schema.sql`보다 늦게 컬럼을 더하기 때문이다.
- `prompts`는 AI 시스템 프롬프트의 판이다. 저장한 판은 바꾸지 않고 새 판을 더한다. `score` 1판과 2판은 `prompts.sql`이 앱 시작 때 넣는다(`INSERT OR IGNORE`라 다시 돌아도 그대로다).
- `analysis_jobs.digest_hash`는 같은 채점 입력의 재제출에 지난 결과를 다시 쓰기 위한 해시다(§4.4). 기존 DB에는 Hibernate가 컬럼을 더한다.
- `ai_usage_days`는 날짜(한국)별 AI 토큰 사용량 합계다(§4.4). 본문은 담지 않는다.
- `assessments.prompt_version`은 그 평가에 쓴 `score` 판 번호다. 기존 DB에는 Hibernate가 컬럼을 더하고, 그 전 평가는 NULL이다.
- `shares`는 링크를 만든 순간의 닉네임·점수·순위·공동 순위·확정 상태·계산 시각을 복사해 둔다. 나중에 순위가 바뀌어도 링크 내용은 그대로다.

닉네임은 NFKC 정규화·앞뒤 공백 제거·영문 대소문자 통일 후 nickname_key를 만든다. 기본안은 2~16자, 한글·영문·숫자·밑줄 허용이다. 정규화 후 검증하며 보이지 않는 문자와 중간 공백은 거부한다. 가용성 조회는 참고용이고 최종 등록은 UNIQUE 제약으로 동시 요청을 처리한다. 중복 충돌은 409를 반환한다.

## 9. API

### 9.1 구현 전 초안 [대체됨]

아래 초안의 `GET /v1/data-guides/{source}`와 `POST /v1/uploads`(서명 업로드 URL)는 만들지 않았다. 내보내기 안내는 프론트엔드가 맡고, 업로드는 앱이 `POST /v1/analyses/files`로 직접 받는다. `DELETE /v1/shares/{id}`는 `{token}`으로 바뀌었다.

| 메서드·경로 | 역할 |
| --- | --- |
| GET /v1/nicknames/availability?nickname= | 닉네임 사용 가능 여부 |
| POST /v1/participants | 닉네임·지역 등록 및 소유자 세션 발급 |
| GET /v1/me | 내 프로필·작업 상태·현재 결과 |
| PATCH /v1/me/region | 동결 전 지역 변경 및 ETA 재계산 요청 |
| GET /v1/regions?parent= | 지역 선택 목록 |
| GET /v1/data-guides/{source} | 내보내기 안내·검증된 외부 링크 |
| POST /v1/uploads | 제한된 업로드 URL 발급 |
| POST /v1/analyses | 업로드 검증 후 AI 작업 생성, 202 반환 |
| GET /v1/analyses/{id} | 소유자만 작업 상태 확인 |
| GET /v1/rankings?season=&cursor= | 완료 스냅샷의 통합 순위 |
| GET /v1/me/result | 점수·순위·배송 순번·ETA·스냅샷 시각 |
| POST /v1/shares | 현재 결과의 공개 공유 카드 생성 |
| GET /s/{public_token} | 공유 페이지·OG 이미지 |
| DELETE /v1/shares/{id} | 소유자의 공유 링크 비활성화 |
| DELETE /v1/me | 계정·원본·공유 결과 삭제 요청 |

### 9.2 현재 API (17개 + dev 전용 3개)

OpenAPI 명세와 같다. 세션은 HttpOnly 쿠키로, CSRF 토큰은 `X-XSRF-TOKEN` 헤더로 보낸다.

| 메서드 | 경로 | 역할 | 인증 |
| --- | --- | --- | --- |
| GET | `/v1/csrf` | CSRF 토큰 발급 | 공개 |
| GET | `/v1/regions` | 지역 목록(`?parent=`로 하위 지역) | 공개 |
| GET | `/v1/nicknames/availability` | 닉네임 가용성 확인(예약 아님) | 공개 |
| POST | `/v1/participants` | 참가 등록, 세션 발급 (`201`) | 공개 · CSRF |
| GET | `/v1/me` | 내 참가 정보 | 세션 |
| PATCH | `/v1/me/region` | 시즌 마감 전 지역 변경, 순위 재계산 | 세션 · CSRF |
| DELETE | `/v1/me` | 내 데이터 삭제, 세션 쿠키 만료 (`204`) | 세션 · CSRF |
| POST | `/v1/analyses` | 정규화한 이벤트로 분석 제출 (`202`) | 세션 · CSRF · `Idempotency-Key` |
| POST | `/v1/analyses/files` | 원본 파일로 분석 제출 (`202`, multipart) | 세션 · CSRF · `Idempotency-Key` |
| GET | `/v1/analyses/{id}` | 분석 작업 진행 단계(소유자만) | 세션 |
| GET | `/v1/me/analysis` | 내 가장 최근 작업의 진행 단계 | 세션 |
| GET | `/v1/me/result` | 내 점수·순위·공동 순위·ETA·확정 상태 | 세션 |
| GET | `/v1/rankings` | 순위표 50줄씩(cursor) | 공개 |
| POST | `/v1/shares` | 공유 링크 생성·재사용 | 세션 · CSRF |
| DELETE | `/v1/shares/{token}` | 공유 링크 취소 (`204`) | 세션 · CSRF |
| GET | `/s/{token}` | 공개 공유 HTML(Open Graph) | 공개 |
| GET | `/s/{token}/image.png` | 공개 공유 PNG(1200×630) | 공개 |

dev 전용 프롬프트 관리 API다. `santacut.prompt-admin.enabled=true`(`PROMPT_ADMIN_ENABLED`)일 때만 컨트롤러가 뜬다. 운영은 이 값을 두지 않으므로 세 경로 모두 `404`(code 없음)다. 로컬 `bootRun`과 테스트는 켠다. 인증은 없다. 프론트엔드용이 아니라 연동 가이드에는 넣지 않는다.

| 메서드 | 경로 | 역할 | 인증 |
| --- | --- | --- | --- |
| POST | `/v1/admin/prompts/{name}` | 다음 판 저장 (`201`, `{name, version, created_at}`) | 공개 · CSRF |
| GET | `/v1/admin/prompts/{name}` | 최신 판(본문 포함) | 공개 |
| GET | `/v1/admin/prompts/{name}/versions` | 판 목록(본문 없음, 높은 판부터) | 공개 |

- `name`은 `[a-z0-9-]{1,40}`, `content`는 1~20,000자다. 어기면 `400 INVALID_REQUEST`다.
- 판 번호는 가장 높은 판 + 1이다. 커넥션 풀이 하나라 읽기와 쓰기 사이에 다른 트랜잭션이 끼지 못하고, `UNIQUE(name, version)`이 한 번 더 막는다.
- 분석 작업자는 AI를 부를 때마다 `score`의 최신 판을 DB에서 읽는다. 캐시하지 않는다. 로그에는 이름과 판 번호만 남긴다. 판이 하나도 없으면 작업은 `ANALYSIS_FAILED`로 끝나고 로그 사유는 `PROMPT_NOT_FOUND`다.

- 진행 상태는 폴링으로 읽는다. 상태 응답에는 결과가 없고, 결과는 `GET /v1/me/result` 하나로만 읽는다.
- 없거나 취소된 공유 링크를 `Accept: text/html`로 열면 `/s/{token}`과 `/s/{token}/image.png` 모두 `404`와 함께 시작 링크가 있는 안내 HTML을 준다. 그 밖의 요청에는 `404` JSON(`SHARE_NOT_FOUND`)을 준다.
- `{id}`가 UUID 형식이 아니면 `400 INVALID_REQUEST`다.

분석 시작에는 Idempotency-Key를 사용한다. 사용자별 원본·분석 상태에 소유권 검사를 적용하고 쿠키 인증 변경 요청에는 CSRF 방어를 적용한다. 랭킹과 공유에는 닉네임·점수·순위·ETA·유쾌한 판정만 기본 공개한다. 원본과 출발 좌표는 반환하지 않는다.

공유 카드는 생성 당시 스냅샷을 고정하고 계산 시각을 표시한다. 최신 결과와 달라질 수 있음을 안내하며 링크 페이지에서 최신 결과를 따로 조회할 수 있다. 실제 출발지·앞사람의 지역을 드러내는 카피는 공개하지 않는다.

## 10. 작업 처리·보관·장애 대응

분석 상태 초안: uploaded → validating → queued → analyzing → succeeded / insufficient_data / failed. **[대체됨]** `uploaded`는 따로 두지 않는다. 접수하는 순간 `VALIDATING`이다.

### 10.1 상태 기계

저장하는 상태(`analysis_jobs.status`)와 응답하는 진행 단계(`status` 필드)는 이름이 다르다. 저장 상태는 작업 규칙에 맞춘 이름이고, 응답 단계는 화면 문구(기획 §4.7)에 맞춘 이름이다.

```mermaid
stateDiagram-v2
    [*] --> VALIDATING: POST /v1/analyses/files
    [*] --> QUEUED: POST /v1/analyses
    VALIDATING --> QUEUED: 검증 통과
    VALIDATING --> FAILED: 형식·화자 오류, 검증 3회 초과, 24시간 만료
    VALIDATING --> INSUFFICIENT_DATA: 본인 메시지 20개 미만
    QUEUED --> RUNNING: 작업 임대(3분)
    QUEUED --> FAILED: 24시간 만료(EXPIRED)
    RUNNING --> RUNNING: 임대 만료 뒤 재시도(최대 3회)
    RUNNING --> SUCCEEDED: 평가 저장 + 순위 재계산
    RUNNING --> FAILED: AI 오류, 재시도 소진, 시즌 마감
    RUNNING --> INSUFFICIENT_DATA: 근거 부족
```

| 저장 상태 | 응답 진행 단계 |
| --- | --- |
| `VALIDATING` | `VALIDATING` |
| `QUEUED` | `QUEUED` |
| `RUNNING` | `ANALYZING` |
| `SUCCEEDED`(참가자의 최신 순위 계산이 완료 시각 이후) | `SUCCEEDED` |
| `SUCCEEDED`(아직 순위에 반영되지 않음) | `RANKING_PENDING` |
| `FAILED`, `INSUFFICIENT_DATA` | `FAILED`(이유는 `error_code`) |

현재 구현의 작업 상태(`analysis_jobs.status`):

- 원본 파일 제출(`POST /v1/analyses/files`): `VALIDATING` → `QUEUED` → `RUNNING` → `SUCCEEDED` / `INSUFFICIENT_DATA` / `FAILED`. 검증 실패는 `VALIDATING`에서 바로 끝난다.
- 이벤트 제출(`POST /v1/analyses`): `QUEUED`부터 시작한다.
- 작업자는 둘이고 같은 JVM에서 `@Scheduled`로 돈다. 검증 작업자(`santacut.upload.poll-delay-ms`, 기본 1초)와 분석 작업자(`santacut.ai.poll-delay-ms`, 기본 3초)다.
- 24시간 넘게 기다린 `VALIDATING`·`QUEUED` 작업은 `EXPIRED`로 끝낸다. 분석 작업자는 AI 모델이 꺼져 있어도 만료 정리부터 하고 쉰다. 검증 작업자도 돌 때마다 같은 정리를 한다.
- 참가자 한 명은 최근 24시간 동안 3번까지 제출할 수 있다. 두 제출 API를 합쳐서 센다. 넘으면 `429 DAILY_USER_LIMIT`이고, 본문 `retry_at`과 `Retry-After` 헤더로 다시 낼 수 있는 시각을 알린다.
- 앱에는 AI 호출 수 한도가 없다. 대신 토큰 비용 가드(하루 0.5달러, 한 달 9달러)가 있고, AWS Bedrock 예산 차단(santacut-infra)이 마지막 방어선이다(§4.4). 참가자별 24시간 3회 제출 한도는 비용 장치가 아니라 제품 규칙이라 남긴다(점수를 거듭 다시 뽑지 못하게).
- 검증 작업자는 만료 정리, 원본 꺼내기, 결과 기록을 트랜잭션 셋으로 나눈다. 파싱은 트랜잭션 밖에서 하므로 그동안 DB 커넥션을 쥐지 않는다.
- 검증을 세 번 넘게 시작하면 `RETRY_EXHAUSTED`로 끝낸다. 통과하면 점수 산정 시도 횟수를 0부터 센다.
- 원본과 본인 이름은 검증이 끝나는 트랜잭션에서 지운다. 24시간 넘게 남은 원본은 지우고 그 작업은 `EXPIRED`로 끝낸다.
- 검증 실패 코드: `UNSUPPORTED_FILE_FORMAT`, `SPEAKER_NOT_IDENTIFIED`(`FAILED`), `INSUFFICIENT_EVENTS`(`INSUFFICIENT_DATA`).
- 참여자 이름은 응답, 로그, 예외 detail에 넣지 않는다. 로그에는 작업 ID와 오류 코드만 남긴다.

### 10.2 오류 코드

HTTP 오류 본문은 `{ timestamp, status, error, path, code, message, retry_at }`이다. `code`는 [`ErrorCode.kt`](../src/main/kotlin/com/santacut/domain/global/exception/ErrorCode.kt)의 상수 이름이고 API 계약이다. CSRF 오류(`403`)나 없는 경로처럼 프레임워크가 만든 오류에는 `code`가 없다. `retry_at`은 `DAILY_USER_LIMIT`에만 붙는다.

| 코드 | 상태 | 뜻 |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | 본문·필드·헤더·ID 형식 오류 |
| `INVALID_IDEMPOTENCY_KEY` | 400 | Idempotency-Key가 영문·숫자·`_`·`-` 16~100자가 아님 |
| `INVALID_EVENT_RANGE` | 400 | 이벤트 시각이 최근 90일~5분 뒤 범위를 벗어나거나 전체가 200,000자 초과 |
| `INSUFFICIENT_EVENTS` | 400 | 이벤트 20개 미만 |
| `TOO_MANY_EVENTS` | 400 | 이벤트 2,000개 초과 |
| `INVALID_NICKNAME` | 400 | 닉네임 형식 오류 |
| `UNSUPPORTED_REGION` | 400 | 없는 지역이거나 하위 지역을 골라야 하는 시·도 |
| `INVALID_CURSOR` | 400 | 이전 응답이 준 값이 아닌 cursor |
| `NOT_AUTHENTICATED` | 401 | 세션이 없거나 만료됨 |
| `NOT_FOUND` | 404 | 분석 작업이 없거나 다른 참가자의 작업 |
| `PARTICIPANT_NOT_FOUND` | 404 | 세션의 참가자가 더 이상 없음 |
| `RESULT_NOT_FOUND` | 404 | 점수가 나온 결과가 없음 |
| `SHARE_NOT_FOUND` | 404 | 공유 링크가 없거나 취소됨 |
| `PROMPT_NOT_FOUND` | 404 | 이 이름의 프롬프트 판이 없음(dev 전용 API, 작업자는 `ANALYSIS_FAILED`로 기록) |
| `SEASON_FROZEN` | 409 | 시즌 마감 뒤 제출·지역 변경 |
| `IDEMPOTENCY_CONFLICT` | 409 | 같은 Idempotency-Key로 다른 요청 |
| `NICKNAME_TAKEN` | 409 | 이미 쓰는 닉네임 |
| `NO_RANKED_RESULT` | 409 | 공유할 순위 결과가 없음 |
| `FILE_TOO_LARGE` | 413 | 파일 20MB 초과 |
| `INSUFFICIENT_EVIDENCE` | 422 | 점수를 매길 근거 부족(작업자 내부, 응답에는 `error_code`로) |
| `UNSUPPORTED_FILE_FORMAT` | 422 | 지원하지 않는 파일 형식 |
| `SPEAKER_NOT_IDENTIFIED` | 422 | 고른 본인 이름이 대화에 없음(작업자 내부, 응답에는 `error_code`로) |
| `DAILY_USER_LIMIT` | 429 | 24시간 제출 한도 초과 |
| `ANALYSIS_PAYLOAD_MISSING` | 500 | 대기 작업에 입력이 없음(내부) |
| `ANALYSIS_JOB_STATE_INVALID` | 500 | 허용하지 않는 상태 변경(내부) |
| `AI_RESPONSE_INVALID` | 502 | AI 응답이 비었거나 너무 김(작업자 내부) |
| `AI_RESPONSE_SCHEMA_INVALID` | 502 | AI 응답 형식 오류(작업자 내부) |
| `AI_EVIDENCE_INVALID` | 502 | 범위 밖·겹친 근거 번호를 지우고 나니 근거가 없는 항목이 있음(작업자 내부) |
| `AI_SCORE_OUT_OF_RANGE` | 502 | AI 점수가 0~100 밖이거나 소수 셋째 자리 이하 값이 있음(작업자 내부) |
| `AI_NOT_CONFIGURED` | 503 | AI 모델이 설정되지 않아 제출을 받지 않음 |
| `AI_PROVIDER_UNAVAILABLE` | 503 | AI 제공자가 권한 거부·요청 한도로 호출을 막음(작업자 내부, 작업은 대기열로 돌아감) |

작업이 실패하면 상태 조회의 `error_code`가 이유를 알려 준다. 값은 [`AnalysisFailureCode`](../src/main/kotlin/com/santacut/domain/analysis/model/AnalysisFailureCode.kt)의 8개다. `SEASON_FROZEN`, `EXPIRED`, `RETRY_EXHAUSTED`, `ANALYSIS_FAILED`, `UNSUPPORTED_FILE_FORMAT`, `SPEAKER_NOT_IDENTIFIED`는 `FAILED`로, `INSUFFICIENT_EVIDENCE`와 `INSUFFICIENT_EVENTS`는 `INSUFFICIENT_DATA`로 저장한다. 작업자 내부 오류(`AI_*` 등)는 `ANALYSIS_FAILED`로 바꿔 기록한다.

### 10.3 보관·장애 대응 원칙

- AI 구조화 응답 스키마·값 범위를 검증한다. 모델이 반환한 최종 합계나 도착 시각을 신뢰하지 않는다.
- 일시적 실패는 지수 백오프로 최대 3회 시도하고, 형식 오류·근거 부족은 무한 재시도하지 않는다.
- 사용자·IP별 업로드/분석 제한과 동시 실행 제한을 두어 비용을 제어한다.
- 객체 저장소는 비공개이며 짧은 수명의 서명 URL만 사용한다. **[대체됨]** 객체 저장소는 쓰지 않는다. 원본은 `analysis_uploads` BLOB으로 받고 검증이 끝나면 지운다.
- 원본은 성공·실패 종료 후 삭제하고, 중단 작업의 원본도 업로드 후 최대 24시간 TTL로 정리하는 기본안을 적용한다.
- 공개 심사평에는 원문 발췌를 저장하지 않는다. 분석 로그는 작업 ID·시간·오류 코드·토큰 사용량 중심으로 남긴다.
- AI에 보내는 것은 본인 메시지로 만든 항목별 신호 4줄과 표본(최대 120줄, 본문 6,000자)뿐이다. 다른 참여자 메시지와 본인 메시지 전체는 Amazon Bedrock에 가지 않는다. 프롬프트, 사용자 메시지, 모델 응답은 로그에 남기지 않는다.
- 삭제 시 세션·공유 링크를 무효화하고 개인 결과를 제거한다. 지금 `DELETE /v1/me`는 참가자 행을 지우고 CASCADE로 세션·작업·원본·평가·스냅숏·공유를 함께 지운다. 시즌 마감 전이면 남은 참가자로 순위를 다시 계산한다. 이미 출발한 배송의 타인 ETA를 바꾸지 않도록 동결 일정에는 비식별 작업 슬롯만 남기는 정책을 검토한다.
- 사용자가 편집한 파일의 진위를 완전히 보증할 수는 없다. 중복·비정상 형식 제한은 제공하되 공식 플랫폼 인증 데이터라고 표현하지 않는다.

## 11. 구현 시 필수 검증

- 동시 닉네임 등록, 정규화 충돌, 타인 결과 수정 차단.
- 파서별 실제 샘플·본인 화자 선택·빈 데이터·과대 파일·본문 프롬프트 주입.
- 점수 범위와 반올림, 출처별 채점 편향, 캐시 버전 변경, 오래된 작업의 덮어쓰기 방지.
- 동점 공개 순위와 고유 배송 순번의 안정성.
- 동일 지역 거리 0, 이동과 배송 합산, 06:00 정확히 완료, 다음 날 재개, 연도 이월.
- 스냅샷 원자적 교체, 동결 직전 경쟁 조건, 동결 후 지역·점수 수정 차단.
- 공유 응답·OG 이미지·번들에서 원문과 출발 좌표가 노출되지 않는지 확인.
- 원본 TTL 삭제 및 실패한 작업 정리.

## 12. 구현 순서와 미확정 사항

구현 순서:

1. 실제 카톡·인스타 샘플 확보 및 지원 스키마·공통 채점 가능성 검증.
2. 닉네임·세션·지역 DB와 업로드 처리.
3. AI 구조화 채점·재현성 캐시·오류 처리.
4. 통합 랭킹·배송 엔진·스냅샷·시즌 동결.
5. 결과 페이지용 API·공유 카드·내보내기 안내.

출시 전 확정할 항목:

- 실제 인스타 JSON에서 평가에 사용할 수 있는 필드와 근거 부족 기준.
- 점수 항목·가중치·판정 구간, 출처 간 검증 결과.
- AI 모델·인프라·분석 단가와 요청 한도.
- 정확한 운영 출발 좌표. 전국 지역 대표 좌표 데이터 출처는 정했다(§14.2).
- 익명 세션 복구, 닉네임 변경·재사용, 시즌 동결 후 참가 정책.

## 13. 참고 자료와 설정의 구분

- [NSAI Santa Equation](https://www.nsai.ie/about/news/scientists-crack-the-code-to-reveal-how-santa-delivers-presents-to-the-worl/): 약 시속 4,500만 km 및 시차를 이용한 31시간이라는 사고실험.
- [레스터대학교 산타 썰매 연구 소개](https://le.ac.uk/news/2015/december/cleared-for-take-off-how-santa2019s-sleigh-takes-to-the-skies): 광속의 0.76%, 연간 하루 12시간 비행 가정.
- [NORAD FAQ](https://www.noradsanta.org/en/noradhq): 산타 추적 세계관과 방문 시간 설명.
- [Meta 정보 다운로드 안내](https://about.fb.com/news/2023/10/manage-your-information-across-apps/): 계정 센터에서 정보 다운로드 제공.

연구 속 속도를 적용하면 국내 지역 간 차이가 거의 사라지므로 산타컷은 시속 100km·인당 3분·하루 6시간을 별도로 설정한다. 연구가 이 서비스 수치를 입증했다고 표현하지 않는다.

## 14. 구현 결정 기록

### 14.1 진행 상태: SSE에서 폴링으로 (2026-11-16)

- 기록: 처음 구현은 `GET /v1/analyses/{id}/events` SSE 스트림이었다. 1초마다 상태를 보내고, 성공하면 결과를 실은 뒤 스트림을 닫았다. 스트림은 최대 150초까지 열렸다.
- 지금은 `GET /v1/analyses/{id}`와 `GET /v1/me/analysis`를 폴링한다(커밋 `9bb9c39`). 화면 기획(`docs/ui-ux-plan.md`)이 상태 폴링을 전제로 하고, 창을 닫았다 다시 와도 같은 API로 상태를 읽을 수 있다. 스트림용 스케줄러 스레드와 재연결 규칙도 함께 없어졌다.
- 응답 단계 이름을 기획 §4.7의 어휘로 바꿨다. 성공했지만 아직 순위가 없으면 오류 대신 `RANKING_PENDING`을 준다.
- 상태 응답에는 결과를 넣지 않는다. 점수와 순위는 `GET /v1/me/result` 한 곳에서만 읽어서 두 곳 값이 어긋날 일을 없앤다.

### 14.2 지역 계층과 좌표 (2026-11-16)

- 서울 25개 자치구만 있던 목록을 전국 시·도 16개, 시·군·구 255개로 넓혔다. 두 단계 선택은 `regions.parent_code`로 만든다.
- 좌표 출처는 vuski/admdongkor `ver20261001` 행정동 경계다(통계청 SGIS 공공누리 제1유형을 가공, CC BY 4.0). 행정동 폴리곤의 면적 중심을 시·군·구와 시·도 단위로 면적 가중 평균했다.
- 청사 좌표 대신 면적 중심을 쓴 까닭: 공개 라이선스 데이터 하나로 전국을 만들 수 있고, 생성 스크립트(`scripts/regions/generate_regions.py`)로 언제든 다시 만들 수 있다. 섬 지역은 좌표가 바다 위에 찍힐 수 있다.
- 시드는 `spring.sql.init.data-locations: classpath:regions.sql`로 앱이 뜰 때마다 돈다. `INSERT OR IGNORE`라 기존 DB에는 새 지역만 들어간다.

### 14.3 원본 파일 업로드 (2026-11-16)

- 브라우저 정규화만 믿지 않고 서버가 원본을 받아 직접 파싱한다. 파서를 서버 한 곳에 두면 출처별 규칙과 테스트가 한데 모인다.
- 원본은 작업 행이 아니라 `analysis_uploads` 테이블에 둔다. 폴링이 작업 행을 자주 읽는데, 같은 행에 20MB BLOB을 둘 수는 없다.
- 검증 작업자는 만료 정리, 원본 꺼내기, 결과 기록을 트랜잭션 셋으로 나누고 파싱은 트랜잭션 밖에서 한다. 커넥션 풀이 1개라서, 파싱하는 동안 DB 커넥션과 SQLite 쓰기 잠금을 쥐면 안 된다.
- 원본과 본인 이름은 검증이 끝나는 트랜잭션에서 지운다. 남아 있는 원본도 24시간 뒤에는 지운다. 파일 이름은 저장하지 않는다.
- 하루 제출 한도와 Idempotency-Key는 이벤트 제출과 함께 쓴다. 같은 키를 두 API에 쓰면 `409 IDEMPOTENCY_CONFLICT`다.

### 14.4 공유 카드와 비활성 링크 (2026-11-16)

- 공유 행에 `tied`, `stage`, `created_at`을 더했다. 카드와 페이지에 `공동 N위`, `미리보기`·`시즌 확정`, 계산 시각, 공유 시각이 나온다.
- 취소됐거나 없는 링크를 브라우저로 열면 JSON 대신 시작 링크가 있는 안내 HTML(`share-inactive.html`)을 준다.

### 14.5 프롬프트를 DB로 (2026-11-16)

- 채점 프롬프트를 `resources/prompts/score-v1.txt`에서 `prompts` 테이블로 옮겼다. 1판은 같은 본문을 `prompts.sql`로 넣는다.
- 시드를 앱 시작 이벤트가 아니라 SQL 초기화로 넣은 까닭은 순서다. SQL 초기화는 분석 작업자 스케줄보다 먼저 끝난다.
- 새 판은 dev에서만 관리자 API로 만든다. 처음에는 헤더 토큰 인증을 검토했으나, 운영에서 아예 끄는 쪽으로 정했다(설정 `santacut.prompt-admin.enabled`).
- 평가에 판 번호를 남겼다. 작업 행보다 결과(평가) 행이 자연스러운 자리라서 `assessments.prompt_version`에 둔다.

### 14.6 AI 입력 줄이기와 비용 가드 (2026-11-16)

- 이벤트를 JSON 객체로 하나씩 보내면 큰 내보내기에서 사용자 메시지가 20만 자 안팎이었다. 항목별 신호와 표본만 보내도록 바꿨다(§4.4).
- 신호 계산과 표본 고르기는 infrastructure가 아니라 도메인에 둔다. 채점 규칙에 가까운 판단이고, 단위 테스트로 고정하기 쉽다.
- 표본은 무작위 대신 주별 라운드 로빈으로 고른다. 결과를 다시 만들 수 있어야 하고, 시드를 따로 관리할 필요도 없다.
- 업로드와 분석 한도(20MB, 90일, 2,000개, 200,000자)는 그대로 둔다. 바뀐 것은 AI에 가는 내용뿐이다.
- 시각, 메시지 양, 시간대 같은 통계는 보내지 않는다. 프롬프트가 점수에 쓰지 못하게 하는 값이라 토큰만 들고 판단을 끌어당긴다.
- 크리스마스 낱말은 점수 항목이 아니라서 신호와 표본 우선순위에서 뺐다.
- 재사용 해시는 페이로드가 아니라 AI 입력으로 만든다. 파일이 조금 달라도 표본이 같으면 다시 쓸 수 있다. 성공과 실패를 한 컬럼(`analysis_jobs.digest_hash`)으로 다루려고 평가가 아니라 작업 행에 둔다.
- 앱 안의 비용 가드는 날짜별 토큰 합계로 판단한다. 호출 수로 세던 예전 `ai_daily_budget`은 비용과 맞지 않아 지웠던 것이다.
- 전문가 검토에서 미룬 것: 항목별 표본 칸 나누기, 본문 정규화와 중복 접기, 저신호 기준 4자로 올리기, 부정 신호 칸, 표본 크기 실험.
