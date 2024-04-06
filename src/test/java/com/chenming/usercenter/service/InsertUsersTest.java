package com.chenming.usercenter.service;

import com.chenming.usercenter.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class InsertUsersTest {
    @Resource
    private UserService userService;
    private ExecutorService executorService = new ThreadPoolExecutor(40,1000,10000, TimeUnit.MINUTES,new ArrayBlockingQueue<>(10000));


    @Test
    public void doInsertUser(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100000;
        ArrayList<User> userArrayList = new ArrayList<>();
        for(int i = 0; i < INSERT_NUM; i ++){
            User user = new User();
            user.setUsername("假数据");
            user.setUserAccount("fakeaccount");
            user.setAvatarUrl("https://img1.baidu.com/it/u=1645832847,2375824523&fm=253&fmt=auto&app=138&f=JPEG?w=480&h=480");
            user.setGender(0);
            user.setUserPassword("231313123");
            user.setPhone("1231312");
            user.setEmail("12331234@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("213123");
            user.setTags("[]");
            userArrayList.add(user);
        }
        userService.saveBatch(userArrayList, 10000);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());

    }


    @Test
    public void doCurrencyInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        //分10组
        int batchSize = 5000;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();   //泛型列表，指定了列表中元素的类型是 CompletableFuture<Void>。这表示列表中存放的是异步任务的结果，这些任务没有返回值（Void）
        for(int i = 0; i < 20; i ++){
            List<User> userList = new ArrayList<>();
            while(true){
                j ++;
                User user = new User();
                user.setUsername("假数据");
                user.setUserAccount("fakeaccount");
                user.setAvatarUrl("https://img1.baidu.com/it/u=1645832847,2375824523&fm=253&fmt=auto&app=138&f=JPEG?w=480&h=480");
                user.setGender(0);
                user.setUserPassword("231313123");
                user.setPhone("1231312");
                user.setEmail("12331234@qq.com");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("213123");
                user.setTags("[]");
                userList.add(user);
                if(j % batchSize == 0){
                    break;
                }
            }
            //异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(()->{
                System.out.println("threadName:" + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();  //创建一个新的 CompletableFuture 对象，该对象会在传入的所有 CompletableFuture 对象都完成时触发

        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
