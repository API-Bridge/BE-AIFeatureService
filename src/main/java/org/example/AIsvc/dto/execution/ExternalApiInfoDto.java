package org.example.AIsvc.dto.execution;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiInfoDto {
    private String apiId;
    private String apiName;
    private String apiUrl;
    private List<ApiParameterDto> parameters;
}