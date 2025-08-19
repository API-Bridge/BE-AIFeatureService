package org.example.AIsvc.client.mock;

import org.example.AIsvc.client.UserApiClient;
import org.example.AIsvc.dto.user.UserPlanResponse;
import org.example.AIsvc.dto.user.UserSecretResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@org.springframework.context.annotation.Primary
public class MockUserApiClient implements UserApiClient {

    @Override
    public UserPlanResponse getUserPlan(String userId) {
        // [핵심] userId에 따라 다른 '구독 플랜' 정보를 반환합니다.
        if (userId.contains("pro")) {
            return new UserPlanResponse("PRO", true); // 'pro'가 포함되면 프로 플랜
        }
        return new UserPlanResponse("FREE", true); // 그 외에는 무료 플랜
    }

    @Override
    public UserSecretResponse getUserSecret(String userId) {
        // 무료 사용자를 위한 가짜 'API 키' 정보를 반환합니다.
        return new UserSecretResponse("arn:fake", "sk-fake-free-user-key");
    }
}