package com.example.flashsale.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
@Slf4j
@RequiredArgsConstructor
public class HazelcastService {

    private final HazelcastInstance hazelcastInstance;

    /**
     * 【功能 1：DataGrid】
     * 使用 Hazelcast IMap 儲存黑名單
     * 這跟 Redis 很像，但它是存在 JVM Heap 裡，讀取速度是奈秒級 (如果命中本機)
     */
    public void addToBlacklist(Long userId) {
        IMap<Long, String> blacklist = hazelcastInstance.getMap("blacklist");
        blacklist.put(userId, "疑似機器人");
        log.info("🚫 [Hazelcast] 用戶 {} 已加入黑名單", userId);
    }

    public boolean isBlacklisted(Long userId) {
        IMap<Long, String> blacklist = hazelcastInstance.getMap("blacklist");
        return blacklist.containsKey(userId);
    }

    /**
     * 【功能 2：分散式計算】
     * 將一個任務 (Task) 發送到叢集的所有節點去執行
     * 場景：當秒殺結束時，通知所有伺服器清空本地快取，或者進行數據匯總
     */
    public void broadcastSystemTask(String message) {
        IExecutorService executor = hazelcastInstance.getExecutorService("default");

        // 這是要傳送的任務 (必須實作 Serializable)
        SystemTask task = new SystemTask(message);

        // 發送給所有成員 (Member)
        executor.executeOnAllMembers(task);

        log.info("📡 [Hazelcast Compute] 任務已廣播給所有節點");
    }

    // 定義一個可序列化的任務類別 (重點：這段程式碼會被序列化後透過網路傳到別台機器執行)
    static class SystemTask implements Runnable, Serializable {
        private final String msg;

        public SystemTask(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            // 這行 Log 會出現在「執行該任務的節點」的 Console 上
            System.out.println("⚠️ [系統廣播 - 執行緒: " + Thread.currentThread().getName() + "] 收到指令: " + msg);
        }
    }

}