package com.specflow.api.modules.family.domain.repository;

import com.specflow.api.modules.family.domain.entity.Family;

import java.util.List;
import java.util.Optional;

/**
 * Family 仓库接口（Domain 层）
 *
 * <p>职责：
 * - 定义家庭实体的持久化操作
 * - 由 Infrastructure 层实现
 */
public interface FamilyRepository {

    /**
     * 根据 ID 查询家庭
     *
     * @param id 家庭 ID
     * @return 家庭实体（可能为空）
     */
    Optional<Family> findById(String id);

    /**
     * 保存家庭（新增或更新）
     *
     * @param family 家庭实体
     * @return 保存后的实体
     */
    Family save(Family family);

    /**
     * 删除家庭
     *
     * @param id 家庭 ID
     */
    void deleteById(String id);

    /**
     * 批量删除家庭（用于解散时的物理删除）
     *
     * @param ids 家庭 ID 列表
     */
    void deleteAllByIdInBatch(List<String> ids);
}
