package com.yuxuan.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 1L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;  //缓存空值过期时间（2分钟，防止缓存穿透）

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
    public static final String FOLLOWS_KEY = "follows:";
    public static final String FEED_RECOMMEND_KEY = "feed:recommend:all";
    public static final String FEED_NEAR_KEY = "feed:near:";
    public static final String FEED_FOLLOW_KEY = "feed:follow:";

    public static final String BLOG_COMMENT_LIKED_KEY = "blog:comment:liked:";
    public static final String BLOG_COLLECTED_KEY = "blog:collected:";
    /**
     * 商家缓存key前缀
     */
    public static final String MERCHANT_CACHE_KEY_PREFIX = "cache:merchant:";

    /**
     * 商家布隆过滤器名称
     */
    public static final String MERCHANT_BLOOM_FILTER = "merchant:bloom:filter";

    /**
     * 缓存过期时间（30分钟）
     */
    public static final Long CACHE_MERCHANT_TTL = 30L;

    /**
     * 互斥锁key前缀
     */
    public static final String LOCK_MERCHANT_KEY = "lock:merchant:";

    /**
     * 互斥锁过期时间（10秒）
     */
    public static final Long LOCK_MERCHANT_TTL = 10L;

    /**
     * 布隆过滤器预期插入数量（根据实际商家数量调整）
     */
    public static final long BLOOM_FILTER_EXPECTED_INSERTIONS = 100000L;

    /**
     * 布隆过滤器误判率
     */
    public static final double BLOOM_FILTER_FALSE_PROBABILITY = 0.01;
}
