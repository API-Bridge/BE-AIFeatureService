package org.example.AIsvc.client;

import org.example.AIsvc.dto.custom_api.InitiateCreationRequest;
import org.springframework.cloud.openfeign.FeignClient;
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
}