package com.leadreach.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leadreach.user.entity.JoinRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * join_request 表在 MybatisPlusConfig.ignoreTable 中豁免租户插件
 * （写入方员工尚无租户上下文），租户条件由 Service 层显式携带
 */
@Mapper
public interface JoinRequestMapper extends BaseMapper<JoinRequest> {
}
