package org.example.AIsvc.client.mock;

import org.example.AIsvc.client.ApiManagementClient;
import org.example.AIsvc.dto.api_management.ApiUrlRequest;
import org.example.AIsvc.dto.api_management.ApiUrlResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Profile("demo")
@org.springframework.context.annotation.Primary
public class MockApiManagementClient implements ApiManagementClient {

    @Override
    public ApiUrlResponse getApiUrls(ApiUrlRequest request) {
        // 가짜 '실행 URL'을 반환합니다.
        ApiUrlResponse.ApiUrlDetail urlDetail = new ApiUrlResponse.ApiUrlDetail("mock-search-api", "http://mock.search.com/api");
        return new ApiUrlResponse(List.of(urlDetail));
    }
}