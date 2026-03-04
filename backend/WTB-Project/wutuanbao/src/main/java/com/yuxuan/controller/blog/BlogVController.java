package com.yuxuan.controller.blog;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuxuan.dto.*;
import com.yuxuan.entity.Blog;
import com.yuxuan.service.*;
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
@RequestMapping("/blog2")
public class BlogVController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;
    @Resource
    private IUserInfoService userInfoService;
    @Resource
    private IFollowService followService;
    @Resource
    private IBlogCommentsService blogCommentsService;



    //“我的”页面点击“笔记”
   @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current,
                              @RequestParam(value = "size", defaultValue = "10") Integer size) {
    // 获取登录用户
    UserDTO user = UserHolder.getUser();
    // 根据用户查询
    Page<Blog> page = blogService.query()
            .eq("user_id", user.getId()).page(new Page<>(current, size));
    // 获取当前页数据
    List<Blog> records = page.getRecords();
    return Result.ok(records);
    }



    //笔记作品上部请求
    //查询该笔记作品的作者信息(可通用,个人主页详情)
    @GetMapping("/author/{authorId}")
    public Result queryAuthorById(@PathVariable("authorId") Long authorId){
        return userService.queryAuthorById(authorId);
    }
    // 点击关注按钮
    @GetMapping("/followed/{id}")
    public Result follow(@PathVariable("id") Long followUserId) {
        return followService.myfollow(followUserId);
    }
    // 进入“笔记”详情页面
    @GetMapping("/detail/{blogId}")
    public Result detail(@PathVariable("blogId") Long blogId) {
       return blogService.queryBlogDetailById(blogId);
    }
    //中间部分：查询评论区内容，战且不用分页
    @GetMapping("/comments/{blogId}")
    public Result comments(@PathVariable("blogId") Long blogId) {
       return blogCommentsService.queryComments(blogId);
    }
    //中间部分: 给某条评论点赞
    @PostMapping("/comments/{commentId}/like")
    public Result likeComment(
            @PathVariable Long commentId,
            @RequestBody LikeRequest req
    ) {
        return blogCommentsService.likeComment(commentId,req);
    }

     // 底部： 发送评论|回复某个人消息
     @PostMapping("/comments/write")
     public Result writeComment(@RequestBody BCommentLeve bCommentLeve){
      return blogCommentsService.writeComment(bCommentLeve);
     }

     // 底部: 给笔记点赞和收藏
    @PostMapping("/likecollection")
    public Result likeOrCollection(@RequestBody NoteActionDTO noteActionDTO){
      return blogService.likeOrCollection(noteActionDTO);
    }

    // 关注:查询笔记作品列表
    @GetMapping("/me/follow")
    public Result queryMyFollwBlog(@RequestParam("lastId") Long lastMin,@RequestParam(value = "offset",defaultValue = "0") Integer offset) {
        return blogService.queryMyFollwBlog(lastMin,offset);
    }

}
