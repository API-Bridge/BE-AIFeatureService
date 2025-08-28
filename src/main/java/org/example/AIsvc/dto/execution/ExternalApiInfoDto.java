package org.example.AIsvc.dto.execution;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiInfoDto {
    private String apiId;
    private String apiName;
    private String endpoint;
    
    @Schema(description = "HTTP 메소드", example = "GET")
    private String httpMethod;
    
    private List<ApiParameterDto> parameters;
}