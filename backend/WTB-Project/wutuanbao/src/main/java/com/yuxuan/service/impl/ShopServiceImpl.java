package com.yuxuan.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.Shop;
import com.yuxuan.mapper.ShopMapper;
import com.yuxuan.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.utils.CacheClient;
import com.yuxuan.utils.RedisData;
import com.yuxuan.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.yuxuan.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 防止缓存穿透！
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //7、返回
        if (shop==null){
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }

    //缓存重建执行器的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //缓存击穿解决(逻辑过期)          不考虑缓存穿透问题
    public Shop queryWithLogicalExire(Long id){
        //1、从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2、判断是否存命中
        if (StrUtil.isBlank(shopJson)) {
            //3、未命中直接返回null
            return null;
        }
        //4、命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5、判断是否过期           过期时间是在当前时间之前，还是之后
        if (expireTime.isAfter(LocalDateTime.now())) {
            //5.1 未过期，直接返回店铺信息
            return shop;
        }
        //5.2 已过期，需要重键缓存
        //TODO: 6、缓存重建
        // 6.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2 获取是否成功
        if (isLock) {
            // 6.3 成功，开启独立线程，实现缓存重建，最后释放锁
            CACHE_REBUILD_EXECUTOR.submit(() ->{
                try {
                    //重建缓存
                    this.saveShop2Redis(id, 20L);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    releaseLock(lockKey);
                }
            });
        }

        // 6.4 失败，返回过期店铺信息
        return shop;
    }

    // 缓存击穿--->获取互斥锁
    public Shop queryWithMutex(Long id){
        //1、从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2、判断是否存命中
        if (StrUtil.isNotBlank(shopJson)) {
            //3、存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class); //将字符串对象反序列化成实体对象
        }
        // TODO: 3、判断命中的的是否是空值(数据库缓存null对象到Redis)
        if( shopJson != null){
            // shopJson == ""
            return null;
        }
        // TODO 4、实现缓存重建
        // 4.1、获取互斥锁                                          每个店铺id都有一把锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2、判断是否获取成功
            if (!isLock){
                //4.3、 失败，则休眠并重试
                Thread.sleep(LOCK_SHOP_TTL);
                return queryWithMutex(id);
            }
            //4.4，成功，根据id查询数据库
            shop = getById(id);
            //  模拟重建的延时
            Thread.sleep(200);
            //TODO 5、数据库不存在，返回错误,"空值存入Redis"
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //6、数据库存在，写入Redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7、释放互斥锁
            releaseLock(lockKey);
        }

        //8、返回
        return shop;
    }

    //缓存穿透解决
//    public Shop queryWithPassThrough(Long id){
//        //1、从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//        //2、判断是否存命中
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3、存在，直接返回
//             return JSONUtil.toBean(shopJson, Shop.class); //将字符串对象反序列化成实体对象
//        }
//        // TODO: 3、判断命中的的是否是空值(数据库缓存null对象到Redis)
//        if( shopJson != null){
//            return null;
//        }
//        //4、不存在，根据id查询数据库
//        Shop shop = getById(id);
//        //TODO 5、数据库不存在，返回错误,"空值存入Redis"
//        if (shop == null) {
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //6、数据库存在，写入Redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        //7、返回
//        return shop;
//    }

    private boolean tryLock(String key){
        // setnx Locak 1 10s
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        // TODO: 拆箱
        return BooleanUtil.isTrue(flag);
    }

    private void releaseLock(String key){
        stringRedisTemplate.delete(key);
    }

    //热Key预热  封装逻辑过期时间
    public void saveShop2Redis(Long id,Long expireSeconeds) throws InterruptedException {
        //1、查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        //2、封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconeds));
        //3、写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional                         //添加事务
    public Result updateShop(Shop shop) {
        Long shopId = shop.getId();
        if (shopId == null) {
            return Result.fail("店铺id不能为空");
        }
        //1、更新数据库
        updateById(shop);
        //2、删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shopId);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //1、判断是否需要根据坐标查询
        if (x == null || y == null) {
            //不需要根据坐标查询，按数据库查询
            Page<Shop> shopPage = query().eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(shopPage);
        }
        //2、计算分页参数
        int start = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        //3、查询redis、按照距离排序、分页。 结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> searchResult = stringRedisTemplate.opsForGeo()     //GEOSEARCH key BYRADIUS 10 WITHDISTANCE
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)   //分页参数
                );

        //4、解析出shopId
        if (searchResult == null) {
            return Result.ok(Collections.emptyList());
        }

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = searchResult.getContent();
        if (list.size() <= start) {
            return Result.ok(Collections.emptyList());
        }
        //5、截取 from ~ end 部分
        ArrayList<Long> ids = new ArrayList<>(list.size());
        HashMap<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(start).forEach(result -> {
            // 5.1获取 Members 即店铺 shopID
            String shopIdStr = result.getContent().getName();  //获取成员名
            ids.add(Long.valueOf(shopIdStr));
            // 5.2获取距离
            Distance distance = result.getDistance();     //获取到中心点距离
            distanceMap.put(shopIdStr, distance);
        });
        //5、根据id查询Shop
        String idStr = StrUtil.join(",",ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops){
            //为当前用户附近的所有店铺shops,给与其实体类的 distance 赋值
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        //6、返回
        return Result.ok(shops);
    }
}
