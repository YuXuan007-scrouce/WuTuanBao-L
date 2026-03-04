package com.yuxuan.config;

import com.yuxuan.mapper.ShopListMapper;
import com.yuxuan.utils.BloomFilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 应用启动时初始化布隆过滤器数据
 * 将所有已存在的商家ID加载到布隆过滤器中
 */
@Slf4j
@Component
@Order(1) // 设置执行顺序，优先级最高
public class BloomFilterInitializer implements ApplicationRunner {

    @Resource
    private ShopListMapper shopListMapper;

//    @Resource
//    private BloomFilterUtil bloomFilterUtil;
      @Resource
      private BloomFilterUtil bloomFilterUtil;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=================================================");
        log.info("开始初始化商家布隆过滤器数据...");

        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询所有商家ID
            List<Long> merchantIds = shopListMapper.selectAllMerchantIds();

            if (merchantIds == null || merchantIds.isEmpty()) {
                log.warn("数据库中没有商家数据，跳过布隆过滤器初始化");
                return;
            }

            // 2. 批量添加到布隆过滤器
            bloomFilterUtil.addMerchantIds(merchantIds);

            long endTime = System.currentTimeMillis();
            long cost = endTime - startTime;

            log.info("商家布隆过滤器初始化完成！");
            log.info("- 加载商家数量: {}", merchantIds.size());
            log.info("- 耗时: {} ms", cost);
            log.info("- 当前布隆过滤器元素数: {}", bloomFilterUtil.getCount());


        } catch (Exception e) {
            log.error("初始化商家布隆过滤器失败", e);
            throw e;
        }
    }
}
