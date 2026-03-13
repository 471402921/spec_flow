package com.specflow.api.modules.user.domain.repository;

import com.specflow.api.modules.user.domain.entity.VerificationToken;

import java.util.List;
import java.util.Optional;

/**
 * VerificationToken 仓库接口（Domain 层）
 *
 * <p>职责：
 * - 定义 VerificationToken 的持久化操作契约
 * - 由 Infrastructure 层实现具体存储逻辑
 */
public interface VerificationTokenRepository {

    /**
     * 保存验证令牌
     *
     * @param token 令牌实体
     * @return 保存后的令牌实体
     */
    VerificationToken save(VerificationToken token);

    /**
     * 根据令牌值查找
     *
     * @param token 令牌值
     * @return 令牌实体（可能为空）
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * 根据用户 ID 和类型查找最新的令牌
     *
     * @param userId 用户 ID
     * @param type 令牌类型
     * @return 最新的令牌实体（可能为空）
     */
    Optional<VerificationToken> findLatestByUserIdAndType(String userId, VerificationToken.Type type);

    /**
     * 查找用户指定类型的所有令牌
     *
     * @param userId 用户 ID
     * @param type 令牌类型
     * @return 令牌列表
     */
    List<VerificationToken> findByUserIdAndType(String userId, VerificationToken.Type type);

    /**
     * 查找已过期且未使用的令牌
     *
     * @return 过期令牌列表
     */
    List<VerificationToken> findExpiredAndUnusedTokens();

    /**
     * 批量删除令牌
     *
     * @param ids 令牌 ID 列表
     */
    void deleteByIds(List<String> ids);
}
