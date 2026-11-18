# 구현과 문서 대조 기록

확인일: 2026-11-18. 기준: develop `e883cc9`(SSE 단독 구독 반영). 실행 코드·환경 변수·DB·배포는 변경하지 않는 문서 점검이다.

## 점검 범위와 근거

- 루트 README·문서 목차, 기획·디자인, API 가이드, AI-DLC·PR 규칙·템플릿
- 프론트 화면·등록·분석·배포·GA 문서와 `screens.tsx`, `upload-draft-screen.tsx`, `registration-screen.tsx`, `preview-context.tsx`, `frame.tsx`, `src/lib/` 및 `next.config.ts`
- 백엔드 README·설계·아키텍처·운영서·컨벤션·과거 리뷰 기록, presentation 컨트롤러·DTO, 채점·평가 재사용 코드, `application.yml`, Compose·백업 스크립트
- `.github/workflows/frontend.yml`, `backend.yml`의 경로 필터·브랜치·배포 조건
- dev `/v3/api-docs`의 API 경로와 결과·공유·랭킹 계약. 배포 서버의 비밀값·DB·인프라 저장소는 이번 점검에서 조회하지 않음

과거 부하 시험·비용·AI 평가 결과는 작성 당시의 기록으로 보존한다. 이번에 재측정한 결과가 아니며 현재 처리량·가격 보장이 아니다. 컨벤션은 규범이므로 문서 작업을 이유로 코드에 맞춰 완화하지 않는다.

## 바로잡은 설명

| 영역 | 변경 전 문제 | 이번 정리 |
| --- | --- | --- |
| 루트·프론트 화면 | 전체가 미리보기·API 미연동으로 기술 | 실제 연결된 화면과 미완료 동선을 분리 |
| 분석 상태 | SSE 실패 시 폴링 예시·권고 잔존 | 초기 GET + SSE, 연결 종료 시 수동 재연결로 통일 |
| 재방문 | 새로고침 뒤 실제 작업 복귀가 완성된 것처럼 설명 | Context 초기화와 가상 화면 분기 한계를 명시 |
| 결과 | 아직 예시라는 설명과 실제 API UI가 혼재 | 점수·유형·항목·공유 링크 구현 범위 기록 |
| 배포 | 초기 미설정 상태를 현재처럼 기술 | develop/main 역할과 Preview 명시 배포 명령 정리 |
| 백엔드 재사용 | 결과 캐시·점수 규칙 버전 없음으로 기술 | `digest_hash`, 규칙 판·프롬프트 판·모델 캐시 키 기록 |
| 아키텍처 | 도메인 미정·worker 1개·`/api` rewrite | 구매 도메인·동시 3개·`/v1` rewrite로 정정 |
| 백업 | 원본은 장기 백업에서 제외한다고 단정 | 전체 SQLite 백업에 처리 중 원본·대화가 포함될 수 있음을 명시 |
| 과거 리뷰 | 예전 구현·남은 개선을 현재 상태처럼 읽을 수 있음 | 과거 기록임을 표시하고 현재 구조로 연결 |

## 남은 코드·운영 작업

이 표는 문서 수정으로 해결된 것이 아니다. 구체적인 화면별 현상은 [프론트 화면 안내](../../frontend/docs/screens.md)에 있다.

| 우선순위 | 작업 | 근거 위치 |
| --- | --- | --- |
| 높음 | 상대 메시지를 Gemini에 전송하는 범위와 삭제 고지를 동의 UI에 반영 | `LiveReviewScreen`, API 가이드 §6.1, 서버 정규화·채점 경로 |
| 높음 | 성공 상태 → 실제 결과 이동, 새로고침 후 최근 작업 복구 | `AnalysisScreen`의 sample 분기·`LiveAnalysisScreen` 성공 문구 |
| 높음 | 근거 부족과 최저 항목 분리 | 결과 UI는 `weakest_dimension`에 근거 부족 표시. 실제 계약은 `low_confidence_dimensions` |
| 높음 | 백업 내 원본 보존·복원 후 삭제 재적용 정책 확정 | `backup.sh` 전체 DB `.backup`, 라이브 삭제와 별개 |
| 높음 | 늦게 끝난 이전 제출의 결과 역전 방지 | 평가는 완료 시각으로 저장하고 최신 평가는 `createdAt DESC, id DESC` 조회. 제출 순번 보호는 현재 없음 |
| 보통 | 랭킹 스냅숏 변경 409 시 첫 페이지 재조회, 더 보기 중복 요청 방지 | `LiveRankingScreen.loadMore`, 서버 cursor 계약 |
| 보통 | 공유 인용 옵션을 JSON `include_quote`로 전송하고 취소·이미지 미리보기 구현 | `results.ts`는 현재 미사용 true 옵션에 쿼리 사용. 서버는 RequestBody |
| 보통 | 지역 변경·내 데이터 삭제 UI | 백엔드 API 존재, 프론트 미연결 |
| 보통 | 결과 없음·미등록·통신 오류·공유 오류의 안내 분리 | 결과·랭킹의 nullable 상태와 오류 처리 |
| 보통 | 실제 화면의 예시 footer·준비 안내·순위 반영 문구 갱신 | `FlowFrame`, `StartScreen`, `PrepareScreen`, 결과 pending 문구 |
| 보통 | 결과 배지·인용·유형 비율·공개 랭킹 도착 날짜 표시 | OpenAPI 필드 대비 프론트 타입·UI 미반영 |
| 보통 | GA 최초 진입·중복·쿼리 전송 검증 및 범위 제한 | [GA 문서](../../frontend/docs/analytics.md) |

운영 API 인프라·DNS·Production BACKEND_ORIGIN과 실제 원본 내보내기 샘플·실제 AI 성공 → 결과·랭킹·공유 통합 검증은 별도 확인 대상이다. HTTP 200이나 모의 API E2E만으로 완료 처리하지 않는다.

## 검증과 한계

검증 결과: 추적 중인 Markdown 31개의 로컬 경로 링크 122개에서 누락 경로 0개, 명시된 Markdown 헤딩 앵커 9개 누락 0개, `git diff --check origin/develop...HEAD` 통과. dev OpenAPI에서 공유·결과·랭킹의 핵심 필드 10개 존재를 대조했다. 외부 사이트 전체 가용성은 별도 자동 검증하지 않았다.

추가 점검에서 배포 스크립트와 설계 목표를 대조했다. 명시적 worker drain·외부 health·별도 보안 스캔은 현재 자동 실행 단계가 아니므로 자동화 완료로 해석하지 않도록 아키텍처에 구분했다.

런타임 코드를 바꾸지 않아 애플리케이션 테스트·유료 AI 호출·부하 시험은 새로 실행하지 않았다. dev OpenAPI 조회는 읽기 전용이며 기존 사용자 계정이나 데이터를 생성·삭제하지 않았다.
