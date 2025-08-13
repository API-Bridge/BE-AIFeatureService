package org.example.AIsvc.service;

import org.example.AIsvc.client.ApiManagementClient;
import org.example.AIsvc.dto.api_management.ApiUrlRequest;
import org.example.AIsvc.dto.api_management.ApiUrlResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AIUsageServiceTest {

    // 테스트용 가짜 API 관리 서비스
    @Mock
    private ApiManagementClient apiManagementClient;

    @InjectMocks
    private AIUsageServiceImpl aiUsageService;

    @Test
    @DisplayName("성공: API ID 목록을 받아 URL 맵으로 정확히 변환한다")
    void resolveApiUrls_Success() {
        // given
        // 변환을 요청할 API ID 목록 정의
        List<String> apiIds = List.of("weather-api-id", "news-api-id");

        // apiManagementClient가 반환할 가짜 응답 데이터를 미리 정의
        List<ApiUrlResponse.ApiUrlDetail> fakeApiDetails = List.of(
                new ApiUrlResponse.ApiUrlDetail("weather-api-id", "http://weather.com/api"),
                new ApiUrlResponse.ApiUrlDetail("news-api-id", "http://news.com/api")
        );
        ApiUrlResponse fakeResponse = new ApiUrlResponse(fakeApiDetails);

        // apiManagementClient의 getApiUrls 메소드가 어떤 입력으로 호출되든, fakeResponse를 반환
        given(apiManagementClient.getApiUrls(any())).willReturn(fakeResponse);

        // when
        Map<String, String> resolvedUrls = aiUsageService.resolveApiUrls(apiIds);

        // then
        ArgumentCaptor<ApiUrlRequest> captor = ArgumentCaptor.forClass(ApiUrlRequest.class);
        
        // verify: apiManagementClient의 getApiUrls 메소드가 정확히 1번 호출되었는지 검증
        // captor.capture(): 이때 메소드로 전달된 인자(ApiUrlRequest 객체)를 캡처
        verify(apiManagementClient).getApiUrls(captor.capture());
        
        // 캡처된 요청의 apiIds가 우리가 처음에 넣은 apiIds와 같은지 확인
        assertThat(captor.getValue().getApiIds()).isEqualTo(apiIds);

        // 최종 반환된 Map의 크기와 내용이 정확한지 확인
        assertThat(resolvedUrls).hasSize(2);
        assertThat(resolvedUrls.get("weather-api-id")).isEqualTo("http://weather.com/api");
        assertThat(resolvedUrls.get("news-api-id")).isEqualTo("http://news.com/api");
    }
}