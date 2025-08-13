package org.example.AIsvc.dto.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomApiResponseDto {
    private String customApiId;
    private String userId;
    private String name;
    private String description;
    @JsonProperty("externalApiUrl_list")
    private List<ExternalApiInfoDto> externalApiInfoList;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}