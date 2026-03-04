package com.yuxuan.service;

import com.yuxuan.dto.NoteActionDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollw(Long lastMin, Integer offset);

    Result queryBlogDetailById(Long blogId);


    Result likeOrCollection(NoteActionDTO noteActionDTO);

    Result queryMyFollwBlog(Long lastMin, Integer offset);
}
