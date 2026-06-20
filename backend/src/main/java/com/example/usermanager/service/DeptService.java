package com.example.usermanager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usermanager.entity.Dept;

import java.util.List;

public interface DeptService extends IService<Dept> {
    List<Dept> listTree();

    List<Dept> getDeptsByUserId(Long userId);

    void assignDepts(Long userId, List<Long> deptIds);

    List<Long> getUserDeptIds(Long userId);

    List<Long> getChildDeptIds(Long deptId);
}
