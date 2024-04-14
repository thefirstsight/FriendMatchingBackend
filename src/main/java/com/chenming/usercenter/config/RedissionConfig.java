package com.chenming.usercenter.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissionConfig {
    private String host;
    private String port;
    private String password; // 添加密码属性

    @Bean
    public RedissonClient redissonClient(){
        //1、创建配置
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port);
        config.useSingleServer().setAddress(redisAddress).setDatabase(3);

        // 如果设置了密码，需要配置密码
        if (password != null && !password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        //2.创建实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
