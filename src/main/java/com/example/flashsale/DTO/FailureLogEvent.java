package com.example.flashsale.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FailureLogEvent {
    private Long userId;
    private Long productId;
    private String reason;
    private String ipAddress;
}