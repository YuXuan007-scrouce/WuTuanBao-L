package com.yuxuan.entity.vo;

import lombok.Data;

@Data
public class NoteActionVo {

    private boolean isliked;
    private Integer liked;
    private boolean iscollected;
    private Integer collection;
}
