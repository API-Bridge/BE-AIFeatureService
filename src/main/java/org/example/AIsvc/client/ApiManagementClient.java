package org.example.AIsvc.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.example.AIsvc.dto.api_management.ApiUrlRequest;
import org.example.AIsvc.dto.api_management.ApiUrlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;

@FeignClient(name = "api-management-svc", url = "${services.api-management.url}", fallback = ApiManagementClient.ApiManagementClientFallback.class)
@org.springframework.context.annotation.Profile("!demo")
public interface ApiManagementClient {

    // name="api-management-svc": application.yml에 정의된 인스턴스 이름을 사용
    @CircuitBreaker(name = "api-management-svc")

    // HTTP POST 요청을 보낼 세부 경로를 지정
    // (예: http://localhost:8082/api/internal/apis/urls)
    @PostMapping("/internal/apis/urls")
    ApiUrlResponse getApiUrls(@RequestBody ApiUrlRequest request);

    @Component
    class ApiManagementClientFallback implements ApiManagementClient {
        // getApiUrls 메소드가 실패했을 때(서킷 OPEN 등) 이 대체 메소드가 대신 실행
        @Override
        public ApiUrlResponse getApiUrls(ApiUrlRequest request) {
            // 빈 리스트를 반환하여, 조회에 실패했음을 알리고 시스템이 멈추지 않도록 합니다.
            return new ApiUrlResponse(Collections.emptyList());
        }
    }
}