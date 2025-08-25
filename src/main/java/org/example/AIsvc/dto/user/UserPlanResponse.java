package org.example.AIsvc.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserPlanResponse {
    // 사용자의 현재 구독 플랜 이름 (예: "FREE", "PRO")
    private String planName;
}