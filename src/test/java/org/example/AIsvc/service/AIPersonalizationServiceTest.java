package org.example.AIsvc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.client.UserApiClient;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.example.AIsvc.dto.user.UserPlanResponse;
import org.example.AIsvc.dto.user.UserSecretResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AIPersonalizationServiceTest {

    @Mock
    private UserApiClient userApiClient;
    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private AIPersonalizationServiceImpl personalizationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("성공: 무료 플랜 사용자가 AI+를 켜면 BYOK로 데이터를 가공한다")
    void personalize_FreeUser_WithAiPlus() throws Exception {
        // given
        String userId = "auth0|free-user";
        String userArn = "arn:aws:secretsmanager:...:user-gemini-key-xyz";
        Map<String, Object> rawData = new HashMap<>(Map.of("weather", "맑음"));
        String personalizedSummary = "오늘 날씨는 맑습니다!";

        // 1. UserApiClient의 가짜 동작 정의
        given(userApiClient.getUserPlan(userId)).willReturn(new UserPlanResponse("FREE"));
        given(userApiClient.getUserSecret(userId)).willReturn(new UserSecretResponse(userArn, "사용자 키"));

        // 2. GeminiClient의 가짜 동작 정의
        GeminiResponse.Part part = new GeminiResponse.Part(personalizedSummary);
        GeminiResponse.Content content = new GeminiResponse.Content(Collections.singletonList(part), "model");
        given(geminiClient.generateContent(anyString(), anyString(), any())).willReturn(new GeminiResponse(Collections.singletonList(new GeminiResponse.Candidate(content))));

//        // 3. ObjectMapper의 가짜 동작 정의
//        given(objectMapper.convertValue(rawData, new TypeReference<Map<String, Object>>() {})).willReturn((Map<String, Object>) rawData);

        // 4. @Value 필드 값 설정
        ReflectionTestUtils.setField(personalizationService, "systemGeminiApiKey", "system-api-key");
        ReflectionTestUtils.setField(personalizationService, "model", "gemini-pro");
        ReflectionTestUtils.setField(personalizationService, "objectMapper", this.objectMapper);

        // when
        Map<String, Object> result = personalizationService.personalize(userId, rawData);

        // then
        assertThat(result.get("summary")).isEqualTo(personalizedSummary);
        verify(userApiClient).getUserPlan(userId); // 플랜 조회 호출 확인
        verify(userApiClient).getUserSecret(userId); // BYOK 키 조회 호출 확인
    }

    @Test
    @DisplayName("성공: 프로 플랜 사용자가 AI+를 켜면 시스템 키로 데이터를 가공한다")
    void personalize_ProUser_WithAiPlus() throws Exception {
        // given
        String userId = "auth0|pro-user";
        Map<String, Object> rawData = new HashMap<>(Map.of("stock", "상승"));
        String personalizedInsights = "주식 시장이 활기를 띠고 있습니다.";
        String fakeLlmResultJson = "{\"summary\":\"요약문\",\"insights\":\"주식 시장이 활기를 띠고 있습니다.\",\"predictions\":\"예측\"}";
        Map<String, Object> fakePersonalizedData = Map.of("insights", personalizedInsights);

        // 1. UserApiClient는 플랜 정보만 반환하도록 설정
        given(userApiClient.getUserPlan(userId)).willReturn(new UserPlanResponse("PRO"));

        // 2. GeminiClient는 인사이트가 포함된 응답을 반환하도록 설정
        GeminiResponse.Part part = new GeminiResponse.Part(fakeLlmResultJson);
        given(geminiClient.generateContent(anyString(), anyString(), any())).willReturn(new GeminiResponse(Collections.singletonList(new GeminiResponse.Candidate(new GeminiResponse.Content(Collections.singletonList(part), "model")))));

        // (ObjectMapper, @Value 설정은 위와 유사하게)
//        given(objectMapper.writeValueAsString(rawData)).willReturn("{\"stock\":\"상승\"}");
//        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(fakePersonalizedData);
//        given(objectMapper.convertValue(rawData, new TypeReference<Map<String, Object>>() {})).willReturn((Map<String, Object>) rawData);
        ReflectionTestUtils.setField(personalizationService, "systemGeminiApiKey", "system-api-key");
        ReflectionTestUtils.setField(personalizationService, "model", "gemini-pro");
        ReflectionTestUtils.setField(personalizationService, "objectMapper", this.objectMapper);

        // when
        Map<String, Object> result = personalizationService.personalize(userId, rawData);

        // then
        assertThat(result.get("insights")).isEqualTo(personalizedInsights);
        verify(userApiClient).getUserPlan(userId); // ### 플랜 조회는 호출
        verify(userApiClient, never()).getUserSecret(userId); // ### BYOK 키 조회는 절대 호출되지 않아야 함
    }

    @Test
    @DisplayName("실패: 무료 사용자가 BYOK 키 없이 AI+를 사용하려 하면 RuntimeException 발생")
    void personalize_FreeUser_WithoutBYOK_ThrowsException() {
        // given
        String userId = "auth0|free-user-no-key";
        Map<String, Object> rawData = new HashMap<>(Map.of("weather", "맑음"));

        // 1. UserApiClient가 FREE 플랜과 키 없음을 반환하도록 설정
        given(userApiClient.getUserPlan(userId)).willReturn(new UserPlanResponse("FREE"));
        given(userApiClient.getUserSecret(userId)).willReturn(new UserSecretResponse(null, null)); // 키가 없음

        // 4. @Value 필드 값 설정
        ReflectionTestUtils.setField(personalizationService, "systemGeminiApiKey", "system-api-key");
        ReflectionTestUtils.setField(personalizationService, "model", "gemini-pro");
        ReflectionTestUtils.setField(personalizationService, "objectMapper", this.objectMapper);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            personalizationService.personalize(userId, rawData);
        });

        assertThat(exception.getMessage()).isEqualTo("AI+ 기능을 사용하려면 먼저 API 키를 설정해주세요.");
        verify(userApiClient).getUserPlan(userId); // 플랜 조회는 호출됨
        verify(userApiClient).getUserSecret(userId); // BYOK 키 조회도 호출됨
    }
}