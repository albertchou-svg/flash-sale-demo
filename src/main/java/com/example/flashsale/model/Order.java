package com.example.flashsale.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders") // ⚠️ 避開 SQL 保留字 order
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    // 為了簡化，我們先不實作 User 系統，暫時用隨機 ID 或固定 ID
    private Long userId;

    // ✅ 新增：交易序號 (對應 DB 的 order_no)
    @Column(nullable = false, unique = true)
    private String orderNo;

    private LocalDateTime createTime;
}