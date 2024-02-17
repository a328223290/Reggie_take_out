package com.itheima.reggie.utils;

import com.itheima.reggie.common.MailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮箱工具类
 */
@Slf4j
public class MailUtils {

//    public static void main(String[] args) {
//        try {
//            sendMail("gexinkai2017@163.com", "abc");
//        }catch (Exception e){
//            System.out.println("发生错误：" + e.toString());
//        }
//    }

    public static void sendMail(String revMailAddr, String code, MailProperties mailProperties) throws MessagingException {
        // 获取邮箱相关配置
        log.info("尝试发送邮件，当前配置信息: {}", mailProperties);

        // 创建Property类，用于记录邮件相关属性
        Properties props = new Properties();
        // 表示SMTP发送邮件，必须进行身份验证
        props.put("mail.smtp.auth", "true");
        //此处填写SMTP服务器
        props.put("mail.smtp.host", mailProperties.getSmtpServer());
        //端口号，QQ邮箱端口587
        props.put("mail.smtp.port", mailProperties.getSmtpPort());
        // 此处填写，写信人的账号
        props.put("mail.user", mailProperties.getUser());
        // 此处填写16位STMP口令
        props.put("mail.password", mailProperties.getAuthentication());

        // 构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String userName = props.getProperty("mail.user");
                String password = props.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };
        // 使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(props, authenticator);
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);
        // 设置发件人
        InternetAddress form = new InternetAddress(props.getProperty("mail.user"));
        message.setFrom(form);
        // 设置收件人的邮箱
        InternetAddress to = new InternetAddress(revMailAddr);
        message.setRecipient(Message.RecipientType.TO, to);
        // 设置邮件标题
        message.setSubject("Kai验证邮件");
        // 设置邮件的内容体
        message.setContent("尊敬的用户:<br>你好!注册验证码为:" + code + "(有效期为一分钟,请勿告知他人)", "text/html;charset=UTF-8");
        // 最后当然就是发送邮件啦
        Transport.send(message);

    }
}
