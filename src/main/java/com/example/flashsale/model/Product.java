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
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Integer price;

    private Integer stock;
}