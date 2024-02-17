package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.SetmealMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        // 1. 新建setmeal
        super.save(setmealDto);

        // 2. 新建setmeal_dish
        for(SetmealDish setmealDish : setmealDto.getSetmealDishes()){
            setmealDish.setSetmealId(setmealDto.getId());
        }
        setmealDishService.saveBatch(setmealDto.getSetmealDishes());
    }

    @Override
    @Transactional
    public SetmealDto getByIdWithDish(Long id) {
        Setmeal setmeal = super.getById(id);

        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id).eq(SetmealDish::getIsDeleted, 0);
        List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(setmealDishList);

        return setmealDto;
    }

    @Override
    @Transactional
    public void updateWithDish(SetmealDto setmealDto) {
        // 1. 更新setmeal
        super.updateById(setmealDto);

        // 2. 更新setmeal_dish
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId()).eq(SetmealDish::getIsDeleted, 0);
        List<SetmealDish> curSetmealDish = setmealDishService.list(setmealDishLambdaQueryWrapper);
        List<SetmealDish> newSetmealDish = setmealDto.getSetmealDishes();

        // 这里采用一个简单粗暴的方法，先删除现有的，然后加入新的
        curSetmealDish = curSetmealDish.stream().map((item) -> {
            item.setIsDeleted(1);
            return item;
        }).collect(Collectors.toList());
        setmealDishService.updateBatchById(curSetmealDish);

        newSetmealDish = newSetmealDish.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(newSetmealDish);
    }

    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        // 1. 检查是否存在未停售的setmeal，如果存在，直接返回错误
        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper<>();
        setmealQueryWrapper.in(Setmeal::getId, ids).eq(Setmeal::getStatus, 1);
        if(super.list(setmealQueryWrapper).size() > 0){
            throw new CustomException("套餐正在售卖，不能删除");
        }

        // 2. 删除setmeal表中的数据，软删除
        Setmeal setmeal = new Setmeal();
        setmeal.setIsDeleted(1);
        LambdaQueryWrapper<Setmeal> setmealQueryWrapperWithoutStatus = new LambdaQueryWrapper<>();
        setmealQueryWrapperWithoutStatus.in(Setmeal::getId, ids);
        super.update(setmeal, setmealQueryWrapperWithoutStatus);

        // 3. 删除setmeal_dish表中的数据
        LambdaQueryWrapper<SetmealDish> setmealDishQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishQueryWrapper.in(SetmealDish::getDishId, ids);
        List<SetmealDish> setmealDishToBeRemoved = new ArrayList<>();
        setmealDishService.updateBatchById(setmealDishToBeRemoved);
    }


}
