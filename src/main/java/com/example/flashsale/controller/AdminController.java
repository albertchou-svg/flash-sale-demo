package com.example.flashsale.controller;

import com.example.flashsale.model.Product;
import com.example.flashsale.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    @Autowired
    private MongoTemplate mongoTemplate;

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 簡單的安全密鑰 (真實專案請用 Spring Security)
    private static final String ADMIN_SECRET = "123456";


    @GetMapping("/analyze/failures")
    public Map<String, Integer> analyzeFailures() {
        // 使用 Aggregation 進行分組統計
        // 語法：Select reason, count(*) from failure_logs group by reason
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("reason").count().as("totalCount"),
                Aggregation.project("totalCount").and("reason").previousOperation()
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "failure_logs", Map.class
        );

        // 整理結果回傳
        Map<String, Integer> stats = new HashMap<>();
        for (Map map : results.getMappedResults()) {
            stats.put((String) map.get("reason"), (Integer) map.get("totalCount"));
        }
        return stats;
    }

    @PostMapping("/sync-stock")
    public String syncStock(@RequestParam String key) {
        // 1. 安全檢查
        if (!ADMIN_SECRET.equals(key)) {
            return "❌ 權限不足！";
        }

        log.info("⚠️ [Admin] 管理員觸發手動庫存同步...");
        long start = System.currentTimeMillis();

        // 2. 從 MySQL 撈全部商品
        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            return "MySQL 沒有商品資料";
        }

        // 3. 使用 Pipeline 批次寫入 Redis
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Product product : products) {
                String redisKey = "product:stock:" + product.getId();
                // 先刪除 (Optional, 看需求)
                connection.del(redisKey.getBytes());
                // 轉 byte[] 寫入
                connection.set(redisKey.getBytes(), String.valueOf(product.getStock()).getBytes());
            }
            return null;
        });

        long end = System.currentTimeMillis();
        String message = String.format("✅ 同步完成！共 %d 筆商品，耗時 %d ms", products.size(), (end - start));
        log.info(message);

        return message;
    }
}