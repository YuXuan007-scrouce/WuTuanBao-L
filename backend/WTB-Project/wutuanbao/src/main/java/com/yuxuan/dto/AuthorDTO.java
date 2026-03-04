package com.yuxuan.dto;

import com.yuxuan.entity.User;
import lombok.Data;

@Data
public class AuthorDTO {
    private Long id;
    private String nickName;
    private String icon;
    private boolean followed;

    public AuthorDTO() {}

    //用于笔记详细页面上部分的作者基本信息查询
    public AuthorDTO(User user) {
        this.id = user.getId();
        this.nickName = user.getNickName();
        this.icon = user.getIcon();
    }
}
