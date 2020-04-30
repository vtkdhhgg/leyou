package com.leyou.item.web;

import com.leyou.common.dto.CartDTO;
import com.leyou.item.service.GoodsService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GoodsControllerTest {
    @Autowired
    private GoodsService goodsService;

    @org.junit.Test
    public void decreaseStock() {
        List<CartDTO> cartDTOS = Arrays.asList(new CartDTO(2600242L, 2), new CartDTO(2600248L, 9998));
        goodsService.decreaseStock(cartDTOS);
    }
}