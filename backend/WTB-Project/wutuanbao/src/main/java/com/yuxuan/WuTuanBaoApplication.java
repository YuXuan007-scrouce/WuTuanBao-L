package com.yuxuan;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//@EnableScheduling  // 开启定时任务
@EnableAspectJAutoProxy(exposeProxy = true)   //开启动态代理
@MapperScan("com.yuxuan.mapper")
@SpringBootApplication
public class WuTuanBaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WuTuanBaoApplication.class, args);
    }

    @Bean
    public RestHighLevelClient client(){
        return new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://localhost:9200")
        ));
    }
}
