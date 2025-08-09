package org.example.AIsvc.service;

import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AIServiceImplTest {

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private AIServiceImpl aiService;

    @Test
    @DisplayName("성공: 쿼리 분석 시 Gemini 클라이언트를 1회 호출한다")
    void analyzeAndInitiateCreation_callsGeminiClient() {
        // given
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(aiService, "model", "gemini-test-model");

        AnalyzeQueryRequest request = new AnalyzeQueryRequest("서울 날씨 알려줘", null, false);

        GeminiResponse.Part part = new GeminiResponse.Part("{\"domains\":[\"weather\"]}");
        GeminiResponse.Content content = new GeminiResponse.Content(Collections.singletonList(part), "model");
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate(content);
        GeminiResponse fakeResponse = new GeminiResponse(Collections.singletonList(candidate));

        given(geminiClient.generateContent(anyString(), anyString(), any())).willReturn(fakeResponse);

        // when
        aiService.analyzeAndInitiateCreation(request);

        // then
        verify(geminiClient).generateContent(anyString(), anyString(), any());
    }
}