package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * FamilyInvitation Mapper
 *
 * <p>职责：
 * - 提供 family_invitations 表的 CRUD 操作
 * - 继承 BaseMapper 获得基础 CRUD 能力
 * - 提供自定义查询方法
 */
@Mapper
public interface FamilyInvitationMapper extends BaseMapper<FamilyInvitationDO> {

    /**
     * 根据邀请码查询
     *
     * @param code 邀请码
     * @return 邀请码记录
     */
    @Select("SELECT * FROM family_invitations WHERE code = #{code}")
    FamilyInvitationDO selectByCode(@Param("code") String code);

    /**
     * 检查邀请码是否存在
     *
     * @param code 邀请码
     * @return 1 表示存在，0 表示不存在
     */
    @Select("SELECT COUNT(*) FROM family_invitations WHERE code = #{code}")
    int countByCode(@Param("code") String code);
}
