package com.example.flashsale.service;

import com.example.flashsale.model.Product;
import com.example.flashsale.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    // 注入剛剛設定的 Lua Script
    private final DefaultRedisScript<Long> stockScript;
    private static final String STOCK_PREFIX = "product:stock:";
    // 定義 Key 的前綴，方便管理 (例如 product:1)
    private static final String PRODUCT_CACHE_PREFIX = "product:";

    // 注入 KafkaService
    private final KafkaService kafkaService;

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

        if (result == 1) {
            // 2. 模擬 User ID (因為無限購，所以同一個 ID 可以一直買)
            Long userId = 1000L + new Random().nextInt(19000);

            // 3. ✅ 生成全域唯一的訂單編號 (UUID)
            // 這代表「這一次的點擊行為」，就算 Kafka 重送，這個 UUID 也不會變
            String orderNo = UUID.randomUUID().toString();

            // 4. 發送訊息 (帶入 UUID)
            kafkaService.sendOrderMessage(productId, userId, orderNo);

            return "搶購成功，訂單處理中...";
        } else {
            return "搶購失敗，庫存不足";
        }
    }
}