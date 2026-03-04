package com.yuxuan.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuxuan.dto.Result;
import com.yuxuan.dto.UserDTO;
import com.yuxuan.entity.Blog;
import com.yuxuan.service.IBlogService;
import com.yuxuan.service.IUserService;
import com.yuxuan.utils.SystemConstants;
import com.yuxuan.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量 update tb_blog set liked = liked + 1 where id = ?
        return blogService.likeBlog(id);
    }

@GetMapping("/of/me")
public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
    // 获取登录用户
    UserDTO user = UserHolder.getUser();
    // 根据用户查询
    Page<Blog> page = blogService.query()
            .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
    // 获取当前页数据
    List<Blog> records = page.getRecords();
    return Result.ok(records);
}

    //查询热门Blog
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    //查寻bolg笔记
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }

    //根据用户id查询其主页详情
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/of/follow")
    public Result queryBlogOfFollw(@RequestParam("lastId") Long lastMin,@RequestParam(value = "offset",defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollw(lastMin,offset);
    }
}
