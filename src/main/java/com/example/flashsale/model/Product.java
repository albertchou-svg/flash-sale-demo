package com.example.flashsale.model;

import jakarta.persistence.*; // Spring Boot 3 使用 jakarta
import lombok.Data;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Data // Lombok 自動生成 Getter/Setter/ToString
@Table(name = "product") // 指定資料表名稱
public class Product implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自動遞增主鍵
    @Schema(
            description = "商品唯一 ID",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Column(nullable = false)
    @Schema(description = "商品名稱", example = "iPhone 15 Pro")
    private String name;

    @Schema(description = "商品單價", example = "29900")
    private Integer price;
    @Schema(description = "剩餘庫存", example = "100")
    private Integer stock;
}