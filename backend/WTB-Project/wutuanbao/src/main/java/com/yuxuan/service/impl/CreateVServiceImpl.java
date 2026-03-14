package com.yuxuan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.config.minio.MinioProperties;
import com.yuxuan.dto.NoteDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.Blog;
import com.yuxuan.entity.Follow;
import com.yuxuan.mapper.BlogMapper;
import com.yuxuan.service.CreateVService;
import com.yuxuan.service.IFollowService;
import com.yuxuan.utils.BloomFilterUtil;
import com.yuxuan.utils.UserHolder;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.yuxuan.utils.RedisConstants.FEED_FOLLOW_KEY;
import static com.yuxuan.utils.RedisConstants.FEED_RECOMMEND_KEY;

@Slf4j
@Service
public class CreateVServiceImpl extends ServiceImpl<BlogMapper, Blog> implements CreateVService {

   @Resource
   private MinioProperties minioProperties;
   @Resource
   private MinioClient minioClient;
   @Resource
   private BlogMapper blogMapper;
   @Resource
   private StringRedisTemplate stringRedisTemplate;
   @Resource
   private IFollowService followService;
    @Resource
    private BloomFilterUtil bloomFilterUtil;

    @Override
    public Result createNote(List<MultipartFile> images,MultipartFile video, NoteDTO noteDTO) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        Long userid = UserHolder.getUser().getId();

        //1、先判断bucket是否存在
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioProperties.getBucketName())
                .build());
        if (!bucketExists) {
              minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
              // 设置权限
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .config(createBucketPolicyConfig(minioProperties.getBucketName()))
                    .build());
        }
        ArrayList<String> imageUrls  = new ArrayList<>();
        String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
        //2、上传至minio，保存在一级当前日期下/对象名(uuid + 原始文件名称)
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                // 生成文件名：日期文件夹/UUID-原始文件名
                String filename = dateFolder + "/" + UUID.randomUUID() + "-" + image.getOriginalFilename();

                // 上传到 MinIO
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(filename)
                                .stream(image.getInputStream(), image.getSize(), -1)
                                .contentType(image.getContentType())
                                .build()
                );
                // 拼接完整 URL：http://localhost:9000/bucket-name/20250113/uuid-photo.jpg
                String imageUrl = minioProperties.getBucketName() + "/" + filename;
                imageUrls.add(imageUrl);
            }
            noteDTO.setImages(imageUrls.isEmpty() ? null : String.join(",", imageUrls));
        }
        // 如果上传的是视频
        String videoUrl = null;
        if (video != null && !video.isEmpty()) {
            String filename = dateFolder + "/" + UUID.randomUUID() + "-" + video.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(filename)
                            .stream(video.getInputStream(), video.getSize(), -1)
                            .contentType(video.getContentType())
                            .build()
            );
            videoUrl = minioProperties.getBucketName() + "/" + filename;
            noteDTO.setImages(videoUrl);
        }

        // 3. 业务校验（至少要有图片或视频）
        if (imageUrls.isEmpty() && videoUrl == null) {
            return Result.fail("请至少上传一张图片或一个视频");
        }

        //4、将 URL 列表转为逗号分隔的字符串
        noteDTO.setUserId(userid);
        noteDTO.setCreateTime(LocalDateTime.now());
        //5、写入数据库
        int note = blogMapper.createNote(noteDTO);
        if (note<=0){
            return Result.fail("上传失败！");
        }
        // 6. DB写入成功后，同步写入布隆过滤器
        try {
            bloomFilterUtil.addBlogId(noteDTO.getId());
        } catch (Exception e) {
            // 布隆过滤器写入失败不影响主流程，只告警
            // 兜底：下次项目重启会从DB重新全量加载
            log.warn("笔记ID写入布隆过滤器失败，blogId: {}", noteDTO.getId(), e);
        }
        //7、写入Redis(Feed流的写扩散，推送到粉丝的Redis邮箱)
        saveNote(userid,noteDTO.getId());
        return Result.ok("成功上传！");
    }

    //上面stream流的三个参数:第一个是获取输入流，第二个☞上传对象总大小，第三个☞分片大小传输，-1代表自动选择合适大小
    private String createBucketPolicyConfig(String bucketName) {
        return String.format(
                "{" +
                        "  \"Statement\" : [ {" +
                        "    \"Action\" : \"s3:GetObject\"," +
                        "    \"Effect\" : \"Allow\"," +
                        "    \"Principal\" : \"*\"," +
                        "    \"Resource\" : \"arn:aws:s3:::%s/*\"" +
                        "  } ]," +
                        "  \"Version\" : \"2012-10-17\"" +
                        "}",
                bucketName
        );
    }
    // 写扩散Feed推流
    private Boolean saveNote(Long id,Long blogId){
        // 写两份 一份写到 所有人可见 另一份写入 粉丝邮箱
        //3、查询笔记作者的所有粉丝 select * from tb_follow where follow_user_id = ?
        List<Follow> follows = followService.query().eq("follow_user_id",id).list();
        //4、推送笔记id给所有粉丝
        for (Follow follow : follows) {
            //4.1 获取粉丝id
            Long userId = follow.getUserId();
            //4.2 推送
            String key = FEED_FOLLOW_KEY + userId;   //推送到粉丝id为Key的Redis里，值为要发布的笔记id
            stringRedisTemplate.opsForZSet().add(key, blogId.toString(), System.currentTimeMillis());
        }
        stringRedisTemplate.opsForZSet().add(FEED_RECOMMEND_KEY,blogId.toString(),System.currentTimeMillis());
        return true;
    }
}
