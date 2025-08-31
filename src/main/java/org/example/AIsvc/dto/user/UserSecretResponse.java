package org.example.AIsvc.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSecretResponse {
    // 실제 API 키 값 (복호화된 값)
    @JsonProperty("secretValue")
    private String secretValue;
    // ARN ID 
    @JsonProperty("arnId")
    private String arnId;
    // 설명
    @JsonProperty("description")
    private String description;
    // Secret 이름
    @JsonProperty("secretName")
    private String secretName;
    
    // 기존 코드와의 호환성을 위한 메소드들
    public String getArn() {
        return this.arnId;
    }
    
    public String getArnDescription() {
        return this.description;
    }
}