package com.specflow.api.modules.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Session Mapper - MyBatis-Plus 接口
 *
 * 继承 BaseMapper 提供基础 CRUD 操作
 * 如需复杂查询，可在此接口中定义自定义方法或使用 XML 映射
 */
@Mapper
public interface SessionMapper extends BaseMapper<SessionDO> {
}
