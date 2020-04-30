package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface SpecificationAPI {
    /**
     * 查询参数集合
     * @param gid
     * @param cid
     * @param searching
     * @return
     */
    @GetMapping("spec/params")
    List<SpecParam> queryParamList(
            @RequestParam(name = "gid", required = false)Long gid,
            @RequestParam(name = "cid", required = false)Long cid,
            @RequestParam(name = "searching", required = false) Boolean searching
    );

    /**
     * 查询指定cid下的规格参数组及其组内的参数
     * @param cid
     * @return
     */
    @GetMapping("spec/group")
    List<SpecGroup> queryGroupByCid(@RequestParam("id") Long cid);
}
