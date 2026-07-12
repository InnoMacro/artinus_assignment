# ARTINUS Backend — 구독 서비스 API

구독하기 / 구독해지 / 구독 이력 조회를 제공하는 백엔드 애플리케이션을 요구사항에 맞추어 구현.
구독 변경은 외부 승인 API(csrng) 결과에 따라 커밋·롤백됨.
이력 조회 시 LLM으로 자연어 요약을 생성하고 AI 호출이 불가능하면 동일한 이력 snapshot으로 규칙 기반 요약을 반환.

## 기술 스택 및 선택 근거

| 기술 | 선택 근거 |
| --- | --- |
| Kotlin 2.3 / JDK 25 | Virtual Thread 및 Spring AI를 지원하는 버전 중 적합한 버전 |
| Spring Boot 4 | webmvc |
| MySQL 8 + JPA + QueryDSL | 조인 및 lock 사용 시에 native query 대신 컴파일 시간에 검증 가능한 jpql 쿼리 빌더 |
| Flyway | 스키마와 채널 seed 데이터를 버전 관리하고 `ddl-auto: validate`로 엔티티-스키마 불일치를 기동 시점에 차단 |
| Resilience4j | retry + circuit breaker, AI 호출 circuit breaker를 선언적으로 구성, bulkhead를 통해 llm api 과다 청구 방지 |
| Spring AI (OpenAI) | 별도 ai 서버 둘 필요 없이 LLM provider를 port 뒤에 격리해 교체 가능하게 유지, prompt cache·모델 옵션 등 OpenAI 전용 기능 사용 |
| Testcontainers + ArchUnit | 실제 MySQL로 트랜잭션·잠금 경계를 검증하고, 계층 역방향 의존을 빌드에서 차단 |

## 실행

기본 실행은 AI 모델을 비활성화하므로 OpenAI API Key 없이도 동작한다.

```shell
docker compose up --build
```

- API 문서(Swagger UI): http://localhost:8080/swagger-ui.html
- 헬스체크: http://localhost:8080/actuator/health

OpenAI 요약을 활성화하려면 `.env.example`을 참고해 `.env`에 다음 값을 설정한다()API Key는 레포지토리에 미포함)

```properties
AI_CHAT_PROVIDER=openai
OPENAI_API_KEY=your-api-key
```

`AI_CHAT_PROVIDER=openai`인데 API Key가 비어 있으면 애플리케이션 시작 단계에서 실패합니다.

## API 명세

### 1. 구독하기 — `POST /api/v1/subscriptions`

```json
{
  "phoneNumber": "010-1234-5678",
  "channelId": 1,
  "targetStatus": "PREMIUM"
}
```

- 구독 가능한(`subscribable`) 채널에서만 허용.
- 최초 회원은 요청 시점에 생성되며 `BASIC` 또는 `PREMIUM`으로 가입 가능.
- 응답: `{ "memberId": 1, "phoneNumber": "010-1234-5678", "status": "PREMIUM" }`

### 2. 구독해지 — `POST /api/v1/subscriptions/unsubscribe`

요청 형식은 구독하기와 동일하며, 해지 가능한(`unsubscribable`) 채널에서만 허용.

### 3. 구독 이력 조회 — `GET /api/v1/subscriptions/{phoneNumber}/histories`

```json
{
  "history": [
    {
      "channelName": "홈페이지",
      "action": "SUBSCRIBE",
      "previousStatus": "NONE",
      "changedStatus": "BASIC",
      "changedAt": "2026-01-01T21:00:00+09:00"
    }
  ],
  "summary": "2026년 1월 1일 홈페이지에서 일반 구독을 시작했습니다.",
  "summarySource": "LLM"
}
```

`summarySource`는 요약 출처(`LLM` 또는 `FALLBACK`)를 나타냄.

### 상태 전이 규칙

`SubscriptionStateMachine`이 (현재 상태, 행위, 목표 상태) 조합을 검증.

| 현재 상태 | SUBSCRIBE | UNSUBSCRIBE |
| --- | --- | --- |
| `NONE` (구독 안함) | `BASIC`, `PREMIUM` | — |
| `BASIC` (일반 구독) | `PREMIUM` | `NONE` |
| `PREMIUM` (프리미엄 구독) | — | `BASIC`, `NONE` |

### 에러 응답

| HTTP | code | 상황 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 휴대폰 번호 형식 등 요청 값 오류 |
| 400 | `INVALID_STATUS_TRANSITION` | 허용되지 않는 상태 전이 |
| 403 | `CHANNEL_ACTION_NOT_ALLOWED` | 채널이 해당 행위(구독/해지)를 지원하지 않음 |
| 404 | `CHANNEL_NOT_FOUND` / `MEMBER_NOT_FOUND` | 채널 또는 회원 없음 |
| 409 | `SUBSCRIPTION_CONFLICT` | 동일 휴대폰 번호 동시 신규 구독 충돌 |
| 422 | `CSRNG_REJECTED` | 외부 승인 API가 `random=0` 반환 — 트랜잭션 롤백(미시작) |
| 502 | `CSRNG_INVALID_RESPONSE` | 외부 승인 API 응답 계약 위반 |
| 503 | `CSRNG_UNAVAILABLE` | 외부 승인 API 타임아웃·서버 오류·circuit open |

## 아키텍처

기능 영역(`subscription`, `channel`)별로 헥사고날(ports & adapters) 구조를 적용.

```
org.artinus.backend
├── channel
│   ├── domain                      # Channel, 채널 행위 가능 규칙
│   ├── application                 # port (outbound)
│   └── adapter/outbound/persistence
├── subscription
│   ├── domain                      # SubscriptionMember, 상태 FSM, 이력, VO
│   ├── application
│   │   ├── port/inbound            # Subscribe/Unsubscribe/GetHistory UseCase
│   │   ├── port/outbound           # ApprovalPort, Summarizer, Repository...
│   │   └── service                 # 유스케이스 조율, 트랜잭션 경계
│   └── adapter
│       ├── inbound/web             # REST controller, 요청/응답 DTO, 예외 매핑
│       └── outbound                # persistence(JPA/QueryDSL), csrng, ai(Spring AI)
└── common / config                 # 공통 에러 응답, RestClient/QueryDSL/Clock 설정
```

- domain은 프레임워크에 의존하지 않고, application은 port 인터페이스만 참조. csrng·Spring AI 타입과 기술 예외는 adapter 밖으로 노출되지 않음.
- `domain -> application/adapter`, `application -> adapter` 역방향 의존은 ArchUnit 테스트(`LayerDependencyTest`, `PackagePathConventionTest`)가 빌드에서 차단.
- Subscription과 Channel은 하나의 bounded context 안의 두 기능 영역으로 둠.

## 구독 변경 트랜잭션 설계

구독·해지는 외부 승인 지연이 DB connection과 회원 row lock으로 전파되지 않도록 다음 단계로 실행.

1. 짧은 read-only 트랜잭션에서 Channel, 회원, 상태 전이를 사전 검증.
2. DB 트랜잭션과 row lock이 없는 상태에서 csrng 승인을 요청.
3. 승인 후 짧은 쓰기 트랜잭션에서 Channel과 회원을 다시 검증하고, 회원을 비관적 잠금으로 조회한 뒤 현재 상태 기준으로 전이와 이력을 저장.

사전 검증은 잘못된 요청으로 외부 API를 호출하지 않기 위한 절차이고, 승인 후 재검증은 동시 요청 중 바뀐 상태를 오래된 이력으로 저장하지 않기 위한 정합성 장치. 변경 유스케이스는 `Propagation.NEVER`로 선언되어 이미 트랜잭션이 시작된 호출자에서는 실행되지 않음.

- 승인 거절(`random=0`)·장애 시 쓰기 트랜잭션을 시작하지 않으므로 회원과 이력은 변경되지 않음.
- 승인 후 DB 저장이 실패하면 회원과 이력을 같은 로컬 트랜잭션에서 함께 rollback.
- csrng는 외부에 비즈니스 상태를 남기지 않는 난수 API이므로 현재는 별도 보상 트랜잭션이나 멱등성 key를 두지 않음.
- 동일 휴대폰 번호의 신규 구독 충돌은 unique constraint를 최종 방어선으로 사용하고 `409 SUBSCRIPTION_CONFLICT`로 변환.

## 외부 API(csrng) 장애 대응

`CsrngSubscriptionApprovalAdapter`가 circuit breaker → retry → HTTP 호출 순으로 감싸 실행.

| 전략 | 설정 | 의도 |
| --- | --- | --- |
| Timeout | connect 1s / read 2s | 외부 지연이 요청 스레드와 DB 커넥션 풀로 전파되는 것을 차단 |
| Retry | 최대 2회 시도, 100ms 지수 backoff + jitter | 일시적 네트워크 오류·5xx만 재시도 (`CsrngUnavailableException`) |
| Circuit Breaker | 최근 20회 중 실패율 50% 이상이면 open, 10s 후 half-open(3회 허용) | 장애 지속 시 즉시 실패로 전환해 스레드 대기와 연쇄 장애 방지 |
| 응답 계약 검증 | status, 항목 수, random 값 검증 | 계약 위반 응답은 재시도·서킷 집계 대상에서 제외하고 `502`로 구분 |

- 타임아웃·5xx·circuit open은 `503 CSRNG_UNAVAILABLE`, 계약 위반은 `502 CSRNG_INVALID_RESPONSE`로 매핑되어 클라이언트가 재시도 가능 여부를 구분.
- 계약 위반(`CsrngInvalidResponseException`)은 재시도해도 성공할 수 없으므로 retry/circuit breaker의 `ignore-exceptions`로 제외.
- 어떤 실패 경로에서도 쓰기 트랜잭션이 시작되기 전이므로 데이터는 변경되지 않음.

## LLM 이력 요약 설계

- application 계층의 `SubscriptionHistorySummarizer` port는 Spring AI 타입을 노출하지 않으며, Spring AI와 OpenAI 전용 옵션은 `subscription/adapter/outbound/ai`에 격리.
- system/user prompt는 `src/main/resources/prompt/subscription-history-summary`에서 버전 관리.
- 휴대폰 번호와 회원 ID는 모델 입력에 포함하지 않음.
- application service가 호출자의 트랜잭션을 suspend하고, 별도 DB read 트랜잭션에서 정렬된 snapshot을 만든 뒤 트랜잭션 밖에서 LLM을 호출.
- timeout, OpenAI 오류, 빈 응답, bulkhead 포화, circuit open에서는 `summarySource=FALLBACK`인 규칙 기반 요약으로 대체되어 조회 자체는 성공. 예상하지 못한 adapter 구현 오류는 fallback으로 숨기지 않고 예외로 노출.
- 모델 입력 이력이 설정한 최대 건수를 넘으면 외부 호출을 생략하고 전체 이력 기반 fallback을 사용.
- semaphore bulkhead가 인스턴스당 동시 호출을 제한하며 포화 요청은 대기하지 않습니다. circuit breaker는 실제 외부 호출 실패만 집계.
- timeout은 HTTP 시도당 제한이며 기본 retry를 0으로 두어 응답 지연과 중복 생성·과금 가능성을 제한.
- provider prompt cache만 사용하며, 다른 회원의 유사 이력을 오인할 수 있는 semantic response cache는 사용하지 않음.

### AI 설정

주요 AI 설정은 환경변수로 변경할 수 있음.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `SUBSCRIPTION_HISTORY_AI_MODEL` | `gpt-5.6-luna` | 비용 민감·대량 처리용 이력 요약 모델 |
| `SUBSCRIPTION_HISTORY_AI_MODEL_PROFILE` | `REASONING` | `STANDARD` 또는 `REASONING` 옵션 프로필 |
| `SUBSCRIPTION_HISTORY_AI_TEMPERATURE` | `0.0` | `STANDARD` 프로필의 요약 결과 변동성. `REASONING`에서는 전송하지 않음 |
| `SUBSCRIPTION_HISTORY_AI_MAX_OUTPUT_TOKENS` | `600` | 프로필별 최대 출력/completion 토큰 |
| `SUBSCRIPTION_HISTORY_AI_MAX_HISTORY_ITEMS` | `100` | 모델에 전달할 최대 이력 수. 초과 시 전체 이력 기반 fallback 사용 |
| `SUBSCRIPTION_HISTORY_AI_REASONING_EFFORT` | `LOW` | 추론 프로필의 reasoning effort |
| `SUBSCRIPTION_HISTORY_AI_MAX_CONCURRENT_CALLS` | `8` | 인스턴스당 동시 모델 호출 상한. 초과 요청은 즉시 fallback |
| `SUBSCRIPTION_HISTORY_AI_PROMPT_CACHE_KEY` | `subscription-history-summary-v1` | OpenAI prompt cache routing key |
| `SUBSCRIPTION_HISTORY_AI_TIMEOUT` | `3s` | 단일 OpenAI HTTP 호출 제한 시간 |
| `SUBSCRIPTION_HISTORY_AI_MAX_RETRIES` | `0` | OpenAI SDK 재시도 횟수. 기본은 빠른 fallback을 위해 비활성화 |

`STANDARD`는 `temperature + maxTokens`, `REASONING`은 `maxCompletionTokens + reasoningEffort`만 전송. 기본 모델인 [`gpt-5.6-luna`](https://developers.openai.com/api/docs/models/gpt-5.6-luna)는 `REASONING` 프로필과 `LOW` effort를 사용한다. Luna는 Chat Completions를 지원하지만 프로젝트별 실제 접근 권한은 운영 전 최소 호출로 확인해야 한다.

```properties
SUBSCRIPTION_HISTORY_AI_MODEL=gpt-5.6-luna
SUBSCRIPTION_HISTORY_AI_MODEL_PROFILE=REASONING
SUBSCRIPTION_HISTORY_AI_MAX_OUTPUT_TOKENS=600
SUBSCRIPTION_HISTORY_AI_REASONING_EFFORT=LOW
```

현재 애플리케이션 옵션 호환성을 검증한 조합은 `gpt-5.6-luna + REASONING`, `gpt-4o-mini + STANDARD`, `gpt-5-mini + REASONING`.
다른 모델은 옵션 호환성 테스트와 allowlist를 함께 확장해야 함.
공용 `spring.ai.openai.chat.*`에는 연결, timeout, retry만 두고 generation option은 이 도메인 설정에서 관리.

### Virtual thread 적용

- `spring.threads.virtual.enabled=true`로 Tomcat 요청 처리와 Boot 관리 executor/scheduler에 virtual thread를 적용다.
- 동기 `ChatClient.call()`은 virtual Tomcat 요청 스레드에서 대기하므로 별도 dispatcher executor를 만들지 않음.
- virtual thread는 외부 응답 시간을 줄이지 않습니다. blocking 대기 중 플랫폼 스레드 점유를 줄여 동시 처리량을 개선.

## 테스트

```shell
./gradlew test
```

Testcontainers(MySQL) 기반으로 실제 외부 API 호출 없이 다음을 검증.

- 승인 호출의 트랜잭션/lock 경계 (`ChangeSubscriptionTransactionBoundaryIntegrationTest` 등)
- 동시 신규 구독 충돌과 unique constraint 방어
- 상태 FSM 전이 규칙, 채널 행위 가능 규칙, 휴대폰 번호 VO
- csrng 장애 시나리오(타임아웃, 5xx, 계약 위반, circuit open)별 예외 매핑
- application fallback 요약, prompt 리소스와 개인정보 제외, Spring AI runtime option
- QueryDSL 이력 조회 projection, API 요청/응답 계약
- ArchUnit 계층 의존 규칙
