package com.yuxuan.mq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuxuan.constants.MqConstants;
import com.yuxuan.entity.MCoupon;
import com.yuxuan.service.MCouponService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
public class CouponMqConsumer {

    @Resource
    private MCouponService couponService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = MqConstants.COUPON_REMOVAL_QUEUE)
    public void handleCouponRemoval(String couponId, Message message, Channel channel) throws IOException {
        log.info("收到优惠券下架消息，couponId={}", couponId);
        try {
            // 1. 查询优惠券当前状态
            MCoupon coupon = couponService.getById(Long.parseLong(couponId));

            // 2. 不存在直接ACK
            if (coupon == null) {
                log.warn("优惠券 {} 不存在，跳过处理", couponId);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 3. 只处理上架中(1)的券，其他状态跳过
            if (coupon.getStatus() != 1) {
                log.info("优惠券 {} 当前状态为 {}，无需处理", couponId, coupon.getStatus());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 4. 判断是下架(2)还是过期(3)
            // 下架时间到了但endTime还没到 → 下架(2)
            // endTime也过了 → 过期(3)
            int newStatus = LocalDateTime.now().isAfter(coupon.getEndTime()) ? 3 : 2;

            // 5. 更新数据库状态
            couponService.update(
                    new LambdaUpdateWrapper<MCoupon>()
                            .eq(MCoupon::getId, coupon.getId())
                            .eq(MCoupon::getStatus, 1)   // 防并发
                            .set(MCoupon::getStatus, newStatus)
                            .set(MCoupon::getRemovalTime, LocalDateTime.now())
            );

            // 6. 删除 Redis 库存 key（券已下架，不再允许抢购）
            stringRedisTemplate.delete("coupon:stock:" + couponId);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("优惠券 {} 自动{}成功", couponId, newStatus == 2 ? "下架" : "过期");

        } catch (Exception e) {
            log.error("优惠券 {} 下架处理失败：{}", couponId, e.getMessage());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}