package com.leyou.item.api;

import com.leyou.common.dto.CartDTO;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GoodsAPI {

    /**
     * 分页查询SPU
     *  SpuBo，是SPU的扩展类，方便展示
     * @param page
     * @param rows
     * @param saleable
     * @param key
     * @return
     */
    @GetMapping("spu/page")
    PageResult<Spu> querySpuByPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page, // 当前页的页码
            @RequestParam(value = "rows", defaultValue = "5") Integer rows, // 每页显示的条数
            @RequestParam(value = "saleable", required = false) Boolean saleable,// 商品是否上架
            @RequestParam(value = "key", required = false) String key // 要过滤的字
    );

    /**
     * 根据spuId删除spu
     * @param spuId
     * @return
     */
    @GetMapping("deleteSpu")
    ResponseEntity<Void> deleteGoodsById(@RequestParam("id") Long spuId);

    /**
     * 根据商品Id查询商品详情Detail
     * @param spuId
     * @return
     */
    @GetMapping("spu/detail/{id}")
    SpuDetail queryDetailById(@PathVariable("id") Long spuId);

    /**
     * 根据spu查询下面的所有sku
     * @param spuId
     * @return
     */
    @GetMapping("sku/list")
    List<Sku> querySkuBySpuId(@RequestParam(name = "id") Long spuId);

    /**
     * 根据skuIds查询的所有sku
     * @param ids
     * @return
     */
    @GetMapping("sku/list/ids")
    List<Sku> querySkuByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 根据spu的id查询spu
     * @param id
     * @return
     */
    @GetMapping("spu/{id}")
    Spu querySpuById(@PathVariable("id") Long id);

    /**
     * 减库存
     * @param cartDTOS
     */
    @PostMapping("stock/decrease")
    void decreaseStock(@RequestBody List<CartDTO> cartDTOS);


}
