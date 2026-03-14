package com.yuxuan.mq;

import com.yuxuan.constants.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class CouponMqProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;
    /**
     * 发送优惠券延迟下架消息
     * @param couponId    优惠券ID
     * @param removalTime 下架时间
     */
    public void sendCouponRemovalMessage(Long couponId, LocalDateTime removalTime) {
        // 计算延迟时间：下架时间 - 当前时间（毫秒）
        long delayTime = ChronoUnit.MILLIS.between(LocalDateTime.now(), removalTime);

        if (delayTime <= 0) {
            log.warn("优惠券 {} 的下架时间已过期，直接下架处理", couponId);
            // 延迟时间<=0说明上架时间设置有问题，可以直接拒绝或立即下架
            // 这里直接return，由业务层校验拦截
            return;
        }

        rabbitTemplate.convertAndSend(
                MqConstants.ORDER_DELAY_EXCHANGE,
                MqConstants.COUPON_REMOVAL_ROUTING_KEY,
                couponId.toString(),
                message -> {
                    message.getMessageProperties().setHeader("x-delay", delayTime);
                    return message;
                }
        );
        log.info("优惠券 {} 延迟消息发送成功，将在 {} 下架", couponId, removalTime);
    }

}
