package org.example.AIsvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApiAnalysisEvent extends BaseEvent {
    private static final String EVENT_TYPE = "API_ANALYSIS_COMPLETED";
    
    private ApiAnalysisPayload payload;

    public ApiAnalysisEvent(String userId, String customApiId, List<String> detectedDomains, List<String> detectedKeywords, String llmModel) {
        super(EVENT_TYPE);
        this.payload = ApiAnalysisPayload.builder()
                .userId(userId)
                .customApiId(customApiId)
                .detectedDomains(detectedDomains)
                .detectedKeywords(detectedKeywords)
                .llmModel(llmModel)
                .build();
    }

    @Override
    public Object getPayload() {
        return this.payload;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiAnalysisPayload {
        private String userId; // API 생성을 요청한 사용자 ID
        private String customApiId; // 생성된(또는 요청된) 커스텀 API ID
        private List<String> detectedDomains; // AI가 감지한 도메인 목록
        private List<String> detectedKeywords; // AI가 감지한 키워드 목록
        private String llmModel; // 사용된 LLM 모델 이름
    }
}