package com.chenming.usercenter.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenming.usercenter.model.domain.User;
import com.chenming.usercenter.service.UserService;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PreCacheJob {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private List<Long> mainUserList = Arrays.asList(1l);

    //每天执行，预热推荐用户
    @Scheduled(cron = "0 24 18 * * *")
    public void doCacheRecommendUser(){
        for (Long userId: mainUserList){
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            Page<User> userPage = userService.page(new Page<>(1, 20), queryWrapper);
            String redisKey = String.format("yupao:user:recommend:%s", userId);
            ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
            try {
                valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
            }catch (Exception e){
                log.error("redis set key error", e);
            }
        }
    }
}
