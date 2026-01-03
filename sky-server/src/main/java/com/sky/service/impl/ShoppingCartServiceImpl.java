package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        // 判断当前商品是否存在于购物车中
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart); // 将DTO对象拷贝到entity对象
        Long userId = BaseContext.getCurrentId(); // 获取当前用户id
        shoppingCart.setUserId(userId); // 设置用户id
        List<ShoppingCart> list = shoppingCartMapper.shoppingCartList(shoppingCart);// 查询购物车
        // 如果当前添加商品已存在于购物车中，数量+1，update
        if(list!=null && !list.isEmpty()){
            ShoppingCart cart = list.get(0); // 获取购物车对象
            cart.setNumber(cart.getNumber()+1);
            shoppingCartMapper.updateNumberById(cart);
        } else {
            // 如果如果当前添加商品不存在于购物车中，插入数据，insert
            // 判断本次添加到购物车的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                // 本次添加到是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                // 本次添加到是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().
                userId(userId).
                build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.shoppingCartList(shoppingCart);
        return shoppingCartList;
    }

    @Override
    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUesrId(userId);
    }

    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO){
        ShoppingCart shoppingCart = new ShoppingCart();
        // 判断当前商品是否存在于购物车中
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart); // 将DTO对象拷贝到entity对象
        // 查询当前登录用户的购物车数据
        shoppingCart.setId(BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.shoppingCartList(shoppingCart);
        // 如果购物车为空，不能删除数据
        if(shoppingCarts != null && !shoppingCarts.isEmpty()){
            shoppingCart = shoppingCarts.get(0); // 获取购物车商品对象
            // 获取购物车中该商品对象的数量
            Integer number = shoppingCart.getNumber();
            if(number == 1){
                shoppingCartMapper.deleteById(shoppingCart.getId());
            } else {
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }
}
