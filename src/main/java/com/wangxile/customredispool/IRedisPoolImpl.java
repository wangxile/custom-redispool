package com.wangxile.customredispool;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author:wangqi
 * @Description:
 * @Date:Created in 2019/8/12
 * @Modified by:
 */
@Service
public class IRedisPoolImpl implements IRedisPool{
    private int maxSize;
    private long maxWaitMillis;
    //空闲队列
    private LinkedBlockingQueue<Jedis> idleQueue = null;
    //活动队列
    private LinkedBlockingQueue<Jedis> activeQueue = null;
    //总连接数
    private AtomicInteger count = new AtomicInteger();

    @Override
    public void init(int maxSize, long maxWaitMillis) {
        this.maxSize = maxSize;
        this.maxWaitMillis = maxWaitMillis;
        idleQueue = new LinkedBlockingQueue<>(maxSize);
        activeQueue = new LinkedBlockingQueue<>(maxSize);
    }

    /**
     * 获取
     * @return
     * @throws Exception
     */
    @Override
    public Jedis getResource() throws Exception {
        //1.记录开始时间戳,用于判断超时时间
        long startTime = System.currentTimeMillis();
        //2.从空闲队列中获取连接,如果获取到一个空闲连接,将该连接放到活跃队列中
        Jedis redis = null;
        while (redis == null) {
            redis = idleQueue.poll();
            if (redis != null) {
                activeQueue.offer(redis);
                return redis;
            }
            //3.如果无法从空闲连接池中拿到链接的话，判断连接是否已满,如果没有满，创建一个新的连接。放入到活跃连队列中
            if (count.get() < maxSize) {
                if (count.incrementAndGet() <= maxSize) {
                    redis = new Jedis("127.0.0.1", 6379);
                    activeQueue.offer(redis);
                    return redis;
                } else {
                    //由于上面加了,所以这里要减掉还回去
                    count.decrementAndGet();
                }
            }
            //4.如果连接池已满，等待其他线程释放连接到空闲队列中，如果说一定时间内,可以获取到连接，将连接放到活跃队列中
            try{
                redis = idleQueue.poll(maxWaitMillis - (System.currentTimeMillis() - startTime), TimeUnit.MILLISECONDS);
                if(redis != null){
                    activeQueue.offer(redis);
                    return redis;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            //5.如果等待时间超过最长等待时间，则抛出等待超时异常
            if(maxWaitMillis < (System.currentTimeMillis() - startTime)){
                throw new Exception("time out,超过最长等待时间");
            }else{
                continue;
            }
        }
        return redis;
    }

    /**
     * 释放
     * @param jedis
     */
    @Override
    public void release(Jedis jedis) {
        if(activeQueue.remove(jedis)){
          idleQueue.offer(jedis);
        }
    }
}
