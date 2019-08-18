package com.wangxile.customredispool;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;

import java.util.concurrent.CountDownLatch;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomRedispoolApplicationTests {

    @Autowired
    private IRedisPool iRedisPool;

    private final static int THREAD_NUM = 50;

    private CountDownLatch cd = new CountDownLatch(THREAD_NUM);

    @Test
    public void contextLoads() throws Exception{
        //初始化
        iRedisPool.init(20, 2000);
        //模拟并发连接
        for(int i = 0; i < THREAD_NUM; i++){
            new Thread(() -> {
                Jedis jedis = null;
                try{
                    cd.await();
                    jedis = iRedisPool.getResource();
                    jedis.incr("pooltest.incr");
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    iRedisPool.release(jedis);
                }
            }).start();
            cd.countDown();
        }
        Thread.sleep(2000);
    }
}
