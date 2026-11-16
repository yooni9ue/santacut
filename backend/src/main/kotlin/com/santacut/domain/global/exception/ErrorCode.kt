package com.santacut.domain.global.exception

/**
 * 오류 코드. 상수 이름은 API 계약이므로 바꾸지 않는다.
 *
 * @property status HTTP 상태 코드
 * @property message 사용자에게 보여 줄 한국어 메시지
 */
enum class ErrorCode(val status: Int, val message: String) {
    INVALID_REQUEST(400, "요청 형식이 올바르지 않습니다. 필수 값, 헤더, 값의 형식을 확인해 주세요."),
    INVALID_IDEMPOTENCY_KEY(400, "Idempotency-Key는 영문, 숫자, 밑줄, 하이픈으로 된 16~100자여야 합니다."),
    INVALID_EVENT_RANGE(400, "이벤트 시각은 최근 90일 안이면서 5분 뒤보다 늦지 않아야 하고, 전체 텍스트는 200,000자 이하여야 합니다."),
    INSUFFICIENT_EVENTS(400, "분석하려면 이벤트가 20개 이상 필요합니다."),
    TOO_MANY_EVENTS(400, "한 번에 보낼 수 있는 이벤트는 2,000개까지입니다."),
    INVALID_NICKNAME(400, "닉네임은 한글, 영문, 숫자, 밑줄로 된 2~16자여야 합니다."),
    UNSUPPORTED_REGION(400, "지원하지 않는 지역입니다. 하위 지역이 있는 지역은 하위 지역을 골라야 합니다."),
    INVALID_CURSOR(400, "cursor가 올바르지 않습니다. 이전 응답의 next_cursor를 그대로 보내야 합니다."),
    NOT_AUTHENTICATED(401, "로그인 세션이 없거나 만료되었습니다."),
    NOT_FOUND(404, "분석 작업이 없거나 다른 참가자의 작업입니다."),
    PARTICIPANT_NOT_FOUND(404, "세션의 참가자가 더 이상 존재하지 않습니다."),
    RESULT_NOT_FOUND(404, "아직 점수가 나온 결과가 없습니다."),
    SHARE_NOT_FOUND(404, "공유 링크가 없거나 취소되었습니다."),
    PROMPT_NOT_FOUND(404, "이 이름의 프롬프트가 없습니다."),
    SEASON_FROZEN(409, "시즌이 마감되어 이번 시즌 결과와 배송 계획이 확정되었습니다."),
    IDEMPOTENCY_CONFLICT(409, "같은 Idempotency-Key로 다른 요청 본문을 보냈습니다."),
    NICKNAME_TAKEN(409, "이미 사용 중인 닉네임입니다."),
    NO_RANKED_RESULT(409, "공유할 순위 결과가 아직 없습니다."),
    FILE_TOO_LARGE(413, "파일이 너무 큽니다. 20MB 이하 파일만 올릴 수 있습니다."),
    INSUFFICIENT_EVIDENCE(422, "점수를 매길 근거가 부족합니다."),
    UNSUPPORTED_FILE_FORMAT(422, "지원하지 않는 파일 형식입니다. 카카오톡 대화 TXT나 인스타그램 메시지 JSON을 올려 주세요."),
    SPEAKER_NOT_IDENTIFIED(422, "선택한 본인 이름을 이 대화에서 찾을 수 없습니다."),
    DAILY_USER_LIMIT(429, "24시간 동안 제출할 수 있는 분석 횟수를 모두 썼습니다."),
    ANALYSIS_PAYLOAD_MISSING(500, "대기 중인 분석 작업에 입력 데이터가 없습니다."),
    ANALYSIS_JOB_STATE_INVALID(500, "분석 작업의 현재 상태에서는 할 수 없는 상태 변경입니다."),
    AI_RESPONSE_INVALID(502, "AI 응답이 비어 있거나 너무 깁니다."),
    AI_RESPONSE_SCHEMA_INVALID(502, "AI 응답 형식이 올바르지 않습니다."),
    AI_EVIDENCE_INVALID(502, "AI 응답의 근거 번호를 정리하고 나니 모든 항목에 근거가 없습니다."),
    AI_SCORE_OUT_OF_RANGE(502, "AI 응답 점수가 0~100 범위를 벗어났거나 소수점 셋째 자리 이하 값이 있습니다."),
    AI_NOT_CONFIGURED(503, "AI 모델이 설정되지 않았습니다."),
    AI_PROVIDER_UNAVAILABLE(503, "AI 제공자가 호출을 막았거나 잠시 쓸 수 없습니다."),
    AI_BUDGET_EXCEEDED(503, "이번 호출의 어림 비용을 더하면 AI 비용 한도를 넘습니다."),
    ANALYSIS_STREAM_LIMIT(503, "지금은 진행 상황 스트림을 더 열 수 없습니다. 상태 조회로 진행 상황을 확인해 주세요."),
}
