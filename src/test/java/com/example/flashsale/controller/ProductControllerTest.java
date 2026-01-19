package com.example.flashsale.controller;

import com.example.flashsale.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1. 只啟動 Web 層，不啟動整個 Spring Context
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc; // 用來模擬發送 HTTP 請求

    // 2. 使用 @MockBean 替換掉 Spring 容器裡的 ProductService
    // 因為我們只測 Controller，不關心 Service 真的做了什麼
    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("API 測試：秒殺請求成功回應")
    void orderProductApi_ShouldReturnOk() throws Exception {
        // --- Given ---
        Long productId = 1L;
        // 告訴 Mockito：當有人呼叫 service.orderProduct(1) 時，回傳 "OK"
        when(productService.orderProduct(productId)).thenReturn("搶購成功，訂單處理中...");

        // --- When & Then ---
        mockMvc.perform(post("/api/products/{id}/order", productId)) // 模擬 POST 請求
                .andExpect(status().isOk()) // 預期 HTTP 200
                .andExpect(content().string("搶購成功，訂單處理中...")); // 預期回傳內容
    }
}