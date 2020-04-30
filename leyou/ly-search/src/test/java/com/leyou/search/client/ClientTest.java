package com.leyou.search.client;

import com.leyou.item.pojo.Category;
import com.leyou.search.service.SearchService;
import com.netflix.discovery.converters.Auto;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ClientTest {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private SearchService searchService;

    @Test
    public void testCategoryClient(){
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(1L, 2L, 3L));
        Assert.assertEquals(3, categories.size());
        for (Category category : categories) {
            System.out.println("category = " + category);
        }
    }


    /**
     * 测试根据spuId删除索引
     */
    @Test
    public void testDeleteSku(){
        searchService.deleteIndex(195L);
    }

}
