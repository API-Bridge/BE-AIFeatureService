package org.example.AIsvc.service;

import org.example.AIsvc.client.CustomApiClient;
import org.example.AIsvc.client.GenericApiClient;
import org.example.AIsvc.client.GeminiClient;
import org.example.AIsvc.dto.execution.ApiParameterDto;
import org.example.AIsvc.dto.execution.CustomApiResponseDto;
import org.example.AIsvc.dto.execution.ExternalApiInfoDto;
import org.example.AIsvc.dto.common.BaseResponse;
import org.example.AIsvc.dto.gemini.GeminiRequest;
import org.example.AIsvc.dto.gemini.GeminiResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AIOrchestrationServiceTest {

    @Mock
    private CustomApiClient customApiClient;
    @Mock
    private GenericApiClient genericApiClient;
    @Mock
    private GeminiClient geminiClient;
    @Mock
    private AIPersonalizationService aiPersonalizationService;

    // ## @InjectMocks: 테스트 대상인 AIOrchestrationServiceImpl 객체를 생성하고, 위 Mock 객체들을 주입합니다.
    @InjectMocks
    private AIOrchestrationServiceImpl aiOrchestrationService;

    // ## 테스트에서 사용할 가짜 '레시피' 객체를 미리 선언해둡니다.
    private CustomApiResponseDto fakeRecipe;

    // ## @BeforeEach: 각 테스트가 실행되기 전에 이 메소드를 실행하여 테스트 환경을 초기화합니다.
    @BeforeEach
    void setUp() {
        // Gemini API 설정값들을 테스트용으로 주입
        ReflectionTestUtils.setField(aiOrchestrationService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(aiOrchestrationService, "model", "test-model");
        // ### Step 1 (날씨 조회)의 파라미터 정의
        ApiParameterDto weatherInput = new ApiParameterDto("location", "INPUT", "도시 이름", true);
        ApiParameterDto weatherOutput = new ApiParameterDto("temperature", "OUTPUT", "현재 기온", false);
        // ### Step 1 (날씨 조회)의 정보 정의
        ExternalApiInfoDto weatherApiInfo = new ExternalApiInfoDto("weather-api", "날씨 API", "http://weather.com/api", List.of(weatherInput, weatherOutput));

        // ### Step 2 (맛집 추천)의 파라미터 정의
        ApiParameterDto restaurantInput = new ApiParameterDto("area", "INPUT", "지역명", true);
        ApiParameterDto restaurantOutput = new ApiParameterDto("restaurants", "OUTPUT", "맛집 목록", false);
        // ### Step 2 (맛집 추천)의 정보 정의
        ExternalApiInfoDto restaurantApiInfo = new ExternalApiInfoDto("restaurant-api", "맛집 API", "http://restaurant.com/api", List.of(restaurantInput, restaurantOutput));
        
        // ### 최종 '레시피' (CustomApiResponseDto)를 생성합니다.
        fakeRecipe = new CustomApiResponseDto("custom-123", "user-abc", "날씨 기반 맛집 추천", "", List.of(weatherApiInfo, restaurantApiInfo), null, null);
    }

    @Test
    @DisplayName("성공: API 레시피에 따라 외부 API들을 순차적으로 호출하고 결과를 조합한다")
    void executeCustomApi_Success() throws Exception {
        // given - 테스트 준비
        String customApiId = "custom-123";
        String query = "서울의 날씨 정보와 함께 맛집도 추천해주세요"; // ### 사용자의 자연어 쿼리

        // ### 1. CustomApiClient가 BaseResponse로 감싼 가짜 레시피를 반환하도록 설정
        BaseResponse<CustomApiResponseDto> successResponse = BaseResponse.success(fakeRecipe);
        given(customApiClient.getApiDetails(customApiId)).willReturn(successResponse);

        // ### 1-1. GeminiClient AI 파라미터 준비 응답 설정
        GeminiResponse.Part weatherPart = new GeminiResponse.Part("{\"location\": \"서울\"}");
        GeminiResponse.Content weatherContent = new GeminiResponse.Content(List.of(weatherPart), "model");
        GeminiResponse.Candidate weatherCandidate = new GeminiResponse.Candidate(weatherContent);
        GeminiResponse weatherAiResponse = new GeminiResponse(List.of(weatherCandidate));

        GeminiResponse.Part restaurantPart = new GeminiResponse.Part("{\"area\": \"서울\"}");
        GeminiResponse.Content restaurantContent = new GeminiResponse.Content(List.of(restaurantPart), "model");
        GeminiResponse.Candidate restaurantCandidate = new GeminiResponse.Candidate(restaurantContent);
        GeminiResponse restaurantAiResponse = new GeminiResponse(List.of(restaurantCandidate));
        
        given(geminiClient.generateContent(eq("test-model"), eq("test-api-key"), any(GeminiRequest.class)))
            .willReturn(weatherAiResponse)
            .willReturn(restaurantAiResponse);

        // ### 2. GenericApiClient의 첫 번째 호출(날씨 API)에 대한 가짜 응답 설정
        Map<String, Object> weatherResult = Map.of("temperature", 25);
        given(genericApiClient.executePost(eq(new URI("http://weather.com/api")), any())).willReturn(weatherResult);
        
        // ### 3. GenericApiClient의 두 번째 호출(맛집 API)에 대한 가짜 응답 설정
        Map<String, Object> restaurantResult = Map.of("restaurants", List.of("A식당", "B식당"));
        given(genericApiClient.executePost(eq(new URI("http://restaurant.com/api")), any())).willReturn(restaurantResult);

        // when - 실제 동작 수행
        Map<String, Object> finalResult = (Map<String, Object>) aiOrchestrationService.executeCustomApi(customApiId, query, "test-user", false);

        // then - 결과 검증
        // ### 최종 결과에 data 필드가 있고, 그 안에 두 API의 응답이 모두 포함되어 있는지 확인
        assertThat(finalResult).isNotNull();
        assertThat(finalResult.get("data")).isNotNull();
        
        Map<String, Object> data = (Map<String, Object>) finalResult.get("data");
        assertThat(data.get("weather-api")).isEqualTo(weatherResult); // ### 날씨 API 결과 확인
        assertThat(data.get("restaurant-api")).isEqualTo(restaurantResult); // ### 맛집 API 결과 확인
    }

    @Test
    @DisplayName("성공: AI+ 기능이 켜져있을 때 개인화된 결과를 반환한다")
    void executeCustomApi_WithAiPlus_Success() throws Exception {
        // given - 테스트 준비
        String customApiId = "custom-123";
        String query = "서울의 날씨와 추천 맛집을 알려주세요";
        String userId = "auth0|user-123";

        // ### 1. CustomApiClient가 BaseResponse로 감싼 가짜 레시피를 반환하도록 설정
        BaseResponse<CustomApiResponseDto> successResponse = BaseResponse.success(fakeRecipe);
        given(customApiClient.getApiDetails(customApiId)).willReturn(successResponse);

        // ### 1-1. GeminiClient AI 파라미터 준비 응답 설정
        GeminiResponse.Part weatherPart = new GeminiResponse.Part("{\"location\": \"서울\"}");
        GeminiResponse.Content weatherContent = new GeminiResponse.Content(List.of(weatherPart), "model");
        GeminiResponse.Candidate weatherCandidate = new GeminiResponse.Candidate(weatherContent);
        GeminiResponse weatherAiResponse = new GeminiResponse(List.of(weatherCandidate));

        GeminiResponse.Part restaurantPart = new GeminiResponse.Part("{\"area\": \"서울\"}");
        GeminiResponse.Content restaurantContent = new GeminiResponse.Content(List.of(restaurantPart), "model");
        GeminiResponse.Candidate restaurantCandidate = new GeminiResponse.Candidate(restaurantContent);
        GeminiResponse restaurantAiResponse = new GeminiResponse(List.of(restaurantCandidate));
        
        given(geminiClient.generateContent(eq("test-model"), eq("test-api-key"), any(GeminiRequest.class)))
            .willReturn(weatherAiResponse)
            .willReturn(restaurantAiResponse);

        // ### 2. GenericApiClient 호출 설정
        Map<String, Object> weatherResult = Map.of("temperature", 25);
        given(genericApiClient.executePost(eq(new URI("http://weather.com/api")), any())).willReturn(weatherResult);
        
        Map<String, Object> restaurantResult = Map.of("restaurants", List.of("A식당", "B식당"));
        given(genericApiClient.executePost(eq(new URI("http://restaurant.com/api")), any())).willReturn(restaurantResult);

        // ### 3. AI 개인화 서비스 mock 설정
        Map<String, Object> personalizedResult = Map.of(
            "summary", "서울의 맛집 추천 요약",
            "data", Map.of("weather-api", weatherResult, "restaurant-api", restaurantResult)
        );
        given(aiPersonalizationService.personalize(eq(userId), any())).willReturn(personalizedResult);

        // when - 실제 동작 수행 (AI+ 활성화)
        Map<String, Object> finalResult = (Map<String, Object>) aiOrchestrationService.executeCustomApi(customApiId, query, userId, true);

        // then - 결과 검증
        // ### AI+ 결과에 요약이 포함되어 있는지 확인
        assertThat(finalResult).isNotNull();
        assertThat(finalResult.get("summary")).isEqualTo("서울의 맛집 추천 요약");
        assertThat(finalResult.get("data")).isNotNull();
    }
}