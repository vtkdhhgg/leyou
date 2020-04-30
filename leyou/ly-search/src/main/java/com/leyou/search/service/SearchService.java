package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.UnmappedTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 导入数据
 */
@Service
@Slf4j
public class SearchService {

    @Autowired
    GoodsClient goodsClient;

    @Autowired
    BrandClient brandClient;

    @Autowired
    CategoryClient categoryClient;

    @Autowired
    SpecificationClient specClient;

    @Autowired
    GoodsRepository goodsRepository;

    @Autowired
    ElasticsearchTemplate template;

    /**
     * 将spu转变成Goods
     * @param spu
     * @return
     */
    public Goods buildGoods(Spu spu){

        // 搜索字段
        String all = this.getAll(spu);

        // 查询sku
        List<Sku> skus = goodsClient.querySkuBySpuId(spu.getId());
        if (CollectionUtils.isEmpty(skus)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        List<Map<String,Object>> skuList = this.getSkuList(skus);

        // 价格集合
        List<Long> priceList = skus.stream().map(Sku::getPrice).collect(Collectors.toList());

        // 获取所有可搜索的规格参数
        Map<String, Object> specs = this.getSpecs(spu);

        Goods goods = new Goods();
        goods.setId(spu.getId());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        goods.setAll(all);//  搜索字段，包含标题，分类，品牌，规格等
        goods.setPrice(priceList);//  所有SKU的价格集合
        goods.setSkus(JsonUtils.toString(skuList));//  所有sku的集合的Json格式
        goods.setSpecs(specs);// 所有的可搜索的规格参数

        return goods;
    }

    /**
     * 获取所有可搜索的规格参数
     * @return
     */
    private Map<String, Object> getSpecs(Spu spu){
        // 查询规格参数
        List<SpecParam> specParams = specClient.queryParamList(null, spu.getCid3(), true);
        if (CollectionUtils.isEmpty(specParams)) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        // 查询商品详情
        SpuDetail spuDetail = goodsClient.queryDetailById(spu.getId());
        // 获取通用规格参数
        Map<Long, String> genericSpec = JsonUtils.toMap(spuDetail.getGenericSpec(), Long.class, String.class);
        // 获取特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {});

        // 规格参数，key是规格参数的名字，值是规格参数的值
        Map<String, Object> specs = new HashMap<>();
        for (SpecParam param : specParams) {
            // 获取规格参数名称
            String key = param.getName();
            Object value = "";
            // 判断当前的规格参数是否是通用的
            if (param.getGeneric()) {
                 value = genericSpec.get(param.getId());
                 //判断当前规格参数是不是数值类型
                if (param.getNumeric()) {
                    // 处理成段
                     value = chooseSegment(value.toString(), param);
                }
            }else{
                value = specialSpec.get(param.getId());
            }
            //存入规格参数map中
            specs.put(key,value);
        }
        return specs;
    }

    /**
     * 将查询到的sku信息，截取主要信息
     * @param skus
     * @return
     */
    private List<Map<String, Object>> getSkuList(List<Sku> skus){
        List<Map<String, Object>> skuList = new ArrayList<>();
        for (Sku sku : skus) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            map.put("images", StringUtils.substringBefore(sku.getImages(), ","));
            skuList.add(map);
        }
        return skuList;
    }

    /**
     * 获取搜索字段
     * @param spu
     * @return
     */
    private String getAll(Spu spu){

        // 查询分类
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if (CollectionUtils.isEmpty(categories)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        // 获取每个分类的名字
        List<String> names = categories.stream().map(Category::getName).collect(Collectors.toList());
        // 查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());

        // 拼接并搜索字段
        return spu.getSubTitle() + " " + StringUtils.join(names, "," + " " + brand.getName());
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * 搜索查询
     * @param request
     * @return
     */
    public PageResult<Goods> search(SearchRequest request) {
        String key = request.getKey();
        // 判断是否有搜索条件，如果没有就直接返回null，不允许搜索全部商品
        if (StringUtils.isBlank(key)) {
            return null;
        }

        int page = request.getPage() - 1;// 当前页 : 因为elasticsearch的分页查询是从0开始的，而SearchRequest中page，默认值为1
        Integer size = request.getSize();// 当前页数据量

        // 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 结果过滤: 过滤掉那些用于搜索的字段
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 1.分页
        queryBuilder.withPageable(PageRequest.of(page, size));
        // 2.搜索条件
        QueryBuilder basicQuery = buildBasicQuery(request);
        queryBuilder.withQuery(basicQuery);

        // 3.聚合品牌和分类
        // 3.1聚合分类
        String CategoryAggName = "category_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(CategoryAggName).field("cid3"));
        // 3.2集合品牌
        String BrandAggName = "brand_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(BrandAggName).field("brandId"));

        // 4.查询
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 5.解析结果
        // 5.1解析分页结果
        long total = result.getTotalElements();// 总数据条数
        long totalPage = result.getTotalPages();// 总页数
        List<Goods> goodsList = result.getContent();// 查询到的数据
        // 5.2解析聚合结果
        Aggregations aggs = result.getAggregations();
        List<Category> categories = parseCategoryAgg(aggs.get(CategoryAggName));
        List<Brand> brands = parseBrandAgg(aggs.get(BrandAggName));

        // 6.规格参数的聚合
        List<Map<String, Object>> specs = null;
        if (categories != null && categories.size() ==1 ){
            //商品分类 存在 并且 数量为1, 就可以进行聚合规格参数
            specs = buildSpecificationAgg(categories.get(0).getId(), basicQuery);
        }
        return new SearchResult(total, totalPage, goodsList, categories, brands, specs);
    }

    private QueryBuilder buildBasicQuery(SearchRequest request) {
        // 创建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 查询条件
        queryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()));
        // 过滤条件
        Map<String, String> map = request.getFilter();
        for (Map.Entry<String, String> entry : map.entrySet()){
            String key = entry.getKey();
            // 处理key
            if (!"cid3".equals(key) && !"brandId".equals(key)){//是分类和品牌的过滤项就不进行处理
                key = "specs."+key+".keyword";  //对规格参数进行过滤
            }
            String value = entry.getValue();
            queryBuilder.filter(QueryBuilders.termQuery(key, value));
        }
        return queryBuilder;
    }

    /**
     * 聚合规格参数
     * @param cid
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> buildSpecificationAgg(Long cid, QueryBuilder basicQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        // 1.查询需要聚合的规格参数
        List<SpecParam> params = specClient.queryParamList(null, cid, true);
        // 2.聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 2.1带上查询条件
        queryBuilder.withQuery(basicQuery);
        // 2.2聚合规格参数
        for (SpecParam param : params) {
            String name = param.getName();
            queryBuilder.addAggregation(
                    AggregationBuilders.terms(name).field("specs."+name+".keyword"));
        }
        // 3.获取结果
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        // 4.解析结果
        Aggregations aggs = result.getAggregations();
        for (SpecParam param : params) {
            // 规格参数名
            String name = param.getName();
            StringTerms terms = aggs.get(name);

            // 准备map
            Map<String, Object> map = new HashMap<>();
            map.put("k", name);
            map.put("options", terms.getBuckets()
                    .stream().map(b ->b.getKeyAsString()).collect(Collectors.toList()));
            specs.add(map);
        }
        return specs;
    }

    private List<Brand> parseBrandAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets()
                    .stream().map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Brand> brands = brandClient.queryBrandByIds(ids);
            return brands;
        }catch (Exception e){
            log.error("[搜索服务]：查询品牌异常");
            return null;
        }
    }


    private List<Category> parseCategoryAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets()
                    .stream().map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Category> categories = categoryClient.queryCategoryByIds(ids);
            return categories;
        }catch (Exception e){
            log.error("[搜索服务]：查询分类异常", e);
            return null;
        }
    }

    /**
     * 根据spuId对索引库进行新增Or修改
     * @param spuId
     */
    public void createOrUpdateIndex(Long spuId) {
        //查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        //构建goods
        Goods goods = buildGoods(spu);
        //进行新增Or修改
        goodsRepository.save(goods);
    }

    /**
     * 根据spuId删除索引库中的数据
     * @param spuId
     */
    public void deleteIndex(Long spuId) {
        //删除索引库
        goodsRepository.deleteById(spuId);
    }
}
