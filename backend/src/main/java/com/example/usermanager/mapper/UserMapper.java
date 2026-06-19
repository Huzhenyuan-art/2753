package com.example.usermanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermanager.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
