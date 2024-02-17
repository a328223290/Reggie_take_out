package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Employee;

// TODO: 这里原本有extend IService<Employee>，但是我个人觉得这个extend没什么意义，所以暂时去掉，之后如果出现bug可以再回来检查。
// 这里不能不继承IService，因为在Controller里面我们是用service接口去声明service变量，虽然springboot会自动将其初始化为对应的impl类，但是如果我们需要继承IService才能调用通用方法，否则通不过编译。
public interface EmployeeService extends IService<Employee>{

}
