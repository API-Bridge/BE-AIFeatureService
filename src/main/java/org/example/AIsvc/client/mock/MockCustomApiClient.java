package org.example.AIsvc.client.mock;

import org.example.AIsvc.client.CustomApiClient;
import org.example.AIsvc.dto.custom_api.InitiateCreationRequest;
import org.example.AIsvc.dto.execution.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Profile("demo") // 'demo' 프로필에서만 이 가짜 클라이언트가 진짜를 대신합니다.
@org.springframework.context.annotation.Primary
public class MockCustomApiClient implements CustomApiClient {

    @Override
    public CustomApiResponseDto getApiDetails(String customApiId) {
        // executeCustomApi를 위한 가짜 'API 실행 계획'을 반환합니다.
        ApiParameterDto param = new ApiParameterDto("query", "INPUT", "검색어", true);
        ExternalApiInfoDto apiInfo = new ExternalApiInfoDto("mock-search-api", "Mock 검색 API", "http://mock.search.com/api", List.of(param));
        return new CustomApiResponseDto("test-api", "user-123", "테스트 API", "가짜 레시피", List.of(apiInfo), null, null);
    }

    @Override
    public void initiateCreation(InitiateCreationRequest request) {
        // analyze-query는 호출만 성공하면 되므로 아무것도 하지 않습니다.
    }
}