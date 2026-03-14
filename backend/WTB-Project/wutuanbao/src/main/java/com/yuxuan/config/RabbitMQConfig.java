package com.yuxuan.config;

import com.yuxuan.constants.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitMQConfig {

    /**
     * 声明延迟交换机
     * 注意：type必须是 "x-delayed-message"，这是延迟插件提供的交换机类型
     * 内部路由类型用 direct
     */
    @Bean
    public CustomExchange orderDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(
                MqConstants.ORDER_DELAY_EXCHANGE,
                "x-delayed-message",  // 延迟插件交换机类型
                true,   // durable 持久化
                false,  // autoDelete
                args
        );
    }

    /**
     * 声明订单超时队列
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder
                .durable(MqConstants.ORDER_TIMEOUT_QUEUE)  // 持久化
                .build();
    }
    /**
     *  优惠卷活动下架的队列
     */
    @Bean
    public Queue couponRemovalQueue(){
        return QueueBuilder
                .durable(MqConstants.COUPON_REMOVAL_QUEUE)
                .build();
    }

    /**
     * 绑定队列到延迟交换机
     */
    @Bean
    public Binding orderTimeoutBinding(Queue orderTimeoutQueue, CustomExchange orderDelayExchange) {
        return BindingBuilder
                .bind(orderTimeoutQueue)
                .to(orderDelayExchange)
                .with(MqConstants.ORDER_TIMEOUT_ROUTING_KEY)
                .noargs();
    }

    /**
     * 优惠卷下架队列绑定到延迟交换机
     */
    @Bean
    public Binding couponRemovalBinding(Queue couponRemovalQueue, CustomExchange orderDelayExchange) {
        return BindingBuilder
                .bind(couponRemovalQueue)
                .to(orderDelayExchange)
                .with(MqConstants.COUPON_REMOVAL_ROUTING_KEY)
                .noargs();
    }
}