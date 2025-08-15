package org.example.AIsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.example.AIsvc.client.CustomApiClient;
import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.dto.custom_api.InitiateCreationRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.example.AIsvc.dto.request.AnalyzeQueryRequest;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse;
import org.example.AIsvc.dto.response.AnalyzeQueryResponse.AnalysisResultDto;
import org.example.AIsvc.enums.ApiDomain;
import org.example.AIsvc.enums.ApiKeyword;
import org.example.AIsvc.event.model.ApiAnalysisEvent;
import org.example.AIsvc.event.publisher.EventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AIServiceImplTest {

    @Mock
    private GeminiClient geminiClient;
    @Mock
    private LLMResponseParser llmResponseParser;
    @Mock
    private CustomApiClient customApiClient;
    @Mock
    private Environment environment;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ObjectWriter objectWriter;

    @InjectMocks
    private AIServiceImpl aiService;

    @Test
    @DisplayName("성공: Gemini 응답을 파싱하여 최종 응답 객체에 포함시킨다")
    void analyzeAndInitiateCreation_parsesResponseAndReturnsResult() {
        // given
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(aiService, "model", "gemini-test-model");

        String userId = "auth0|user-123";
        AnalyzeQueryRequest request = new AnalyzeQueryRequest("서울 날씨와 미세먼지", "seoul-air-weather", false);

        // 0. dev 프로필을 시뮬레이션 (CustomAPI 호출 건너뜀)
        given(environment.acceptsProfiles(any(org.springframework.core.env.Profiles.class))).willReturn(true);

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
        AnalyzeQueryResponse finalResponse = aiService.analyzeAndInitiateCreation(request, userId);

        // then
        assertThat(finalResponse).isNotNull();
        assertThat(finalResponse.getRequestedApiId()).isEqualTo("seoul-air-weather");
        assertThat(finalResponse.getAnalysisResult()).isNotNull();
        assertThat(finalResponse.getAnalysisResult().getDetectedDomains())
                .containsExactly(ApiDomain.WEATHER.name().toLowerCase());

        assertThat(finalResponse.getAnalysisResult().getDetectedKeywords()).containsExactlyInAnyOrder(
                ApiKeyword.CURRENT_WEATHER.name().toLowerCase(),
                ApiKeyword.AIR_QUALITY.name().toLowerCase()
        );
    }

    @Test
    @DisplayName("성공: 분석 결과를 CustomApiClient로 전달하고 Kafka 이벤트를 발행한다")
    void analyzeAndInitiateCreation_sendsResultToCustomApiClient() throws JsonProcessingException{
        // given
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(aiService, "model", "gemini-test-model");

        String userId = "auth0|user-123";
        AnalyzeQueryRequest request = new AnalyzeQueryRequest("서울 날씨와 미세먼지", "seoul-air-weather", false);

        // 0. prod 프로필을 시뮬레이션 (CustomAPI 호출 실행)
        given(environment.acceptsProfiles(any(org.springframework.core.env.Profiles.class))).willReturn(false);

        // ### 1. Gemini 클라이언트의 가짜 응답 정의
        String fakeLlmResponseJson = "{\"domains\":[\"weather\"], \"keywords\":[\"current_weather\", \"air_quality\"]}";
        GeminiResponse.Part part = new GeminiResponse.Part(fakeLlmResponseJson);
        GeminiResponse.Content content = new GeminiResponse.Content(Collections.singletonList(part), "model");
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate(content);
        GeminiResponse fakeGeminiResponse = new GeminiResponse(Collections.singletonList(candidate));
        given(geminiClient.generateContent(anyString(), anyString(), any())).willReturn(fakeGeminiResponse);

        // 2. LLM 파서의 가짜 동작 정의
        AnalysisResultDto fakeParsedResult = AnalysisResultDto.builder()
                .detectedDomains(Collections.singletonList("weather"))
                .detectedKeywords(List.of("current_weather", "air_quality"))
                .build();
        given(llmResponseParser.parse(anyString())).willReturn(fakeParsedResult);
        given(objectMapper.writerWithDefaultPrettyPrinter()).willReturn(objectWriter);
        given(objectWriter.writeValueAsString(any())).willReturn("{\"key\":\"value\"}");


        // when
        aiService.analyzeAndInitiateCreation(request, userId);

        // then
        ArgumentCaptor<InitiateCreationRequest> captor = ArgumentCaptor.forClass(InitiateCreationRequest.class);

        verify(customApiClient).initiateCreation(captor.capture());

        InitiateCreationRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.getUserId()).isEqualTo(userId);
        assertThat(capturedRequest.getCustomApiId()).isEqualTo("seoul-air-weather");
        assertThat(capturedRequest.getOriginalQuery()).isEqualTo(request.getQuery());
        assertThat(capturedRequest.getDomains()).containsExactly("weather");
        assertThat(capturedRequest.getKeywords()).containsExactlyInAnyOrder("current_weather", "air_quality");
        // eventPublisher의 publishEvent 메소드가 1번 호출되었는지 검증
        verify(eventPublisher).publishEvent(anyString(), any(ApiAnalysisEvent.class));
    }
}