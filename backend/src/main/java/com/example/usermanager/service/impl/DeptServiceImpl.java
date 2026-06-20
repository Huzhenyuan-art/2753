package com.example.usermanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.usermanager.entity.Dept;
import com.example.usermanager.entity.UserDept;
import com.example.usermanager.mapper.DeptMapper;
import com.example.usermanager.mapper.UserDeptMapper;
import com.example.usermanager.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    @Autowired
    private UserDeptMapper userDeptMapper;

    @Override
    public List<Dept> listTree() {
        List<Dept> allDepts = this.list(new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getSortOrder));
        Map<Long, List<Dept>> parentMap = allDepts.stream()
                .filter(dept -> dept.getParentId() != null && dept.getParentId() != 0)
                .collect(Collectors.groupingBy(Dept::getParentId));

        List<Dept> rootDepts = allDepts.stream()
                .filter(dept -> dept.getParentId() == null || dept.getParentId() == 0)
                .collect(Collectors.toList());

        buildTree(rootDepts, parentMap);
        return rootDepts;
    }

    private void buildTree(List<Dept> depts, Map<Long, List<Dept>> parentMap) {
        for (Dept dept : depts) {
            List<Dept> children = parentMap.get(dept.getId());
            if (children != null && !children.isEmpty()) {
                dept.setChildren(children);
                buildTree(children, parentMap);
            }
        }
    }

    @Override
    public List<Dept> getDeptsByUserId(Long userId) {
        List<UserDept> userDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<UserDept>().eq(UserDept::getUserId, userId));
        if (userDepts == null || userDepts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> deptIds = userDepts.stream().map(UserDept::getDeptId).collect(Collectors.toList());
        return this.listByIds(deptIds);
    }

    @Override
    @Transactional
    public void assignDepts(Long userId, List<Long> deptIds) {
        userDeptMapper.delete(new LambdaQueryWrapper<UserDept>().eq(UserDept::getUserId, userId));
        if (deptIds != null && !deptIds.isEmpty()) {
            for (Long deptId : deptIds) {
                UserDept userDept = new UserDept();
                userDept.setUserId(userId);
                userDept.setDeptId(deptId);
                userDeptMapper.insert(userDept);
            }
        }
    }

    @Override
    public List<Long> getUserDeptIds(Long userId) {
        List<UserDept> userDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<UserDept>().eq(UserDept::getUserId, userId));
        if (userDepts == null || userDepts.isEmpty()) {
            return new ArrayList<>();
        }
        return userDepts.stream().map(UserDept::getDeptId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getChildDeptIds(Long deptId) {
        List<Long> result = new ArrayList<>();
        result.add(deptId);
        collectChildDeptIds(deptId, result);
        return result;
    }

    private void collectChildDeptIds(Long parentId, List<Long> result) {
        List<Dept> children = this.list(new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, parentId));
        if (children != null && !children.isEmpty()) {
            for (Dept child : children) {
                result.add(child.getId());
                collectChildDeptIds(child.getId(), result);
            }
        }
    }
}
