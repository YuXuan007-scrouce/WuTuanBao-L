package com.yuxuan.controller.blog;


import com.yuxuan.dto.NoteDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.service.CreateVService;
import io.minio.errors.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.management.openmbean.InvalidKeyException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/note")
public class CreateVController {

    @Resource
    private CreateVService createVService;

    @PostMapping("/create")
    public Result createNote(
            @RequestPart(value = "images",required = false) List<MultipartFile> images,
            @RequestPart(value = "video", required = false) MultipartFile video,
            @RequestPart("data") NoteDTO noteDTO
    ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, java.security.InvalidKeyException {
        return createVService.createNote(images,video,noteDTO);
    }
}
