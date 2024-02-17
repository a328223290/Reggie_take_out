package com.itheima.reggie.dto;

import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 这是Dish相关的DTO对象。
 * DTO即Data Transfer Object：数据传输对象，一般用于展示层和服务层之间的数据传输。
 * 这里使用到DTO是因为我们的请求中出现了无法通过entity类来表示的请求数据，所以用DTO类进行一个扩展。
 */
@Data
public class DishDto extends Dish {

    private List<DishFlavor> flavors = new ArrayList<>();

    private String categoryName;

    private Integer copies;
}
