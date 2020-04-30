package com.leyou.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.interceptor.UserInterceptor;
import com.leyou.pojo.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    //redis的存储数据的前缀名
    private static final String PREFIX_KEY = "cart:uid:";

    /**
     * 添加到购物车
     * @param cart
     */
    public void addCart(Cart cart) {
        // 获取登录的用户----从线程中获得
        UserInfo user = UserInterceptor.getUser();
        // 获取key
        String key = PREFIX_KEY + user.getId();
        // hashKey
        String hashKey = cart.getSkuId().toString();
        // 商品的数量
        Integer num = cart.getNum();

        // redis存储的结构是：Map<String, Map<String,String>>,第一个是用户的id，第二个map的key是商品id，value是cart
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        //判断当前商品是否存在与购物车中
        if (operations.hasKey(hashKey)) {
            // 如果存在,先取出cart
            String json = operations.get(hashKey).toString();
            cart = JsonUtils.toBean(json, Cart.class);
            // 添加商品数量
            cart.setNum(cart.getNum() + num);
        }
        //再次存入redis中
        operations.put(hashKey, JsonUtils.toString(cart));
    }

    /**
     * 获取购物车
     * @return
     */
    public List<Cart> queryCartList() {
        // 获取登录的用户----从线程中获得
        UserInfo user = UserInterceptor.getUser();
        // 获取key
        String key = PREFIX_KEY + user.getId();

        if (!redisTemplate.hasKey(key)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);

        List<Cart> cartList = operations.values().stream().map(o -> JsonUtils.toBean(o.toString(), Cart.class)).collect(Collectors.toList());

        return cartList;
    }

    /**
     * 修改商品数量
     * @param skuId
     * @param num
     */
    public void updateCartNum(Long skuId, Integer num) {
        // 获取登录的用户----从线程中获得
        UserInfo user = UserInterceptor.getUser();
        // 获取key
        String key = PREFIX_KEY + user.getId();
        // hashKey
        String hashKey = skuId.toString();
        //获取购物车
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);

        // 判断用户是否有购物车
        if (redisTemplate.hasKey(hashKey)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        //获取购物车中的数据
        Cart cart = JsonUtils.toBean(operations.get(hashKey).toString(), Cart.class);
        cart.setNum(num);

        //再写回redis中
        operations.put(hashKey, JsonUtils.toString(cart));


    }

    /**
     * 删除购物车商品
     * @param skuId
     */
    public void deleteCart(Long skuId) {
        // 获取登录的用户----从线程中获得
        UserInfo user = UserInterceptor.getUser();
        // 获取key
        String key = PREFIX_KEY + user.getId();
        // hashKey
        String hashKey = skuId.toString();

        //删除商品
        redisTemplate.opsForHash().delete(key, hashKey);

    }
}
