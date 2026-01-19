package com.example.flashsale.controller;

import com.example.flashsale.model.Product;
import com.example.flashsale.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "商品管理系統", description = "提供商品的 CRUD 功能") // Swagger 大標題
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "新增商品", description = "建立一筆新的商品資料，會自動寫入 MySQL") // API 說明
    public Product create(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢商品", description = "輸入 ID 查詢商品庫存與價格")
    public Product get(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PostMapping("/{id}/order")
    @Operation(summary = "搶購商品 (秒殺)", description = "使用 Redis Lua 腳本扣減庫存，防止超賣")
    public String order(
            @Parameter(description = "商品 ID", example = "1", required = true) // 👈 參數說明
            @PathVariable Long id
    ) {
        return productService.orderProduct(id);
    }

    @PostMapping("/{id}/order/zk")
    @Operation(summary = "搶購商品 (Zookeeper)", description = "使用 Zookeeper 分散式鎖 (Curator)")
    public String orderZk(
            @Parameter(description = "商品 ID", example = "1", required = true) // 👈 參數說明
            @PathVariable Long id
    ) {
        return productService.orderProductByZk(id);
    }
}