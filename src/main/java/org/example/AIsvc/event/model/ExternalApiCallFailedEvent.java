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
public class ExternalApiCallFailedEvent extends BaseEvent {

    private ExternalApiCallFailedPayload payload;

    /**
     * 외부 API 호출 실패 이벤트 생성자
     * 
     * @param apiId API ID
     * @param apiName API 이름
     * @param apiUrl 실패한 외부 API URL
     * @param httpMethod HTTP 메소드
     * @param statusCode HTTP 상태 코드
     * @param errorMessage 실패 원인 메시지
     * @param errorType 에러 타입
     * @param responseTime 응답 시간
     * @param requestId 요청 ID
     * @param calledBy 호출자
     */
    public ExternalApiCallFailedEvent(String apiId, String apiName, String apiUrl, String httpMethod,
                                      Integer statusCode, String errorMessage, String errorType, 
                                      Integer responseTime, String requestId, String calledBy) {
        super("external-api-call-failed");
        this.payload = ExternalApiCallFailedPayload.builder()
                .apiId(apiId)
                .apiName(apiName)
                .apiUrl(apiUrl)
                .httpMethod(httpMethod)
                .statusCode(statusCode)
                .errorMessage(errorMessage)
                .errorType(errorType)
                .responseTime(responseTime)
                .requestId(requestId)
                .calledBy(calledBy)
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
    public static class ExternalApiCallFailedPayload {
        /** API ID */
        private String apiId;
        
        /** API 이름 */
        private String apiName;
        
        /** API URL */
        private String apiUrl;
        
        /** HTTP 메소드 */
        private String httpMethod;
        
        /** HTTP 상태 코드 */
        private Integer statusCode;
        
        /** 에러 메시지 */
        private String errorMessage;
        
        /** 에러 타입 */
        private String errorType;
        
        /** 응답 시간 (밀리초) */
        private Integer responseTime;
        
        /** API 호출 실패 발생 시각 */
        private LocalDateTime failedAt;
        
        /** 요청 ID */
        private String requestId;
        
        /** 호출자 */
        private String calledBy;
    }
}