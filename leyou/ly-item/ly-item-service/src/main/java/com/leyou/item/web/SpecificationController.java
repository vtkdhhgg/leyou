package com.leyou.item.web;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    SpecificationService specService;


    /**
     * 根据分类id查询规格组
     * @param cid
     * @return
     */
    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> querySpecGroups(@PathVariable("cid")Long cid){
        return ResponseEntity.ok(specService.querySpecGroups(cid));
    }

    /**
     * 新增规格组
     * @param group
     * @return
     */
    @PostMapping("group")
    public ResponseEntity<Void> saveSpecGroup(@RequestBody SpecGroup group){
        specService.saveSpecGroup(group);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 更新规格组
     * @param group
     * @return
     */
    @PutMapping("group")
    public ResponseEntity<Void> updateSpecGroup(@RequestBody SpecGroup group){
        specService.updateSpecGroup(group);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据gid删除规格组
     * @param gid
     * @return
     */
    @DeleteMapping("group/{gid}")
    public ResponseEntity<Void> deleteSpecGroupByGid(@PathVariable(name = "gid")Long gid){
        specService.deleteSpecGroupByGid(gid);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * 查询参数集合
     * @param gid
     * @param cid
     * @param searching
     * @return
     */
    @GetMapping("params")
    public ResponseEntity<List<SpecParam>> queryParamList(
            @RequestParam(name = "gid", required = false)Long gid,
            @RequestParam(name = "cid", required = false)Long cid,
            @RequestParam(name = "searching", required = false) Boolean searching
            ){
        return ResponseEntity.ok(specService.querySpecParamByGid(gid, cid, searching));
    }

    /**
     * 新增规格参数
     * @param param
     * @return
     */
    @PostMapping("param")
    public ResponseEntity<Void> saveSpecParam(@RequestBody SpecParam param){
        specService.saveSpecParam(param);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 修改规格参数
     * @param param
     * @return
     */
    @PutMapping("param")
    public ResponseEntity<Void> updateSpecParam(@RequestBody SpecParam param){
        specService.updateSpecParam(param);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据pid，删除规格参数
     * @param pid
     * @return
     */
    @DeleteMapping("param/{pid}")
    public ResponseEntity<Void> deleteSpecParamByPid(@PathVariable(name = "pid")Long pid){
        specService.deleteSpecParamByPid(pid);
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    /**
     * 查询指定cid下的规格参数组及其组内的参数
     * @param cid
     * @return
     */
    @GetMapping("group")
    public ResponseEntity<List<SpecGroup>> queryGroupByCid(@RequestParam("id") Long cid){
      return ResponseEntity.ok(specService.queryGroupByCid(cid));
    }

}
