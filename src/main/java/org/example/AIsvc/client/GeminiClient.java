package org.example.AIsvc.client;

import org.example.AIsvc.dto.gemini.GeminiRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gemini-api", url = "${gemini.api.url}")
public interface GeminiClient {

    @PostMapping("/v1beta/models/{model}:generateContent")
    GeminiResponse generateContent(
            @PathVariable("model") String model,
            @RequestParam("key") String apiKey,
            @RequestBody GeminiRequest request
    );
}