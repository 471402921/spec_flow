package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * FamilyMember Mapper
 *
 * <p>职责：
 * - 提供 family_members 表的 CRUD 操作
 * - 继承 BaseMapper 获得基础 CRUD 能力
 * - 提供自定义查询方法
 */
@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMemberDO> {

    /**
     * 检查用户是否是任何家庭的主人
     *
     * @param userId 用户 ID
     * @return 1 表示是主人，0 表示不是
     */
    @Select("SELECT COUNT(*) FROM family_members WHERE user_id = #{userId} AND role = 'OWNER'")
    int countByUserIdAndRoleOwner(@Param("userId") String userId);

    /**
     * 查询用户作为主人的所有家庭关系
     *
     * @param userId 用户 ID
     * @return 成员关系列表
     */
    @Select("SELECT * FROM family_members WHERE user_id = #{userId} AND role = 'OWNER'")
    List<FamilyMemberDO> selectAllByUserIdAndRoleOwner(@Param("userId") String userId);
}
