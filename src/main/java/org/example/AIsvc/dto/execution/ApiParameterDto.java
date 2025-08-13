package org.example.AIsvc.dto.execution;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterDto {
    private String paramName;
    private String paramType; // "INPUT" 또는 "OUTPUT"
    private String description;
    private boolean necessary;
}