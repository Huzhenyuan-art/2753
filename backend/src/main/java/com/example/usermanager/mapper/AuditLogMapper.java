package com.example.usermanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermanager.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
