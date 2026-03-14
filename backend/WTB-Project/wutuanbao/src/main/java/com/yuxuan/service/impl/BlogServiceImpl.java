package com.yuxuan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuxuan.dto.*;
import com.yuxuan.entity.Blog;
import com.yuxuan.entity.Follow;
import com.yuxuan.entity.User;
import com.yuxuan.entity.vo.NoteActionVo;
import com.yuxuan.mapper.BlogDetailMapper;
import com.yuxuan.mapper.BlogMapper;
import com.yuxuan.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.service.IFollowService;
import com.yuxuan.service.IUserService;
import com.yuxuan.utils.BloomFilterUtil;
import com.yuxuan.utils.SystemConstants;
import com.yuxuan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import com.yuxuan.utils.RedisConstants;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.yuxuan.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Slf4j
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;
    @Resource
    private BlogDetailMapper blogDetailMapper;
    @Resource
    private BlogMapper blogMapper;

    @Resource
    private BloomFilterUtil bloomFilterUtil;


    private final Random random = new Random();


    //首页展示的Blog分页查询
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.extracted(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    // 给Blog实体添加 user信息
    private void extracted(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result queryBlogById(Long id) {
        //1、查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 2、查询bolg
        extracted(blog);
        //3、查询blog 是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            //缺:把blog设置为null或false    用户未登录不能执行次操作
            return;
        }
        //1、获取登录用户
        Long userId = userDTO.getId();
        //2、判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    //个人用户点赞接口实现
    @Override
    public Result likeBlog(Long id) {    //此id是blog-id
        //1、获取登录用户
        Long userId = UserHolder.getUser().getId();
        //2、判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score == null) {
            //3、如果未点赞，可以liked + 1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户到Redis的zset集合
            if(isSuccess) {
                stringRedisTemplate.opsForZSet().add( key , userId.toString(),System.currentTimeMillis()); //时间戳为分数
            }
        } else {
            //4、如果已点赞，取消点赞
            //4.1、数据库点赞 -1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(isSuccess) {
                //4.2、把用户从Redis的Zset集合移除
                stringRedisTemplate.opsForZSet().remove( key , userId.toString());
            }
        }
        return Result.ok();
    }

    //查询点赞排名 与 点赞排序
    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 1、查询top5的点赞用户 zrange 0 4  查到是用户id 和分数
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //2、解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        //3、根据用户id查询用户  WHERE id IN (5, 1) ORDER BY FIELD(id, 5,1)
        List<UserDTO> userDTOs = userService.query()
                .in("id",ids).last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        //4、返回
        return Result.ok(userDTOs);
    }

    //滚动分页查询
    @Override
    public Result saveBlog(Blog blog) {
        //1、获取当前登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        //保存探店笔记
        boolean isSuccess = save(blog);
        if(!isSuccess) {
            return Result.fail("新增笔记失败!");
        }
        //3、查询笔记作者的所有粉丝 select * from tb_follow where follow_user_id = ?
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        //4、推送笔记id给所有粉丝
        for (Follow follow : follows) {
            //4.1 获取粉丝id
            Long userId = follow.getUserId();
            //4.2 推送
            String key = FEED_KEY + userId;   //推送到粉丝id为Key的Redis里，值为要发布的笔记id
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(), System.currentTimeMillis());
        }
        //3、返回id
        return Result.ok(blog.getId());
    }

    //在个人关注页面进行滚动的分页查询
    @Override
    public Result queryBlogOfFollw(Long lastMin, Integer offset) {
        //1、获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2、滚动查询收件箱 zrevrangebyscore Key Max min WITHSCORES limit offset count 
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, lastMin, offset, 2);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        //3、解析数据: blogId、minTime(时间戳)、offset
         List<Long> ids = new ArrayList<>(typedTuples.size());
         //下一次偏移量(用于记录上次查询的最小值有多少个相同的分数？)
        int os = 1;
        long minTime = 0;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            long time = tuple.getScore().longValue();
            if(time == minTime){
                os++;
            } else {
                minTime = time;
                os = 1;  //没有重复的分数就重置偏移量
            }
        }
        //blogId用于展示Blog;minTime用于下一次查询的最大值(最小值为0); offset用于跳过相同分数的

        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        //4、根据id 查询Blog
        for (Blog blog : blogs) {
            // 5.2、查询bolg
            extracted(blog);
            //5.3、查询blog 是否被点赞
            isBlogLiked(blog);
        }
        //5、封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setMinTime(minTime);
        r.setOffset(os);
        return Result.ok(r);
    }

    //与商铺一致使用Redis的布隆过滤器
    @Override
    public Result queryBlogDetailById(Long blogId) {
        if (blogId == null) {
            return Result.fail("blog不存在");
        }
        // 1. 布隆过滤器判断（防止缓存穿透）
        if (!bloomFilterUtil.mightContain2(blogId)) {
            log.warn("布隆过滤器判断bolg笔记不存在，merchantId: {}", blogId);
            return Result.fail("该商家不存在");
        }
        //2、使用互斥锁查询缓存(解决缓存击穿)
        BlogDTO blog = queryWithMutex(blogId);
        // 3. 判断是否存在
        if (blog == null) {
            log.info("博客笔记不存在，blogId: {}", blogId);
            return Result.fail("该博客笔记不存在");
        }
        // 从Redis的计数器中查询, 覆盖原值，如果没有被点赞，就使用旧数据兜底
        String likedStr   = stringRedisTemplate.opsForValue().get(BLOG_LIKED_COUNT_KEY + blogId);
        String collectStr = stringRedisTemplate.opsForValue().get(BLOG_COLLECT_COUNT_KEY + blogId);
        if (likedStr != null)   blog.setLiked(Integer.parseInt(likedStr));
        if (collectStr != null) blog.setCollection(Integer.parseInt(collectStr));

        log.info("查询笔记成功，blogId: {}, blogTitle: {}", blogId, blog.getTitle());
        return Result.ok(blog);
    }

    @Override
    public Result likeOrCollection(NoteActionDTO noteActionDTO) {
        //1、获取登录用户
        Long userId = UserHolder.getUser().getId();
        //先判断是收藏还是进行点赞，还是收藏操作
        String action = noteActionDTO.getAction();
        Long id = noteActionDTO.getBlogId();
        NoteActionVo noteActionVo = new NoteActionVo();
        if ("LIKE".equals(action)){
            // 判断该用户是否点赞过
            String key = BLOG_LIKED_KEY + id;
            Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
            if (score == null) {
                //3、如果未点赞，可以liked + 1
                boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
                // 保存用户到Redis的zset集合
                if(isSuccess) {
                    stringRedisTemplate.opsForZSet().add( key , userId.toString(),System.currentTimeMillis()); //时间戳为分数
                }
                noteActionVo.setIsliked(true);
            } else {
                //如果已点赞，可以liked - 1
                boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
                // 保存用户到Redis的zset集合
                if(isSuccess) {
                    stringRedisTemplate.opsForZSet().remove( key , userId.toString()); //时间戳为分数
                }
                noteActionVo.setIsliked(false);
            }
        } else {
            // 收藏操作
          String key = BLOG_COLLECTED_KEY + id;
          Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
          if (score == null) {  //没有收藏过
              //3、如果未收藏，可以collection + 1
              boolean isSuccess = update().setSql("collection = collection + 1").eq("id", id).update();
              // 保存用户到Redis的zset集合
              if(isSuccess) {
                  stringRedisTemplate.opsForZSet().add( key , userId.toString(),System.currentTimeMillis());
              }
              noteActionVo.setIscollected(true);
          } else {
              boolean isSuccess = update().setSql("collection = collection - 1").eq("id", id).update();
              if(isSuccess) {
                  stringRedisTemplate.opsForZSet().remove( key , userId.toString());
              }
              noteActionVo.setIscollected(false);
          }
        }
        Integer likes = blogMapper.queryLikedById(id);
        noteActionVo.setLiked(likes);
        Integer collections = blogMapper.queryCollectionById(id);
        noteActionVo.setCollection(collections);

        //  最新计数同步到 Redis，供详情页读取
        stringRedisTemplate.opsForValue().set(BLOG_LIKED_COUNT_KEY + id, likes.toString());
        stringRedisTemplate.opsForValue().set(BLOG_COLLECT_COUNT_KEY + id, collections.toString());
        return Result.ok(noteActionVo);
    }

    //自定义关注列表查询
    @Override
    public Result queryMyFollwBlog(Long lastMin, Integer offset) {
        //1、获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = FEED_KEY + userId;
        //2、滚动查询收件箱 zrevrangebyscore Key Max min WITHSCORES limit offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, lastMin, offset, 4);
        //3、解析数据: blogId、minTime(时间戳)、offset
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        List<Long> ids = new ArrayList<>(typedTuples.size());
        //下一次偏移量(用于记录上次查询的最小值有多少个相同的分数？)
        int os = 1;
        long minTime = 0;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            Long time = tuple.getScore().longValue();
            if(time == minTime){
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }
        //blogId用于展示Blog;minTime用于下一次查询的最大值(最小值为0); offset用于跳过相同分数的

        List<BlogWithAuthorDTO> list = blogMapper.queryMyFollowBlog(ids);
        //4、根据id 查询Blog
        for( BlogWithAuthorDTO blogWithAuthor : list) {
          isBlogLiked2(blogWithAuthor,userId);
        }
        //5、封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(list);
        r.setMinTime(minTime);
        r.setOffset(os);
        return Result.ok(r);
    }
    private void isBlogLiked2(BlogWithAuthorDTO blogWithAuthor,Long userId) {
        //2、判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + blogWithAuthor.getBlogId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blogWithAuthor.setIsLike(score != null);
    }

    /**
     * 使用互斥锁解决缓存击穿问题
     * @param id blogID
     * @return 笔记内容主体部分
     */
    private BlogDTO queryWithMutex(Long id) {
        String blogKey = RedisConstants.BLOG_CACHE_KEY_PREFIX + id;

        // 1. 从Redis查询缓存
        String blogJson = stringRedisTemplate.opsForValue().get(blogKey);

        // 2. 判断缓存是否命中
        if (StrUtil.isNotBlank(blogJson)) {
            log.debug("缓存命中，blogId: {}",id);
            return JSONUtil.toBean(blogJson, BlogDTO.class);
        }

        // 3. 判断命中的是否是空值（防止缓存穿透）
        if (blogJson != null) {
            // 命中空值，说明数据库中不存在
            log.debug("缓存命中空值，merchantId: {}", id);
            return null;
        }
        // 4. 未命中缓存，需要重建缓存
        log.debug("缓存未命中，准备查询数据库，merchantId: {}", id);

        String lockKey = RedisConstants.LOCK_BLOG_KEY + id;
        BlogDTO blogDTO = null;

        try {
            // 4.1 尝试获取互斥锁
            boolean isLock = tryLock(lockKey);

            // 4.2 判断是否获取成功
            if (!isLock) {
                // 获取锁失败，说明有其他线程在重建缓存
                log.debug("获取互斥锁失败，等待重试，blogId: {}", id);

                // 休眠后重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            // 4.3 获取锁成功，Double Check缓存是否存在
            log.debug("获取互斥锁成功，Double Check缓存，blogId: {}", id);
            blogJson = stringRedisTemplate.opsForValue().get(blogKey);

            if (StrUtil.isNotBlank(blogJson)) {
                // 其他线程已经重建了缓存
                log.debug("Double Check发现缓存已存在，blogId: {}", id);
                return JSONUtil.toBean(blogJson, BlogDTO.class);
            }

            // 4.4 查询数据库
            log.debug("开始查询数据库，merchantId: {}", id);
            blogDTO = blogMapper.queryBlogetail(id);

            // 4.5 数据库中不存在，缓存空值（防止缓存穿透）
            if (blogDTO == null) {
                log.warn("数据库中笔记不存在，缓存空值，blogId: {}", id);

                stringRedisTemplate.opsForValue().set(
                        blogKey,
                        "",
                        RedisConstants.CACHE_NULL_TTL,
                        TimeUnit.MINUTES
                );
                return null;
            }

            // 4.6 数据库中存在，写入Redis缓存
            // 添加随机过期时间，防止缓存雪崩
            long randomExpire = RedisConstants.CACHE_BLOG_TTL + random.nextInt(5);

            stringRedisTemplate.opsForValue().set(
                    blogKey,
                    JSONUtil.toJsonStr(blogDTO),
                    randomExpire,
                    TimeUnit.MINUTES
            );

            log.info("缓存重建成功，merchantId: {}, 过期时间: {} 分钟", id, randomExpire);

        } catch (InterruptedException e) {
            log.error("查询商家详情时发生异常，merchantId: {}", id, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询商家详情失败", e);
        } finally {
            // 5. 释放互斥锁
            unlock(lockKey);
            log.debug("释放互斥锁，merchantId: {}", id);
        }

        return blogDTO;
    }

    /**
     * 尝试获取互斥锁
     * 使用Redis的SETNX命令实现
     *
     * @param key 锁的key
     * @return true-获取成功, false-获取失败
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", RedisConstants.LOCK_BLOG_TTL, TimeUnit.SECONDS);
        // 防止自动拆箱时出现空指针
        return Boolean.TRUE.equals(flag);
    }

    /**
     * 释放互斥锁
     *
     * @param key 锁的key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
