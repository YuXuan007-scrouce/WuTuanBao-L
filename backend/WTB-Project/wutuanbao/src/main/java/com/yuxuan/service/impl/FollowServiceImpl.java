package com.yuxuan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuxuan.dto.AuthorDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.dto.UserDTO;
import com.yuxuan.entity.Follow;
import com.yuxuan.mapper.FollowMapper;
import com.yuxuan.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.service.IUserService;
import com.yuxuan.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    //关注按钮，查看是否关注
    @Override
    public Result follow(Long followUserId, boolean isFollow) {
        //1、拿到登录的用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        //2、判断到底是关注还是取关
        if (isFollow) {
            //关注，增加数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                //把关注用户的id,放入redis的set集合，sadd userId followerUserId
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        } else {
            //取关，删除数据 delete from tb_follow where user_id = ? and follow_user_id = ?
            LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Follow::getUserId, userId);
            queryWrapper.eq(Follow::getFollowUserId, followUserId);
            boolean isSuccess =remove(queryWrapper);
            if (isSuccess) {
                //把关注用户的id从Redis集合移除
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        //1、拿到登录的用户
        Long userId = UserHolder.getUser().getId();
        //2、查询是否关注 select * from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();

        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        //1、获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;   //你
        String key2 = "follows:" + id;    //目标用户
        //2、求交集
        Set<String> intersectId = stringRedisTemplate.opsForSet().intersect(key, key2);
        if(intersectId == null || intersectId.isEmpty() ){
            return Result.ok(Collections.emptyList());
        }
        //3、解析id集合
        List<Long> ids = intersectId.stream().map(Long::valueOf).collect(Collectors.toList());
        //4、查询用户
        List<UserDTO> users = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }

    @Override
    public Result myfollow(Long followUserId) {
        //1、拿到登录的用户
        Long userId = UserHolder.getUser().getId();
        AuthorDTO authorDTO = new AuthorDTO();
        String key = "follows:" + userId;
        Boolean member = stringRedisTemplate.opsForSet().isMember(key, followUserId.toString());
        if (member == null || !member){
            //没关注过
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            follow.setCreateTime(LocalDateTime.now());
            boolean save = save(follow);
            authorDTO.setFollowed(true);
            if (save){
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        } else {
            //取关，删除数据 delete from tb_follow where user_id = ? and follow_user_id = ?
            LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Follow::getUserId, userId);
            queryWrapper.eq(Follow::getFollowUserId, followUserId);
            boolean isSuccess =remove(queryWrapper);
            authorDTO.setFollowed(false);
            if (isSuccess) {
                //把关注用户的id从Redis集合移除
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok(authorDTO);
    }


}
