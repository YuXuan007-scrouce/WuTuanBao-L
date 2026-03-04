package com.yuxuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.dto.GDOrderDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.GroupDeal;
import com.yuxuan.entity.UserCoupon;
import com.yuxuan.entity.UserOrder;
import com.yuxuan.entity.vo.PendingPayOrderVO;
import com.yuxuan.mapper.UserCouponMapper;
import com.yuxuan.mapper.UserOrderMapper;
import com.yuxuan.mq.OrderMqProducer;
import com.yuxuan.service.GroupDealService;
import com.yuxuan.service.UserCouponService;
import com.yuxuan.service.UserOrderService;
import com.yuxuan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserOrderServiceImpl extends ServiceImpl<UserOrderMapper, UserOrder> implements UserOrderService {

    @Resource
    private UserOrderMapper userOrderMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private GroupDealService groupDealService;
    @Resource
    private UserCouponService userCouponService;
    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private OrderMqProducer orderMqProducer;

    /**
     * 用户提交订单首次--创建订单
     * @param orderDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(GDOrderDTO orderDTO) {
        //使用注解给字段自动拦截无效请求
        //1、获取用户登录id
        Long userId = UserHolder.getUser().getId();
        //2、幂等性校验 防重复提交（Redis SetNX，5秒内不允许重复提交
        String lockKey = "DP:order:lock:" + userId + ":" + orderDTO.getProductId();
        Boolean isLocked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isLocked)) {
            return Result.fail("请勿重复提交订单");
        }
        try {
            // 3. 服务端校验：查询团购商品真实数据，防止价格篡改
            GroupDeal groupDeal = groupDealService.getById(orderDTO.getProductId());
            if (groupDeal == null || groupDeal.getStatus() !=1) {
                return Result.fail("团购商品不存在或结束");
            }
            if (groupDeal.getDealPrice().compareTo(orderDTO.getDealPrice()) !=0){
                return Result.fail("价格异常，请刷新重试");
            }
             if (groupDeal.getStock() < orderDTO.getQuantity()) {
                 return Result.fail("库存不足");
             }

            BigDecimal couponAmount = BigDecimal.ZERO;
            // 4. 优惠券处理（如果有）
            if (orderDTO.getCouponId() != null) {
                // 先查券面额（用于后续金额校验），再锁定
                couponAmount = userCouponService.queryCoupon(userId, orderDTO.getCouponId());
                if (couponAmount == null) {
                    return Result.fail("优惠券不存在或不可用");
                }

                Boolean lockCoupon = userCouponService.lockCoupon(userId, orderDTO.getCouponId());
                if (!lockCoupon) {
                    return Result.fail("优惠券不可用");
                }
            }
            // 5. 校验实付金额逻辑是否正确（服务端算一遍）
            // 实付金额 = 团购价 × 数量 - 优惠券面额，且不能低于0
            BigDecimal expectedTotal = orderDTO.getDealPrice()
                    .multiply(BigDecimal.valueOf(orderDTO.getQuantity()))
                    .subtract(couponAmount)                        // 减去优惠券面额
                    .max(BigDecimal.valueOf(0.01));                         // 兜底：最低为0，防止券面额大于订单金额

            // 与前端传来的实付金额比对（精度用compareTo，不用equals）
            if (expectedTotal.compareTo(orderDTO.getRealPayAmount()) != 0) {
                return Result.fail("金额校验失败，请刷新后重试");
            }
            // 6. 生成订单号
            String orderNo = generateOrderNo(userId);

            // 7. 构建订单对象写入数据库
            UserOrder userOrder = new UserOrder();
            userOrder.setOrderNo(orderNo)
                    .setUserId(userId)
                    .setDeal_id(orderDTO.getProductId())
                    .setMerchantId(orderDTO.getMerchantId())
                    .setCouponId(orderDTO.getCouponId())
                    .setDealTitle(orderDTO.getProductTitle())
                    .setDealPrice(orderDTO.getDealPrice())
                    .setQuantity(orderDTO.getQuantity())
                    .setTotalAmount(orderDTO.getTotalAmount())
                    .setPayAmount(orderDTO.getRealPayAmount())
                    .setStatus(1)  // 支付中
                    .setPayType(orderDTO.getPayType())
                    .setCreatedTime(LocalDateTime.now());

            userOrderMapper.insert(userOrder);

            // MQ处理：发送延迟消息，1小时后触发超时取消
            // 1小时 = 60 * 60 * 1000 毫秒;测试3分钟
            orderMqProducer.sendOrderDelayMessage(orderNo, 3 * 60 * 1000L);

            // 8. 将订单号存入Redis，设置1小时过期（用于待支付倒计时超时处理）
            String orderExpireKey = "DP:order:expire:" + orderNo;
            stringRedisTemplate.opsForValue().set(orderExpireKey, String.valueOf(userOrder.getId()), 1, TimeUnit.HOURS);

            // 9. 返回订单号给前端（前端拿着订单号跳转支付页面）
            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("orderId", userOrder.getId());
            result.put("payAmount", userOrder.getPayAmount());
            return Result.ok(result);

        } finally {
            // 释放幂等锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    private String generateOrderNo(Long userId) {
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String userSuffix = String.format("%04d", userId % 10000);
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        return timeStr + userSuffix + randomSuffix;
    }

    /**
     * 确认支付： 支付中1 --> 已完成2
     * @param orderNo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result confirmPay(String orderNo) {
        // 1. 获取当前登录用户
        Long userId = UserHolder.getUser().getId();

        // 2. 查询订单
        UserOrder order = userOrderMapper.selectOne(
                new LambdaQueryWrapper<UserOrder>()
                        .eq(UserOrder::getOrderNo, orderNo)
                        .eq(UserOrder::getUserId, userId)
        );

        // 3. 校验订单是否存在
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 4. 校验订单状态是否为"支付中"（status=1）
        if (order.getStatus() != 1) {
            return Result.fail("订单状态异常，请刷新后重试");
        }

        // 5. 更新订单状态：支付中(1) → 已完成(2)
        int rows = userOrderMapper.update(null,
                new LambdaUpdateWrapper<UserOrder>()
                        .eq(UserOrder::getOrderNo, orderNo)
                        .eq(UserOrder::getUserId, userId)
                        .eq(UserOrder::getStatus, 1)       // 再次确认状态，防并发
                        .set(UserOrder::getStatus, 2)
                        .set(UserOrder::getPayTime, LocalDateTime.now())
        );

        if (rows == 0) {
            return Result.fail("支付失败，请重试");
        }

        // 6. 如果使用了优惠券，更新券状态：锁定(1) → 已使用(2)
        if (order.getCouponId() != null) {
            userCouponMapper.update(null,
                    new LambdaUpdateWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId)
                            .eq(UserCoupon::getCouponId, order.getCouponId())
                            .eq(UserCoupon::getStatus, 1)     // 必须是锁定状态
                            .set(UserCoupon::getStatus, 2)     //更新为已使用
                            .set(UserCoupon::getUseTime, LocalDateTime.now())
                            .set(UserCoupon::getOrderId, order.getId())
            );
        }

        // 7. 清除Redis中的订单倒计时key（订单已完成，不再需要超时监听）
        String orderExpireKey = "DP:order:expire:" + orderNo;
        stringRedisTemplate.delete(orderExpireKey);

        // 8. 返回订单信息给前端展示（二维码内容或订单号）
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("payAmount", order.getPayAmount());
        result.put("payTime", order.getPayTime() != null ? order.getPayTime() : LocalDateTime.now());
        result.put("dealTitle", order.getDealTitle());
        return Result.ok(result);
    }

    /**
     * 订单详情页面(用户取消、完成、待支付)
     * @param orderNo
     * @return
     */
    @Override
    public Result orderDetail(String orderNo) {
        Long userId = UserHolder.getUser().getId();

        PendingPayOrderVO vo = userOrderMapper.selectPendingPayOrder(orderNo, userId);
        if (vo == null) {
            return Result.fail("订单不存在");
        }
        return Result.ok(vo);

    }

    /**
     * 用户主动取消支付
     * @param orderNo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelPay(String orderNo) {
        // 1. 获取当前登录用户
        Long userId = UserHolder.getUser().getId();

        // 2. 查询订单
        UserOrder order = userOrderMapper.selectOne(
                new LambdaQueryWrapper<UserOrder>()
                        .eq(UserOrder::getOrderNo, orderNo)
                        .eq(UserOrder::getUserId, userId)
        );

        // 3. 校验订单是否存在
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 4. 校验状态是否为"支付中"(1)，其他状态不允许取消
        if (order.getStatus() != 1) {
            switch (order.getStatus()) {
                case 0: return Result.fail("订单已是待支付状态");
                case 2: return Result.fail("订单已完成，无法取消");
                case 3: return Result.fail("订单已取消");
                default: return Result.fail("订单状态异常");
            }
        }

        // 5. 更新订单状态：支付中(1) → 待支付(0)
        int rows = userOrderMapper.update(null,
                new LambdaUpdateWrapper<UserOrder>()
                        .eq(UserOrder::getOrderNo, orderNo)
                        .eq(UserOrder::getUserId, userId)
                        .eq(UserOrder::getStatus, 1)   // 防并发，再次确认状态
                        .set(UserOrder::getStatus, 0)
        );

        if (rows == 0) {
            return Result.fail("取消失败，请重试");
        }

        // 6. 优惠券处理：如果有锁定的优惠券，释放回未使用(0)
//        if (order.getCouponId() != null) {
//            int couponRows = userCouponMapper.update(null,
//                    new LambdaUpdateWrapper<UserCoupon>()
//                            .eq(UserCoupon::getUserId, userId)
//                            .eq(UserCoupon::getCouponId, order.getCouponId())
//                            .eq(UserCoupon::getStatus, 1)    // 是锁定状态才释放
//                            .set(UserCoupon::getStatus, 0)   // 释放回未使用
//            );
//            if (couponRows == 0) {
//                // 券释放失败不影响主流程，记录日志即可
//                log.warn("订单 {} 取消时优惠券释放失败");
//            }
//        }

        // 7. 返回 orderNo，前端拿到后跳转待支付详情页
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("status", 0);
        result.put("message", "已取消支付，订单保留1小时");
        return Result.ok(result);
    }

    /**
     * 公共取消订单方法（超时系统调用）
     * 待支付(0) → 已取消(3)
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderBySystem(UserOrder order) {
        // 1. 更新订单状态：待支付(0) → 已取消(3)
        int rows = userOrderMapper.update(null,
                new LambdaUpdateWrapper<UserOrder>()
                        .eq(UserOrder::getId, order.getId())
                        .eq(UserOrder::getStatus, 0)   // 防并发，只处理待支付的
                        .set(UserOrder::getStatus, 3)
        );

        if (rows == 0) {
            // 说明订单状态已被其他操作修改（比如用户刚好续费支付），跳过
            log.info("订单 {} 状态已变更，跳过超时取消", order.getOrderNo());
            return;
        }

        // 2. 释放优惠券：锁定(1) → 未使用(0)
        if (order.getCouponId() != null) {
            userCouponMapper.update(null,
                    new LambdaUpdateWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, order.getUserId())
                            .eq(UserCoupon::getCouponId, order.getCouponId())
                            .eq(UserCoupon::getStatus, 1)
                            .set(UserCoupon::getStatus, 0)
            );
        }

        // 3. 清除Redis倒计时key
        String orderExpireKey = "DP:order:expire:" + order.getOrderNo();
        stringRedisTemplate.delete(orderExpireKey);

        log.info("订单 {} 超时自动取消成功", order.getOrderNo());
    }

    @Override
    public Result repay(String orderNo) {
        //1、获取ThreadLocal中获取用户id
        Long userId = UserHolder.getUser().getId();
        // 2. 查询订单
        UserOrder order = userOrderMapper.selectOne(
                new LambdaQueryWrapper<UserOrder>()
                        .eq(UserOrder::getOrderNo, orderNo)
                        .eq(UserOrder::getUserId, userId)
        );
        if (order == null) {
            return Result.fail("订单不存在！");
        }
        if (order.getStatus() != 0) {
            return Result.fail("订单已过期！");
        }
        //3、更新订单为支付中
        int rows = userOrderMapper.update(null,
                new LambdaUpdateWrapper<UserOrder>()
                        .eq(UserOrder::getId, order.getId())
                        .eq(UserOrder::getStatus, 0)   // 防并发，只处理待支付的
                        .set(UserOrder::getStatus, 1)
        );
        if (rows == 0) {
            Result.fail(500,"订单已自动取消");
        }
        //4、更新成功，即锁住订单状态“支付中”
        return Result.ok(orderNo);
    }


}
