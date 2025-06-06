package com.sanshao.controller.sys;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sanshao.common.lang.Result;
import com.sanshao.entity.SysRole;
import com.sanshao.entity.SysRoleMenu;
import com.sanshao.entity.SysUserRole;
import com.sanshao.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色信息
 */
@RestController
@RequestMapping("/sys/role")
public class SysRoleController {

    @Autowired
    private HttpServletRequest request;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysRoleService sysRoleService;
    @Autowired
    private SysMenuService sysMenuService;
    @Autowired
    private SysUserRoleService sysUserRoleService;
    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    private Page getPage(){
        int current = ServletRequestUtils.getIntParameter(request,"current",1);
        int size = ServletRequestUtils.getIntParameter(request,"size",10);
        return new Page(current,size);
    }

    @GetMapping("list")
    @PreAuthorize("hasAuthority('sys:role:list') Or hasAuthority('sys:user:list')")
    public Result list(String name){
        Page<SysRole> pageData = sysRoleService.page(getPage(),new LambdaQueryWrapper<SysRole>().like(StrUtil.isNotBlank(name),SysRole::getName,name));
        return Result.ok().data("pageData",pageData);
    }

    @GetMapping("info/{id}")
    @PreAuthorize("hasAuthority('sys:role:list')")
    public Result info(@PathVariable("id") Long id){
        SysRole role = sysRoleService.getById(id);
        List<SysRoleMenu> menuList = sysRoleMenuService.list(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        List<Long> menuIds = menuList.stream().map(p -> p.getMenuId()).collect(Collectors.toList());
        role.setMenuIds(menuIds);
        return Result.ok().data("info",role);
    }

    @PostMapping("save")
    @PreAuthorize("hasAuthority('sys:role:save')")
    public Result save(@Validated @RequestBody SysRole sysRole){
        sysRole.setCreated(new Date());
        boolean flag = sysRoleService.save(sysRole);
        return flag ? Result.ok() : Result.error();
    }

    @PostMapping("update")
    @PreAuthorize("hasAuthority('sys:role:update')")
    public Result update(@Validated @RequestBody SysRole sysRole){
        sysRole.setUpdated(new Date());
        sysUserService.clearUserAuthorityInfoByRoleId(sysRole.getId());
        boolean flag = sysRoleService.updateById(sysRole);
        return flag ? Result.ok() : Result.error();
    }

    @PostMapping("delete")
    @PreAuthorize("hasAuthority('sys:role:delete')")
    public Result delete(@RequestBody Long[] roleIds){
        // 角色相对于用户和菜单，是不能直接删除的
        // 检查是否有用户关联和菜单关联
        boolean flag;
        try {
            flag = sysRoleService.removeByIds(Arrays.asList(roleIds));
        } catch (Exception e) {
            // 有关联关系，无法删除
            return Result.error().message("该角色下有用户，无法删除");
        }
        Arrays.stream(roleIds).forEach(id -> {
            sysUserService.clearUserAuthorityInfoByRoleId(id);
        });
        return flag ? Result.ok() : Result.error();
    }

    @PostMapping("perm/{roleId}")
    @PreAuthorize("hasAuthority('sys:role:perm')")
    @Transactional
    public Result perm(@PathVariable("roleId") Long roleId,@RequestBody Long[] menuIds){
        List<SysRoleMenu> sysRoleMenus = new ArrayList<>();
        Arrays.stream(menuIds).forEach(menuId -> {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setMenuId(menuId);
            roleMenu.setRoleId(roleId);
            sysRoleMenus.add(roleMenu);
        });
        sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId,roleId));
        sysRoleMenuService.saveBatch(sysRoleMenus);
        sysUserService.clearUserAuthorityInfoByRoleId(roleId);
        return Result.ok();
    }

}

