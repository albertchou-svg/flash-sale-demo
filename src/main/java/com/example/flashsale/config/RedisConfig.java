package com.example.flashsale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisConfig {

    /**
     * 自定義 RedisTemplate
     * Key 使用 String 序列化 (可讀性高)
     * Value 使用 JSON 序列化 (方便跨語言讀取)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 Jackson 2 將物件轉為 JSON
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 設定 Key 的序列化方式
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 設定 Value 的序列化方式
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public DefaultRedisScript<Long> stockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        // 設定腳本位置
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/stock_deduct.lua")));
        // 設定回傳型別
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}