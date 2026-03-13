package com.specflow.api.modules.user.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * User Mapper - MyBatis-Plus
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 根据邮箱查询用户（不区分大小写）
     *
     * @param email 邮箱
     * @return 用户 DO
     */
    @Select("SELECT * FROM users WHERE LOWER(email) = LOWER(#{email}) AND deleted = false LIMIT 1")
    UserDO selectByEmail(@Param("email") String email);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(#{email}) AND deleted = false")
    int countByEmail(@Param("email") String email);
}
