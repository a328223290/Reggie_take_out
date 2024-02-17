package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

/**
 * 文件上传与下载的控制器
 */
@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    // 将basePath记录在配置文件中，通过@Value注解注入值
    @Value("${reggie.path}")
    private String basePath;

    @PostMapping("/upload")
    /*
        MultipartFile 是 Spring Framework 中的一个接口，用于处理 HTTP 请求中的文件上传。
        它是 Spring 提供的一种方便的方式，用于在 Spring Web 应用程序中接收文件上传的数据。
     */
    public R<String> upload(MultipartFile file){
        // 经测试，file实际上是一个临时文件，当请求完成后会被自动清除，因此需要将这个临时文件被清除前转移到其他位置进行持久化
        log.info("文件上传：{}", file.toString());

        // 1. 获取原始文件名
        String fileName = file.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf('.'));

        // 2. 生成UUID，将UUID嵌入到保存的文件名中，以防止重名覆盖
        String finalFileName = UUID.randomUUID().toString() + suffix;

        // 3. 创建文件夹
        File dir = new File(basePath);
        // 如果不存在，则创建
        if(!dir.exists()) {
            dir.mkdirs();
        }

        // 4. 将临时文件转存到指定位置
        try {
            file.transferTo(new File(basePath + finalFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 5. 返回文件名称
        return R.success(finalFileName);
    }

    @GetMapping("/download")
    public void download(@RequestParam("name") String fileName, HttpServletResponse response){
        log.info("下载文件{}", fileName);
        try {
            // 1. 通过输入流读取文件内容
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + fileName));

            // 2. 通过输出流将内容输出到response中
            ServletOutputStream outputStream = response.getOutputStream();

            int len = 0;
            byte[] bytes = new byte[1024];
            // fileInputStream.read(bytes)这个是读取最多bytes.length这么大的数据量，并将其写入bytes数组中。当返回-1的时候表示读完了.
            // 这里循环是说一次读1KB的数据，一直到读完为止。
            while((len = fileInputStream.read(bytes)) != -1){
                outputStream.write(bytes, 0, len);
                // flush()可以将outputStream缓存中的数据强制发送到客户端，这样做可以避免缓存堆积太多数据。
                outputStream.flush();
            }

            // 3. 关闭资源
            fileInputStream.close();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
