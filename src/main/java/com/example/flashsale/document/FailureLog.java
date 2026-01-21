package com.example.flashsale.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "failure_logs") // 指定 Collection 名稱 (類似 Table)
public class FailureLog {
    @Id
    private String id;          // Mongo ID 自動生成 (字串)
    private Long userId;
    private Long productId;
    private String reason;      // 失敗原因 (例如: "BLACKLIST", "OUT_OF_STOCK")
    private LocalDateTime failedAt;
    private String ipAddress;
}