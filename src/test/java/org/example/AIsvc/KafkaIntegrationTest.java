package org.example.AIsvc;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.AIsvc.event.model.ApiAnalysisEvent;
import org.example.AIsvc.event.publisher.EventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
// 테스트를 실행할 때는 'test'와 'dev' 프로필을 활성화
// 'dev' 프로필을 활성화하면 SecurityConfig의 @Profile("!dev") 조건에 의해 SecurityConfig가 로드되지 않음
@ActiveProfiles({"test", "dev"})
// 테스트 실행 시 내장 Kafka 브로커(Docker 컨테이너)를 자동으로 실행하고, 관련 설정을 주입
@EmbeddedKafka(partitions = 1, topics = {"ai-analysis-logs"})
class KafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker; // 테스트용 내장 Kafka 브로커 객체
    @Autowired
    private EventPublisher eventPublisher; // 테스트 대상인 실제 이벤트 발행기

    @Test
    @DisplayName("성공: ApiAnalysisEvent가 Kafka 토픽에 정상적으로 발행되고 수신된다")
    void publishApiAnalysisEvent_Success() throws Exception {
        // given
        // 메시지를 발행할 토픽 이름을 지정
        String topic = "ai-analysis-logs";

        // 테스트용 Kafka Consumer를 생성하여, 발행된 메시지를 확인할 준비
        // Producer가 JsonSerializer를 사용하므로, Consumer도 JsonDeserializer를 사용
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        Consumer<String, ApiAnalysisEvent> consumer = new DefaultKafkaConsumerFactory<String, ApiAnalysisEvent>(
            consumerProps,
            new org.apache.kafka.common.serialization.StringDeserializer(),
            new JsonDeserializer<>(ApiAnalysisEvent.class)
        ).createConsumer();
        consumer.subscribe(Collections.singleton(topic));

        // 발행할 테스트용 이벤트 객체를 생성
        ApiAnalysisEvent event = new ApiAnalysisEvent("user-123", "test-api", List.of("weather"), List.of("current_weather"), "gemini-pro");

        // when
        // ### 실제 EventPublisher Bean을 사용하여 이벤트를 발행합니다.
        eventPublisher.publishEvent(topic, event);

        // then -
        // KafkaTestUtils.getSingleRecord: 지정된 토픽에서 하나의 메시지를 가져옴
        ConsumerRecord<String, ApiAnalysisEvent> singleRecord = KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofMillis(10000));

        // 메시지가 정상적으로 수신되었는지 확인
        assertThat(singleRecord).isNotNull();

        // 받아온 메시지는 이미 ApiAnalysisEvent 객체로 역직렬화
        ApiAnalysisEvent receivedEvent = singleRecord.value();

        // 발행한 이벤트와 수신한 이벤트의 내용이 일치하는지 상세히 검증
        assertThat(receivedEvent.getUserId()).isEqualTo(event.getUserId());
        assertThat(receivedEvent.getCustomApiId()).isEqualTo(event.getCustomApiId());
        assertThat(receivedEvent.getDetectedDomains()).isEqualTo(event.getDetectedDomains());

        // 컨슈머를 안전하게 종료
        consumer.close();
    }
}