package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CacheManager cacheManager;


    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page={}, pageSize={}, name={}", page, pageSize, name);

        Page<Setmeal> setmealpageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!StringUtils.isEmpty(name), Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        queryWrapper.eq(Setmeal::getIsDeleted, 0);
        setmealService.page(setmealpageInfo, queryWrapper);

        Page<SetmealDto> setmealDtoPageInfo = new Page<>();
        BeanUtils.copyProperties(setmealpageInfo, setmealDtoPageInfo, "records");

        List<SetmealDto> setmealDtoList = setmealpageInfo.getRecords().stream().map((item) -> {
            Category category = categoryService.getById(item.getCategoryId());
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            setmealDto.setCategoryName(category.getName());
            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPageInfo.setRecords(setmealDtoList);

        return R.success(setmealDtoPageInfo);
    }

    @CacheEvict(cacheNames = "setMealCache", key = "#setmealDto.categoryId + '_1'")
    @PostMapping
    public R<String> saveWithDish(@RequestBody SetmealDto setmealDto){
        setmealService.saveWithDish(setmealDto);
        return R.success("保存成功");
    }

    @GetMapping("/{id}")
    public R<SetmealDto> getByIdWithDish(@PathVariable Long id){
        log.info("getByIdWithDishes当前id: {}", id);

        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    @CacheEvict(cacheNames = "setMealCache", key = "#setmealDto.categoryId + '_1'")
    @PutMapping
    public R<String> updateWithDish(@RequestBody SetmealDto setmealDto){
        setmealService.updateWithDish(setmealDto);
        return R.success("更新成功");
    }

    @DeleteMapping
    public R<String> removeWithDish(String ids){
        List<Long> idList = new ArrayList<>();
        for(String id : ids.split(",")){
            idList.add(Long.valueOf(id));
        }
        setmealService.removeWithDish(idList);
        return R.success("删除成功");
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable int status, @RequestParam List<Long> ids){
        log.info("updateStatus, status: {}, ids: {}", status, ids);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);

        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);

        setmealService.update(setmeal, queryWrapper);
        return R.success("更新启售/停售状态成功");
    }

    @Cacheable(cacheNames = "setMealCache", key = "#setmeal.categoryId + '_1'")
    @GetMapping("/list")
    public R<List> list(Setmeal setmeal){
        log.info("setmeal list, categoryId: {}, status: {}", setmeal.getCategoryId(), setmeal.getStatus());
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        setmealLambdaQueryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        setmealLambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> setmealList = setmealService.list(setmealLambdaQueryWrapper);
        return R.success(setmealList);
    }

    @GetMapping("/dish/{id}")
    public R<List> getDish(@PathVariable Long id){
        LambdaQueryWrapper<SetmealDish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id).eq(SetmealDish::getIsDeleted, 0);
        dishLambdaQueryWrapper.orderByDesc(SetmealDish::getUpdateTime);
        List<SetmealDish> dishList = setmealDishService.list(dishLambdaQueryWrapper);

        return R.success(dishList);
    }
}
