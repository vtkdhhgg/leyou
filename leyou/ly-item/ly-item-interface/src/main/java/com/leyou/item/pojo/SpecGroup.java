package com.leyou.item.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;

@Table(name = "tb_spec_group")
@Data
public class SpecGroup {

    @Id
    @KeySql(useGeneratedKeys = true)    //回显id属性
    private Long id;

    private Long cid;

    private String name;

    @Transient //这个注解表示：这个属性与实体类无关
    private List<SpecParam> params;//该组下的所有规格参数集合

   // getter和setter省略
}