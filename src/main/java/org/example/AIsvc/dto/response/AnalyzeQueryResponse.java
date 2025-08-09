package org.example.AIsvc.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 쿼리 분석 요청에 대한 응답 정보를 담는 DTO
@Getter
@Builder
public class AnalyzeQueryResponse {
    
    private String status;
    private String message;
    private String requestedApiId;
    private AnalysisResultDto analysisResult;

    @Getter
    @Builder
    public static class AnalysisResultDto {
        private List<String> detectedDomains;
        private List<String> detectedKeywords;
    }
}