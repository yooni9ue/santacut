# Google Analytics

프론트엔드는 `NEXT_PUBLIC_GA_MEASUREMENT_ID`가 있을 때만 Google Analytics 4를 활성화한다.

## 환경 변수

- Preview/Development: Vercel 환경 변수에 측정 ID를 등록하면 해당 환경에서도 페이지뷰를 확인할 수 있다.
- Production: 운영 측정 ID를 별도로 등록한다.
- 값이 없으면 스크립트를 삽입하지 않으며, 로컬·테스트 환경은 기본적으로 비활성화된다.

## 추적 범위

Next.js 경로가 바뀔 때 페이지뷰만 전송한다. 닉네임, 파일 이름, 파일 원문, 분석 ID 같은 개인정보는 이벤트나 URL에 넣지 않는다.
