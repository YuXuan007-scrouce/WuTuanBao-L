package com.yuxuan.service.impl;


import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yuxuan.dto.BCommentDTO;
import com.yuxuan.dto.BCommentLeve;
import com.yuxuan.dto.LikeRequest;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.BComment;
import com.yuxuan.entity.Blog;

import com.yuxuan.mapper.BCommentMapper;

import com.yuxuan.mapper.BlogMapper;
import com.yuxuan.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.yuxuan.utils.RedisConstants.BLOG_COMMENT_LIKED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BCommentMapper, BComment> implements IBlogCommentsService {

    @Resource
    private BCommentMapper bCommentMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserInfoServiceImpl userInfoServiceImpl;
    @Resource
    private BlogMapper blogMapper;

    // 拉取评论
    @Override
    public Result queryComments(Long blogId) {

        List<BCommentDTO> commentList = bCommentMapper.queryComments(blogId);
        return Result.ok(commentList);
    }
    // 点赞某条评论
    @Override
    public Result likeComment(Long commentId, LikeRequest req) {
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_COMMENT_LIKED_KEY + commentId;
        //去Redis中查询是否点过赞
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null ) {
            // 缓存点赞
            boolean liked = update().setSql("like_count = like_count + 1").eq("id", commentId).update();
            if (liked) {
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
            req.setLike(true);
        } else {
            // 已点过赞，取消点赞
            boolean liked = update().setSql("like_count = like_count - 1").eq("id", commentId).update();
            if (liked) {
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
            req.setLike(false);
        }
        return Result.ok(req);
    }

    @Override
    public Result writeComment(BCommentLeve bCommentLeve) {
        Long userId = UserHolder.getUser().getId();
        BComment bComment = new BComment();
        bComment.setUserId(userId);    //先补充数据
        bComment.setAddress(userInfoServiceImpl.getById(userId).getCity());  //设置地址
        bComment.setBlogId(bCommentLeve.getBlogId());
        bComment.setContent(bCommentLeve.getContent());
        // 1、先判断是一级评论||二级评论
        Long parentId = bCommentLeve.getParentId();
        if (parentId == null) {
            bComment.setParentId(null);
            bComment.setReplyToUserId(null);
            // 是一级评论 直接评论笔记
            int insert = bCommentMapper.insert(bComment);
            if (insert < 0) {
                return Result.fail("评论失败,该笔记作作品被删除！");
            }
        } else {
            // 二级评论 这里我觉得没必要考虑回复某个用户在不在的问题
          bComment.setParentId(parentId);
          bComment.setReplyToUserId(bCommentLeve.getReplyToUserId());
          int update = bCommentMapper.insert(bComment);
          if (update < 0) {
              return Result.fail("回复失败，该评论被删除");
          }
        }
        //笔记评论数加一
        boolean b = incrementCommentCount(bCommentLeve.getBlogId(), 1);
        System.out.println("评论数增加:"+ b);
        return Result.ok();
    }
    public boolean incrementCommentCount(Long blogId, int delta) {
        UpdateWrapper<Blog> updateWrapper = new UpdateWrapper<>();
        updateWrapper
                .eq("id", blogId)
                .setSql("comments = comments + " + delta);

        return blogMapper.update(null, updateWrapper) > 0;
    }
}
