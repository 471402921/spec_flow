package com.specflow.api.modules.family.domain.repository;

import com.specflow.api.modules.family.domain.entity.FamilyInvitation;

import java.util.List;
import java.util.Optional;

/**
 * FamilyInvitation 仓库接口（Domain 层）
 *
 * <p>职责：
 * - 定义家庭邀请码的持久化操作
 * - 提供按邀请码查询的能力
 */
public interface FamilyInvitationRepository {

    /**
     * 根据 ID 查询邀请码
     *
     * @param id 邀请码 ID
     * @return 邀请码实体（可能为空）
     */
    Optional<FamilyInvitation> findById(String id);

    /**
     * 保存邀请码
     *
     * @param invitation 邀请码实体
     * @return 保存后的实体
     */
    FamilyInvitation save(FamilyInvitation invitation);

    /**
     * 根据邀请码查询
     *
     * @param code 邀请码（大写）
     * @return 邀请码实体（可能为空）
     */
    Optional<FamilyInvitation> findByCode(String code);

    /**
     * 查询家庭的所有未撤销邀请码
     *
     * @param familyId 家庭 ID
     * @return 邀请码列表
     */
    List<FamilyInvitation> findAllByFamilyIdAndRevokedFalse(String familyId);

    /**
     * 查询家庭的所有邀请码
     *
     * @param familyId 家庭 ID
     * @return 邀请码列表
     */
    List<FamilyInvitation> findAllByFamilyId(String familyId);

    /**
     * 删除邀请码
     *
     * @param id 邀请码 ID
     */
    void deleteById(String id);

    /**
     * 删除指定家庭的所有邀请码（物理删除）
     *
     * @param familyId 家庭 ID
     */
    void deleteAllByFamilyId(String familyId);

    /**
     * 检查邀请码是否存在
     *
     * @param code 邀请码
     * @return true 表示存在
     */
    boolean existsByCode(String code);
}
