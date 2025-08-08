package org.example.AIsvc.dto.request;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeQueryRequest {
    private String query;
    private String customApiId;
    private Boolean isPublic;
}