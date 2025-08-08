package org.example.AIsvc.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 쿼리 분석 요청에 대한 응답 정보를 담는 DTO
@Getter
@Builder
public class AnalyzeQueryResponse {
    
    private String status; // 리 상태 (e.g., "ACCEPTED")
    private String message;
    private String requestedApiId; // 요청된 (또는 생성된) 커스텀 API ID
    private AnalysisResultDto analysisResult; // AI가 분석한 결과 (Day 2부터 채워질 예정)

    // ## 분석 결과를 담는 내부(Nested) DTO 클래스
    @Getter
    @Builder
    public static class AnalysisResultDto {
        private List<String> detectedDomains;
        private List<String> detectedKeywords;
        private String priority; // 처리 우선순위 (e.g., "parallel")
    }
}