package com.yuxuan.service;

import com.yuxuan.dto.NoteDTO;
import com.yuxuan.dto.Result;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface CreateVService {
    Result createNote(List<MultipartFile> images,MultipartFile video, NoteDTO noteDTO) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, IOException, NoSuchAlgorithmException, InvalidKeyException;
}
