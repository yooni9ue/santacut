# Google Analytics 4 운영 안내

## 현재 설정

프론트엔드는 `NEXT_PUBLIC_GA_MEASUREMENT_ID`가 있을 때 Google Analytics 4(GA4)를 활성화한다. 측정 ID는 공개 설정값이며 인증용 비밀키가 아니다.

| 항목 | 설정 |
| --- | --- |
| 측정 ID | `G-VNQV2KB25S` |
| Vercel 프로젝트 | `santacut` |
| 환경 변수 | `NEXT_PUBLIC_GA_MEASUREMENT_ID` |
| Development / Preview / Production | 같은 측정 ID 등록 완료 |
| 개발 도메인 | `https://dev.santacut.me` |
| 운영 도메인 | `https://santacut.me` |

개발 배포에서는 브라우저의 GA 스크립트 삽입을 확인했다. 운영 환경 변수 등록과 운영 사이트의 GA 배포 완료는 별개다. 운영은 GA 코드가 포함된 `main` 릴리스와 재배포 이후 확인해야 한다. GA 보고서 수신까지 확인한 상태는 아니다.

같은 측정 ID를 사용하므로 개발·운영 방문 데이터가 한 속성에 모인다. 보고서를 볼 때 호스트 이름으로 구분하거나, 추후 개발용 속성을 따로 만들어 Preview/Development 측정 ID를 변경한다.

## 구현과 추적 범위

- [측정 컴포넌트](../src/app/analytics.tsx): `next/script`로 스크립트를 로드하고, 경로 또는 쿼리 변경 시 `config`에 `page_path`를 전달한다.
- [루트 레이아웃](../src/app/layout.tsx): `Suspense` 안에 측정 컴포넌트를 배치한다. URL 조회 훅의 프로덕션 빌드 요구사항 때문이다.
- 초기 설정에는 `send_page_view: false`를 지정하며 페이지 이동 처리는 컴포넌트에서 수행한다.
- 별도 가입·업로드·분석·공유 커스텀 이벤트와 사용자 ID 전송은 구현하지 않았다.
- ID가 없으면 GA 스크립트를 삽입하지 않는다. 로컬·테스트에서는 기본적으로 ID를 설정하지 않는다.

파일 원문·파일명·닉네임·분석 ID를 명시적인 이벤트 매개변수로 보내지 않는다. 다만 현재 `page_path`에는 쿼리 문자열 전체가 포함되므로, URL에 개인정보나 인증·공유 토큰을 넣으면 측정에 포함될 수 있다. URL 및 쿼리에 이런 값을 추가하지 않는다. GA 속성에서 켜진 향상된 측정 항목은 별도로 점검한다.

## 환경 변수 변경과 배포

1. Vercel 프로젝트의 Settings → Environment Variables에서 `NEXT_PUBLIC_GA_MEASUREMENT_ID`를 등록하거나 수정한다.
2. 적용할 Development·Preview·Production 환경을 선택한다.
3. 해당 브랜치를 다시 배포한다. `NEXT_PUBLIC_*` 값은 빌드 때 포함되므로 저장만 해서는 기존 배포가 바뀌지 않는다.

개발 배포는 `develop`, 운영 배포는 `main`을 사용한다. 상세 흐름은 [배포 안내](deployment.md)를 참고한다. 비활성화하려면 해당 환경의 측정 ID를 제거하고 재배포한다.

## 수집 확인

1. 배포된 사이트를 열고 홈 → 시작 → 파일 선택 화면으로 이동한다.
2. 브라우저 개발자 도구의 Network에서 `gtag/js` 로드와 `collect` 요청을 확인한다. 요청의 측정 ID가 `G-VNQV2KB25S`인지 확인한다.
3. Google Analytics의 실시간 보고서에서 방문과 페이지뷰를 확인한다. 스크립트 로드 성공만으로 보고서 수신을 확정하지 않는다.
4. 첫 진입과 페이지 이동을 각각 확인하고, 한 번의 이동에 페이지뷰가 중복되는지도 확인한다.

데이터가 보이지 않으면 배포 시점의 환경 변수, 광고 차단 확장 기능, 브라우저 추적 차단, Network 오류를 순서대로 확인한다. 현재 코드는 스크립트 초기화와 페이지 이동 효과의 실행 순서에 영향을 받을 수 있으므로 첫 페이지뷰가 누락되지 않는지 실제 요청으로 확인한다.

## 후속 작업

- GA 실시간 보고서에서 실제 수신 확인
- 개발·운영 데이터 분리 여부 결정
- 첫 진입 페이지뷰와 향상된 측정에 의한 중복 페이지뷰 검증
- 쿼리 매개변수 허용 목록 등 URL 전송 범위 제한

공식 안내: [GA4 설정](https://support.google.com/analytics/answer/14183469), [측정 ID 찾기](https://support.google.com/analytics/answer/12270356).
