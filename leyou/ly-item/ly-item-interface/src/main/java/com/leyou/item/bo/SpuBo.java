package com.leyou.item.bo;

import com.leyou.item.pojo.Spu;
import lombok.Data;

/**
 * Spu的扩展类
 *  扩展了商品分类名称
 *  品牌名称
 */
@Data
public class SpuBo extends Spu {

    String cname;// 商品分类名称
    
    String bname;// 品牌名称
    
    // 略 。。
}