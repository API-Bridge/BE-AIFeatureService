package org.example.AIsvc.dto.execution;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterDto {
    private String paramName;
    private String paramType;
    private String description;
    private boolean necessary;
    private String defaultValue;
}