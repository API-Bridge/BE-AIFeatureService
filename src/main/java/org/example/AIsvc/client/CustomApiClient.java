package org.example.AIsvc.client;

import org.example.AIsvc.dto.custom_api.InitiateCreationRequest;
import org.example.AIsvc.dto.execution.CustomApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// name="custom-api-svc": 이 클라이언트의 고유 이름 (서킷 브레이커 등에서 사용)
// url="${services.custom-api.url}": 호출할 `커스텀API 관리 서비스`의 URL을 application.yml에서 가져옴
@FeignClient(name = "custom-api-svc", url = "${services.custom-api.url}")
public interface CustomApiClient {

    @PostMapping("/custom-apis")
    void initiateCreation(
            @RequestBody InitiateCreationRequest request
    );
    /**
     * 특정 커스텀 API의 상세 설정 정보를 조회합니다. (API 실행 계획/레시피)
     * @param customApiId 조회할 커스텀 API의 고유 ID
     * @return 커스텀 API의 상세 정보
     */
    @GetMapping("/custom-apis/{customApiId}") // ## GET 메소드와 경로를 명세서에 맞게 지정합니다.
    CustomApiResponseDto getApiDetails(
            @PathVariable("customApiId") String customApiId // ## URL 경로의 {customApiId} 부분을 파라미터로 받습니다.
    );
}