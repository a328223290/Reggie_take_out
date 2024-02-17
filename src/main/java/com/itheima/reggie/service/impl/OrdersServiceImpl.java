package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrdersMapper;
import com.itheima.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private UserService userService;

    /**
     * 用户下单
     * @param order
     */
    @Override
    @Transactional
    public void submit(Orders order) {
        // 获得当前用户id
        Long userId = BaseContext.getCurId();

        // 查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        if (shoppingCartList == null || shoppingCartList.size() == 0){
            throw new CustomException("购物车为空，不能下单");
        }

        // 查询用户数据
        User user = userService.getById(userId);

        // 查询地址数据
        AddressBook addressBook = addressBookService.getById(order.getAddressBookId());
        if(addressBook == null){
            throw new CustomException("用户地址不存在，不能下单");
        }
        // 生成订单唯一标识符，这里提前生成是因为订单详情会需要用到，如果让数据库自动生成我们就没办法提前拿到这个id。
        // IdWorker.getId()可以生成一个长整形的唯一标识符。
        Long orderId = IdWorker.getId();


        // 计算订单价格，顺便补充订单详情数据
        // AtomicInteger可以保证多线程情况下计算不会错误。
        // TODO：为什么这里要采用AtomicInteger？为什么多线程的情况下会出现错误？
        AtomicInteger amount = new AtomicInteger(0);
        List<OrderDetail> orderDetailList = shoppingCartList.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            // 这里顺便计算一下订单总价格
            amount.getAndAdd(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;

        }).collect(Collectors.toList());

        // 向订单表插入数据，一条数据
        order.setId(orderId);
        order.setOrderTime(LocalDateTime.now());
        order.setCheckoutTime(LocalDateTime.now());
        order.setStatus(2);
        order.setUserId(userId);
        order.setNumber(String.valueOf(orderId));
        order.setUserName(user.getName());
        order.setPhone(addressBook.getPhone());
        order.setConsignee(addressBook.getConsignee());
        String address = ((addressBook.getProvinceName() == null) ? "" : addressBook.getProvinceCode())
                        + ((addressBook.getCityName() == null) ? "" : addressBook.getCityName())
                        + ((addressBook.getDistrictName() == null) ? "" : addressBook.getDistrictName())
                        + ((addressBook.getDetail() == null) ? "" : addressBook.getDetail());
        order.setAddress(address);
        order.setAddressBookId(order.getAddressBookId());
        order.setAmount(new BigDecimal(amount.get()));
        this.save(order);

        // 向订单详情表插入数据
        orderDetailService.saveBatch(orderDetailList);
        // 删除购物车数据
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);

        // TODO：支付功能

    }
}
