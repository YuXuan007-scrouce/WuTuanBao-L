package com.yuxuan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.dto.BlogWithAuthorDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.dto.ScrollResult;
import com.yuxuan.entity.Blog;
import com.yuxuan.mapper.BlogMapper;
import com.yuxuan.service.HomeService;
import com.yuxuan.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.yuxuan.utils.RedisConstants.*;

@Service
public class HomeServiceImpl extends ServiceImpl<BlogMapper, Blog> implements HomeService {

    @Resource
    private BlogMapper blogMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //顶部导航栏切换
    @Override
    public Result switchTab(Long lastMin, Integer offset, String type) {
        //1、获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key;
        ScrollResult r = new ScrollResult();
        if (type.equals("recommend")) {
             key = FEED_RECOMMEND_KEY;
             r.setType("recommend");
        } else if (type.equals("near")) {
            key = FEED_NEAR_KEY + userId;
            r.setType("near");
        } else {
            key = FEED_FOLLOW_KEY + userId;
            r.setType("follow");
        }

        //2、滚动查询收件箱 zrevrangebyscore Key Max min WITHSCORES limit offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, lastMin, offset, 6);
        //3、解析数据: blogId、minTime(时间戳)、offset
        if (typedTuples == null || typedTuples.isEmpty()) {

            return Result.ok("没有最新作品");
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

            //4、根据id 查询Blog是否被当前用户赞过？
            for( BlogWithAuthorDTO blogWithAuthor : list) {
                isBlogLiked2(blogWithAuthor,userId);
            }

        //5、封装并返回
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
}
