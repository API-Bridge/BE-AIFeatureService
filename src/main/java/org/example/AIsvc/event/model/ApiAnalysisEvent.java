package org.example.AIsvc.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Getter
@SuperBuilder // 부모(BaseEvent)의 필드까지 빌더에 포함
@NoArgsConstructor
public class ApiAnalysisEvent extends BaseEvent {
    private static final String EVENT_TYPE = "API_ANALYSIS_COMPLETED";
    
    private String userId; // API 생성을 요청한 사용자 ID
    private String customApiId; // 생성된(또는 요청된) 커스텀 API ID
    private List<String> detectedDomains; //AI가 감지한 도메인 목록
    private List<String> detectedKeywords; // AI가 감지한 키워드 목록
    private String llmModel; // 사용된 LLM 모델 이름
    // (실제로는 토큰 사용량, 처리 시간 등 더 많은 데이터를 포함할 수 있음)

    public ApiAnalysisEvent(String userId, String customApiId, List<String> detectedDomains, List<String> detectedKeywords, String llmModel) {
        super(EVENT_TYPE); // 부모 생성자를 호출하여 eventType을 설정
        this.userId = userId;
        this.customApiId = customApiId;
        this.detectedDomains = detectedDomains;
        this.detectedKeywords = detectedKeywords;
        this.llmModel = llmModel;
    }
}