package com.leyou.search.repository;

import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Spu;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GoodsRepositoryTest {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    /**
     * 创建索引库
     */
    @Test
    public void testCreateIndex(){
        elasticsearchTemplate.createIndex(Goods.class);
        elasticsearchTemplate.putMapping(Goods.class);
    }

    /**
     * 加载数据
     */
    @Test
    public void loadData(){
        int page = 1;
        int rows = 100;
        int size = 0;   // 查询出的每页spu的数量
        do{
            //查询spu
            PageResult<Spu> result = goodsClient.querySpuByPage(page, rows, true, null);
            //构建成goods
            List<Spu> spuList = result.getItems();
            if (CollectionUtils.isEmpty(spuList)) {//没有数据了
                break;
            }

            List<Goods> goodsList = spuList.stream().map(searchService::buildGoods)
                    .collect(Collectors.toList());
            //存入索引库
            goodsRepository.saveAll(goodsList);

            // 翻页
            page ++;
            size = spuList.size();
        }while (size == 100);
    }
}