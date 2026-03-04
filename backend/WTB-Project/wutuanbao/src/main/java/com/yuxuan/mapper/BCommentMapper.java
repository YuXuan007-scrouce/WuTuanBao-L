package com.yuxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxuan.dto.BCommentDTO;
import com.yuxuan.entity.BComment;

import java.util.List;

public interface BCommentMapper extends BaseMapper<BComment> {

    List<BCommentDTO> queryComments(Long blogId);
}
