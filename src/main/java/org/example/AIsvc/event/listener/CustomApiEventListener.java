package org.example.AIsvc.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.event.model.CustomApiCalledEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 커스텀 API 이벤트 리스너
 * custom_api_events 토픽에서 발행되는 이벤트를 구독하여 처리
 * 
 * 주요 기능:
 * - 커스텀 API 호출 이벤트 수신 및 처리
 * - 이벤트 기반 비즈니스 로직 실행
 * - 모니터링 및 로깅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomApiEventListener {

    /**
     * custom_api_events 토픽에서 CustomApiCalled 이벤트를 구독
     * 다른 서비스에서 커스텀 API 호출 이벤트를 발행하면 이를 수신하여 처리
     * ErrorHandlingDeserializer와 함께 사용하여 역직렬화 오류 처리
     * 
     * @param event 수신된 커스텀 API 호출 이벤트 (또는 Object 타입)
     * @param partition Kafka 파티션 번호
     * @param offset Kafka 오프셋
     */
    @KafkaListener(topics = "custom_api_events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCustomApiCalledEvent(
            @Payload Object event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        // 역직렬화 오류 처리
        if (event == null) {
            log.warn("Received null event from partition: {}, offset: {}", partition, offset);
            return;
        }

        // CustomApiCalledEvent 타입 처리
        if (event instanceof CustomApiCalledEvent) {
            CustomApiCalledEvent customApiEvent = (CustomApiCalledEvent) event;
            log.info("Received CustomApiCalled event - eventId: {}, customApiId: {}, userId: {}, partition: {}, offset: {}",
                    customApiEvent.getEventId(), 
                    customApiEvent.getPayload() != null ? ((CustomApiCalledEvent.CustomApiCalledPayload) customApiEvent.getPayload()).getCustomApiId() : "unknown",
                    customApiEvent.getPayload() != null ? ((CustomApiCalledEvent.CustomApiCalledPayload) customApiEvent.getPayload()).getUserId() : "unknown",
                    partition, 
                    offset);

            try {
                processCustomApiCalledEvent(customApiEvent);
            } catch (Exception e) {
                log.error("Failed to process CustomApiCalled event: {}", customApiEvent.getEventId(), e);
            }
        } else {
            // 다른 타입의 이벤트 처리
            log.info("Received event of type: {} from partition: {}, offset: {}", 
                    event.getClass().getSimpleName(), partition, offset);
        }
    }

    /**
     * 커스텀 API 호출 이벤트 처리 로직
     * 실제 비즈니스 로직을 구현하는 메서드
     * 
     * @param event 처리할 이벤트
     */
    private void processCustomApiCalledEvent(CustomApiCalledEvent event) {
        CustomApiCalledEvent.CustomApiCalledPayload payload = 
                (CustomApiCalledEvent.CustomApiCalledPayload) event.getPayload();
        
        if (payload == null) {
            log.warn("Event payload is null for eventId: {}", event.getEventId());
            return;
        }

        log.debug("Processing custom API call - customApiId: {}, userId: {}, requestSource: {}, calledAt: {}",
                payload.getCustomApiId(),
                payload.getUserId(),
                payload.getRequestSource(),
                payload.getCalledAt());

        // TODO: 실제 비즈니스 로직 구현
        // 예시:
        // - 사용자별 API 사용량 통계 업데이트
        // - 커스텀 API 호출 이력 저장
        // - 모니터링 메트릭 업데이트
        // - 알림 발송 (사용량 초과 등)
        
        log.info("Successfully processed CustomApiCalled event: {}", event.getEventId());
    }
}