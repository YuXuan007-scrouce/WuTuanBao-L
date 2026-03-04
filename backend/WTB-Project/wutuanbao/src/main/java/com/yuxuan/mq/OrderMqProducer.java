package com.yuxuan.mq;

import com.yuxuan.constants.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class OrderMqProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送订单延迟消息
     * @param orderNo   订单号
     * @param delayTime 延迟时间（毫秒）
     */
    public void sendOrderDelayMessage(String orderNo, long delayTime) {
        rabbitTemplate.convertAndSend(
                MqConstants.ORDER_DELAY_EXCHANGE,
                MqConstants.ORDER_TIMEOUT_ROUTING_KEY,
                orderNo,
                message -> {
                    // 设置延迟时间（毫秒），延迟插件通过这个header控制延迟
                    message.getMessageProperties().setHeader("x-delay", delayTime);
                    return message;
                }
        );
        log.info("订单 {} 延迟消息发送成功，将在 {} 分钟后超时取消",
                orderNo, delayTime / 1000 / 60);
    }
}