package com.yuxuan.controller.home;


import com.yuxuan.dto.Result;
import com.yuxuan.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/home")
public class HomeController {

    @Resource
    private HomeService homeService;

    @GetMapping("/topbar")
    public Result topbar(@RequestParam("lastId") Long lastMin, @RequestParam(value = "offset",defaultValue = "0") Integer offset,
                         @RequestParam(value = "type",defaultValue = "recommend") String type){
        return homeService.switchTab(lastMin,offset,type);
    }
}
