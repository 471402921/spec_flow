package com.specflow.api.modules.user.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VerificationToken Mapper - MyBatis-Plus
 */
@Mapper
public interface VerificationTokenMapper extends BaseMapper<VerificationTokenDO> {

    /**
     * 根据令牌值查询
     *
     * @param token 令牌值
     * @return 令牌 DO
     */
    @Select("SELECT * FROM verification_tokens WHERE token = #{token} LIMIT 1")
    VerificationTokenDO selectByToken(@Param("token") String token);

    /**
     * 根据用户 ID 和类型查询最新的令牌
     *
     * @param userId 用户 ID
     * @param type   令牌类型
     * @return 最新的令牌 DO
     */
    @Select("SELECT * FROM verification_tokens WHERE user_id = #{userId} AND type = #{type} " +
            "ORDER BY created_at DESC LIMIT 1")
    VerificationTokenDO selectLatestByUserIdAndType(@Param("userId") String userId,
                                                    @Param("type") String type);

    /**
     * 根据用户 ID 和类型查询所有令牌
     *
     * @param userId 用户 ID
     * @param type   令牌类型
     * @return 令牌 DO 列表
     */
    @Select("SELECT * FROM verification_tokens WHERE user_id = #{userId} AND type = #{type} " +
            "ORDER BY created_at DESC")
    List<VerificationTokenDO> selectByUserIdAndType(@Param("userId") String userId,
                                                    @Param("type") String type);

    /**
     * 查询已过期且未使用的令牌
     *
     * @param now 当前时间
     * @return 过期令牌 DO 列表
     */
    @Select("SELECT * FROM verification_tokens WHERE expired_at < #{now} AND used = false")
    List<VerificationTokenDO> selectExpiredAndUnusedTokens(@Param("now") LocalDateTime now);
}
