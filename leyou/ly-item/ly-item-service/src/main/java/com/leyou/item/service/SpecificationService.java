package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 根据分类查询规格参数组
     * @param cid
     * @return
     */
    public List<SpecGroup> querySpecGroups(Long cid) {

        // 查询条件
        SpecGroup group = new SpecGroup();
        group.setCid(cid);
        // 查询
        List<SpecGroup> list = specGroupMapper.select(group);
        if (CollectionUtils.isEmpty(list)){
            // 没有此分组
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return list;
    }

    public List<SpecParam> querySpecParamByGid(Long gid, Long cid, Boolean searching) {
        // 查询条件
        SpecParam param = new SpecParam();
        param.setGroupId(gid);
        param.setCid(cid);
        param.setSearching(searching);

        // 查询
        List<SpecParam> list = specParamMapper.select(param);
        // 判断
        if (CollectionUtils.isEmpty(list)) {
            // 没有此分组的参数信息
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        return list;
    }


    /**
     * 新增商品规格参数
     * @param group
     */
    @Transactional
    public void saveSpecGroup(SpecGroup group) {
        // 添加规格组
        group.setId(null);
        int count = specGroupMapper.insert(group);
        if (count != 1){
            // 新增失败
            throw new LyException(ExceptionEnum.SPEC_GROUP_SAVE_ERROR);
        }
    }

    /**
     * 修改商品规格参数
     * @param group
     */
    @Transactional
    public void updateSpecGroup(SpecGroup group) {
        // 更新规格组
        int count = specGroupMapper.updateByPrimaryKey(group);  // 根据主键，更新所有字段
        if (count != 1){
            // 更新失败
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
    }

    /**
     * 根据gid删除规格参数
     * @param gid
     */
    @Transactional
    public void deleteSpecGroupByGid(Long gid) {
        // 根据gid删除规格组
        int count = specGroupMapper.deleteByPrimaryKey(gid);    // 根据主键删除，
        if (count != 1){
            // 删除规格组失败
            throw new LyException(ExceptionEnum.SPEC_GROUP_DELETE_ERROR);
        }
    }

    @Transactional
    public void saveSpecParam(SpecParam param) {
        // 新增规格参数
        int count = specParamMapper.insert(param);
        if (count != 1){
            // 新增规格参数失败
            throw new LyException(ExceptionEnum.SPEC_PARAM_SAVE_ERROR);
        }
    }

    @Transactional
    public void updateSpecParam(SpecParam param) {
        // 修改规格参数
        int count = specParamMapper.updateByPrimaryKey(param);
        if (count != 1) {
            // 修改失败
            throw new LyException(ExceptionEnum.SPEC_PARAM_UPDATE_ERROR);
        }
    }

    @Transactional
    public void deleteSpecParamByPid(Long pid) {
        // 根据pid，删除规格参数
        int count = specParamMapper.deleteByPrimaryKey(pid);
        if (count != 1){
            // 删除失败
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
    }


    //查询分类下的规格参数组及其组内的参数
    public List<SpecGroup> queryGroupByCid(Long cid) {
        //查询规格参数组
        List<SpecGroup> specGroups = querySpecGroups(cid);
        //查询当前分类下的参数
        List<SpecParam> specParams = querySpecParamByGid(null, cid, null);
        //先将规格参数变成map，map的key是规格参数组的id，map的值是组内所有的参数
        Map<Long, List<SpecParam>> paramMap = new HashMap<>();
        for (SpecParam param : specParams) {
            if (!paramMap.containsKey(param.getId())){
                //说明这个规格参数组不存在
                paramMap.put(param.getId(), new ArrayList<>());
            }
            //将这个组内的所有参数，添加到map中
            paramMap.get(param.getId()).add(param);
        }
        //填充param到group
        for (SpecGroup specGroup : specGroups) {
            specGroup.setParams(paramMap.get(specGroup.getId()));
        }
        return specGroups;
    }
}
