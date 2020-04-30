package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import com.netflix.discovery.converters.Auto;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 分页查询 spu
     * @param page
     * @param rows
     * @param saleable
     * @param key
     * @return
     */
    public PageResult querySpuPage(Integer page, Integer rows, Boolean saleable, String key) {

        // 分页
        PageHelper.startPage(page, rows);
        // 过滤
        Example example = new Example(Spu.class);
        // 搜索字段过滤
        Example.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        // 上下架过滤
        if (saleable != null){
            criteria.andEqualTo("saleable", saleable);
        }
        // 默认排序
        example.setOrderByClause("last_update_time DESC");
        // 查询
        List<Spu> spus = spuMapper.selectByExample(example);

        // 判断
        if (CollectionUtils.isEmpty(spus)) {
            // 商品不存在
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        // 解析分类和品牌的名称
        loadCategoryAndBrandName(spus);
        // 解析分页结果
        PageInfo<Spu> pageInfo = new PageInfo<>(spus);

        return new PageResult<>(pageInfo.getTotal(), spus);
    }

    /**
     * 解析分类和品牌名称
     * @param spus
     */
    private void loadCategoryAndBrandName(List<Spu> spus) {
        for (Spu spu : spus){
            // 处理分类名称
            List<String> names = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names, "/"));
            // 处理品牌名称
            spu.setBname(brandService.queryById(spu.getBrandId()).getName());
        }
    }

    /**
     * 新增商品
     * @param spu
     */
    @Transactional
    public void saveGoods(Spu spu) {
        // 新增spu
        spu.setId(null);
        spu.setSaleable(true);  //默认上架
        spu.setValid(false); //默认有效
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        int count = spuMapper.insert(spu);
        if (count != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }

        // 新增spuDetail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        spuDetailMapper.insert(spuDetail);

        //新增sku和库存
        saveSkuAndStock(spu);

        //发送mq消息:spuId
        amqpTemplate.convertAndSend("item.insert", spu.getId());
    }

    /**
     * 新增sku和库存
     * @param spu
     */
    private void saveSkuAndStock(Spu spu) {
        int count;//新建库存集合
        ArrayList<Stock> stockList = new ArrayList<Stock>();
        // 新增sku
        List<Sku> skus = spu.getSkus();
        for (Sku sku : skus) {
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            count = skuMapper.insert(sku);
            if (count != 1) {
                throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
            }

            // 设置stock属性
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            // 将设置好stock添加到集合中
            stockList.add(stock);
        }

        // 批量新增库存
        count = stockMapper.insertList(stockList);
        if (count != stockList.size()) {
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    /**
     * 根据SpuId查询商品详情SpuDetail
     * @param spuId
     * @return
     */
    public SpuDetail queryDetailById(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return spuDetail;
    }

    /**
     * 根据SPUId查询sku
     * @param spuId
     * @return
     */
    public List<Sku> querySkuBySpuId(Long spuId) {
        // 查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        // 获取要查询库存的skuIds
        List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());

        loadStock(ids, skuList);

        return skuList;
    }

    /**
     * 更新商品
     * @param spu
     */
    @Transactional
    public void updateGoods(Spu spu) {
        if (spu == null){
            throw new LyException(ExceptionEnum.GOODS_ID_CANNOT_BE_NULL);
        }

        //删除sku
        this.deleteSkuBySpuId(spu.getId());

        // 修改spu
        spu.setValid(null);
        spu.setSaleable(null);
        spu.setLastUpdateTime(new Date());
        spu.setCreateTime(null);

        int count = spuMapper.updateByPrimaryKeySelective(spu); // 根据主键更新属性不为null的值
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }

        // 修改Detail
        count = spuDetailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }

        // 新增spu和stock
        saveSkuAndStock(spu);

        //发送mq消息
        amqpTemplate.convertAndSend("item.update", spu.getId());
    }

    //判断是否有数据，如果有删除
    private void deleteSkuBySpuId(Long spuId){
        //先删除sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        // 查询sku，如果以前的存在，则删除
        List<Sku> skuList = skuMapper.select(sku);
        if (!CollectionUtils.isEmpty(skuList)){ // 是否有数据
            // 删除sku
            skuMapper.delete(sku);
            // 删除stock
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
    }

    /**
     * 根据spuId删除商品
     * @param spuId
     */
    public void deleteGoodsById(Long spuId){

        //先根据spuId删除sku
        this.deleteSkuBySpuId(spuId);

        //再删除spu
        spuMapper.deleteByPrimaryKey(spuId);

        //发送mq消息
        amqpTemplate.convertAndSend("item.delete", spuId);
    }

    /**
     * 根据spuId查询spu
     * @param id
     * @return
     */
    public Spu querySpuById(Long id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //查询商品详情
        SpuDetail detail = queryDetailById(spu.getId());
        spu.setSpuDetail(detail);
        //查询sku
        List<Sku> skus = querySkuBySpuId(spu.getId());
        spu.setSkus(skus);

        return spu;
    }

    /**
     * 根据skuIds查询sku
     * @param ids
     * @return
     */
    public List<Sku> querySkuBySkuIds(List<Long> ids) {
        //查询skuList
        List<Sku> skuList = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        loadStock(ids, skuList);
        return skuList;
    }

    //查询库存
    private void loadStock(List<Long> ids, List<Sku> skuList) {
        // 查询库存
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stockList)) {
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }
        // 我们把stock变成一个map，其key是：sku的id，值是库存值
        Map<Long, Integer> stockMap = stockList.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skuList.forEach(s -> s.setStock(stockMap.get(s.getId())));  //给skuList中所有的sku添加库存
    }

    //减少库存
    @Transactional
    public void decreaseStock(List<CartDTO> cartDTOS) {
        for (CartDTO cartDTO : cartDTOS) {
            int count = stockMapper.decreaseStock(cartDTO.getSkuId(), cartDTO.getNum());
            if (count != 1) {
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
