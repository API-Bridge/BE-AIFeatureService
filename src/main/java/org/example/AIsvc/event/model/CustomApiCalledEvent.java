package org.example.AIsvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 커스텀 API 호출 이벤트
 * 커스텀 API가 실행될 때 발행되는 이벤트로 호출 정보와 메타데이터를 포함
 * 
 * 주요 기능:
 * - 커스텀 API 호출 추적 및 모니터링
 * - 사용자별 API 사용량 통계 수집
 * - 서비스 간 호출 이력 기록
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomApiCalledEvent extends BaseEvent {

    private CustomApiCalledPayload payload;

    /**
     * 커스텀 API 호출 이벤트 생성자
     * 
     * @param customApiId 호출된 커스텀 API ID
     * @param userId 호출한 사용자 ID
     * @param requestSource 요청 소스 (예: "ai-service", "web-app" 등)
     */
    public CustomApiCalledEvent(String customApiId, String userId, String requestSource) {
        super("CustomApiCalled");
        this.payload = CustomApiCalledPayload.builder()
                .customApiId(customApiId)
                .userId(userId)
                .requestSource(requestSource)
                .calledAt(LocalDateTime.now())
                .build();
    }

    @Override
    public Object getPayload() {
        return this.payload;
    }

    /**
     * 커스텀 API 호출 이벤트 페이로드
     * 실제 이벤트 데이터를 담고 있는 내부 클래스
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomApiCalledPayload {
        /** 호출된 커스텀 API의 고유 식별자 */
        private String customApiId;
        
        /** API를 호출한 사용자의 ID */
        private String userId;
        
        /** 요청을 발생시킨 소스 시스템 또는 서비스 */
        private String requestSource;
        
        /** API 호출 발생 시각 */
        private LocalDateTime calledAt;
    }
}