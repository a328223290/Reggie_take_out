package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.MailProperties;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.service.impl.UserServiceImpl;
import com.itheima.reggie.utils.MailUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private MailProperties mailProperties;

    /**
     *  发送手机验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        log.info("sendMsg: {}", user);
        // 获取邮箱
        String email = user.getEmail();

        if(!StringUtils.isEmpty(email)){
            // 生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(6).toString();
            // TODO: 调用短信/邮箱服务，暂时先用log记录
            try{
                MailUtils.sendMail(email, code, mailProperties);
            } catch (Exception e){
                throw new CustomException("发送验证邮件失败，请再次发送。");
            }
            log.info("当前邮箱验证码为: {}", code);

            // 将生成的验证码存入session中
            session.setAttribute(email, code);

            return R.success("邮箱验证码短信发送成功");
        } else throw new CustomException("邮箱不能为空");
    }

    @PostMapping("/login")
    // 这里有一个值得学习的地方，我们之前总是采用dto来接受entity无法接受的数据，但其实每次都这样会比较麻烦，如果只是想接收数据而非进一步处理，可以考虑直接用一个Map接受，比较方便
    public R<String> login(@RequestBody Map map, HttpSession session){
        log.info("user login: {}", map);

        // 获取邮箱
        String email = map.get("email").toString();

        // 获取验证码
        String code = map.get("code").toString();

        // 从Session中获取保存的验证码
        Object codeInSession = session.getAttribute(email);

        // 进行验证码比对，如成功则登陆成功
        if(codeInSession != null && codeInSession.equals(code)) {
            // 判断当前邮箱是否为新用户，如果为新用户则自动注册
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getEmail, email).eq(User::getStatus, 1);
            User user = userService.getOne(queryWrapper);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user", user.getId());
            return R.success("登录成功");
        }
        throw new CustomException("验证码输入错误");
    }

    @PostMapping("/loginout")
    public R<String> logout(HttpSession session){
        log.info("user logout");
        // 删除session中的user邮箱数据
        session.removeAttribute("user");
        return R.success("退出成功");
    }

}
