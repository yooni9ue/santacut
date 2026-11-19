# 백엔드 코드 컨벤션

`backend/`의 모든 Kotlin 코드에 적용하는 규칙이다. 리뷰 에이전트와 사람 리뷰어 모두 이 문서를 기준으로 판단한다. 규칙마다 확인 방법을 함께 적어 두었으니, 위반 여부는 감이 아니라 확인 방법으로 판정한다.

## 1. 아키텍처: 4계층 레이어드 아키텍처

패키지는 `presentation → application → domain → infrastructure` 네 계층으로 나눈다. 의존은 위에서 아래로만 흐른다. 포트·어댑터(헥사고날), 의존성 역전용 인터페이스, UseCase 인터페이스는 쓰지 않는다. 아래 계층은 위 계층을 모른다.

**`com.santacut` 바로 아래에는 이 네 패키지와 `SantaCutApplication.kt`만 둔다.** `common`, `global`, `config`, `util`, `support` 같은 계층 밖 패키지를 만들지 않는다. 확인: `ls backend/src/main/kotlin/com/santacut` 결과가 `application domain infrastructure presentation SantaCutApplication.kt` 다섯 개뿐이어야 한다.

**각 계층 아래는 도메인 이름 패키지로 나누고, 도메인 패키지 아래를 역할 패키지로 나눈다(계층 → 도메인 → 역할).** 도메인은 `participant`(참가자·지역), `session`(세션), `analysis`(분석 작업·점수), `ranking`(순위·결과·배송), `share`(공유), `prompt`(AI 시스템 프롬프트 판), `dossier`(산타 조서 집계와 장면)다. 여러 도메인이 함께 쓰는 것(보안, 예외 처리, Swagger, ErrorCode, Clock 같은 설정, 커스텀 애노테이션)은 그 계층의 `global` 패키지에 둔다. `global`은 계층 안에서만 만들 수 있다.

```text
com.santacut
├─ presentation                   HTTP 진입점
│  ├─ <도메인>
│  │  ├─ controller               XxxController
│  │  ├─ request                  XxxRequest, XxxRequests (요청 일급 컬렉션)
│  │  ├─ response                 XxxResponse, XxxResponses (응답 일급 컬렉션)
│  │  ├─ stream                   SSE 스트림과 그 목록 (XxxStream, XxxStreams, XxxEvent)
│  │  └─ docs                     엔드포인트별 Swagger 메타 애노테이션
│  └─ global
│     ├─ advice                   전역 예외 처리 (@RestControllerAdvice), ApiErrorResponse
│     ├─ security                 Spring Security 설정, 세션 필터, 세션 쿠키
│     ├─ docs                     Swagger 설정, 공통 문서 상수
│     └─ config                   웹 계층이 쓰는 @ConfigurationProperties·@Configuration
├─ application                    유스케이스 조합, 트랜잭션 경계
│  ├─ <도메인>
│  │  ├─ facade                   XxxFacade
│  │  ├─ runner                   XxxRunner (facade가 쓰는 실행 기법)
│  │  ├─ command                  XxxCommand, XxxCommands
│  │  └─ result                   XxxResult, XxxResults
│  └─ global                      @Facade·@Runner 애노테이션, TransactionRunner
├─ domain                         비즈니스 규칙
│  ├─ <도메인>
│  │  ├─ model                    엔티티, 값 객체, 일급 컬렉션, 열거형
│  │  └─ service                  XxxService
│  └─ global
│     ├─ exception                ErrorCode, SantaCutException
│     ├─ model                    여러 도메인이 쓰는 값(시각 변환 등)
│     └─ config                   도메인 규칙이 쓰는 @ConfigurationProperties
└─ infrastructure                 데이터 접근과 외부 시스템
   ├─ <도메인>
   │  ├─ repository               Spring Data JPA 리포지토리
   │  ├─ client                   외부 시스템 호출 (AI 점수 클라이언트 등)
   │  └─ render                   기술 도구 (공유 이미지 렌더링 등)
   └─ global
      └─ config                   Clock·스케줄러·외부 연동 설정
```

- 역할 패키지는 그 도메인에 해당 파일이 있을 때만 만든다(빈 패키지 금지).
- 한 클래스는 정확히 한 도메인에 속한다. 어느 도메인 것인지 애매하면 그 클래스를 가장 많이 쓰는 유스케이스의 도메인에 둔다. 두 도메인 이상이 같은 비중으로 쓰면 `global`에 둔다.
- 다른 도메인의 같은 계층 타입을 참조해도 된다(예: `domain.ranking.model`이 `domain.analysis.model.Assessment`를 씀). 계층 간 의존 방향(2장)과 facade·service 주입 금지는 도메인과 상관없이 지킨다.
- 설정 클래스는 그 설정을 읽는 계층의 `global.config`에 둔다. 여러 계층이 같은 설정을 읽으면 위 계층이 아래 계층 값을 Command·메서드 인자로 넘기도록 바꾼다.
- 확인: `find backend/src/main/kotlin/com/santacut -mindepth 2 -maxdepth 2 -type d | sort`의 결과가 각 계층 아래 도메인 이름과 `global`뿐이어야 한다. 테스트 코드도 같은 구조를 따른다.
- 금지 이름: `Port`, `Adapter`, `UseCase`. 확인: `grep -rniE "\b\w*(port|adapter|usecase)\b" backend/src/main --include=*.kt | grep -viE "import|support|report|export"` 결과가 비어 있어야 한다.

## 2. 의존 방향

이 문서에서 `domain.model`, `presentation.request`처럼 계층과 역할만 쓴 이름은 `domain.<도메인>.model`, `presentation.<도메인>.request`를 함께 뜻한다(도메인 생략 표기).

```text
presentation ──▶ application ──▶ domain ──▶ infrastructure
```

| 계층 | 의존해도 되는 것 | 의존하면 안 되는 것 |
| --- | --- | --- |
| presentation | application(facade, command, result), presentation 내부 | domain, infrastructure |
| application | domain(service, model, exception), application 내부 | presentation, infrastructure, **다른 facade**(runner는 facade와 다른 runner도) |
| domain | infrastructure(repository, client, render, config), domain 내부 | presentation, application, **다른 service** |
| infrastructure | domain.model(repository: 엔티티 제네릭과 반환 타입, client: 값 객체·열거형 입출력), infrastructure 내부 | presentation, application, domain.service, domain.exception, domain.config |

- 바로 아래 계층만 쓴다. presentation이 domain을, application이 infrastructure를 건너뛰어 쓰지 않는다.
- infrastructure가 `domain.model`을 참조하는 예외는 두 가지뿐이다.
  - infrastructure.repository는 JPA 엔티티를 리포지토리 제네릭과 반환 타입으로 쓴다.
  - infrastructure.client는 외부 호출의 입출력 계약으로 domain.model의 값 객체·열거형을 쓸 수 있다(엔티티는 제외).
- 그 밖의 infrastructure(render, config)는 domain을 참조하지 않는다. render는 자기 패키지의 값 타입(`ShareCard` 등)을 입력으로 받고, 엔티티를 그 값으로 바꾸는 일은 domain service가 한다.
- **facade는 다른 facade를 주입받지 않는다.** 두 유스케이스가 같은 로직을 쓰면 그 로직을 domain service로 내린다.
- **service는 다른 service를 주입받지 않는다.** 여러 service를 조합하는 일은 facade가 한다.
- **runner는 facade나 다른 runner를 주입받지 않는다.** runner는 service와 `TransactionRunner`만 써서 실행 기법 하나를 감싸고, 여러 runner를 엮는 일은 facade가 한다. 예외로 application.global의 `TransactionRunner`는 다른 runner가 주입받을 수 있다(트랜잭션 경계는 runner 안에서도 이것으로만 연다).
- **facade는 흐름만 쓴다.** public 메서드는 유스케이스 단계를 높은 추상화 수준으로 나열해 위에서 아래로 읽혀야 한다. 한 메서드 안에 추상화 수준을 섞지 않는다. 스레드·슬롯·잠금·리스·재시도 같은 실행 기법과 점수 계산·실패 분류 같은 업무 규칙은 이름 있는 협력 객체로 내린다. 실행 기법은 application runner(3장)로, 업무 규칙은 domain service나 domain.model 정책으로 보낸다. 의존성 6개 이상이나 private 함수 10개 이상이면 관심사를 나눌 신호다.
  - 확인: 리뷰어가 facade public 메서드를 처음부터 끝까지 읽고 유스케이스 단계를 업무 용어로 한 줄씩 말할 수 있어야 한다. 실행 기법(`Executor`, `Semaphore`, `synchronized`, `TransactionTemplate` 직접 조합, 재시도 루프)이 facade public 메서드 본문에 보이면 위반이다.
- 엔티티는 application 밖(presentation)으로 나가지 않는다.
- 확인: `backend/src/test`의 ArchUnit 테스트(`LayerDependencyTest`)가 위 표, infrastructure의 domain.model 참조 예외, facade 간 주입 금지, runner의 facade·runner 주입 금지, service 간 주입 금지를 검사하고 `./gradlew check`에서 통과해야 한다.

## 3. 컴포넌트 역할 명확화

스테레오타입 애노테이션으로 역할을 드러낸다.

| 역할 | 위치 | 애노테이션 | 이름 규칙 |
| --- | --- | --- | --- |
| HTTP 진입점 | presentation.<도메인>.controller | `@RestController` | `XxxController` |
| 유스케이스 조합 | application.<도메인>.facade | `@Facade` (application.global의 커스텀 애노테이션, `@Component` 메타) | `XxxFacade` |
| 실행 기법(트랜잭션 나누기, 스레드·슬롯, 잠금, 리스, 비용 예약) | application.<도메인>.runner, application.global | `@Runner` (application.global의 커스텀 애노테이션, `@Component` 메타) | `XxxRunner` |
| 비즈니스 규칙 | domain.<도메인>.service | `@Service` | `XxxService` |
| 데이터 접근 | infrastructure.<도메인>.repository | Spring Data 인터페이스 | `XxxRepository` |
| 외부 연동·기술 도구 | infrastructure.<도메인>.client, infrastructure.<도메인>.render | `@Component` | 하는 일을 드러내는 명사 (`ScoreClient`, `ShareImageRenderer`) |
| 웹 보안 | presentation.global.security | `@Configuration`, 필터 | `XxxConfiguration`, `XxxFilter` |
| 설정 | 각 계층의 global.config | `@Configuration`, `@ConfigurationProperties` | `XxxConfiguration`, `XxxProperties` |

- 모든 Spring 빈은 위 표의 역할 중 정확히 하나에 속한다. 표에 없는 역할의 빈을 만들지 않는다.
- 범용 `@Component`는 infrastructure의 client·render 패키지에서만 쓴다.
- service가 외부 연동(AI 호출, 렌더링)을 직접 다루지 않는다. 외부 연동 코드는 infrastructure.client·render 컴포넌트에 두고, service는 그 컴포넌트를 호출해 결과를 도메인 모델로 받는다.
- 주입은 생성자 주입만 쓴다. 필드 주입, `lateinit` 주입 금지.
- 설정값은 `@Value` 대신 `@ConfigurationProperties` 데이터 클래스로 묶는다.
- 현재 시각은 `Instant.now()`를 직접 부르지 않고 주입받은 `Clock`에서 얻는다. 테스트에서 시각을 고정하기 위해서다.

## 4. Command와 Result

계층 사이 데이터는 목적이 드러나는 타입으로만 주고받는다. `Map<String, Any>`, `Pair`, `Triple`로 데이터를 넘기지 않는다.

```text
[presentation]                [application]              [domain]
Request ──toCommand()──▶ Command ──▶ Facade ──▶ Service / Model
Response ◀──from()──── Result ◀──from()── Model
```

- `XxxRequest`(presentation.request): HTTP 본문, 검증 애노테이션(`@field:Size` 등), Swagger 스키마 설명. `toCommand()`로 Command를 만든다.
- `XxxCommand`(application.command): facade 입력. 인증 사용자 식별자처럼 HTTP 본문 밖의 값도 여기 담는다.
- `XxxResult`(application.result): facade 출력. 엔티티를 담지 않고 값만 담는다. 도메인 모델에서 만드는 정적 팩토리 `from()`을 둔다.
- `XxxResponse`(presentation.response): HTTP 응답 본문과 Swagger 스키마 설명. `from(result)`로 만든다.
- 목록은 `List<XxxResult>`, `List<XxxResponse>`로 넘기지 않고 일급 컬렉션(`XxxResults`, `XxxResponses`)으로 넘긴다. 응답 일급 컬렉션은 리스트 필드에 `@JsonValue`를 달아 JSON을 지금처럼 배열로 내보낸다. API 계약(JSON 형태)은 바꾸지 않는다.
- 확인: controller 메서드 반환 타입은 `XxxResponse`, `XxxResponses`, `ResponseEntity` 중 하나다. facade public 메서드 반환 타입은 `XxxResult`, `XxxResults` 또는 `Unit`이다. `grep -rnE "List<\w+(Result|Response)>" backend/src/main` 결과가 일급 컬렉션 클래스 자신의 필드 선언뿐이어야 한다.

## 5. 도메인과 일급 컬렉션

- 비즈니스 규칙(점수 판정, 순위 계산, 배송 시각 계산, 상태 전이, 검증)은 `domain.model`의 객체 메서드에 둔다. service는 모델에 일을 시키고 결과를 저장한다.
- 엔티티 상태는 의미 있는 메서드로만 바꾼다(`job.succeed(assessmentId, finishedAt)`). 외부에서 `job.status = "FAILED"`처럼 필드를 직접 바꾸지 않는다. 엔티티 필드는 `protected set` 또는 `private set`이다.
- 상태와 코드 값은 문자열 대신 열거형을 쓴다(`AnalysisJobStatus`, `ErrorCode`).
- **컬렉션을 다루는 로직은 일급 컬렉션에 둔다.** 예: `Assessments`, `RankingEntries`, `AnalysisEvents`, `Regions`. 일급 컬렉션은 리스트 하나만 필드로 가지고, 그 리스트에 대한 연산을 메서드로 제공한다.
- **반복문과 스트림·컬렉션 순회는 일급 컬렉션 안에서만 쓴다. 예외 없음.** `for`, `while`, `forEach`, `map`, `mapNotNull`, `filter`, `flatMap`, `associate*`, `groupBy`, `sumOf`, `any`, `all`, `none`, `count`, `first`/`firstOrNull`(람다), `fold`, `reduce`, `sorted*`, `distinct`, `stream()`이 대상이다. service·facade뿐 아니라 controller, request·response·command·result의 변환 메서드, 엔티티·값 객체, infrastructure(client, render, repository), security 필터에서도 쓰지 않는다.
  - 일급 컬렉션은 컬렉션 하나만 필드로 가지는 클래스이고 이름은 복수형이다(`Assessments`, `RankingEntries`, `RegionResults`, `RankingResponses`, `ScoreDimensionNodes`). 일급 컬렉션은 그 컬렉션이 속한 계층에 둔다: 도메인은 `domain.model`, facade 출력은 `application.result`, 응답은 `presentation.response`, 요청은 `presentation.request`, 외부 응답 파싱은 `infrastructure.client`.
  - 변환도 일급 컬렉션의 메서드로 한다. 예: `RankingResponses.from(results: RankingRowResults)`, `AnalysisEventRequests.toCommands()`.
  - 확인: `grep -rlE "\bfor ?\(|while ?\(|\.(forEach|map|mapNotNull|filter|flatMap|associate\w*|groupBy|sumOf|any|all|none|count|first|firstOrNull|fold|reduce|sorted\w*|distinct|stream)\b *[({]" backend/src/main/kotlin | xargs -n1 basename` 결과의 모든 파일이 복수형 이름의 일급 컬렉션이어야 하고, 각 파일의 클래스 필드는 컬렉션 하나뿐이어야 한다.

## 6. 예외

- 오류는 `domain.global.exception`의 `ErrorCode` 열거형(HTTP 상태, 한국어 메시지)과 `SantaCutException` 하나로 표현한다. `ResponseStatusException`, `error()`, `check()`로 비즈니스 오류를 던지지 않는다.
- `presentation.global.advice`의 `@RestControllerAdvice`가 예외를 오류 응답으로 바꾼다. 오류 응답 형식은 Swagger `ApiErrorResponse`에 정의한 필드와 같아야 한다.
- 오류 코드 문자열(`SEASON_FROZEN` 등)은 API 계약이므로 기존 값을 유지한다.

## 7. 트랜잭션과 JPA

- 트랜잭션 경계는 facade 메서드다. 쓰기는 `@Transactional`, 읽기는 `@Transactional(readOnly = true)`.
- 한 메서드에서 트랜잭션을 직접 나눠야 하면 application.global의 `TransactionRunner`(`run`, `call`)를 쓴다. `TransactionTemplate`은 `TransactionRunner` 안에서만 쓴다. `TransactionRunner`는 아래 두 경우에만 쓰고, 그 이유를 주석으로 남긴다.
  - 백그라운드 작업처럼 한 흐름에서 트랜잭션을 여러 번 나눠야 할 때.
  - 실패해도 호출한 쪽을 막으면 안 되는 부수 작업(앱 시작 때 다시 세기 등)에서 트랜잭션을 롤백한 뒤 예외를 잡아 로그만 남겨야 할 때. `@Transactional` 메서드 안에서 예외를 잡으면 반쯤 쓴 변경이 커밋되거나, 안쪽 트랜잭션이 rollback-only로 표시해 커밋 때 `UnexpectedRollbackException`이 난다. 그래서 트랜잭션을 롤백으로 끝낸 뒤 경계 바깥에서 잡는다. 예: `ResultFacade.refreshPopulation`의 `transaction.run { ... }`.
- 엔티티 하나당 파일 하나, 리포지토리 하나당 파일 하나.

## 8. 한국어 규칙

사람이 읽는 텍스트는 모두 한국어로 쓴다.

- 주석, KDoc, Swagger `summary`·`description`, 예외 메시지, 로그 메시지, 테스트 이름(`@DisplayName` 또는 백틱 함수명)은 한국어다.
- 프로그래밍 식별자(클래스·함수·변수명), 패키지 이름, JSON 필드명, 열거형 상수, HTTP 경로·헤더, 오류 코드 값, 설정 키는 영어를 유지한다. 이것들은 코드와 API 계약의 일부이기 때문이다.
- 확인: `grep -rnE "//.*[A-Za-z]{4,}|description *= *\"[A-Za-z]" backend/src/main`에서 식별자 인용을 제외한 영어 문장이 나오지 않아야 한다.

## 9. 클린 코드

- **파일 하나에 최상위 타입 하나**, 파일 이름은 타입 이름과 같다. 클래스, 데이터 클래스, 열거형, 인터페이스, object, 애노테이션 클래스(엔드포인트별 Swagger 애노테이션 포함), DTO, 예외 모두 각자 파일을 가진다. `private` 보조 클래스도 예외가 아니다. 확인: 테스트 `OneTopLevelTypePerFileTest`가 `src/main`과 `src/test`의 모든 `.kt` 파일을 검사하고 `./gradlew check`에서 통과해야 한다.
- 함수는 한 가지 일만 하고 15줄을 넘기지 않는다. 들여쓰기 깊이는 2단계까지다.
- 매직 넘버와 매직 문자열은 이름 있는 상수나 설정으로 뺀다(`MAX_DAILY_SUBMISSIONS = 3`).
- 와일드카드 import와 본문 안의 완전 수식 이름(`java.time.LocalDate.of(...)`)을 쓰지 않는다.
- `!!`를 쓰지 않는다.
- 쉼표와 연산자 앞뒤 공백 등 서식은 Kotlin 공식 코딩 컨벤션을 따른다.
- 이름은 하는 일을 드러낸다. `tx`, `h`, `n` 같은 축약 이름을 쓰지 않는다.

## 10. Swagger 문서

- 엔드포인트마다 그 도메인의 `presentation.<도메인>.docs` 패키지에 있는 전용 메타 애노테이션 하나(`@SubmitAnalysisApi` 등)를 붙인다. controller 메서드에 `@Operation`을 직접 쓰지 않는다.
- 전용 애노테이션에는 요약, 설명, 필요한 헤더, 발생 가능한 모든 상태 코드를 한국어로 적는다.
- Request·Response의 모든 필드에 `@field:Schema(description = ...)`을 단다.

## 11. 테스트

- 도메인 모델과 일급 컬렉션은 단위 테스트를 가진다.
- facade 흐름은 `@SpringBootTest` 통합 테스트로 확인한다. 테스트 DB는 `build/` 아래 파일을 쓴다.
- 계층 의존 규칙은 ArchUnit 테스트로 강제한다.
- `./gradlew check`가 통과해야 커밋한다.

## 12. JPA 엔티티와 스키마

스키마는 엔티티가 정한다. Flyway 같은 마이그레이션 도구 없이 Hibernate `ddl-auto: update`가 테이블과 컬럼을 만들고, 그 뒤에 시드 SQL(`seed.sql`, `regions.sql`, `prompts.sql`)이 돈다(`spring.jpa.defer-datasource-initialization: true`).

### 애그리거트와 참조

연관관계(`@ManyToOne`, `@OneToMany`, `@OneToOne`)는 애그리거트 안에서만 쓴다. 애그리거트 사이는 id 컬럼(`userId: String` 등)으로만 가리킨다. 지금 애그리거트는 모두 엔티티 하나짜리라 연관관계가 하나도 없다.

| 애그리거트 | 테이블 | id로 가리키는 것 |
| --- | --- | --- |
| 지역 | `regions` | 상위 지역(`parent_code`) |
| 참가자 | `participants` | 지역 |
| 세션 | `sessions` | 참가자 |
| 지역 변경 기록 | `region_changes` | 참가자 |
| 분석 작업 | `analysis_jobs` | 참가자, 평가(`assessment_id`) |
| 업로드 원본 | `analysis_uploads` | 분석 작업(`job_id`가 곧 기본 키) |
| 평가 | `assessments` | 참가자 |
| 순위 스냅숏 | `result_snapshots` | 참가자, 평가 |
| 공유 링크 | `shares` | 참가자, 순위 스냅숏 |
| 산타 조서 | `dossiers` | 참가자, 평가 |
| 독립 행 | `prompts`, `ranking_refresh`, `result_populations`, `ai_usage_days` | 없음 |

- 분석 작업과 업로드 원본도 id로 잇는다. 둘을 `@OneToOne`으로 묶으면 역방향 1:1은 바이트코드 향상 없이 LAZY가 되지 않아 작업을 읽을 때마다 최대 20MB 원본까지 읽는다. `orphanRemoval`도 지우기 전에 원본을 읽는다.
- 애그리거트 안에 연관관계를 새로 둘 때는 `fetch = FetchType.LAZY`로 두고, 함께 필요한 화면에서는 fetch join 쿼리로 한 번에 읽는다.
- 참가자 삭제는 DB가 맡는다. 참가자 행 하나를 지우면 외래 키의 `ON DELETE CASCADE`가 그 참가자의 행을 모두 지운다. 코드에서 테이블마다 지우지 않는다.

### SQLite 방언의 한계

Hibernate의 SQLite 방언(`org.hibernate.community.dialect.SQLiteDialect`)이 만드는 것과 만들지 않는 것이 다르다.

| 적는 법 | SQLite에 생기나 | 쓰는 법 |
| --- | --- | --- |
| `@JoinColumn`·`@ForeignKey`·`@OnDelete` | 안 생긴다 | 쓰지 않는다 |
| `@Column(columnDefinition = "TEXT REFERENCES x(id) ON DELETE CASCADE")` | 생긴다(새 테이블만) | 외래 키는 이렇게 적는다. 정의는 `ForeignKeyColumns`에 모은다 |
| `@Table(indexes = [Index(...)])`(유일하지 않은 인덱스) | 생긴다(기존 테이블에도) | 이름을 붙여 적는다 |
| `@Table(uniqueConstraints)`, `Index(unique = true)` | 안 생긴다 | `seed.sql`에 `CREATE UNIQUE INDEX IF NOT EXISTS`로 적는다 |
| `@Column(unique = true)` | 생긴다(새 테이블만, 컬럼 하나) | 컬럼 하나짜리 유일 제약에만 쓴다 |
| `@Check` | 생긴다(새 테이블만) | 값 범위 같은 규칙에 쓴다 |
| 컬럼 기본값 | `columnDefinition`으로만 생긴다 | 시드 SQL처럼 값을 주지 않고 넣는 컬럼에 적는다 |

- `ddl-auto: update`는 이미 있는 테이블의 제약을 고치지 않는다. 외래 키·CHECK·기본값을 바꾸려면 테이블을 다시 만들어야 하므로 배포 운영서에 절차를 적고 따로 한다.
- 앱은 뜰 때 삭제 규칙이 있는 외래 키 12개를 `PRAGMA foreign_key_list`로 확인하고, 하나라도 없으면 뜨지 않는다(`ForeignKeyCheckConfiguration`). 외래 키 컬럼을 더하거나 바꾸면 그 목록도 함께 고친다.
- enum 컬럼에는 상수 목록 CHECK를 만들지 않는다(`EnumCheckFreeSQLiteDialect`). 기본 방언은 `CHECK (col IN (지금 상수들))`을 붙이는데, 기존 DB에서는 고쳐지지 않아 상수를 더하면 그 뒤 새로 만든 DB에서만 INSERT가 깨진다. `AttributeConverter`나 `columnDefinition`을 써도 붙는다. 값을 막아야 하면 `@Check`로 직접 적는다.
- 스키마가 예전 `schema.sql`과 같은지는 `SchemaBaselineTest`가 빈 DB를 띄워 확인한다(외래 키, 인덱스, CHECK, 기본값). 일부러 다르게 만든 것은 그 테스트의 목록에 이유와 함께 적는다.

### 조회 수(N+1)

- 반복 안에서 리포지토리를 부르지 않는다. 여러 행이 필요하면 id 목록을 모아 `in`으로 한 번에 읽고, 메모리에서 맞춘다. 예: 순위 계산 뒤 알림은 작업 목록과 참가자별 순위 반영 시각을 한 번씩만 읽는다(`AnalysisStatusFacade.announceRankedStatusChanges`).
- 한 화면에 두 엔티티가 함께 필요하면 조인 쿼리 하나로 읽는다. 엔티티 전체가 아니라 몇 값만 필요하면 `select new ...` 프로젝션을 쓴다.
- 자주 부르는 조회와 알림의 SQL 수는 `QueryCountApiTest`가 센다. 조회 수가 바뀌는 변경은 그 테스트의 기대값을 함께 고치고 이유를 커밋에 적는다.

### `@Modifying` 쿼리

벌크 `update`·`delete`는 영속성 컨텍스트를 거치지 않는다. 아래 기준으로 옵션을 고른다.

- `flushAutomatically = true`: 같은 트랜잭션에서 앞서 바꾼 엔티티가 그 쿼리의 결과에 영향을 주면 쓴다. 예: 순위 계산 요청 표시(`RankingRefreshRepository`).
- `clearAutomatically = true`: 쿼리 뒤에 같은 엔티티를 다시 읽어야 하면 쓴다. 영속성 컨텍스트를 비우므로 아직 flush하지 않은 다른 엔티티의 변경은 버려진다. 앞에서 바꾼 엔티티가 있으면 `flushAutomatically`와 함께 쓴다. 예: 공유되지 않은 스냅숏 지우기(`ResultSnapshotRepository.deleteUnshared`).
- `AnalysisUploadRepository`의 두 삭제는 옵션 없이 둔다. 원본을 지운 뒤 같은 트랜잭션에서 업로드를 다시 읽지 않고, 같은 트랜잭션에서 바꾼 작업 상태가 아직 flush되지 않았을 수 있어 `clearAutomatically`를 켜면 그 변경이 사라진다.
