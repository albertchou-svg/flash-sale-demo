package com.example.flashsale.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private KafkaService kafkaService;

    // 由於 ProductService 現在有 @RequiredArgsConstructor (建構子)，
    // @InjectMocks 會自動把這個 stockScript 注入進去。
    @Mock
    private RedisScript<Long> stockScript;

    @InjectMocks
    private ProductService productService;


    @Test
    @DisplayName("測試搶購成功")
    void testOrderProduct_Success() {
        Long productId = 1L;

        when(redisTemplate.execute(
                eq(stockScript),      // 👈 指定必須是這個腳本
                anyList(),            // Keys
                any(Object[].class)   // Args (Varargs)
        )).thenReturn(1L);

        String result = productService.orderProduct(productId);

        assertEquals("搶購成功，訂單處理中...", result);
        verify(kafkaService, times(1)).sendOrderMessage(eq(productId), anyLong(), anyString());
    }

    @Test
    @DisplayName("測試搶購失敗")
    void testOrderProduct_Failure() {
        Long productId = 1L;

        // 模擬回傳 0 (搶失敗)
        when(redisTemplate.execute(
                eq(stockScript),      // 👈 指定必須是這個腳本
                anyList(),
                any(Object[].class)
        )).thenReturn(0L);

        String result = productService.orderProduct(productId);

        assertEquals("搶購失敗，庫存不足", result);
        verify(kafkaService, never()).sendOrderMessage(anyLong(), anyLong(), anyString());
    }
}