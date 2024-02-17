package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page={}, pageSize={}, name={}", page, pageSize, name);

        // 1. 获取出Dish的分页信息
        Page<Dish> dishPageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!StringUtils.isEmpty(name), Dish::getName, name).eq(Dish::getIsDeleted, 0).orderByDesc(Dish::getUpdateTime);
        dishService.page(dishPageInfo, queryWrapper);

        // 2. 由于Dish本身不包含category_name字段，需要通过查询Category表获取出category_name的值
        // TODO：这里采用了循环查询，会有性能问题吗？

        // 这里需要一个新的Page而不是一开始就采用DishDto范型是因为查询的时候需要用到Dish model。
        Page<DishDto> dtoPageInfo = new Page<>();
        // 复制属性值
        BeanUtils.copyProperties(dishPageInfo, dtoPageInfo, "records");
        dtoPageInfo.setRecords(dishPageInfo.getRecords().stream().map((item) -> {
            // 创建一个新的dishDto对象
            DishDto dishDto = new DishDto();
            // 复制属性值
            BeanUtils.copyProperties(item, dishDto);
            // 查询出category_name并赋值给dishDto
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if(category != null) dishDto.setCategoryName(category.getName());
            return dishDto;
        }).collect(Collectors.toList()));

        // 3.将dtoPageInfo返回给客户端
        return R.success(dtoPageInfo);
    }

    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("保存菜品：{}", dishDto);

        dishService.saveWithFlavor(dishDto);
        return R.success("保存菜品成功");
    }

    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id) {
        return R.success(dishService.getByIdWithFlavor(id));
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        return R.success("更新菜品成功");
    }

    @DeleteMapping
    public R<String> removeByIds(String ids){
        log.info("删除菜品:{}", ids);
        List<Long> dishIds = new ArrayList<>();
        for(String id : ids.split(",")){
            dishIds.add(Long.valueOf(id));
        }
        dishService.removeByIdsWithFlavor(dishIds);
        return R.success("删除菜品成功");
    }
//
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable int status, @RequestParam String ids){
        log.info("更新状态，当前准备更新的状态:{}, 当前ids: {}", status, ids);
        List<Dish> dishList = new ArrayList<>();
        List<Long> dishIds = new ArrayList<>();
        for(String id : ids.split(",")){
            Long dishId = Long.valueOf(id);
            dishIds.add(dishId);

            Dish dishItem = new Dish();
            dishItem.setId(dishId);
            dishItem.setStatus(status);
            dishList.add(dishItem);
        }

        dishService.updateBatchById(dishList);
        return R.success("更新启售/停售状态成功");
    }

    @GetMapping("/list")
    public R<List> list(Long categoryId){
        log.info("当前categoryId: {}", categoryId);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId, categoryId).orderByDesc(Dish::getUpdateTime);
        List<Dish> dishList = dishService.list(queryWrapper);

        List<DishDto> dishDtoList = dishList.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, item.getId()).eq(DishFlavor::getIsDeleted, 0);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }

}
