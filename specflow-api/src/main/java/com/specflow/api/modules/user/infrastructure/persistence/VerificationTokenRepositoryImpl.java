package com.specflow.api.modules.user.infrastructure.persistence;

import com.specflow.api.modules.user.domain.entity.VerificationToken;
import com.specflow.api.modules.user.domain.repository.VerificationTokenRepository;
import com.specflow.api.modules.user.infrastructure.persistence.converter.VerificationTokenConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * VerificationToken 仓库实现 - Infrastructure 层
 *
 * <p>职责：
 * - 实现 VerificationTokenRepository 接口
 * - 使用 MyBatis-Plus Mapper 进行数据库操作
 * - 使用 Converter 进行 DO 与 Entity 的转换
 */
@Repository
@RequiredArgsConstructor
public class VerificationTokenRepositoryImpl implements VerificationTokenRepository {

    private final VerificationTokenMapper verificationTokenMapper;

    @Override
    public VerificationToken save(VerificationToken token) {
        VerificationTokenDO tokenDO = VerificationTokenConverter.toDataObject(token);
        // 使用 updateById 返回值判断记录是否存在，避免先查后写的竞态条件
        int affected = verificationTokenMapper.updateById(tokenDO);
        if (affected == 0) {
            verificationTokenMapper.insert(tokenDO);
        }
        return VerificationTokenConverter.toDomain(tokenDO);
    }

    @Override
    public Optional<VerificationToken> findByToken(String token) {
        VerificationTokenDO tokenDO = verificationTokenMapper.selectByToken(token);
        return Optional.ofNullable(VerificationTokenConverter.toDomain(tokenDO));
    }

    @Override
    public Optional<VerificationToken> findLatestByUserIdAndType(String userId, VerificationToken.Type type) {
        VerificationTokenDO tokenDO = verificationTokenMapper.selectLatestByUserIdAndType(
                userId, type.name());
        return Optional.ofNullable(VerificationTokenConverter.toDomain(tokenDO));
    }

    @Override
    public List<VerificationToken> findByUserIdAndType(String userId, VerificationToken.Type type) {
        List<VerificationTokenDO> tokenDOs = verificationTokenMapper.selectByUserIdAndType(
                userId, type.name());
        return tokenDOs.stream()
                .map(VerificationTokenConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<VerificationToken> findExpiredAndUnusedTokens() {
        List<VerificationTokenDO> tokenDOs = verificationTokenMapper.selectExpiredAndUnusedTokens(
                LocalDateTime.now());
        return tokenDOs.stream()
                .map(VerificationTokenConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        verificationTokenMapper.deleteBatchIds(ids);
    }
}
