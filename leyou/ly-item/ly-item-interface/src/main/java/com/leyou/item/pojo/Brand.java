package com.leyou.item.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "tb_brand")
@Data
public class Brand {
    @Id
    @KeySql(useGeneratedKeys=true)  //表示如果插入的表id以自增列为主键，则允许 JDBC 支持自动生成主键，并可将自动生成的主键id返回。
    private Long id;
    private String name;// 品牌名称
    private String image;// 品牌图片
    private Character letter;   //品牌名称首字母
}