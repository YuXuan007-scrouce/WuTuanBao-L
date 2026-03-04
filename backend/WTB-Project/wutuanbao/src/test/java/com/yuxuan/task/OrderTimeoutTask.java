package com.yuxuan.task;

//@Slf4j
//@Component
public class OrderTimeoutTask {

//    @Resource
//    private UserOrderMapper userOrderMapper;
//
//    @Resource
//    private UserOrderService userOrderService;
//
//    /**
//     * 每5分钟扫描一次超时订单
//     * cron表达式：秒 分 时 日 月 周
//     */
//    @Scheduled(cron = "0 */5 * * * ?")
//    public void cancelTimeoutOrders() {
//        log.info("开始扫描超时订单...");
//
//        // 查询所有超时的待支付订单：status=0 且 创建时间超过1小时
//        LocalDateTime timeoutTime = LocalDateTime.now().minusHours(1);
//        List<UserOrder> timeoutOrders = userOrderMapper.selectList(
//                new LambdaQueryWrapper<UserOrder>()
//                        .eq(UserOrder::getStatus, 0)
//                        .lt(UserOrder::getCreatedTime, timeoutTime)
//        );
//
//        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
//            log.info("暂无超时订单");
//            return;
//        }
//
//        log.info("扫描到 {} 条超时订单，开始处理...", timeoutOrders.size());
//
//        // 逐条处理，每条单独事务，一条失败不影响其他
//        for (UserOrder order : timeoutOrders) {
//            try {
//                userOrderService.cancelOrderBySystem(order);
//            } catch (Exception e) {
//                log.error("订单 {} 超时取消失败，原因：{}", order.getOrderNo(), e.getMessage());
//            }
//        }
//
//        log.info("超时订单处理完成");
//    }
}
