package org.example.AIsvc.service;

import org.example.AIsvc.client.ApiManagementClient;
import org.example.AIsvc.client.CustomApiClient;
import org.example.AIsvc.client.GenericApiClient;
import org.example.AIsvc.dto.api_management.ApiUrlRequest;
import org.example.AIsvc.dto.api_management.ApiUrlResponse;
import org.example.AIsvc.dto.execution.ApiParameterDto;
import org.example.AIsvc.dto.execution.CustomApiResponseDto;
import org.example.AIsvc.dto.execution.ExternalApiInfoDto;
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
    private ApiManagementClient apiManagementClient;

    // ## @InjectMocks: 테스트 대상인 AIOrchestrationServiceImpl 객체를 생성하고, 위 Mock 객체들을 주입합니다.
    @InjectMocks
    private AIOrchestrationServiceImpl aiOrchestrationService;

    // ## 테스트에서 사용할 가짜 '레시피' 객체를 미리 선언해둡니다.
    private CustomApiResponseDto fakeRecipe;

    // ## @BeforeEach: 각 테스트가 실행되기 전에 이 메소드를 실행하여 테스트 환경을 초기화합니다.
    @BeforeEach
    void setUp() {
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
        String query = "location=서울"; // ### 사용자의 최초 입력 쿼리

        // ### 1. CustomApiClient가 가짜 레시피를 반환하도록 설정
        given(customApiClient.getApiDetails(customApiId)).willReturn(fakeRecipe);

        // ### 1-1. ApiManagementClient가 API URL 매핑을 반환하도록 설정
        ApiUrlResponse.ApiUrlDetail weatherApiUrl = new ApiUrlResponse.ApiUrlDetail("weather-api", "http://weather.com/api");
        ApiUrlResponse.ApiUrlDetail restaurantApiUrl = new ApiUrlResponse.ApiUrlDetail("restaurant-api", "http://restaurant.com/api");
        ApiUrlResponse apiUrlResponse = new ApiUrlResponse(List.of(weatherApiUrl, restaurantApiUrl));
        given(apiManagementClient.getApiUrls(any(ApiUrlRequest.class))).willReturn(apiUrlResponse);

        // ### 2. GenericApiClient의 첫 번째 호출(날씨 API)에 대한 가짜 응답 설정
        Map<String, Object> weatherResult = Map.of("temperature", 25);
        given(genericApiClient.executePost(eq(new URI("http://weather.com/api")), any())).willReturn(weatherResult);
        
        // ### 3. GenericApiClient의 두 번째 호출(맛집 API)에 대한 가짜 응답 설정
        Map<String, Object> restaurantResult = Map.of("restaurants", List.of("A식당", "B식당"));
        given(genericApiClient.executePost(eq(new URI("http://restaurant.com/api")), any())).willReturn(restaurantResult);

        // when - 실제 동작 수행
        Map<String, Object> finalResult = (Map<String, Object>) aiOrchestrationService.executeCustomApi(customApiId, query);

        // then - 결과 검증
        // ### 최종 결과에 두 API의 응답이 모두 포함되어 있는지 확인
        assertThat(finalResult).isNotNull();
        assertThat(finalResult.get("weather-api")).isEqualTo(weatherResult); // ### 날씨 API 결과 확인
        assertThat(finalResult.get("restaurant-api")).isEqualTo(restaurantResult); // ### 맛집 API 결과 확인
    }
}