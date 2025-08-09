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

import org.example.AIsvc.dto.response.AnalyzeQueryResponse;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse.AnalysisResultDto;
import org.example.AIsvc.enums.ApiDomain;
import org.example.AIsvc.enums.ApiKeyword;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class AIServiceImplTest {

    @Mock
    private GeminiClient geminiClient;
    @Mock
    private LLMResponseParser llmResponseParser;

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

    @Test
    @DisplayName("성공: Gemini 응답을 파싱하여 최종 응답 객체에 포함시킨다")
    void analyzeAndInitiateCreation_parsesResponseAndReturnsResult() {
        // given
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(aiService, "model", "gemini-test-model");
        AnalyzeQueryRequest request = new AnalyzeQueryRequest("서울 날씨와 미세먼지", "seoul-air-weather", false);

        // 1. Gemini 클라이언트의 가짜 응답을 정의
        String fakeLlmResponseJson = "{\"domains\":[\"weather\"], \"keywords\":[\"current_weather\", \"air_quality\"]}";
        GeminiResponse.Part part = new GeminiResponse.Part(fakeLlmResponseJson);
        GeminiResponse.Content content = new GeminiResponse.Content(Collections.singletonList(part), "model");
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate(content);
        GeminiResponse fakeGeminiResponse = new GeminiResponse(Collections.singletonList(candidate));
        given(geminiClient.generateContent(anyString(), anyString(), any())).willReturn(fakeGeminiResponse);

        // 2. LLM 파서의 가짜 동작을 정의
        AnalysisResultDto fakeParsedResult = AnalysisResultDto.builder()
                .detectedDomains(Collections.singletonList(ApiDomain.WEATHER.name().toLowerCase()))
                .detectedKeywords(List.of(
                        ApiKeyword.CURRENT_WEATHER.name().toLowerCase(),
                        ApiKeyword.AIR_QUALITY.name().toLowerCase()
                ))
                .build();
        given(llmResponseParser.parse(fakeLlmResponseJson)).willReturn(fakeParsedResult);

        // when
        AnalyzeQueryResponse finalResponse = aiService.analyzeAndInitiateCreation(request);

        // then
        assertThat(finalResponse).isNotNull();
        assertThat(finalResponse.getRequestedApiId()).isEqualTo("seoul-air-weather");
        assertThat(finalResponse.getAnalysisResult()).isNotNull();
        assertThat(finalResponse.getAnalysisResult().getDetectedDomains())
                .containsExactly(ApiDomain.WEATHER.name().toLowerCase());

        assertThat(finalResponse.getAnalysisResult().getDetectedKeywords()).containsExactlyInAnyOrder(
                ApiKeyword.CURRENT_WEATHER.name().toLowerCase(),
                ApiKeyword.AIR_QUALITY.name().toLowerCase()
        );    }
}