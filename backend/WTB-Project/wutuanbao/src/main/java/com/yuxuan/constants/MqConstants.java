package com.yuxuan.constants;

public class MqConstants {

    /**
     * 订单延迟交换机（使用延迟插件提供的类型）
     */
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";

    /**
     * 订单超时取消队列
     */
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";

    /**
     * 路由key
     */
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
}
