package com.yuxuan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.MCoupon;
import com.yuxuan.entity.UserCoupon;
import com.yuxuan.mapper.UserCouponMapper;
import com.yuxuan.service.MCouponService;
import com.yuxuan.service.UserCouponService;
import com.yuxuan.utils.RedisIdWork;
import com.yuxuan.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements UserCouponService {

    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private MCouponService mCouponService;   //超做merchant_coupon表
    @Resource
    private RedisIdWork redisIdWork;   // 全局唯一ID
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> COUPONKILL_SCRIPT;    //静态的 初始化就加载好lua脚本
    private UserCouponService proxy;  // 代理对象

    static {
        COUPONKILL_SCRIPT = new DefaultRedisScript<>();
        //使用spring提供的ClassPathResource加在类路径下的resources目录下的资源
        COUPONKILL_SCRIPT.setLocation(new ClassPathResource("couponkill.lua"));
        COUPONKILL_SCRIPT.setResultType(Long.class);
    }
    //线程池
    private static final ExecutorService USER_COUPON_EXECUTOR = Executors.newSingleThreadExecutor();  //单线程执行

    @PostConstruct //当前类初始化完毕加载
    private void init(){
        USER_COUPON_EXECUTOR.submit(new UCHandler()); //让线程执行VoucherOrderHandler里任务
    }
    //应该在用户抢单开始前执行线程任务
    class UCHandler implements Runnable {

        @Override
        public void run() {
            String queueName = "Queue:coupons";
            while (true) {
                try {
                    //1、获取消息队列中的订单信息 XREADGROP g1 c1 COUNT 1 BLOCK 2000 STREAMS Queue:coupons >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //2、判断订单信息获取是否成功
                    if (list == null || list.isEmpty()) {
                        //如果获取失败，就进入下一轮循环
                        continue;
                    }
                    //3、解析消息中的订单消息
                    MapRecord<String, Object, Object> read = list.get(0);
                    Map<Object, Object> record = read.getValue();
                    // 取出的属性只有user_id、couponId、id(订单id)
                    UserCoupon userCoupon = BeanUtil.fillBeanWithMap(record, new UserCoupon(), true);
                    //4、创建订单进行写操作
                    handleCouponOrder(userCoupon);
                    //4、ACK确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", read.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList(queueName);
                }
            }
        }
    }

    //其实下面不需要锁了，单独线程执行写操作,代理对象也拿不到(基于ThreadLocal)
    private void handleCouponOrder(UserCoupon myCoupon){
        //1、 因为是独立的线程，不能从ThreadLocal取userId,从阻塞队列的元素里拿
        Long userId = myCoupon.getUserId(); //基于用户id的范围加把锁
        //2、 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();   //无参表示失败不等待立即返回和30秒锁超时释放 3个参数:线程最大等待时间、锁的超时释放、时间单位
        //3、判断锁是否获取成功
        if (!isLock) {
            //获取失败，记录一下日志
            log.error("不允许重复下单"); //理论上不会出现并发问题，因为Redis进行了判断一人一单
            return ;
        }
        try {
            //调用代理对象创建订单 这里的 proxy是之前获取到的代理对象
            proxy.createCoupon(myCoupon);
        } catch (Exception e) {
            throw new RuntimeException("异常信息："+e);
        } finally {
            lock.unlock();    //TODO: 即使出现异常也会执行(手动释放)释放锁    手动判断
        }
    }
    // 订单异常处理
    private void handlePendingList(String queueName){
        while(true){
            try {
                //1、获取pendingList中的订单信息 XREADGROP g1 c1 COUNT 1 STREAMS stream.order 0
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(queueName, ReadOffset.from("0"))
                );
                //2、判断订单信息获取是否成功
                if (list == null || list.isEmpty()){
                    //如果获取失败，说明pendinglist没异常消息,结束循环
                    return;
                }
                //3、解析消息中的订单消息
                MapRecord<String, Object, Object> read = list.get(0);
                Map<Object, Object> record = read.getValue();
                // 取出的属性只有user_id、couponId、id(订单id)
                UserCoupon userCoupon = BeanUtil.fillBeanWithMap(record, new UserCoupon(), true);
                //4、创建订单进行写操作
                handleCouponOrder(userCoupon);
                //5、ACK确认
                stringRedisTemplate.opsForStream().acknowledge(queueName,"g1", read.getId());
            } catch (Exception e) {
                log.error("处理pending-lis异常",e);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ei){
                    ei.printStackTrace();
                }
            }
        }
    }


    @Override
    public Result deskillCoupon(Long id) {
        //0、订单id与用户id
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWork.nextId("coupon:"+userId);
        //1、查询优惠卷
        MCoupon mCoupon = mCouponService.getById(id);
        //2、判断秒杀是否开始
        if (mCoupon.getBeginTime().isAfter(LocalDateTime.now())){
            // 尚未开始
            return Result.fail(501,"活动暂未开始");
        }
        //3、判断秒杀是否结束
        if (mCoupon.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail(502,"活动已结束");
        }
        // 4、执行Lua脚本(库存与一人一单) 第一个参数是脚本、第二个是参数集合,第三个是脚本需要的参数
        Long result = stringRedisTemplate.execute(
                COUPONKILL_SCRIPT,
                Collections.emptyList(),
                id.toString(), userId.toString(),String.valueOf(orderId)    //第三个参数用于成功后向队列发送订单消息
        );
        int r = result.intValue();
        // 4.2 判断结果
        if (r == 1){
            return Result.fail( 503,"库存不足");
        } else if (r == 2){
            return Result.fail(504,"不可重复领取！");
        }
        // 4.3 结果为0代表有资格领取优惠卷
        // 5、另开一个线程进行异步创建优惠卷订单
        proxy = (UserCouponService) AopContext.currentProxy();
        return Result.ok("领取成功");
    }
    @Transactional
    public void createCoupon(UserCoupon userCoupon){
            // 子线程只能从实体中获取user_id、couponId
            Long userId = userCoupon.getUserId();
            Long couponId = userCoupon.getCouponId();
            // 先去merchant表查找基本信息:name、actualValue、merchantId、过期时间、状态
            MCoupon mCoupon = mCouponService.getById(couponId);
            // MySQL中检查一人一单
        UserCoupon existingCoupon = userCouponMapper.selectOne(new QueryWrapper<UserCoupon>()
                .eq("user_id", userId)
                .eq("coupon_id", couponId));
        if (existingCoupon != null) {
            // 如果已经领取过该优惠券，则返回提示
            log.error("已领取过优惠卷");
        }
            // MySQL 扣减库存
            boolean success = mCouponService.update().setSql("stock = stock - 1")
                  .eq("id", userCoupon.getCouponId())
                  .ge("stock", 0) // where id = ? and stock = ?
                  .update();
           if (!success) {
               //扣减失败
               log.error("库存不足");
               return;
           }
            //7.2 商家id
            userCoupon.setMerchantId(mCoupon.getMerchantId());  // 用于关联支付界面的勾选优惠

            //7.3 优惠卷的过期时间
            userCoupon.setExpireTime(mCoupon.getEndTime());// 过期时间即有效时间内
            //7.4 优惠卷面额与状态
            userCoupon.setCouponAmount(mCoupon.getActualValue());
            userCoupon.setStatus(0);
            //7.5 当前领取时间
            userCoupon.setReceiveTime(LocalDateTime.now());
            userCouponMapper.insert(userCoupon);
            //7.6 Lua脚本已写入Redis中
        }

    /**
     * 用户在使用优惠卷时进行上锁:优惠卷标记为“使用中”
     */
    @Override
    public Boolean lockCoupon(Long userId, Long couponId) {
        // 1. 先查询券是否存在且合法（给出明确的失败原因）
        UserCoupon coupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
        );

        if (coupon == null) {
            throw new RuntimeException("优惠券不存在");
        }
        if (coupon.getStatus() == 1) {
            throw new RuntimeException("优惠券使用中，请勿重复提交");
        }
        if (coupon.getStatus() == 2) {
            throw new RuntimeException("优惠券已使用");
        }
        if (coupon.getStatus() == 3 || LocalDateTime.now().isAfter(coupon.getExpireTime())) {
            throw new RuntimeException("优惠券已过期");
        }

        // 2. 乐观锁更新（返回影响行数，0说明被并发抢占）
        int rows = userCouponMapper.lockCoupon(userId, couponId);
        if (rows == 0) {
            throw new RuntimeException("优惠券锁定失败，请重试");
        }

        return true;
    }

    /**
     * 计算优惠卷面额
     * @return
     */
    @Override
    public BigDecimal queryCoupon(Long userId, Long couponId) {
        return userCouponMapper.queryCoupon(userId,couponId);
    }

}
