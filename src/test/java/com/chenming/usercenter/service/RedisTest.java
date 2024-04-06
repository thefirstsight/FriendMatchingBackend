package com.chenming.usercenter.service;

import com.chenming.usercenter.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {
    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void test(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //添加数据
        valueOperations.set("chenming", "no1");
        valueOperations.set("chenmingInt", 1);
        valueOperations.set("chenmingDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("yupi");
        valueOperations.set("yupiUser", user);

        //查找
        Object chen = valueOperations.get("chenming");
        Assertions.assertTrue("no1".equals((String) chen));     //因为之前值设置的是Object类型
        chen = valueOperations.get("chenmingInt");
        Assertions.assertTrue(1==((Integer)chen));
        chen = valueOperations.get("chenmingDouble");
        Assertions.assertTrue(2.0==((Double)chen));
        System.out.println(valueOperations.get("yupiUser"));
    }
}
