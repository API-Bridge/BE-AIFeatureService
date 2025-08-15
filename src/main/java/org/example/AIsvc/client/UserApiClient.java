package org.example.AIsvc.client;

import org.example.AIsvc.dto.user.UserPlanResponse;
import org.example.AIsvc.dto.user.UserSecretResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-svc", url = "${services.user.url}")
public interface UserApiClient {

    // API 명세서에 정의된 `/internal/ai-service/users/{userId}/secret` 경로로 GET 요청 보냄
    @GetMapping("/internal/ai-service/users/{userId}/secret")
    UserSecretResponse getUserSecret(
            // URL 경로의 {userId} 부분에 이 파라미터 값을 채워 넣음
            @PathVariable("userId") String userId
    );

    // 사용자의 구독 플랜 정보를 조회하기 위한 메소드 추가
    @GetMapping("/users/{userId}/subscription") // API 명세서에 따른 경로
    UserPlanResponse getUserPlan(@PathVariable("userId") String userId);
}