package com.example.flashsale.service;

import com.example.flashsale.model.Order;
import com.example.flashsale.repository.OrderRepository;
import com.example.flashsale.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
            // 1. 解析訊息 "userId:productId:orderNo"
            String[] parts = message.split(":");
            if (parts.length < 3) {
                log.error("❌ 訊息格式錯誤: {}", message);
                ack.acknowledge(); // 格式錯誤直接丟掉，避免卡死
                return;
            }

            Long userId = Long.parseLong(parts[0]);
            Long productId = Long.parseLong(parts[1]);
            String orderNo = parts[2];

            // 2. 扣減 MySQL 庫存 (Redis 已扣，這裡做同步)
            int updateCount = productRepository.decreaseStock(productId);

            if (updateCount > 0) {
                // 3. 建立訂單
                Order order = new Order();
                order.setProductId(productId);
                order.setUserId(userId);
                order.setOrderNo(orderNo); // ✅ 寫入 UUID
                order.setCreateTime(LocalDateTime.now());

                try{
                    // 4. 寫入資料庫
                    orderRepository.save(order);

                    // ⚠️ 關鍵：強制 Flush 讓 SQL 立刻執行
                    // 這樣才能立刻觸發 Unique Key 檢查並拋出異常
                    orderRepository.flush();

                    log.info("✅ [MySQL] 訂單建立成功: {}", orderNo);
                }catch (DataIntegrityViolationException e) {
                    // 🛑 5. 冪等性防禦 (Idempotency)
                    // 捕捉到 order_no 重複，代表這是 Kafka 重複發送的訊息
                    log.warn("⚠️ [重複消費] 攔截到重複訂單，忽略處理: {}", orderNo);

                    // 這裡必須當作「成功」處理，因為我們已經擋下了重複攻擊
                    // 如果拋出異常，Kafka 會一直重試，永遠卡在這裡
                }

                // 6. 手動提交 (防掉單)
                // 只有程式跑到這裡沒崩潰，才告訴 Kafka 可以刪除訊息
                ack.acknowledge();
            } else {
                log.warn("⚠️ [MySQL] 庫存不足 (Redis 與 MySQL 資料不一致)");
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