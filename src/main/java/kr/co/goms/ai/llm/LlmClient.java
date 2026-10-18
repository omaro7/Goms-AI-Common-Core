/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

/**
 * GomsBook AI Agent에서 LLM 요청을 실행하는 공통 인터페이스입니다.
 *
 * <p>
 * 애플리케이션과 Tool 계층은 OpenAI, Ollama, Gemini, Claude 등
 * 특정 Provider 구현체에 직접 의존하지 않고 이 인터페이스에만
 * 의존합니다.
 * </p>
 *
 * <p>
 * Provider별 요청 변환, HTTP 통신, 응답 정규화는
 * 실제 구현체가 담당합니다.
 * </p>
 * LlmClient client = new OllamaLlmClient(...);
 * 	client.requireAvailable();
 * 	LlmRequest request = LlmRequest.chat(
 *             "qwen2.5:7b",
 *             "당신은 EPUB3 XHTML 생성 전문가입니다.",
 *             "다음 원고를 XHTML로 생성해 주세요."
 *     );
 * 	LlmResponse response = client.generate(request);
 * 	System.out.println(response.firstText());
 */
public interface LlmClient {

    /**
     * LLM 요청을 실행합니다.
     *
     * @param request LLM 요청
     * @return LLM 응답
     */
    LlmResponse chat(LlmRequest request);
    

    /**
     * 현재 Client가 요청을 실행할 수 있는 상태인지 반환합니다.
     *
     * <p>
     * 기본값은 {@code true}입니다. Ollama 서버 상태, API Key,
     * 네트워크 연결 여부 등을 확인해야 하는 구현체는 이 메서드를
     * 재정의할 수 있습니다.
     * </p>
     *
     * @return 사용 가능하면 true
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Client 또는 Provider의 식별 이름을 반환합니다.
     *
     * <p>
     * 예: {@code ollama}, {@code openai}, {@code gemini}
     * </p>
     *
     * @return Client 이름
     */
    default String getName() {
        return getClass()
                .getSimpleName();
    }

    /**
     * Client 구현 버전을 반환합니다.
     *
     * @return Client 버전
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 지정한 요청을 현재 Client가 처리할 수 있는지 확인합니다.
     *
     * <p>
     * 기본 구현은 요청이 null이 아니고 Client가 사용 가능한 경우
     * 처리 가능한 것으로 판단합니다.
     * </p>
     *
     * @param request 확인할 요청
     * @return 처리 가능하면 true
     */
    default boolean supports(
            LlmRequest request
    ) {
        return request != null
                && isAvailable();
    }

    /**
     * Client 사용 가능 여부를 확인하고, 사용할 수 없으면 예외를 발생시킵니다.
     *
     * @throws LlmUnavailableException Client를 사용할 수 없는 경우
     */
    default void requireAvailable() {
        if (!isAvailable()) {
            throw new LlmUnavailableException(
                    getName()
            );
        }
    }
}