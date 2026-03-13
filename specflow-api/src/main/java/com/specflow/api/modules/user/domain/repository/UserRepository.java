package com.specflow.api.modules.user.domain.repository;

import com.specflow.api.modules.user.domain.entity.User;

import java.util.Optional;

/**
 * User 领域仓库接口
 *
 * <p>职责：
 * - 定义 User 实体的持久化操作
 * - 实现由 Infrastructure 层提供
 */
public interface UserRepository {

    /**
     * 保存用户
     *
     * @param user 用户实体
     */
    void save(User user);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户实体（可能为空）
     */
    Optional<User> findById(String id);

    /**
     * 根据邮箱查询用户（不区分大小写）
     *
     * @param email 邮箱
     * @return 用户实体（可能为空）
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return true 如果已存在
     */
    boolean existsByEmail(String email);
}
