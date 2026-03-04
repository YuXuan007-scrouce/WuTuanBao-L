--1、参数列表
--1.1 优惠卷id
local couponId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]
-- 1.3 订单id
local orderId = ARGV[3]

-- 2.数据Key
-- 2.1 库存key
local stockKey = 'coupon:stock:' .. couponId
-- 2.2 用户优惠卷key：存储用户id
local orderKey = 'coupon:orders:' .. couponId
-- 2.3 优惠卷时间信息
--local voucherKey = 'coupon:voucher' .. couponId

-- 3.1 判断库存
-- 3.1 判断库存
local stock = tonumber(redis.call('get', stockKey))
if stock == nil or stock <= 0 then
    -- 库存不足，返回1
    return 1
end
-- 3.2 判断用户是否下单
if (redis.call('sismember',orderKey,userId) == 1) then
    -- 用户已经领取过优惠卷，返回2
    return 2
end


-- 4.1 订单不存在 扣库存
redis.call('incrby',stockKey,-1)
-- 4.2 下单(保存用户)
redis.call('sadd',orderKey,userId)
-- 4.3 发送消息到组队列中， XADD Queue:coupons * k1 v1 k2 v2
redis.call('XADD','Queue:coupons', '*','userId',userId,'couponId',couponId, 'id',orderId)
return 0