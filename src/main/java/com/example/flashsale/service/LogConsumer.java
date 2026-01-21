package com.example.flashsale.service;

import com.example.flashsale.DTO.FailureLogEvent;
import com.example.flashsale.document.FailureLog;
import com.example.flashsale.repository.FailureLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogConsumer {

    private final FailureLogRepository failureLogRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "flash-sale-failure", groupId = "log-group")
    public void consumeFailureLog(String message) {
        try {
            // 1. 反序列化 JSON
            FailureLogEvent event = objectMapper.readValue(message, FailureLogEvent.class);

            // 2. 轉換成 MongoDB Document
            FailureLog doc = FailureLog.builder()
                    .userId(event.getUserId())
                    .productId(event.getProductId())
                    .reason(event.getReason())
                    .ipAddress(event.getIpAddress())
                    .failedAt(LocalDateTime.now())
                    .build();

            // 3. 寫入 MongoDB (超快，單純插入)
            failureLogRepository.save(doc);

            log.debug("✅ 已寫入 MongoDB: {}", doc);

        } catch (Exception e) {
            log.error("寫入日誌失敗", e);
        }
    }
}