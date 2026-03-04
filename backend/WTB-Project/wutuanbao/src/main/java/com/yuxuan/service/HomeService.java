package com.yuxuan.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.Blog;

public interface HomeService extends IService<Blog> {

    Result switchTab(Long lastMin, Integer offset, String type);
}
