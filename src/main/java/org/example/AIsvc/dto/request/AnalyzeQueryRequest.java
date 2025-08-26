package org.example.AIsvc.dto.request;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeQueryRequest {
    @NotBlank(message = "쿼리 내용은 비워둘 수 없습니다.")
    private String query;
    
    @JsonProperty("custom_api_id")
    private String customApiId;
}