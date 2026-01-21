package com.example.flashsale.controller;

import com.example.flashsale.service.HazelcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hazelcast")
@Tag(name = "Hazelcast 管理", description = "測試 DataGrid 與 分散式計算")
@RequiredArgsConstructor
public class HazelcastController {

    private final HazelcastService hazelcastService;

    @PostMapping("/blacklist/{userId}")
    @Operation(summary = "加入黑名單 (DataGrid)", description = "將用戶 ID 加入 Hazelcast IMap")
    public String addBlacklist(@PathVariable Long userId) {
        hazelcastService.addToBlacklist(userId);
        return "用戶 " + userId + " 已加入黑名單";
    }

    @PostMapping("/broadcast")
    @Operation(summary = "執行全叢集廣播 (Compute)", description = "使用 IExecutorService 在所有節點印出 Log")
    public String broadcast(@RequestParam String msg) {
        hazelcastService.broadcastSystemTask(msg);
        return "廣播任務已發送";
    }
}