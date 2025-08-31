package org.example.AIsvc.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.dto.user.UserPlanResponse;
import org.example.AIsvc.dto.user.UserSecretResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-svc", url = "${services.user.url}", fallback = UserApiClient.UserApiClientFallback.class)
public interface UserApiClient {

    // API 명세서에 정의된 `/api/secrets/arn` 경로로 GET 요청 보냄
    @GetMapping("/api/secrets/arn")
    @CircuitBreaker(name = "user-svc")
    org.example.AIsvc.dto.user.UserServiceResponse getUserSecretWrapped(@RequestHeader("X-User-Id") String userId);
    
    // 래핑된 응답에서 data만 추출하는 default 메소드
    default UserSecretResponse getUserSecret(String userId) {
        org.example.AIsvc.dto.user.UserServiceResponse response = getUserSecretWrapped(userId);
        return (response != null && response.isSuccess()) ? response.getData() : null;
    }

    // 사용자의 구독 플랜 정보를 조회하기 위한 메소드 추가
    @GetMapping("/users/{userId}/subscription")
    @CircuitBreaker(name = "user-svc")
    UserPlanResponse getUserPlan(@PathVariable("userId") String userId);

    @Slf4j
    @Component
    class UserApiClientFallback implements UserApiClient {
        @Override
        public org.example.AIsvc.dto.user.UserServiceResponse getUserSecretWrapped(String userId) {
            log.error("UserApiClient getUserSecretWrapped fallback executed for userId: {}", userId);
            // BYOK 키 조회가 실패하면, 키가 없는 것처럼 null을 반환
            return null;
        }

        @Override
        public UserPlanResponse getUserPlan(String userId) {
            log.error("UserApiClient getUserPlan fallback executed for userId: {}", userId);
            // 플랜 조회 실패 시, 가장 안전한 기본값인 'FREE' 플랜으로 반환
            return new UserPlanResponse("FREE");
        }
    }
}