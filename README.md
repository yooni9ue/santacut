# 🎅 산타컷 (SantaCut)

> 너 올해 산타컷 통과 가능?

카카오톡 대화 또는 인스타그램 활동 데이터를 AI가 분석해 산타 점수를 매기고, 통합 랭킹과 선물 도착 예정일을 알려주는 크리스마스 서비스입니다.

## 주요 기능

- **유니크 닉네임·지역 등록** — DB에서 닉네임 중복을 막고 시·군·구를 선택합니다.
- **데이터 가져오기** — 카카오톡·인스타그램 내보내기 안내를 따라 파일을 준비합니다.
- **AI 산타 심사** — 100점 만점, 소수점 둘째 자리의 점수와 유쾌한 판정문을 받습니다.
- **통합 랭킹** — 입력 데이터 종류와 관계없이 하나의 순위에서 경쟁합니다.
- **선물 도착 예상** — 산타가 순위대로 이동하는 거리와 배송·휴식 시간을 계산합니다.
- **공유하기** — 내 점수·순위·도착 예정일을 결과 카드로 공유합니다.

## 배송 세계관

산타는 12월 25일 00:00에 출발합니다. 초기 기획은 루돌프 시속 100km, 1명당 배송 3분, 매일 00:00~06:00 활동입니다. 지역 대표 좌표를 순위대로 방문하므로 앞사람들의 이동 거리와 배송 시간이 내 도착 예정일에 반영됩니다.

수치는 재미를 위한 자체 시뮬레이션 설정이며 실제 선물 배송을 제공하지 않습니다.

## 설계 문서

[백엔드 설계·기획](docs/backend-design.md)에서 데이터 모델, 채점·동점 처리, API, 배송 계산식, 시즌 운영 및 구현 순서를 확인할 수 있습니다.

[UI·UX 기획](docs/ui-ux-plan.md)에서 화면 구성, 사용자 흐름, 상태별 안내, 공유 경험과 구현 우선순위를 확인할 수 있습니다.

## 프로젝트 상태

Next.js 기반 기본 프로젝트 설정을 완료했습니다. 위 서비스 기능은 개발 예정입니다.


## 개발 환경

- Node.js 24 LTS (`client/.nvmrc`)
- npm 및 `package-lock.json`
- Next.js App Router, React, TypeScript
- Tailwind CSS, ESLint
- `src/` 구조 및 `@/*` 경로 별칭

## 실행 방법

Node.js 24를 설치한 뒤 아래 명령을 실행합니다. 프론트엔드 코드와 설정은 `client/`에 있습니다. nvm을 사용한다면 `client/`에서 `nvm install`과 `nvm use`를 실행하세요.

```bash
cd client
npm ci
npm run dev
```

[http://localhost:3000](http://localhost:3000)에서 확인할 수 있습니다. 현재 기본 화면 실행에는 환경 변수가 필요하지 않습니다.

## 확인 및 빌드

`client/` 디렉터리에서 실행합니다.

```bash
npm run lint
npm run typecheck
npm run build
npm start
```

`npm start`는 빌드 결과를 실행합니다.

## 기본 파일

- `client/src/app/page.tsx`: 첫 화면
- `client/src/app/layout.tsx`: 공통 레이아웃과 메타데이터
- `client/src/app/globals.css`: 전역 스타일과 Tailwind CSS
- `client/public/`: 정적 파일
