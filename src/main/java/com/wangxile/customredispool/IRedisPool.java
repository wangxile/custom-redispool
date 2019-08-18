package com.wangxile.customredispool;


import redis.clients.jedis.Jedis;

/**
 * @Author:wangqi
 * @Description:
 * @Date:Created in 2019/8/12
 * @Modified by:
 */
public interface IRedisPool {
    public void init(int maxSize, long maxWaitMillis);

    public Jedis getResource() throws Exception;

    public void release(Jedis jedis);


}
