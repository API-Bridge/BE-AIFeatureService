package org.example.AIsvc.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.dto.gemini.GeminiRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Collections;

@FeignClient(name = "gemini-api", url = "${gemini.api.url}", fallback = GeminiClient.GeminiClientFallback.class)
public interface GeminiClient {

    @PostMapping("/v1beta/models/{model}:generateContent")
    @CircuitBreaker(name = "gemini-api")
    GeminiResponse generateContent(
            @PathVariable("model") String model,
            @RequestParam("key") String apiKey,
            @RequestBody GeminiRequest request
    );

    @Slf4j
    @Component
    class GeminiClientFallback implements GeminiClient {
        // generateContent 메소드가 실패했을 때(서킷 OPEN 등) 이 대체 메소드가 대신 실행
        @Override
        public GeminiResponse generateContent(String model, String apiKey, GeminiRequest request) {
            // 로그를 남겨 어떤 API 호출이 실패했는지 기록
            log.error("GeminiClient generateContent fallback executed. model: {}, query: {}", model, request.getContents().get(0).getParts().get(0).getText());
            // AI의 응답이 실패했음을 알리는 메시지를 담아 기본 응답 객체를 반환
            GeminiResponse.Part part = new GeminiResponse.Part("{\"error\":\"AI 모델 호출에 실패했습니다.\"}");
            GeminiResponse.Content content = new GeminiResponse.Content(Collections.singletonList(part), "model");
            GeminiResponse.Candidate candidate = new GeminiResponse.Candidate(content);
            return new GeminiResponse(Collections.singletonList(candidate));
        }
    }
}