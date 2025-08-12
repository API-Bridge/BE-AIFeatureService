package org.example.AIsvc.client;

import org.example.AIsvc.dto.api_management.ApiUrlRequest;
import org.example.AIsvc.dto.api_management.ApiUrlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "api-management-svc", url = "${services.api-management.url}")
public interface ApiManagementClient {

    // HTTP POST 요청을 보낼 세부 경로를 지정
    // (예: http://localhost:8082/api/internal/apis/urls)
    @PostMapping("/internal/apis/urls")
    ApiUrlResponse getApiUrls(
            @RequestBody ApiUrlRequest request
    );
}