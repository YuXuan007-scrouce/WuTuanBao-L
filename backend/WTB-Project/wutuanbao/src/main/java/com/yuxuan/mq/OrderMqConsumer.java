package com.yuxuan.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuxuan.constants.MqConstants;
import com.yuxuan.entity.UserOrder;
import com.yuxuan.mapper.UserOrderMapper;
import com.yuxuan.service.UserOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@Component
public class OrderMqConsumer {

    @Resource
    private UserOrderService userOrderService;

    @Resource
    private UserOrderMapper userOrderMapper;

    /**
     * 监听订单超时队列
     */
    @RabbitListener(queues = MqConstants.ORDER_TIMEOUT_QUEUE)
    public void handleOrderTimeout(String orderNo, Message message, Channel channel) throws IOException {
        log.info("收到订单超时消息，orderNo={}", orderNo);
        try {
            // 查询订单当前状态
            UserOrder order = userOrderMapper.selectOne(
                    new LambdaQueryWrapper<UserOrder>()
                            .eq(UserOrder::getOrderNo, orderNo)
            );

            // 订单不存在，直接确认消息
            if (order == null) {
                log.warn("订单 {} 不存在，跳过处理", orderNo);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 只处理待支付(0)状态的订单
            // 其他状态说明用户已支付或已手动取消，不需要处理
            if (order.getStatus() != 0) {
                log.info("订单 {} 当前状态为 {}，无需超时取消", orderNo, order.getStatus());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 执行超时取消
            userOrderService.cancelOrderBySystem(order);

            // 手动确认消息消费成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("订单 {} 超时取消处理完成", orderNo);

        } catch (Exception e) {
            log.error("订单 {} 超时取消处理失败：{}", orderNo, e.getMessage());
            // 消费失败，拒绝消息并重新入队（结合yaml里的retry配置）
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}