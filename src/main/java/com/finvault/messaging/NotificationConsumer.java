package com.finvault.messaging;

import com.finvault.event.NotificationEvent;
import com.finvault.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    @KafkaListener(topics = "${finvault.kafka.topics.transactions}", groupId = "finvault-audit")
    public void handleTransactionEvent(TransactionEvent event) {
        log.info("Transaction processed: ref={}, type={}, amount={} {}, status={}",
                event.getReference(), event.getType(), event.getAmount(),
                event.getCurrency(), event.getStatus());
    }

    @KafkaListener(topics = "${finvault.kafka.topics.notifications}", groupId = "finvault-notifications")
    public void handleNotification(NotificationEvent event) {
        log.info("Notification sent to user {}: [{}] {} - {}",
                event.getUserId(), event.getType(), event.getTitle(), event.getMessage());
    }
}
