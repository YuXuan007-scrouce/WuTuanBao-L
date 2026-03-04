-- 释放锁的脚本：先比较锁的值，匹配则删除
-- KEYS[1] 是锁的键，ARGV[1] 是期望的锁标识（用于确认当前客户端拥有该锁）
if (redis.call('get', KEYS[1]) == ARGV[1]) then
    -- 值匹配，说明是当前客户端持有的锁，执行删除
    return redis.call('del', KEYS[1])
else
    -- 值不匹配，说明可能已过期或被其他客户端持有，返回0表示未执行删除
    return 0
end