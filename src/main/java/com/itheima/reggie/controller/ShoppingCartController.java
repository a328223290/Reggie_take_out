package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    // TODO: 这个之后要修改
    @GetMapping("/list")
    public R<List> list(){
        Long userId = BaseContext.getCurId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId).orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(queryWrapper);
        return R.success(shoppingCartList);
    }

    @PostMapping("/add")
    public R<ShoppingCart> save(@RequestBody ShoppingCart shoppingCart) {
        // 设置用户id，指定当前是哪个用户的购物车数据
        Long userId = BaseContext.getCurId();
        shoppingCart.setUserId(userId);
        shoppingCart.setNumber(1);
        shoppingCart.setCreateTime(LocalDateTime.now());
        // 查询当前菜品或套餐是否在购物车中
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, userId);
        shoppingCartLambdaQueryWrapper.eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId());
        shoppingCartLambdaQueryWrapper.eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        ShoppingCart curShoppingCartItem = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);

        // 如果已经存在，则在原来的数量基础上加一
        if(curShoppingCartItem != null){
            Integer curNum = curShoppingCartItem.getNumber();
            curShoppingCartItem.setNumber(curNum + 1);
            shoppingCartService.updateById(curShoppingCartItem);
            return R.success(curShoppingCartItem);
        }

        // 如果不存在，则添加到购物车，数量默认就是一
        shoppingCartService.save(shoppingCart);
        return R.success(shoppingCart);
    }

    @PostMapping("/sub")
    public R<ShoppingCart> subByOne(@RequestBody ShoppingCart shoppingCart) {
        Long userId = BaseContext.getCurId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        queryWrapper.eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId());
        queryWrapper.eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        ShoppingCart curShoppingCartItem = shoppingCartService.getOne(queryWrapper);
        if(curShoppingCartItem != null){
            Integer curNum = curShoppingCartItem.getNumber();
            if(curNum > 1){
                // 如果当前的number大于1，则更新number
                curShoppingCartItem.setNumber(curNum - 1);
                shoppingCartService.updateById(curShoppingCartItem);
            } else{
                // 否则直接删除
                curShoppingCartItem.setNumber(0);
                shoppingCartService.removeById(curShoppingCartItem.getId());
            }
            return R.success(curShoppingCartItem);
        } else throw new CustomException("菜品/套餐不存在");
    }
}
