package com.yuxuan.service;

import com.yuxuan.dto.BCommentLeve;
import com.yuxuan.dto.LikeRequest;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.BComment;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类 自定义
 */
public interface IBlogCommentsService extends IService<BComment> {

    Result queryComments(Long blogId);

    Result likeComment(Long commentId, LikeRequest req);

    Result writeComment(BCommentLeve bComment);
}
