package com.example.flashsale.service;

import com.example.flashsale.model.Product;
import com.example.flashsale.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Lombok 自動生成 Constructor DI (依賴注入)
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    // 注入我們剛剛設定好的 RedisTemplate
    private final RedisTemplate<String, Object> redisTemplate;
    // Mockito 測試時也能把 Mock 物件注入進來
    private final RedisScript<Long> stockScript;
    private static final String STOCK_PREFIX = "product:stock:";
    // 定義 Key 的前綴，方便管理 (例如 product:1)
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    // 注入 HazelcastService
    private final HazelcastService hazelcastService;

    // 注入 KafkaService
    private final KafkaService kafkaService;

    // 注入 Zookeeper Client
    private final CuratorFramework curatorFramework;

    @Transactional(rollbackFor = Exception.class)
    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);

        // 【庫存預熱】將庫存數量寫入 Redis (重要！)
        // Key: product:stock:1, Value: 100
        redisTemplate.opsForValue().set(STOCK_PREFIX + savedProduct.getId(), savedProduct.getStock());

        return savedProduct;
    }

    /**
     * 查詢商品 (加入 Redis 快取邏輯)
     */
    public Product getProduct(Long id) {
        String cacheKey = PRODUCT_CACHE_PREFIX + id;

        // 1. 先查 Redis
        Product cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("🔥 [Cache Hit] 從 Redis 讀取商品: {}", id);
            return cachedProduct;
        }

        // 2. Redis 沒有，查 MySQL
        log.info("🐢 [Cache Miss] 從 MySQL 讀取商品: {}", id);
        Product product = productRepository.findById(id).orElse(null);

        // 3. 如果 MySQL 有資料，寫入 Redis (並設定 10 分鐘過期，避免髒資料永久存在)
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, 10, TimeUnit.MINUTES);
        }

        return product;
    }

    /**
     * 搶購商品 (秒殺核心邏輯)
     */
    public String orderProduct(Long productId) {
        // 1. Redis 扣庫存 (Lua 腳本)
        String key = STOCK_PREFIX + productId;
        Long result = redisTemplate.execute(stockScript, Collections.singletonList(key));
        // 2. 模擬 User ID (因為無限購，所以同一個 ID 可以一直買)
        Long userId = 1000L + new Random().nextInt(19000);

        // ✅ [新功能] Hazelcast 分散式黑名單檢查
        // IMap 的效能極高，因為它可能直接讀取本機記憶體
        if (hazelcastService.isBlacklisted(userId)) {
            // 🔥 發送 Kafka 異步紀錄
            kafkaService.sendFailureLog(userId, productId, "BLACKLIST_HIT");
            log.warn("🛑 用戶 {} 在黑名單中，拒絕搶購", userId);
            return "您的帳號異常，無法參與活動";
        }

        if (result != null && result == 1) {

            // 3. ✅ 生成全域唯一的訂單編號 (UUID)
            // 這代表「這一次的點擊行為」，就算 Kafka 重送，這個 UUID 也不會變
            String orderNo = UUID.randomUUID().toString();

            // 4. 發送訊息 (帶入 UUID)
            kafkaService.sendOrderMessage(productId, userId, orderNo);

            return "搶購成功，訂單處理中...";
        } else {
            // 🔥 發送 Kafka 異步紀錄
            kafkaService.sendFailureLog(userId, productId, "OUT_OF_STOCK");
            return "搶購失敗，庫存不足";
        }
    }

    /**
     * 【新功能】使用 Zookeeper 分散式鎖進行搶購
     * 特點：強一致性，但效能比 Redis Lua 差
     */
    public String orderProductByZk(Long productId) {
        String lockPath = "/lock/product/" + productId;

        Long userId = 1000L + new Random().nextInt(19000);

        // ✅ [新功能] Hazelcast 分散式黑名單檢查
        // IMap 的效能極高，因為它可能直接讀取本機記憶體
        if (hazelcastService.isBlacklisted(userId)) {
            // 🔥 發送 Kafka 異步紀錄
            kafkaService.sendFailureLog(userId, productId, "BLACKLIST_HIT");
            log.warn("🛑 用戶 {} 在黑名單中，拒絕搶購", userId);
            return "您的帳號異常，無法參與活動";
        }

        // 1. 定義鎖 (針對該商品 ID)
        InterProcessMutex lock = new InterProcessMutex(curatorFramework, lockPath);

        try {
            // 2. 嘗試獲取鎖 (最多等 3 秒)
            // 這一行對應 ZK 內部：建立 Ephemeral Sequential Node
            if (lock.acquire(3, TimeUnit.SECONDS)) {
                try {
                    // --- 進入 Critical Section (臨界區) ---
                    // 在這裡面，同一時間只有一個執行緒能執行！

                    // A. 查 Redis 庫存 (普通的 get，不需要 Lua)
                    String stockKey = STOCK_PREFIX + productId;
                    Object stockObj = redisTemplate.opsForValue().get(stockKey);
                    int stock = stockObj == null ? 0 : Integer.parseInt(stockObj.toString());

                    if (stock > 0) {
                        // B. 扣 Redis 庫存
                        redisTemplate.opsForValue().set(stockKey, String.valueOf(stock - 1));
                        // C. 發送 Kafka (建立訂單流程)

                        String orderNo = UUID.randomUUID().toString();
                        kafkaService.sendOrderMessage(productId, userId, orderNo);

                        log.info("✅ [ZK鎖] 搶購成功，剩餘庫存: {}", (stock - 1));
                        return "搶購成功 (ZK Lock)";
                    } else {
                        log.warn("❌ [ZK鎖] 庫存不足");
                        return "搶購失敗，庫存不足";
                    }

                } finally {
                    // 3. 務必釋放鎖！(對應 ZK 內部：刪除節點)
                    lock.release();
                }
            } else {
                // 獲取鎖失敗 (超時)
                log.warn("⏳ [ZK鎖] 搶鎖失敗 (人太多，排隊超時)");
                return "搶購失敗，系統忙碌中";
            }
        } catch (Exception e) {
            log.error("ZK 系統錯誤", e);
            return "系統錯誤";
        }
    }


}