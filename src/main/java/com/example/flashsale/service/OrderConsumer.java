package com.example.flashsale.service;

import com.example.flashsale.model.Order;
import com.example.flashsale.repository.OrderRepository;
import com.example.flashsale.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * 監聽 "flash-sale-topic"
     * 當有訊息進來時，這個方法會被觸發
     */
    @KafkaListener(topics = "flash-sale-topic", groupId = "flash-sale-group")
    @Transactional(rollbackFor = Exception.class) // 資料庫交易控制
    public void handleOrder(String message, Acknowledgment ack) {
        log.info("📥 [Kafka Consumer] 開始處理訂單: {}", message);

        try {
            // 1. 解析訊息 (訊息格式: "USER_ORDER:商品ID")
            String[] parts = message.split(":");
            if (parts.length < 2) {
                // 格式錯誤的壞訊息，直接 ack 掉，不然會一直卡在佇列頭部
                ack.acknowledge();
                return;
            }

            Long productId = Long.parseLong(parts[1]);

            // 2. 扣減 MySQL 庫存
            int updateCount = productRepository.decreaseStock(productId);

            if (updateCount > 0) {
                // 3. 建立訂單
                Order order = new Order();
                order.setProductId(productId);
                order.setUserId(1001L); // 模擬一個用戶 ID
                order.setCreateTime(LocalDateTime.now());
                orderRepository.save(order);

                log.info("✅ [MySQL] 訂單建立成功，庫存已同步！商品ID: {}", productId);

                // 3. ⚠️ 關鍵：最後才提交 Offset！
                // 這代表：「我確定資料庫已經安全了，Kafka 你可以把這條劃掉了」
                ack.acknowledge();

            } else {
                // 這種情況理論上極少發生 (因為 Redis 已經擋過一次)，除非 Redis 與 MySQL 資料嚴重不一致
                log.warn("⚠️ [MySQL] 扣庫存失敗 (可能庫存已歸零)，但 Redis 卻放行了？需檢查資料一致性。");
                // 邏輯上執行完畢，也算消費成功
                ack.acknowledge();
            }

        } catch (Exception e) {
            log.error("❌ 處理訂單失敗", e);
            // 在真實場景，這裡可能需要發送「補償訊息」到 Dead Letter Queue (DLQ) 進行人工處理
            // ⚠️ 這裡「不要」呼叫 ack.acknowledge()
            // 這樣 Kafka 會知道這則訊息沒處理成功，稍後會重新投遞給 Consumer (或別的 Consumer)
            // 這就保證了訊息不遺失！
        }
    }
}