--1、参数列表
--1.1 优惠卷id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]
-- 1.3 订单id
local orderId = ARGV[3]

-- 2.数据Key
-- 2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key
local orderKey = 'seckill:order:' .. voucherId
-- 2.3 优惠卷时间信息
local voucherKey = 'seckill:voucher' .. voucherId

-- 3.1 判断库存
if (tonumber(redis.call('get',stockKey)) <= 0) then
    -- 库存不足，返回1
    return 1
end
-- 3.2 判断用户是否下单
if (redis.call('sismember',orderKey,userId) == 1) then
    -- 订单存在，返回2
    return 2
end
-- 3.3 判断秒杀活动时间
-- 3.3.1 获取优惠券信息（开始时间和结束时间）
local voucherData = redis.call('HMGET', voucherKey, 'beginTime', 'endTime')
local beginTime = voucherData[1]
local endTime = voucherData[2]
-- 3.3.2 获取Redis服务器当前时间
local currentTime = redis.call('TIME')
-- Redis的TIME命令返回一个包含两个元素的数组：秒级时间戳和微秒数[6,7](@ref)
-- 我们取第一个元素（秒级时间戳）并转换为数字进行后续比较
local currentTimestamp = tonumber(currentTime[1])

-- 3.3.3 判断秒杀是否已经开始
if (beginTime ~= false and currentTimestamp < tonumber(beginTime)) then
    return 3 -- 秒杀未开始
end
-- 3.3.4 判断秒杀是否已经结束
if (endTime ~= false and currentTimestamp > tonumber(endTime)) then
    return 4 -- 秒杀已结束
end

-- 4.1 订单不存在 扣库存
redis.call('incrby',stockKey,-1)
-- 4.2 下单(保存用户)
redis.call('sadd',orderKey,userId)
-- 4.3 发送消息到组队列中， XADD stream.orders * k1 v1 k2 v2
redis.call('XADD','stream.orders', '*','userId',userId,'voucherId',voucherId, 'id',orderId)
return 0