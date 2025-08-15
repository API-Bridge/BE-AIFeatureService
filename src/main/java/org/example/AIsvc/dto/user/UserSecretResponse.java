package org.example.AIsvc.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSecretResponse {
    // 사용자의 암호화된 API 키가 저장된 AWS Secrets Manager의 ARN 주소
    private String arn;
    // 해당 ARN에 대한 설명
    private String arnDescription;
}