package com.sanshao.controller.sys;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanshao.common.lang.Result;
import com.sanshao.entity.SysEquipment;
import com.sanshao.service.SysEquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;

/**
 * 器材信息以及操作
 */
@RestController
@RequestMapping("/sys/equipment")
public class SysEquipmentController {

    @Autowired
    private HttpServletRequest request;
    @Autowired
    private SysEquipmentService sysEquipmentService;

    private Page getPage(){
        int current = ServletRequestUtils.getIntParameter(request,"current",1);
        int size = ServletRequestUtils.getIntParameter(request,"size",10);
        return new Page(current,size);
    }

    @GetMapping("list")
    @PreAuthorize("hasAuthority('sys:equipment:list')")
    public Result list(String name){
        Page<SysEquipment> pageData = sysEquipmentService.page(getPage(),new LambdaQueryWrapper<SysEquipment>().like(StrUtil.isNotBlank(name), SysEquipment::getName,name));
        return Result.ok().data("pageData",pageData);
    }

    @PostMapping("add")
    @PreAuthorize("hasAuthority('sys:equipment:add')")
    public Result add(@Validated @RequestBody SysEquipment sysEquipment){
        sysEquipment.setCreated(new Date());
        sysEquipment.setSurplus(sysEquipment.getTotal());
        boolean flag = sysEquipmentService.save(sysEquipment);
        return flag ? Result.ok() : Result.error();
    }



    @PostMapping("update")
    @PreAuthorize("hasAuthority('sys:equipment:update')")
    public Result update(@Validated @RequestBody SysEquipment sysEquipment){
        boolean flag = sysEquipmentService.updateById(sysEquipment);
        return flag ? Result.ok() : Result.error();
    }

    @PostMapping("delete")
    @PreAuthorize("hasAuthority('sys:equipment:delete')")
    public Result delete(@RequestBody Long[] equipmentIds){
        boolean flag;
        try {
            flag = sysEquipmentService.removeByIds(Arrays.asList(equipmentIds));
        } catch (Exception e) {
            return Result.error().message("该器材仍有信息关联，无法删除");
        }
        return flag ? Result.ok() : Result.error();
    }



    @GetMapping("info/{id}")
    @PreAuthorize("hasAuthority('sys:equipment:list')")
    public Result info(@PathVariable("id") Long id){
        SysEquipment sysEquipment = sysEquipmentService.getById(id);
        return Result.ok().data("info", sysEquipment);
    }



}

