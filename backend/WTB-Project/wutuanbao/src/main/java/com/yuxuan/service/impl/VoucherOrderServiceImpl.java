package com.yuxuan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.SeckillVoucher;
import com.yuxuan.entity.VoucherOrder;
import com.yuxuan.mapper.VoucherOrderMapper;
import com.yuxuan.service.ISeckillVoucherService;
import com.yuxuan.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.utils.RedisIdWork;
import com.yuxuan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWork redisIdWork;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;    //静态的 初始化就加载好lua脚本
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        //使用spring提供的ClassPathResource加在类路径下的resources目录下的资源
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    //线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();  //单线程执行

    @PostConstruct //在当前类初始化完毕后执行
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler()); //让线程执行VoucherOrderHandler里任务
    }

    //应该在用户抢单开始前执行线程任务
    public class VoucherOrderHandler implements Runnable{
        String queueName = "stream.orders";
        @Override
        public void run() {
            while(true){
                try {
                    //1、获取消息队列中的订单信息 XREADGROP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //2、判断订单信息获取是否成功
                   if (list == null || list.isEmpty()){
                       //如果获取失败，就进入下一轮循环
                       continue;
                   }
                   //3、解析消息中的订单消息
                    MapRecord<String, Object, Object> read = list.get(0);
                    Map<Object, Object> record = read.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(record, new VoucherOrder(), true);
                    //4、创建订单进行写操作
                    handleVoucherOrder(voucherOrder);
                    //4、ACK确认
                  stringRedisTemplate.opsForStream().acknowledge(queueName,"g1", read.getId());
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                    handlePendingList(queueName);
                }
            }
        }
    }

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
                    //清空pendinglist,结束循环
                    return;
                }
                //3、解析消息中的订单消息
                MapRecord<String, Object, Object> read = list.get(0);
                Map<Object, Object> record = read.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(record, new VoucherOrder(), true);
                //4、创建订单进行写操作
                handleVoucherOrder(voucherOrder);
                //4、ACK确认
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

    //阻塞队列     当一个线程尝试从该队列里获取元素时，如果队列没元素，该线程就会被阻塞，直到队列里有元素，该线程才会被唤醒并获取元素
//    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024); //设置大小
//    //应该在用户抢单开始前执行线程任务
//    public class VoucherOrderHandler implements Runnable{
//        @Override
//        public void run() {
//            while(true){
//            try {
//                //1、取出阻塞队列头部任务
//                VoucherOrder voucherOrder = orderTasks.take(); //取出阻塞队列头部任务
//                // 2、线程执行创建订单任务
//                handleVoucherOrder(voucherOrder);
//            } catch (Exception e) {
//              log.error("处理订单异常",e);
//            }
//            }
//        }
//    }
    //其实下面不需要锁了，单独线程执行写操作,代理对象也拿不到(基于ThreadLocal)
    private void handleVoucherOrder(VoucherOrder voucherOrder){
        //1、 因为是独立的线程，不能从ThreadLocal取userId,从阻塞队列的元素里拿
        Long userId = voucherOrder.getUserId(); //基于用户id的范围加把锁
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
           proxy.createVoucherOder(voucherOrder);
        } catch (Exception e) {
            throw new RuntimeException("异常信息："+e);
        } finally {
            lock.unlock();    //TODO: 即使出现异常也会执行(手动释放)释放锁    手动判断
        }
    }

    private IVoucherOrderService proxy;

    // 使用Redis的消息队列 实现异步秒杀业务的优化
    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        // 订单id
        long orderId = redisIdWork.nextId("order");

        // 假设voucher是您的秒杀券对象
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 将LocalDateTime转换为秒级时间戳
        long beginTimeStamp = voucher.getBeginTime().toEpochSecond(ZoneOffset.UTC);
        long endTimeStamp = voucher.getEndTime().toEpochSecond(ZoneOffset.UTC);

// 存储到Redis的Hash结构中
        stringRedisTemplate.opsForHash().put(
                "seckill:voucher:" + voucherId,
                "beginTime",
                String.valueOf(beginTimeStamp)
        );
        stringRedisTemplate.opsForHash().put(
                "seckill:voucher:" + voucherId,
                "endTime",
                String.valueOf(endTimeStamp)
        ); ////////////

        //1、执行lua脚本
        Long executeResult = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int r = executeResult.intValue();
        //2、判断结果是否为0
        String message = "";
        if (r != 0) {
            switch (r) {
                case 1:
                    message = "库存不足";
                    break;
                case 2:
                    message = "不允许重复下单";
                    break;
                case 3:
                    message = "优惠券秒杀未开始！";
                    break;
                case 4:
                    message = "优惠券已过期！";
                    break;
                default:
                    message = "秒杀失败";
            }
            }

            //2.2 为0， 有购买资格，把下单信息保存到Redis里的消息队列
            // 3、获取代理对象
            proxy = (IVoucherOrderService) AopContext.currentProxy();
            //4、返回订单id
            return Result.ok(message);
        }


        //基于异步优化秒杀业务  阻塞队列方式
//    @Override
//    public Result seckillVoucher(Long voucherId){
//        //获取用户id
//        Long userId = UserHolder.getUser().getId();
//        //1、执行lua脚本
//        Long executeResult = stringRedisTemplate.execute(
//                SECKILL_SCRIPT,
//                Collections.emptyList(),
//                voucherId.toString(), userId.toString()
//        );
//        int r = executeResult.intValue();
//        //2、判断结果是否为0
//        if (r != 0){
//            //2.1 不为 0，代表没有购买资格
//            return Result.fail( r==1 ?"库存不足" : "重复下单");
//        }
//
//        //2.2 为0， 有购买资格，把下单信息保存到阻塞队列
//        //2.3、创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        //2.4 订单id
//        long orderId = redisIdWork.nextId("order");
//        voucherOrder.setId(orderId);
//        //2.5 用户id      从ThreadLocal里面拿到
//        voucherOrder.setUserId(userId);
//        //2.6 优惠卷id
//        voucherOrder.setVoucherId(voucherId);
//        //2.7 放入阻塞队列
//        orderTasks.add(voucherOrder);
//        // 3、获取代理对象
//         proxy = (IVoucherOrderService) AopContext.currentProxy();
//        //4、返回订单id
//        return Result.ok(orderId);
//    }

        //下面是基于Redisson分布式锁实现业务(同步完成)
//    @Override
//    public Result seckillVoucher(Long voucherId){  //优惠卷表的id是与秒杀优惠卷的id共享的
//        //1、查询优惠卷   tb_seckill_voucher
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        //2、判断秒杀是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            //优惠卷秒杀未开始
//            Result.fail("优惠卷使用未开始！");
//        }
//        //3、判断秒杀是否已经结束
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            //优惠卷秒杀已结束佛晚饭后符合哈到额骄傲的
//            Result.fail("优惠卷已过期！");
//        }
//        //4、判断库存是否充足
//        if (voucher.getStock() < 1) {
//            // 优惠卷库存不足 (共享一个库存)
//            Result.fail("库存不足");
//        }
//        //5、一人一单
//        Long userId = UserHolder.getUser().getId();    //基于用户id的范围加把锁
//        //SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        boolean isLock = lock.tryLock();   //无参表示失败不等待立即返回和30秒锁超时释放 3个参数:线程最大等待时间、锁的超时释放、时间单位
//        //判断锁是否获取成功
//        if (!isLock) {
//            //获取失败，返回错误信息
//            return Result.fail("不允许重复下单");
//        }
//       try {
//           IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//           //8、返回创建订单的id
//           return proxy.createVoucherOder(voucherId);
//       } catch (Exception e) {
//           throw new RuntimeException("异常信息："+e);
//       } finally {
//           lock.unlock();    //TODO: 即使出现异常也会执行(手动释放)释放锁    手动判断
//       }
//
//    }


        //这里加悲观锁，同步锁锁是当前对象，线程安全
        @Transactional
        public void createVoucherOder (VoucherOrder voucherOrder){
            //异步的不能从ThreadLocal取
            //一人一单
            Long userId = voucherOrder.getUserId();
            //5.1 查询订单
            int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
            // 5.2、判断是否存在
            if (count > 0) {
                // 用户已买了
                log.error("用户已购买一次");
                return;
            }

            //TODO: 6、扣减库存  操作tb_seckill_voucher表
            boolean success = seckillVoucherService.update().setSql("stock = stock - 1")  //set stock=stock-1
                    .eq("voucher_id", voucherOrder.getVoucherId())
                    .gt("stock", 0)       // where id = ? and stock = ?
                    .update();
            if (!success) {
                //扣减失败
                log.error("库存不足");
                return;
            }
            //7、创建订单
            save(voucherOrder);              // 操作tb_voucher_order
            //先释放锁，事务(由spring管理)才会提交订单
        }


    //这里加悲观锁，同步锁锁是当前对象，线程安全
//    @Transactional
//    public  Result createVoucherOder(Long voucherId) {
//
//        Long userId = UserHolder.getUser().getId();
//        //5.1 查询订单
//        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//        // 5.2、判断是否存在
//        if (count > 0) {
//            // 用户已买了
//            return Result.fail("用户已经购买过一次！");
//        }
//
//        //TODO: 6、扣减库存  操作tb_seckill_voucher表
//        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")  //set stock=stock-1
//                .eq("voucher_id", voucherId)
//                .gt("stock", 0)       // where id = ? and stock >= 0
//                .update();
//        if (!success) {
//            //扣减失败
//            return Result.fail("库存不足");
//        }
//        //7、创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        //7.1 订单id
//        long orderId = redisIdWork.nextId("order");
//        voucherOrder.setId(orderId);
//        //7.2 用户id      从ThreadLocal里面拿到
//        voucherOrder.setUserId(userId);
//        //7.3 优惠卷id
//        voucherOrder.setVoucherId(voucherId);
//        save(voucherOrder);              // 操作tb_voucher_order
//        // 7、返回订单
//        return Result.ok(orderId);
//
//        //先释放锁，事务(由spring管理)才会提交订单
//    }
}
