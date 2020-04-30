package com.leyou.order.service;

import com.leyou.common.dto.CartDTO;
import com.leyou.order.client.GoodsClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private GoodsClient goodsClient;

    /**
     * 测试减库存
     */
    @Test
    @Transactional
    public void test(){
        List<CartDTO> cartDTOS = Arrays.asList(new CartDTO(2600242L, 2), new CartDTO(2600242L, 9998));
        goodsClient.decreaseStock(cartDTOS);
    }

}