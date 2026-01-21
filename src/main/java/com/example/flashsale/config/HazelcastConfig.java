package com.example.flashsale.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setClusterName("flash-sale-cluster");

        // 網路設定：單機開發模式
        // 關閉 Multicast (廣播發現)，開啟 TCP/IP (指定本機)
        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true).addMember("127.0.0.1");

        // ✅ 修改點：直接由這裡建立並回傳 Instance
        // 這樣 Spring 容器裡就一定會有一個 HazelcastInstance 的 Bean
        return Hazelcast.newHazelcastInstance(config);
    }
}