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
    public void sendOrderMessage(Long productId, Long userId, String orderNo) {
        // 訊息格式： "userId:productId:orderNo"
        String message = userId + ":" + productId + ":" + orderNo;
        log.info("📤 [Kafka Producer] 發送搶購訊息: {}", message);
        kafkaTemplate.send("flash-sale-topic", message);
    }
}