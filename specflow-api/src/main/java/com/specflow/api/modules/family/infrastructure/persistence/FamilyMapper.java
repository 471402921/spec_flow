package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Family Mapper
 *
 * <p>职责：
 * - 提供 families 表的 CRUD 操作
 * - 继承 BaseMapper 获得基础 CRUD 能力
 */
@Mapper
public interface FamilyMapper extends BaseMapper<FamilyDO> {
}
