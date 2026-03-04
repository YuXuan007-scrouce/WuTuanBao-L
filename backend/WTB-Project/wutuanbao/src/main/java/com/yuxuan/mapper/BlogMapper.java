package com.yuxuan.mapper;

import com.yuxuan.dto.BlogWithAuthorDTO;
import com.yuxuan.dto.NoteDTO;
import com.yuxuan.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface BlogMapper extends BaseMapper<Blog> {
    Integer queryLikedById(Long id);

    Integer queryCollectionById(Long id);

    List<BlogWithAuthorDTO> queryMyFollowBlog(List<Long> ids);

    int createNote(NoteDTO noteDTO);



}
