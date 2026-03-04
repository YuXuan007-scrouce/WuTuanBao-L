package com.yuxuan.utils;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 布隆过滤器工具类
 */
@Slf4j
@Component
public class BloomFilterUtil {

    @Resource
    private RedissonClient redissonClient;

    private RBloomFilter<Long> merchantBloomFilter;

    /**
     * 初始化布隆过滤器
     * 在Spring容器初始化后自动执行
     */
    @PostConstruct
    public void init() {
        merchantBloomFilter = redissonClient.getBloomFilter(RedisConstants.MERCHANT_BLOOM_FILTER);

        // 初始化布隆过滤器
        // expectedInsertions: 预期插入的数据量
        // falseProbability: 误判率（0.01表示1%的误判率）
        merchantBloomFilter.tryInit(
                RedisConstants.BLOOM_FILTER_EXPECTED_INSERTIONS,
                RedisConstants.BLOOM_FILTER_FALSE_PROBABILITY
        );

        log.info("商家布隆过滤器初始化成功 - 预期数量: {}, 误判率: {}",
                RedisConstants.BLOOM_FILTER_EXPECTED_INSERTIONS,
                RedisConstants.BLOOM_FILTER_FALSE_PROBABILITY);
    }

    /**
     * 添加商家ID到布隆过滤器
     * @param merchantId 商家ID
     */
    public void addMerchantId(Long merchantId) {
        if (merchantId != null) {
            merchantBloomFilter.add(merchantId);
            log.debug("商家ID已添加到布隆过滤器: {}", merchantId);
        }
    }

    /**
     * 批量添加商家ID到布隆过滤器
     * @param merchantIds 商家ID列表
     */
    public void addMerchantIds(java.util.Collection<Long> merchantIds) {
        if (merchantIds != null && !merchantIds.isEmpty()) {
            for (Long merchantId : merchantIds) {
                if (merchantId != null) {
                    merchantBloomFilter.add(merchantId);
                }
            }
            log.info("批量添加商家ID到布隆过滤器，数量: {}", merchantIds.size());
        }
    }

    /**
     * 判断商家ID是否可能存在
     * @param merchantId 商家ID
     * @return true-可能存在, false-一定不存在
     */
    public boolean mightContain(Long merchantId) {
        if (merchantId == null) {
            return false;
        }
        boolean result = merchantBloomFilter.contains(merchantId);
        log.debug("布隆过滤器检查商家ID: {}, 结果: {}", merchantId, result ? "可能存在" : "不存在");
        return result;
    }

    /**
     * 获取布隆过滤器当前大小
     * @return 当前元素数量
     */
    public long getCount() {
        return merchantBloomFilter.count();
    }

    /**
     * 获取布隆过滤器实例
     * @return 布隆过滤器
     */
    public RBloomFilter<Long> getMerchantBloomFilter() {
        return merchantBloomFilter;
    }
}
