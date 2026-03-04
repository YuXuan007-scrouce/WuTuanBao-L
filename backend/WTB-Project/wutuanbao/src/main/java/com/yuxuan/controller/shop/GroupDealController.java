package com.yuxuan.controller.shop;

import com.yuxuan.dto.GroupDetailDTO;
import com.yuxuan.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchandise")
public class GroupDealController {

    @PostMapping
    public Result addGroupDeal(@RequestBody GroupDetailDTO groupDetailDTO) {
       return null;
    }
}
