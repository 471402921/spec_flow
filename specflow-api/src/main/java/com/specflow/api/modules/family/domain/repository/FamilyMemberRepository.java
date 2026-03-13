package com.specflow.api.modules.family.domain.repository;

import com.specflow.api.modules.family.domain.entity.FamilyMember;

import java.util.List;
import java.util.Optional;

/**
 * FamilyMember 仓库接口（Domain 层）
 *
 * <p>职责：
 * - 定义家庭成员关系的持久化操作
 * - 提供按家庭、按用户查询的能力
 */
public interface FamilyMemberRepository {

    /**
     * 根据 ID 查询成员关系
     *
     * @param id 成员关系 ID
     * @return 成员关系实体（可能为空）
     */
    Optional<FamilyMember> findById(String id);

    /**
     * 保存成员关系
     *
     * @param familyMember 成员关系实体
     * @return 保存后的实体
     */
    FamilyMember save(FamilyMember familyMember);

    /**
     * 根据家庭 ID 查询所有成员
     *
     * @param familyId 家庭 ID
     * @return 成员列表
     */
    List<FamilyMember> findAllByFamilyId(String familyId);

    /**
     * 根据用户 ID 查询所有家庭关系
     *
     * @param userId 用户 ID
     * @return 成员关系列表
     */
    List<FamilyMember> findAllByUserId(String userId);

    /**
     * 查询指定家庭中的指定用户
     *
     * @param familyId 家庭 ID
     * @param userId 用户 ID
     * @return 成员关系（可能为空）
     */
    Optional<FamilyMember> findByFamilyIdAndUserId(String familyId, String userId);

    /**
     * 统计家庭的成员数量
     *
     * @param familyId 家庭 ID
     * @return 成员数量
     */
    long countByFamilyId(String familyId);

    /**
     * 统计用户加入的家庭数量
     *
     * @param userId 用户 ID
     * @return 家庭数量
     */
    long countByUserId(String userId);

    /**
     * 删除成员关系
     *
     * @param id 成员关系 ID
     */
    void deleteById(String id);

    /**
     * 删除指定家庭中的所有成员（物理删除）
     *
     * @param familyId 家庭 ID
     */
    void deleteAllByFamilyId(String familyId);

    /**
     * 删除指定用户的所有家庭关系（用于注销账号）
     *
     * @param userId 用户 ID
     */
    void deleteAllByUserId(String userId);

    /**
     * 检查用户是否是指定家庭的成员
     *
     * @param familyId 家庭 ID
     * @param userId 用户 ID
     * @return true 表示是成员
     */
    boolean existsByFamilyIdAndUserId(String familyId, String userId);

    /**
     * 检查用户是否是任何家庭的主人
     *
     * @param userId 用户 ID
     * @return true 表示是至少一个家庭的主人
     */
    boolean existsByUserIdAndRoleOwner(String userId);

    /**
     * 查询用户作为主人的所有家庭
     *
     * @param userId 用户 ID
     * @return 成员关系列表
     */
    List<FamilyMember> findAllByUserIdAndRoleOwner(String userId);
}
