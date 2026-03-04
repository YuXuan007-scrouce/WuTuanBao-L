package com.yuxuan.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.dto.*;
import com.yuxuan.entity.Merchant;
import com.yuxuan.entity.PageResult;
import com.yuxuan.entity.ShopDoc;
import com.yuxuan.mapper.ShopListMapper;
import com.yuxuan.service.ShopListService;
import com.yuxuan.utils.BloomFilterUtil;
import com.yuxuan.utils.RedisConstants;
import com.yuxuan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShopListServiceImpl extends ServiceImpl<ShopListMapper, Merchant> implements ShopListService {

    @Resource
    private RestHighLevelClient client;

    @Resource
    private ShopListMapper shopListMapper;
    @Resource
    private BloomFilterUtil bloomFilterUtil;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Random random = new Random();
    @Override
    public Result search(ShopListParams shopParams) {
        try {
            //1、准备Request
            SearchRequest request = new SearchRequest("merchant");
            //2、准备DSL
            //2.1 query
            buildBaiscQuery(shopParams,request);
            // 2.2 分页
            int page = shopParams.getPage();
            int size = shopParams.getSize();
            request.source().from((page-1)*size).size(size);
            // 2.3 排序("评论数|默认|星级")
            String sort = shopParams.getSortBy();
            if ("rating".equals(sort)) {
                request.source().sort(
                        SortBuilders.fieldSort("rating")
                                .order(SortOrder.DESC)
                );
            } else if ("comments".equals(sort)) {
                // 🔥 人气排序
                request.source().sort(
                        SortBuilders.fieldSort("total_reviews")
                                .order(SortOrder.DESC)
                );
            }

            //3、发送请求，得到响应
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            System.err.println(request);
            //4、解析JSON
            PageResult pageResult = releaseHightlight(response);
            return Result.ok(pageResult);
        } catch (IOException e) {
            throw new RuntimeException(e+"错误请求");
        }
    }

    /**查询商家详情的接口+ 商家关联便签的接口
     */
    @Override
    public Result queryMDetail(Long id) {
        // 先查merchant表的字段，再查询merchant_tag表的字段
        MerchantDTO merchantDTO;
        merchantDTO = shopListMapper.queryMDetail(id);
        if (merchantDTO == null) {
            return Result.fail("该商家也不存在");
        }
        return Result.ok(merchantDTO);
    }

    public Result queryMDetail2(Long id) {
       if (id == null || id <= 0) {
           return Result.fail("商家ID不能为null");
       }
        // 1. 布隆过滤器判断（防止缓存穿透）
        if (!bloomFilterUtil.mightContain(id)) {
            log.warn("布隆过滤器判断商家不存在，merchantId: {}", id);
            return Result.fail("该商家不存在");
        }

        // 2. 使用互斥锁方式查询缓存（防止缓存击穿）
        MerchantDTO merchantDTO = queryWithMutex(id);

        // 3. 判断是否存在
        if (merchantDTO == null) {
            log.info("商家不存在，merchantId: {}", id);
            return Result.fail("该商家不存在");
        }

        log.info("查询商家详情成功，merchantId: {}, merchantName: {}", id, merchantDTO.getName());
        return Result.ok(merchantDTO);

    }
    /**
     * 使用互斥锁解决缓存击穿问题
     *
     * @param id 商家ID
     * @return 商家详情
     */
    private MerchantDTO queryWithMutex(Long id) {
        String key = RedisConstants.MERCHANT_CACHE_KEY_PREFIX + id;

        // 1. 从Redis查询缓存
        String merchantJson = stringRedisTemplate.opsForValue().get(key);

        // 2. 判断缓存是否命中
        if (StrUtil.isNotBlank(merchantJson)) {
            log.debug("缓存命中，merchantId: {}", id);
            return JSONUtil.toBean(merchantJson, MerchantDTO.class);
        }

        // 3. 判断命中的是否是空值（防止缓存穿透）
        if (merchantJson != null) {
            // 命中空值，说明数据库中不存在
            log.debug("缓存命中空值，merchantId: {}", id);
            return null;
        }
        // 4. 未命中缓存，需要重建缓存
        log.debug("缓存未命中，准备查询数据库，merchantId: {}", id);

        String lockKey = RedisConstants.LOCK_MERCHANT_KEY + id;
        MerchantDTO merchantDTO = null;

        try {
            // 4.1 尝试获取互斥锁
            boolean isLock = tryLock(lockKey);

            // 4.2 判断是否获取成功
            if (!isLock) {
                // 获取锁失败，说明有其他线程在重建缓存
                log.debug("获取互斥锁失败，等待重试，merchantId: {}", id);

                // 休眠后重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            // 4.3 获取锁成功，Double Check缓存是否存在
            log.debug("获取互斥锁成功，Double Check缓存，merchantId: {}", id);
            merchantJson = stringRedisTemplate.opsForValue().get(key);

            if (StrUtil.isNotBlank(merchantJson)) {
                // 其他线程已经重建了缓存
                log.debug("Double Check发现缓存已存在，merchantId: {}", id);
                return JSONUtil.toBean(merchantJson, MerchantDTO.class);
            }

            // 4.4 查询数据库
            log.debug("开始查询数据库，merchantId: {}", id);
            merchantDTO = shopListMapper.queryMDetail(id);

            // 4.5 数据库中不存在，缓存空值（防止缓存穿透）
            if (merchantDTO == null) {
                log.warn("数据库中商家不存在，缓存空值，merchantId: {}", id);

                stringRedisTemplate.opsForValue().set(
                        key,
                        "",
                        RedisConstants.CACHE_NULL_TTL,
                        TimeUnit.MINUTES
                );
                return null;
            }

            // 4.6 数据库中存在，写入Redis缓存
            // 添加随机过期时间，防止缓存雪崩
            long randomExpire = RedisConstants.CACHE_MERCHANT_TTL + random.nextInt(5);

            stringRedisTemplate.opsForValue().set(
                    key,
                    JSONUtil.toJsonStr(merchantDTO),
                    randomExpire,
                    TimeUnit.MINUTES
            );

            log.info("缓存重建成功，merchantId: {}, 过期时间: {} 分钟", id, randomExpire);

        } catch (InterruptedException e) {
            log.error("查询商家详情时发生异常，merchantId: {}", id, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询商家详情失败", e);
        } finally {
            // 5. 释放互斥锁
            unlock(lockKey);
            log.debug("释放互斥锁，merchantId: {}", id);
        }

        return merchantDTO;
    }

    /**
     * 尝试获取互斥锁
     * 使用Redis的SETNX命令实现
     *
     * @param key 锁的key
     * @return true-获取成功, false-获取失败
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", RedisConstants.LOCK_MERCHANT_TTL, TimeUnit.SECONDS);
        // 防止自动拆箱时出现空指针
        return Boolean.TRUE.equals(flag);
    }

    /**
     * 释放互斥锁
     *
     * @param key 锁的key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**查询商家的团购商品列表
     * 团购商品是多家商店可用
     */
    @Override
    public Result queryGroup(Long id) {

        List<GroupDealDTO> groupDealDTOList;
        groupDealDTOList = shopListMapper.queryGroup(id);
        if (groupDealDTOList == null) {
            Result.fail("该商家商品不存在");
        }
        return Result.ok(groupDealDTOList);
    }

    /**
     *职责:精选评分3条
     */
    @Override
    public Result queryComment(Long merchantId) {
        List<MCommentDTO> commentDTOList;
        commentDTOList = shopListMapper.queryComment(merchantId);
        if (commentDTOList == null) {
            Result.fail("该商家赞无评论，快来评论吧");
        }
        return Result.ok(commentDTOList);
    }

    /**
     *根据商家id查询该其优惠卷,并且判断该用户是否领取过优惠卷
     */
    @Override
    public Result queryMerchantCoupon(Long merchantId) {
        UserDTO user = UserHolder.getUser();
        Long userId = user == null ? null : user.getId();
        List<MCouponDTO> couponDTOList;
        couponDTOList = shopListMapper.queryCoupon(userId,merchantId);
        if (couponDTOList == null) {
            Result.fail("该商家暂无优惠卷活动");
        }
        return Result.ok(couponDTOList);
    }

    /**
     * 支付界面的团购查询
     * @param id
     * @return
     */
    @Override
    public Result queryGroupProduct(Long id) {
        // 单表查询团购商品
        GroupPayDTO groupPayDTO = shopListMapper.queryGroupProduct(id);
        if (groupPayDTO == null) {
            Result.fail(500,"该商品已下架！");
        }
        return Result.ok(groupPayDTO);
    }

    /**
     * 支付界面的用户优惠卷查询，只查询该商家下能够使用的优惠卷id
     */
    @Override
    public Result queryUserCoupon(Long merchantId) {
        Long userId = UserHolder.getUser().getId();
        List<UserCouponDTO> userCouponDTOList;
        userCouponDTOList = shopListMapper.queryUserCoupon(merchantId,userId);
        if (userCouponDTOList == null) {
            Result.fail(500,"暂无可用");
        }
        return Result.ok(userCouponDTOList);
    }



    private static void buildBaiscQuery(ShopListParams shopParams, SearchRequest request) {
        //2.1 Boolean 原始查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        String key = shopParams.getKeyword();

        Double distance = shopParams.getNear();
        Double latitude = shopParams.getLatitude();
        Double longitude = shopParams.getLongitude();
        if (distance == null) {
            distance = 5.0; // 默认 5km（可以按业务来）
        }
        //关键字搜索
        if (key == null || "".equals(key)) {
            //为空直接搜索全部
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("name", key));
        }
        // 2. 距离过滤（重点）
        if (shopParams.getLatitude() != null && shopParams.getLongitude() != null) {
            boolQuery.filter(
                    QueryBuilders.geoDistanceQuery("location")
                            .point(
                                    latitude,
                                    longitude
                            )
                            .distance(distance, DistanceUnit.KILOMETERS)
            );
            // 4️⃣ 距离排序（关键）
            request.source().sort(
                    SortBuilders.geoDistanceSort("location", latitude, longitude)
                            .order(SortOrder.ASC)
                            .unit(DistanceUnit.KILOMETERS)
            );
        }
        // 3. 把 query 设置进 request
        request.source().query(boolQuery);
    }

    //解析JSON
    private static PageResult releaseHightlight(SearchResponse response) {
        //4、解析响应
        SearchHits hits = response.getHits();
        //4.1 获取总条数
        //4.2 获取文档数组
        SearchHit[] hits1 = hits.getHits();
        //4.3 遍历文档数组的source
        List<ShopDoc> shops = new ArrayList<>();
        for (SearchHit hit : hits1) {
            String source = hit.getSourceAsString();
            // json的反序列化
            ShopDoc shopDoc = JSON.parseObject(source, ShopDoc.class);
            // 获取排序值
            Object[] sortValues = hit.getSortValues();

            if(sortValues != null && sortValues.length > 0){
                shopDoc.setDistance((Double) sortValues[0]);
            }
            shops.add(shopDoc);
        }
        return new PageResult(shops);
    }
}
