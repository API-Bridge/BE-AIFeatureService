package org.example.AIsvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 커스텀 API 생성 실패 이벤트
 * 커스텀 API 생성이 실패했을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomApiCreateFailedEvent extends BaseEvent {

    private CustomApiCreateFailedPayload payload;

    /**
     * 커스텀 API 생성 실패 이벤트 생성자
     * 
     * @param userId 사용자 ID
     * @param name 생성하려던 커스텀 API 이름
     * @param failureReason 실패 사유
     * @param errorMessage 실패 상세 메시지
     * @param failureStage 실패한 단계
     */
    public CustomApiCreateFailedEvent(String userId, String name, String failureReason, String errorMessage, String failureStage) {
        super("CustomApiCreateFailed");
        this.payload = new CustomApiCreateFailedPayload(userId, name, failureReason, errorMessage, failureStage);
    }

    @Override
    public CustomApiCreateFailedPayload getPayload() {
        return payload;
    }

    /**
     * 커스텀 API 생성 실패 이벤트의 페이로드
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomApiCreateFailedPayload {
        /** 커스텀 API 생성을 시도한 사용자 ID */
        private String userId;
        
        /** 생성하려던 커스텀 API 이름 */
        private String name;
        
        /** 실패 사유 */
        private String failureReason;
        
        /** 실패 상세 메시지 */
        private String errorMessage;
        
        /** 실패한 단계 (예: VALIDATION, AI_GENERATION, DATABASE_SAVE) */
        private String failureStage;
    }
}