package org.example.AIsvc.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.dto.custom_api.InitiateCreationRequest;
import org.example.AIsvc.dto.execution.CustomApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// name="custom-api-svc": 이 클라이언트의 고유 이름 (서킷 브레이커 등에서 사용)
// url="${services.custom-api.url}": 호출할 `커스텀API 관리 서비스`의 URL을 application.yml에서 가져옴
@FeignClient(name = "custom-api-svc", url = "${services.custom-api.url}", fallback = CustomApiClient.CustomApiClientFallback.class)
public interface CustomApiClient {

    @PostMapping("/custom-apis")
    @CircuitBreaker(name = "custom-api-svc")
    void initiateCreation(@RequestBody InitiateCreationRequest request);

    /**
     * 특정 커스텀 API의 상세 설정 정보를 조회합니다. (API 실행 계획/레시피)
     * @param customApiId 조회할 커스텀 API의 고유 ID
     * @return 커스텀 API의 상세 정보
     */
    @GetMapping("/custom-apis/{customApiId}")
    @CircuitBreaker(name = "custom-api-svc")
    CustomApiResponseDto getApiDetails(@PathVariable("customApiId") String customApiId);

    @Slf4j
    @Component
    class CustomApiClientFallback implements CustomApiClient {
        @Override
        public void initiateCreation(InitiateCreationRequest request) {
            // 반환 타입이 void인 경우, 에러 로그만 남기고 아무것도 하지 않아 장애 전파를 막음
            log.error("CustomApiClient initiateCreation fallback executed for user: {}", request.getUserId());
        }

        @Override
        public CustomApiResponseDto getApiDetails(String customApiId) {
            log.error("CustomApiClient getApiDetails fallback executed for customApiId: {}", customApiId);
            // API 상세 정보 조회 실패 시, null을 반환하여 호출한 쪽에서 실패를 인지하고 처리
            return null;
        }
    }
}