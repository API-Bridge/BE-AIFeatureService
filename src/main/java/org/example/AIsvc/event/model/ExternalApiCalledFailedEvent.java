package org.example.AIsvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 외부 API 호출 실패 이벤트
 * 커스텀 API 실행 중 외부 API 호출이 실패했을 때 발행되는 이벤트
 * 
 * 주요 기능:
 * - 외부 API 호출 실패 추적 및 모니터링
 * - 실패 원인 분석을 위한 상세 정보 기록
 * - 장애 알림 및 복구 프로세스 트리거
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExternalApiCalledFailedEvent extends BaseEvent {

    private ExternalApiCalledFailedPayload payload;

    /**
     * 외부 API 호출 실패 이벤트 생성자
     * 
     * @param customApiId 실행 중인 커스텀 API ID
     * @param userId 요청한 사용자 ID
     * @param externalApiUrl 실패한 외부 API URL
     * @param errorMessage 실패 원인 메시지
     * @param httpStatusCode HTTP 상태 코드 (있는 경우)
     * @param requestSource 요청 소스
     */
    public ExternalApiCalledFailedEvent(String customApiId, String userId, String externalApiUrl, 
                                       String errorMessage, Integer httpStatusCode, String requestSource) {
        super("external-api-called-failed");
        this.payload = ExternalApiCalledFailedPayload.builder()
                .customApiId(customApiId)
                .userId(userId)
                .externalApiUrl(externalApiUrl)
                .errorMessage(errorMessage)
                .httpStatusCode(httpStatusCode)
                .requestSource(requestSource)
                .failedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public Object getPayload() {
        return this.payload;
    }

    /**
     * 외부 API 호출 실패 이벤트 페이로드
     * 실제 실패 정보를 담고 있는 내부 클래스
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalApiCalledFailedPayload {
        /** 실행 중인 커스텀 API의 고유 식별자 */
        private String customApiId;
        
        /** API를 호출한 사용자의 ID */
        private String userId;
        
        /** 실패한 외부 API의 URL */
        private String externalApiUrl;
        
        /** 실패 원인 메시지 */
        private String errorMessage;
        
        /** HTTP 상태 코드 (있는 경우) */
        private Integer httpStatusCode;
        
        /** 요청을 발생시킨 소스 시스템 또는 서비스 */
        private String requestSource;
        
        /** API 호출 실패 발생 시각 */
        private LocalDateTime failedAt;
    }
}