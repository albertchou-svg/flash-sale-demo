package com.example.flashsale.repository;

import com.example.flashsale.document.FailureLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FailureLogRepository extends MongoRepository<FailureLog, String> {
    // 您可以直接定義查詢方法，例如：
    // long countByReason(String reason);
}