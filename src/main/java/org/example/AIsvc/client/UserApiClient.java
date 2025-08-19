package org.example.AIsvc.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.dto.user.UserPlanResponse;
import org.example.AIsvc.dto.user.UserSecretResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-svc", url = "${services.user.url}", fallback = UserApiClient.UserApiClientFallback.class)
@org.springframework.context.annotation.Profile("!demo")
public interface UserApiClient {

    // API 명세서에 정의된 `/internal/ai-service/users/{userId}/secret` 경로로 GET 요청 보냄
    @GetMapping("/internal/ai-service/users/{userId}/secret")
    @CircuitBreaker(name = "user-svc")
    UserSecretResponse getUserSecret(@PathVariable("userId") String userId);

    // 사용자의 구독 플랜 정보를 조회하기 위한 메소드 추가
    @GetMapping("/users/{userId}/subscription")
    @CircuitBreaker(name = "user-svc")
    UserPlanResponse getUserPlan(@PathVariable("userId") String userId);

    @Slf4j
    @Component
    class UserApiClientFallback implements UserApiClient {
        @Override
        public UserSecretResponse getUserSecret(String userId) {
            log.error("UserApiClient getUserSecret fallback executed for userId: {}", userId);
            // BYOK 키 조회가 실패하면, 키가 없는 것처럼 null을 반환
            return null;
        }

        @Override
        public UserPlanResponse getUserPlan(String userId) {
            log.error("UserApiClient getUserPlan fallback executed for userId: {}", userId);
            // 플랜 조회 실패 시, 가장 안전한 기본값인 'FREE' 플랜, 비활성 상태로 반환
            return new UserPlanResponse("FREE", false);
        }
    }
}