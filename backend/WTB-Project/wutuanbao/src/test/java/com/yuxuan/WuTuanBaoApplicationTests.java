package com.yuxuan;

import com.yuxuan.entity.Shop;
import com.yuxuan.service.impl.ShopServiceImpl;
import com.yuxuan.utils.CacheClient;
import com.yuxuan.utils.RedisIdWork;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.yuxuan.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.yuxuan.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class WuTuanBaoApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private RedisIdWork redisIdWork;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    //用线程创建任务，实现Runnable接口任务的简化
   @Test
   public void testIdWorker() throws InterruptedException {
       CountDownLatch countDownLatch = new CountDownLatch(300);
       Runnable task = () -> {
           for (int i = 0; i < 100; i++) {
               long id = redisIdWork.nextId("order");
               System.out.println("id = " + id);
           }
           countDownLatch.countDown();
       };
       long bengin = System.currentTimeMillis();
       for (int i = 0; i < 300; i++) {
           es.submit(task);
       }
       countDownLatch.await();
       long end = System.currentTimeMillis();
       System.out.println("time" + (end - bengin));
   }

    @Test
    void testSaveShop() throws InterruptedException {
        shopService.saveShop2Redis(1L,10L);
    }

    @Test
    void testSaveShop2Redis() throws InterruptedException {
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY,shop,10L, TimeUnit.SECONDS);
    }

    @Test
    void loadShop() {
       // 1.查询店铺信息
        List<Shop> list = shopService.list();
        //2、把店铺分组，按照typeId分组，typeId一致的放到一个集合
        //Long相当于Key,list()集合装店铺id和经纬度
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //3、分批写入Redis
        // 每一个entry里面有个 List<Shop>
        for (Map.Entry<Long,List<Shop>> entry : map.entrySet()){
            //3.1 获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY+typeId;
            //3.2 获取同类型的店铺的集合
            List<Shop> shops = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(shops.size());
            //3.3 写入redis GEOADD key 经度纬度
            for (Shop shop : shops) {
               // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(),shop.getY()),shop.getId().toString());
                locations.add( new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),      //shopId作为member,经纬度作为score
                        new Point(shop.getX(), shop.getY())));
            }
            //先把 含多个组的商铺的集合进行遍历，比如拿到A组集合，就把A组里的所有商铺进行批量存储到Redis里，这里是将店铺id和经纬度一存入了locations集合
            stringRedisTemplate.opsForGeo().add(key, locations);//对Redis进行了一次写操作
        }
    }

    @Test
    void testHyperLogLog() {
       String[] values = new String[1000];
       int j =0;
       for (int i=0;i<1000000;i++){
           j = i%1000;
           values[j] = "user_" + i;
           if ( j == 999) {
               //发送到Redis
               stringRedisTemplate.opsForHyperLogLog().add("hl2",values);
           }
       }
       // 统计数量
        Long count = stringRedisTemplate.opsForHyperLogLog().size("hl2");
       System.out.println(count);
    }

}
