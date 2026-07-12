# ARTINUS Backend

구독 상태 변경과 변경 이력 조회를 제공하는 Spring Boot 애플리케이션입니다. 이력 조회 시 Spring AI로 자연어 요약을 생성하며, AI 호출이 불가능하면 동일한 이력 snapshot으로 규칙 기반 요약을 반환합니다.

## 실행

기본 실행은 AI 모델을 비활성화하므로 OpenAI API Key 없이도 동작합니다.

```shell
docker compose up --build
```

OpenAI 요약을 활성화하려면 `.env.example`을 참고해 다음 값을 설정합니다.

```properties
AI_CHAT_PROVIDER=openai
OPENAI_API_KEY=your-api-key
```

`AI_CHAT_PROVIDER=openai`인데 API Key가 비어 있으면 애플리케이션 시작 단계에서 실패합니다.

주요 AI 설정은 환경변수로 변경할 수 있습니다.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `SUBSCRIPTION_HISTORY_AI_MODEL` | `gpt-4o-mini` | 이력 요약용 비추론 모델 |
| `SUBSCRIPTION_HISTORY_AI_MODEL_PROFILE` | `STANDARD` | `STANDARD` 또는 `REASONING` 옵션 프로필 |
| `SUBSCRIPTION_HISTORY_AI_TEMPERATURE` | `0.0` | 요약 결과의 변동성 |
| `SUBSCRIPTION_HISTORY_AI_MAX_OUTPUT_TOKENS` | `300` | 프로필별 최대 출력/completion 토큰 |
| `SUBSCRIPTION_HISTORY_AI_MAX_HISTORY_ITEMS` | `100` | 모델에 전달할 최대 이력 수. 초과 시 전체 이력 기반 fallback 사용 |
| `SUBSCRIPTION_HISTORY_AI_REASONING_EFFORT` | `LOW` | 추론 프로필의 reasoning effort |
| `SUBSCRIPTION_HISTORY_AI_MAX_CONCURRENT_CALLS` | `8` | 인스턴스당 동시 모델 호출 상한. 초과 요청은 즉시 fallback |
| `SUBSCRIPTION_HISTORY_AI_PROMPT_CACHE_KEY` | `subscription-history-summary-v1` | OpenAI prompt cache routing key |
| `SUBSCRIPTION_HISTORY_AI_TIMEOUT` | `3s` | 단일 OpenAI HTTP 호출 제한 시간 |
| `SUBSCRIPTION_HISTORY_AI_MAX_RETRIES` | `0` | OpenAI SDK 재시도 횟수. 기본은 빠른 fallback을 위해 비활성화 |

`STANDARD`는 `temperature + maxTokens`, `REASONING`은 `maxCompletionTokens + reasoningEffort`만 전송합니다. GPT-5/o-series 같은 추론 모델을 사용할 때는 모델과 함께 프로필도 `REASONING`으로 변경해야 합니다.

```properties
SUBSCRIPTION_HISTORY_AI_MODEL=gpt-5-mini
SUBSCRIPTION_HISTORY_AI_MODEL_PROFILE=REASONING
SUBSCRIPTION_HISTORY_AI_MAX_OUTPUT_TOKENS=600
SUBSCRIPTION_HISTORY_AI_REASONING_EFFORT=LOW
```

현재 검증된 조합은 `gpt-4o-mini + STANDARD`, `gpt-5-mini + REASONING`과 각 모델의 snapshot suffix입니다. 다른 모델은 옵션 호환성 테스트와 allowlist를 함께 확장해야 합니다. 모델별 지원 effort가 다르므로 실제 모델 문서에 맞춰 값을 선택해야 합니다. 공용 `spring.ai.openai.chat.*`에는 연결, timeout, retry만 두고 generation option은 이 도메인 설정에서 관리합니다.

## 구독 변경 transaction 설계

구독·해지는 외부 승인 지연이 DB connection과 회원 row lock으로 전파되지 않도록 다음 단계로 실행합니다.

1. 짧은 read-only transaction에서 Channel, 회원, 상태 전이를 사전 검증합니다.
2. DB transaction과 row lock이 없는 상태에서 CSRNG 승인을 요청합니다.
3. 승인 후 짧은 쓰기 transaction에서 Channel과 회원을 다시 검증하고, 회원을 비관적 잠금으로 조회한 뒤 현재 상태에서 전이와 이력을 저장합니다.

사전 검증은 잘못된 요청으로 외부 API를 호출하지 않기 위한 절차이고, 승인 후 재검증은 동시 요청 중 바뀐 상태를 오래된 이력으로 저장하지 않기 위한 정합성 장치입니다. 변경 use case는 이미 transaction이 시작된 호출자에서는 실행하지 않습니다.

- 승인 거절·장애 시 쓰기 transaction을 시작하지 않으므로 회원과 이력은 변경되지 않습니다.
- 승인 후 DB 저장이 실패하면 회원과 이력을 같은 로컬 transaction에서 함께 rollback합니다.
- CSRNG는 외부에 비즈니스 상태를 남기지 않는 난수 API이므로 현재는 별도 보상 transaction이나 멱등성 key를 두지 않습니다.
- 동일 휴대폰 번호의 신규 구독 충돌은 unique constraint를 최종 방어선으로 사용하고 `409 SUBSCRIPTION_CONFLICT`로 변환합니다.

Subscription과 Channel의 경계는 [`docs/design/05-subscription-channel-boundary.md`](docs/design/05-subscription-channel-boundary.md)에 문서화했습니다.

## 이력 조회 API

```http
GET /api/v1/subscriptions/{phoneNumber}/histories
```

응답에는 시간순 이력, 요약, 요약 출처가 포함됩니다.

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

## AI 설계

- application 계층의 `SubscriptionHistorySummarizer`는 Spring AI 타입을 노출하지 않습니다.
- Spring AI와 OpenAI 전용 옵션은 `subscription/adapter/outbound/ai`에 격리했습니다.
- system/user prompt는 `src/main/resources/prompt/subscription-history-summary`에서 버전 관리합니다.
- 휴대폰 번호와 회원 ID는 모델 입력에 포함하지 않습니다.
- application service가 호출자의 transaction을 suspend하고, 별도 DB read transaction에서 정렬된 snapshot을 만든 뒤 transaction 밖에서 LLM을 호출합니다.
- timeout, OpenAI 오류, 빈 응답, bulkhead 포화, circuit open에서는 `summarySource=FALLBACK`으로 조회 자체는 성공합니다.
- 예상하지 못한 adapter 구현 오류는 fallback으로 숨기지 않고 예외로 노출합니다.
- 모델 입력 이력이 설정한 최대 건수를 넘으면 외부 호출을 생략하고 전체 이력 기반 fallback을 사용합니다.
- semaphore bulkhead가 인스턴스당 동시 호출을 제한하며 포화 요청은 대기하지 않습니다. circuit breaker는 실제 외부 호출 실패만 집계합니다.
- timeout은 HTTP 시도당 제한입니다. 기본 retry를 0으로 두어 응답 지연과 중복 생성·과금 가능성을 제한합니다.
- provider prompt cache만 사용하며, 다른 회원의 유사 이력을 오인할 수 있는 semantic response cache는 사용하지 않습니다.
- `spring.threads.virtual.enabled=true`로 Tomcat 요청 처리와 Boot 관리 executor/scheduler에 virtual thread를 적용했습니다.
- 현재 동기 `ChatClient.call()`은 virtual Tomcat 요청 스레드에서 대기하므로 별도 OkHttp dispatcher executor를 만들지 않습니다.
- virtual thread는 외부 응답 시간을 줄이지 않습니다. blocking 대기 중 플랫폼 스레드 점유를 줄여 동시 처리량을 개선하는 수단입니다.

## 검증

```shell
./gradlew test
```

테스트는 승인 호출의 transaction/lock 경계, 동시 신규 구독 충돌, application fallback, prompt 리소스와 개인정보 제외, Spring AI runtime option, QueryDSL 이력 조회, API 응답을 실제 외부 API 호출 없이 검증합니다. ArchUnit은 `domain -> application/adapter`, `application -> adapter` 역방향 의존을 빌드에서 차단합니다.
