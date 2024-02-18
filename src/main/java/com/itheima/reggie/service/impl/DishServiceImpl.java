package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Value("${reggie.path}")
    private String basePath;

    @Override
    // @Transactional注解用于标注一个方法应该被Spring的事务管理器所管理。
    // saveWithFlavor涉及多个插入操作，因此应该被注册为事务，一旦出错就回滚。
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        // 1. 保存菜品
        this.save(dishDto);

        // 2. 保存口味数据
        // 先给dishDto的flavors数据赋值dishId
        Long dishId = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors().stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        // 执行批量保存操作
        dishFlavorService.saveBatch(flavors);
    }

    public DishDto getByIdWithFlavor(Long id){
        // 需要查两张表：dish, dishFlavor
        DishDto dishDto = new DishDto();
        // 1. 查询dish中的数据
        Dish dish = super.getById(id);
        BeanUtils.copyProperties(dish, dishDto);

        // 2. 查询dishFlavor
        LambdaQueryWrapper<DishFlavor> dishFlavorQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorQueryWrapper.eq(DishFlavor::getDishId, id).eq(DishFlavor::getIsDeleted, 0);
        List<DishFlavor> list = dishFlavorService.list(dishFlavorQueryWrapper);
        if(list != null && list.size() > 0) dishDto.setFlavors(list);

        return dishDto;
    }

    // 这个函数我的逻辑和官方不一样，官方是先删除所有，再添加，而我是单独检测出需要删除的。
    @Transactional
    public void updateWithFlavor(DishDto dishDto){

        // 1. 更新菜品
        super.updateById(dishDto);

        // 2. 更新口味数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        List<DishFlavor> curDishFlavor = dishFlavorService.list(queryWrapper);
        List<DishFlavor> newDishFlavor = dishDto.getFlavors();

        // 因为这里有传DishFlavor的id进来，所以可以先比较newDishFlavor中是否包括所有的curDishFlavor，如果否，则把不在newDishFlavor中的加入删除列表中
        Set<Long> newDishFlavorIdSet = new HashSet<>();
        for(DishFlavor item : newDishFlavor){
            if(item.getId() != null){
                newDishFlavorIdSet.add(item.getId());
            }
            if(item.getDishId() == null){
                item.setDishId(dishDto.getId());
            }
        }
        List<DishFlavor> dishFlavorToBeRemoved = new ArrayList<>();
        for(DishFlavor item : curDishFlavor){
            if(!newDishFlavorIdSet.contains(item.getId())){
                item.setIsDeleted(1);
                dishFlavorToBeRemoved.add(item);
            }
        }
        // 找出需要删除的元素后，我们只需要对这些元素进行软删除
        dishFlavorService.updateBatchById(dishFlavorToBeRemoved);
        // 对新加入的元素执行saveOrUpdate操作即可。
        dishFlavorService.saveOrUpdateBatch(newDishFlavor);
    }

    @Transactional
    // 注意这里是软删除，所以只需要把is_delete变成1就好了，不需要真的删除
    public void removeByIdsWithFlavor(List<Long> ids){
        // TODO: 下面的注释部分为硬删除代码
//        // 1. 删除图片
//        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
//        dishQueryWrapper.in(Dish::getId, ids);
//        List<Dish> dishList = super.list(dishQueryWrapper);
//        for(Dish dish : dishList){
//            File pic = new File(basePath + dish.getImage());
//            log.info("删除图片: {}", basePath + dish.getImage());
//            if(pic.exists()){
//                pic.delete();
//            }
//        }

//        // 2. 删除菜品
//        super.removeByIds(ids);
//
//        // 3. 删除口味数据
//        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.in(DishFlavor::getDishId, ids);
//        dishFlavorService.remove(queryWrapper);

        // 1. 判断是否存在停售的菜品，如果存在则返回错误
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        dishQueryWrapper.in(Dish::getId, ids).eq(Dish::getStatus, 1);
        if(super.list(dishQueryWrapper).size() > 0){
            throw new CustomException("菜品正在售卖中，不能删除");
        }

        // 1. 删除菜品
        Dish dish = new Dish();
        dish.setIsDeleted(1);
        LambdaQueryWrapper<Dish> dishQueryWrapperWithoutStatus = new LambdaQueryWrapper<>();
        dishQueryWrapperWithoutStatus.in(Dish::getId, ids);
        super.update(dish, dishQueryWrapperWithoutStatus);

        // 2. 删除口味数据
        DishFlavor dishFlavor = new DishFlavor();
        dishFlavor.setIsDeleted(1);
        LambdaQueryWrapper<DishFlavor> dishFlavorQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorQueryWrapper.in(DishFlavor::getDishId, ids).eq(DishFlavor::getIsDeleted, 0);
        dishFlavorService.update(dishFlavor, dishFlavorQueryWrapper);

        // 3. 删除setmeal_dish数据
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getDishId, ids).eq(SetmealDish::getIsDeleted, 0);
        List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishLambdaQueryWrapper);
        setmealDishService.updateBatchById(setmealDishList.stream().map((item)->{
            item.setIsDeleted(1);
            return item;
        }).collect(Collectors.toList()));

    }
}
