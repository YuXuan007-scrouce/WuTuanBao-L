package com.yuxuan.service;

import com.yuxuan.dto.Result;
import com.yuxuan.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {


    Result follow(Long followUserId, boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);

    //判断是否关注，关注就移除数据，没关注就增加上数据
    Result myfollow(Long followUserId);
}
