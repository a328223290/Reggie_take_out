package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;


    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {

        /**
         * login逻辑步骤：
         * 1. 对密码进行md5加密；
         * 2. 根据用户名查询数据库，判断用户是否存在；
         * 3. 比对密码是否一致；
         * 4. 查看员工是否处于禁用状态；
         * 5. 将用户存入session中并返回成功结果；
         */

        // 1. 对密码进行md5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 2. 根据用户名查询数据库，判断用户是否存在
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);
        if(emp == null){
            return R.error("用户不存在");
        }

        // 3. 比对密码是否一致
        if(!emp.getPassword().equals(password)){
            return R.error("登录失败");
        }

        // 4. 查看员工是否处于禁用状态
        if(emp.getStatus() == 0){
            return R.error("用户已禁用");
        }

        // 5. 将员工id存入session中并返回成功结果
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }

    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        // 将用户信息从session中删除，然后返回success消息
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {

        // 创建用户的时候需要给用户一个默认密码123456
        String password = DigestUtils.md5DigestAsHex("123456".getBytes());
        employee.setPassword(password);

        employeeService.save(employee);
        return R.success("创建成功");
    }

    // Get方法的参数名称和url中的参数名称相同的话，会直接映射。
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page={}, pageSize={}, name={}", page, pageSize, name);

        // 构造分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件：如果姓名不为空则加入name过滤条件，这里通过like函数做模糊匹配
        lambdaQueryWrapper.like(!StringUtils.isEmpty(name), Employee::getName, name);
        // 添加排序条件：按照更新时间倒叙
        lambdaQueryWrapper.orderByDesc(Employee::getUpdateTime);

        // MP已经封装好分页查询，只需要调用page函数，会直接将结果写入pageInfo中，因此只需要将pageInfo封装在响应类的data里即可
        employeeService.page(pageInfo, lambdaQueryWrapper);
        return R.success(pageInfo);
    }

    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee){
        // 获取员工id
        Long id = (Long)request.getSession().getAttribute("employee");
        // 需要注意的是js支持的整数的范围是-2^53 - 2^53，即从最小值-9007199254740992到最大值+9007199254740992之间的范围，而id超过了这个范围，因此直接使用js传过来的id会出现精度损失
        employeeService.updateById(employee);
        return R.success("员工信息修改成功！");
    }

    @GetMapping("/{id}")
    // @PathVariable用于获取路径参数。
    public R<Employee> getById(@PathVariable("id") Long id){
        Employee employee = employeeService.getById(id);
        if(employee == null){
            return R.error("员工不存在");
        }
        return R.success(employee);
    }

}
