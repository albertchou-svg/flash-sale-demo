package com.example.flashsale.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // 發送訂單訊息
    public void sendOrderMessage(Long productId) {
        String message = "USER_ORDER:" + productId;
        log.info("📤 [Kafka Producer] 發送搶購訊息: {}", message);

        // send(topic名稱, 訊息內容)
        kafkaTemplate.send("flash-sale-topic", message);
    }
}