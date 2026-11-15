# 백엔드 코드 컨벤션

`backend/`의 모든 Kotlin 코드에 적용하는 규칙이다. 리뷰 에이전트와 사람 리뷰어 모두 이 문서를 기준으로 판단한다. 규칙마다 확인 방법을 함께 적어 두었으니, 위반 여부는 감이 아니라 확인 방법으로 판정한다.

## 1. 아키텍처: 4계층 레이어드 아키텍처

패키지는 `presentation → application → domain → infrastructure` 네 계층으로 나눈다. 의존은 위에서 아래로만 흐른다. 포트·어댑터(헥사고날), 의존성 역전용 인터페이스, UseCase 인터페이스는 쓰지 않는다. 아래 계층은 위 계층을 모른다.

**`com.santacut` 바로 아래에는 이 네 패키지와 `SantaCutApplication.kt`만 둔다.** `common`, `global`, `config`, `util`, `support` 같은 계층 밖 패키지를 만들지 않는다. 설정·보안·애노테이션도 그것을 쓰는 계층 안에 둔다. 확인: `ls backend/src/main/kotlin/com/santacut` 결과가 `application domain infrastructure presentation SantaCutApplication.kt` 다섯 개뿐이어야 한다.

```text
com.santacut
├─ presentation           HTTP 진입점
│  ├─ controller          XxxController
│  ├─ request             XxxRequest (HTTP 요청 본문)
│  ├─ response            XxxResponse (HTTP 응답 본문), XxxResponses (응답 일급 컬렉션)
│  ├─ docs                엔드포인트별 Swagger 메타 애노테이션, Swagger 설정
│  ├─ advice              전역 예외 처리 (@RestControllerAdvice)
│  ├─ security            Spring Security 설정, 세션 필터, 세션 쿠키
│  └─ config              웹 계층이 쓰는 @ConfigurationProperties·@Configuration
├─ application            유스케이스 조합, 트랜잭션 경계
│  ├─ facade              XxxFacade, @Facade 애노테이션
│  ├─ command             XxxCommand (facade 입력)
│  └─ result              XxxResult (facade 출력), XxxResults (결과 일급 컬렉션)
├─ domain                 비즈니스 규칙
│  ├─ service             XxxService (도메인 서비스)
│  ├─ model               엔티티, 값 객체, 일급 컬렉션, 열거형
│  ├─ exception           ErrorCode, SantaCutException
│  └─ config              도메인 규칙이 쓰는 @ConfigurationProperties (시즌, 일일 한도 등)
└─ infrastructure         데이터 접근과 외부 시스템
   ├─ repository          Spring Data JPA 리포지토리
   ├─ client              외부 시스템 호출 (AI 점수 클라이언트 등)
   ├─ render              기술 도구 (공유 이미지 렌더링 등)
   └─ config              Clock·스케줄러·외부 연동 설정
```

- 설정 클래스는 그 설정을 읽는 계층의 `config` 패키지에 둔다. 여러 계층이 같은 설정을 읽으면 가장 아래 계층에 두지 않고, 위 계층이 아래 계층 값을 Command·메서드 인자로 넘기도록 바꾼다.
- 금지 이름: `Port`, `Adapter`, `UseCase`. 확인: `grep -rniE "\b\w*(port|adapter|usecase)\b" backend/src/main --include=*.kt | grep -viE "import|support|report|export"` 결과가 비어 있어야 한다.
- 계층 패키지 안에서 기능별 하위 패키지를 만들지 않는다. 클래스 이름 접두사로 기능을 구분한다(`ParticipantService`, `AnalysisFacade`).

## 2. 의존 방향

```text
presentation ──▶ application ──▶ domain ──▶ infrastructure
```

| 계층 | 의존해도 되는 것 | 의존하면 안 되는 것 |
| --- | --- | --- |
| presentation | application(facade, command, result), presentation 내부 | domain, infrastructure |
| application | domain(service, model, exception), application 내부 | presentation, infrastructure, **다른 facade** |
| domain | infrastructure(repository, client, render, config), domain 내부 | presentation, application, **다른 service** |
| infrastructure | domain.model(repository: 엔티티 제네릭과 반환 타입, client: 값 객체·열거형 입출력), infrastructure 내부 | presentation, application, domain.service, domain.exception, domain.config |

- 바로 아래 계층만 쓴다. presentation이 domain을, application이 infrastructure를 건너뛰어 쓰지 않는다.
- infrastructure가 `domain.model`을 참조하는 예외는 두 가지뿐이다.
  - infrastructure.repository는 JPA 엔티티를 리포지토리 제네릭과 반환 타입으로 쓴다.
  - infrastructure.client는 외부 호출의 입출력 계약으로 domain.model의 값 객체·열거형을 쓸 수 있다(엔티티는 제외).
- 그 밖의 infrastructure(render, config)는 domain을 참조하지 않는다. render는 자기 패키지의 값 타입(`ShareCard` 등)을 입력으로 받고, 엔티티를 그 값으로 바꾸는 일은 domain service가 한다.
- **facade는 다른 facade를 주입받지 않는다.** 두 유스케이스가 같은 로직을 쓰면 그 로직을 domain service로 내린다.
- **service는 다른 service를 주입받지 않는다.** 여러 service를 조합하는 일은 facade가 한다.
- 엔티티는 application 밖(presentation)으로 나가지 않는다.
- 확인: `backend/src/test`의 ArchUnit 테스트(`LayerDependencyTest`)가 위 표, infrastructure의 domain.model 참조 예외, facade 간 주입 금지, service 간 주입 금지를 검사하고 `./gradlew check`에서 통과해야 한다.

## 3. 컴포넌트 역할 명확화

스테레오타입 애노테이션으로 역할을 드러낸다.

| 역할 | 위치 | 애노테이션 | 이름 규칙 |
| --- | --- | --- | --- |
| HTTP 진입점 | presentation.controller | `@RestController` | `XxxController` |
| 유스케이스 조합 | application.facade | `@Facade` (같은 패키지의 커스텀 애노테이션, `@Component` 메타) | `XxxFacade` |
| 비즈니스 규칙 | domain.service | `@Service` | `XxxService` |
| 데이터 접근 | infrastructure.repository | Spring Data 인터페이스 | `XxxRepository` |
| 외부 연동·기술 도구 | infrastructure.client, infrastructure.render | `@Component` | 하는 일을 드러내는 명사 (`ScoreClient`, `ShareImageRenderer`) |
| 웹 보안 | presentation.security | `@Configuration`, 필터 | `XxxConfiguration`, `XxxFilter` |
| 설정 | 각 계층의 config | `@Configuration`, `@ConfigurationProperties` | `XxxConfiguration`, `XxxProperties` |

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
- 확인: controller 메서드 반환 타입은 `XxxResponse`, `XxxResponses`, `SseEmitter`, `ResponseEntity` 중 하나다. facade public 메서드 반환 타입은 `XxxResult`, `XxxResults` 또는 `Unit`이다. `grep -rnE "List<\w+(Result|Response)>" backend/src/main` 결과가 일급 컬렉션 클래스 자신의 필드 선언뿐이어야 한다.

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

- 오류는 `domain.exception`의 `ErrorCode` 열거형(HTTP 상태, 한국어 메시지)과 `SantaCutException` 하나로 표현한다. `ResponseStatusException`, `error()`, `check()`로 비즈니스 오류를 던지지 않는다.
- `presentation.advice`의 `@RestControllerAdvice`가 예외를 오류 응답으로 바꾼다. 오류 응답 형식은 Swagger `ApiErrorResponse`에 정의한 필드와 같아야 한다.
- 오류 코드 문자열(`SEASON_FROZEN` 등)은 API 계약이므로 기존 값을 유지한다.

## 7. 트랜잭션과 JPA

- 트랜잭션 경계는 facade 메서드다. 쓰기는 `@Transactional`, 읽기는 `@Transactional(readOnly = true)`.
- 백그라운드 작업처럼 한 흐름에서 트랜잭션을 여러 번 나눠야 할 때만 `TransactionTemplate`을 쓰고, 그 이유를 주석으로 남긴다.
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

- 엔드포인트마다 `presentation.docs` 패키지의 전용 메타 애노테이션 하나(`@SubmitAnalysisApi` 등)를 붙인다. controller 메서드에 `@Operation`을 직접 쓰지 않는다.
- 전용 애노테이션에는 요약, 설명, 필요한 헤더, 발생 가능한 모든 상태 코드를 한국어로 적는다.
- Request·Response의 모든 필드에 `@field:Schema(description = ...)`을 단다.

## 11. 테스트

- 도메인 모델과 일급 컬렉션은 단위 테스트를 가진다.
- facade 흐름은 `@SpringBootTest` 통합 테스트로 확인한다. 테스트 DB는 `build/` 아래 파일을 쓴다.
- 계층 의존 규칙은 ArchUnit 테스트로 강제한다.
- `./gradlew check`가 통과해야 커밋한다.
