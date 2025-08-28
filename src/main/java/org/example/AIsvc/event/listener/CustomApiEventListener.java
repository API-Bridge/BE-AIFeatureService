package org.example.AIsvc.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.AIsvc.event.model.CustomApiCalledEvent;
import org.example.AIsvc.event.model.CustomApiCreateFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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

        // ConsumerRecord로 수신된 경우 수동 역직렬화 시도
        if (event instanceof ConsumerRecord) {
            ConsumerRecord<?, ?> consumerRecord = (ConsumerRecord<?, ?>) event;
            log.info("Received ConsumerRecord - attempting manual deserialization from partition: {}, offset: {}", partition, offset);
            
            Object value = consumerRecord.value();
            log.debug("Raw event value type: {}, value: {}", value.getClass().getSimpleName(), value);
            
            // value가 Map 형태로 역직렬화된 경우 처리
            if (value instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> eventMap = (java.util.Map<String, Object>) value;
                log.debug("Event map: {}", eventMap);
                
                String eventType = (String) eventMap.get("eventType");
                log.info("Processing event with eventType: {}", eventType);
                
                if ("CustomApiCreateFailed".equals(eventType)) {
                    try {
                        // Map을 CustomApiCreateFailedEvent로 변환
                        CustomApiCreateFailedEvent failedEvent = objectMapper.convertValue(eventMap, CustomApiCreateFailedEvent.class);
                        processCustomApiCreateFailedEvent(failedEvent);
                        return;
                    } catch (Exception e) {
                        log.error("Failed to convert Map to CustomApiCreateFailedEvent: {}", e.getMessage(), e);
                        log.debug("Event map details: {}", eventMap);
                    }
                } else {
                    log.info("Unhandled eventType: {}", eventType);
                }
            } else {
                log.warn("Unexpected value type in ConsumerRecord: {}", value.getClass().getSimpleName());
            }
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
        } else if (event instanceof CustomApiCreateFailedEvent) {
            CustomApiCreateFailedEvent failedEvent = (CustomApiCreateFailedEvent) event;
            log.info("Received CustomApiCreateFailed event - eventId: {}, userId: {}, name: {}, reason: {}, partition: {}, offset: {}",
                    failedEvent.getEventId(),
                    failedEvent.getPayload() != null ? failedEvent.getPayload().getUserId() : "unknown",
                    failedEvent.getPayload() != null ? failedEvent.getPayload().getName() : "unknown",
                    failedEvent.getPayload() != null ? failedEvent.getPayload().getFailureReason() : "unknown",
                    partition,
                    offset);

            try {
                processCustomApiCreateFailedEvent(failedEvent);
            } catch (Exception e) {
                log.error("Failed to process CustomApiCreateFailed event: {}", failedEvent.getEventId(), e);
            }
        } else {
            // 다른 타입의 이벤트 처리
            log.info("Received unhandled event of type: {} from partition: {}, offset: {}", 
                    event.getClass().getSimpleName(), partition, offset);
            log.debug("Event details: {}", event.toString());
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

    /**
     * 커스텀 API 생성 실패 이벤트 처리 로직
     * 
     * @param event 처리할 실패 이벤트
     */
    private void processCustomApiCreateFailedEvent(CustomApiCreateFailedEvent event) {
        CustomApiCreateFailedEvent.CustomApiCreateFailedPayload payload = event.getPayload();
        
        if (payload == null) {
            log.warn("Event payload is null for eventId: {}", event.getEventId());
            return;
        }

        log.warn("Processing custom API creation failure - userId: {}, name: {}, reason: {}, stage: {}, message: {}",
                payload.getUserId(),
                payload.getName(),
                payload.getFailureReason(),
                payload.getFailureStage(),
                payload.getErrorMessage());

        // TODO: 실제 비즈니스 로직 구현
        // 예시:
        // - 사용자에게 실패 알림 발송
        // - 실패 통계 업데이트
        // - 재시도 큐에 추가 (특정 실패 유형의 경우)
        // - 관리자 알림 (시스템 오류의 경우)
        
        log.info("Successfully processed CustomApiCreateFailed event: {}", event.getEventId());
    }
}