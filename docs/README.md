# 문서 목차

문서를 찾는 목적에 따라 분류한다. 기획 초안, 현재 디자인 기준, 실제 API 계약, 개발 규칙을 구분해서 읽는다.

| 분류 | 문서 | 내용 |
| --- | --- | --- |
| 기획 | [UI·UX 기획](planning/ui-ux-plan.md) | 사용자 흐름, 화면 구성, 상태별 안내, 우선순위 |
| 디자인 | [현재 디자인 방향](design/visual-direction.md) | 채택한 색상·서체·캐릭터·모바일 시각 기준 |
| API | [프론트엔드 API 연동 가이드](api/frontend-api-guide.md) | API 호출 순서, 세션·CSRF, 분석·랭킹·공유 연동 |
| 개발 규칙 | [PR 작성 규칙](development/pr-guide.md) | 브랜치·제목·본문·검증 결과 작성 방식 |
| 공통 양식 | [PR 템플릿](pull_request_template.md) | GitHub PR 작성 시 자동 감지할 본문 양식 |
| AI-DLC | [개발 흐름](ai-dlc/README.md) · [역할과 인계](ai-dlc/roles.md) | 요구사항부터 검증·운영까지 이어가는 개발 방식 |
| 작업 템플릿 | [산출물 템플릿](templates/README.md) | 작업 목적과 검증 결과를 일관되게 남기는 양식 |

## 영역별 운영 문서

- 프론트엔드: [Vercel 배포](../frontend/docs/deployment.md)
- 백엔드: [백엔드 README](../backend/README.md), [백엔드 기획](../backend/docs/backend-design.md), [시스템 아키텍처](../backend/docs/system-architecture.md), [배포 운영서](../backend/docs/deployment.md)

새 프론트 전용 문서는 `frontend/docs/`, 백엔드 전용 문서는 `backend/docs/`에 둔다. 기획·디자인·공통 API 계약·저장소 개발 규칙은 이 목차에서 연결한다. `pull_request_template.md`는 GitHub 자동 감지를 위해 `docs/` 루트에 유지한다.
