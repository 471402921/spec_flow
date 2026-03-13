package com.specflow.api.modules.user.domain.repository;

import com.specflow.api.modules.user.domain.entity.Pet;

import java.util.List;
import java.util.Optional;

/**
 * Pet 领域仓库接口
 *
 * <p>职责：
 * - 定义 Pet 实体的持久化操作
 * - 实现由 Infrastructure 层提供
 */
public interface PetRepository {

    /**
     * 保存宠物
     *
     * @param pet 宠物实体
     */
    void save(Pet pet);

    /**
     * 根据 ID 查询宠物
     *
     * @param id 宠物 ID
     * @return 宠物实体（可能为空）
     */
    Optional<Pet> findById(String id);

    /**
     * 根据主人 ID 查询宠物列表（不含软删除的）
     *
     * @param ownerId 主人 ID
     * @return 宠物列表
     */
    List<Pet> findByOwnerId(String ownerId);

    /**
     * 查询用户的所有宠物（含软删除的）
     *
     * @param ownerId 主人 ID
     * @return 宠物列表
     */
    List<Pet> findAllByOwnerIdIncludingDeleted(String ownerId);

    /**
     * 根据主人 ID 和删除状态查询宠物列表
     *
     * @param ownerId 主人 ID
     * @param deleted 删除状态
     * @return 宠物列表
     */
    List<Pet> findByOwnerIdAndDeleted(String ownerId, boolean deleted);

    /**
     * 查询用户已删除的同名同种类宠物
     *
     * @param ownerId 主人 ID
     * @param name 宠物名字
     * @param species 宠物种类
     * @return 已删除的宠物列表
     */
    List<Pet> findDeletedByOwnerIdAndNameAndSpecies(String ownerId, String name, Pet.Species species);

    /**
     * 统计用户的宠物数量（不含软删除的）
     *
     * @param ownerId 主人 ID
     * @return 宠物数量
     */
    long countByOwnerId(String ownerId);

    /**
     * 统计用户的宠物数量（含软删除的）
     *
     * @param ownerId 主人 ID
     * @return 宠物数量
     */
    long countAllByOwnerId(String ownerId);
}
