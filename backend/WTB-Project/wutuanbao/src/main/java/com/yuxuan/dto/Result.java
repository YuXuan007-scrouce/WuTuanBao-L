package com.yuxuan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;
    // 1. 新增字段
    private Integer code;

    public static Result ok(){
        return new Result(true, null, null, null,200);
    }
    public static Result ok(Object data){
        return new Result(true, null, data, null,200);
    }
    public static Result ok(List<?> data, Long total){
        return new Result(true, null, data, total,200);
    }
    public static Result fail(String errorMsg){
        return new Result(false, errorMsg, null, null,500);
    }
    // --- 2. 新增一个专门用于秒杀（带错误码）的方法 ---

    public static Result fail(Integer code, String errorMsg){
        return new Result(false, errorMsg, null, null, code);
    }
}
