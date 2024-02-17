package com.itheima.reggie.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
// 需要通过@autowire才能将数据绑定上来
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private String suffix;
    private String smtpServer;
    private int smtpPort;
    private String authentication;
    private String user;
}
