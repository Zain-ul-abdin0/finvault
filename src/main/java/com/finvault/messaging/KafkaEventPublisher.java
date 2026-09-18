package com.finvault.messaging;

import com.finvault.event.NotificationEvent;
import com.finvault.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${finvault.kafka.topics.transactions}")
    private String transactionsTopic;

    @Value("${finvault.kafka.topics.notifications}")
    private String notificationsTopic;

    public void publishTransactionEvent(TransactionEvent event) {
        kafkaTemplate.send(transactionsTopic, event.getReference(), event);
        log.debug("Published transaction event: {}", event.getReference());
    }

    public void publishNotification(NotificationEvent event) {
        kafkaTemplate.send(notificationsTopic, event.getUserId().toString(), event);
        log.debug("Published notification for user: {}", event.getUserId());
    }
}
