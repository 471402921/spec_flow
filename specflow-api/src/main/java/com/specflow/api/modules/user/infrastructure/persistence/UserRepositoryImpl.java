package com.specflow.api.modules.user.infrastructure.persistence;

import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.domain.repository.UserRepository;
import com.specflow.api.modules.user.infrastructure.persistence.converter.UserConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 仓库实现 - Infrastructure 层
 *
 * <p>职责：
 * - 实现 UserRepository 接口
 * - 使用 MyBatis-Plus Mapper 进行数据库操作
 * - 使用 Converter 进行 DO 与 Entity 的转换
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public void save(User user) {
        UserDO userDO = UserConverter.toDataObject(user);
        // 使用 updateById 返回值判断记录是否存在，避免先查后写的竞态条件
        int affected = userMapper.updateById(userDO);
        if (affected == 0) {
            userMapper.insert(userDO);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        UserDO userDO = userMapper.selectById(id);
        return Optional.ofNullable(UserConverter.toDomain(userDO));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        UserDO userDO = userMapper.selectByEmail(email);
        return Optional.ofNullable(UserConverter.toDomain(userDO));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.countByEmail(email) > 0;
    }
}
