package com.example.flashsale.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZookeeperConfig {

    // Docker 裡的 Zookeeper 位址
    private static final String ZK_ADDRESS = "localhost:2181";

    // 重試策略：每 1秒重試一次，最多 3次
    private static final RetryPolicy RETRY_POLICY = new ExponentialBackoffRetry(1000, 3);

    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        return CuratorFrameworkFactory.newClient(
                ZK_ADDRESS,
                60000, // Session timeout
                15000, // Connection timeout
                RETRY_POLICY
        );
    }
}