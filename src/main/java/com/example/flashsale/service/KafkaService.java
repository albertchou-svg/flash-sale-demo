package com.example.flashsale.service;

import com.example.flashsale.DTO.FailureLogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // 定義一個專門的 Topic
    private static final String TOPIC_FAILURE = "flash-sale-failure";

    // 發送訂單訊息
    public void sendOrderMessage(Long productId, Long userId, String orderNo) {
        // 訊息格式： "userId:productId:orderNo"
        String message = userId + ":" + productId + ":" + orderNo;
        log.info("📤 [Kafka Producer] 發送搶購訊息: {}", message);
        kafkaTemplate.send("flash-sale-topic", message);
    }

    public void sendFailureLog(Long userId, Long productId, String reason) {
        // 模擬取得 IP (在真實 Controller 層可以透過 HttpServletRequest 取得)
        String fakeIp = "192.168.1." + (new Random().nextInt(255));

        FailureLogEvent event = new FailureLogEvent(userId, productId, reason, fakeIp);
        try {
            // 2. 在這裡就地解決異常
            String message = new ObjectMapper().writeValueAsString(event);
            kafkaTemplate.send(TOPIC_FAILURE, message);
            log.info("📝 失敗日誌已發送 Kafka: User={}, Reason={}", userId, reason);

        } catch (JsonProcessingException e) {
            // 3. 如果轉 JSON 失敗，印出 Error Log 即可，不要讓程式崩潰
            log.error("❌ Kafka 日誌序列化失敗: {}", event, e);
        }
    }
}