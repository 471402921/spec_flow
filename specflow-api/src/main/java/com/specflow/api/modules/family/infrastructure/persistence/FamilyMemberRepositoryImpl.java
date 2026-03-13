package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.specflow.api.modules.family.domain.entity.FamilyMember;
import com.specflow.api.modules.family.domain.repository.FamilyMemberRepository;
import com.specflow.api.modules.family.infrastructure.persistence.converter.FamilyMemberConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * FamilyMember 仓库实现（Infrastructure 层）
 *
 * <p>职责：
 * - 实现 FamilyMemberRepository 接口
 * - 使用 MyBatis-Plus 进行数据库操作
 * - 负责 DO 与 Entity 之间的转换
 */
@Repository
@RequiredArgsConstructor
public class FamilyMemberRepositoryImpl implements FamilyMemberRepository {

    private final FamilyMemberMapper familyMemberMapper;

    @Override
    public Optional<FamilyMember> findById(@NonNull String id) {
        Objects.requireNonNull(id, "id must not be null");
        FamilyMemberDO dataObject = familyMemberMapper.selectById(id);
        return Optional.ofNullable(FamilyMemberConverter.toDomain(dataObject));
    }

    @Override
    public FamilyMember save(@NonNull FamilyMember familyMember) {
        Objects.requireNonNull(familyMember, "familyMember must not be null");
        FamilyMemberDO dataObject = FamilyMemberConverter.toDataObject(familyMember);
        if (familyMemberMapper.selectById(familyMember.getId()) == null) {
            familyMemberMapper.insert(dataObject);
        } else {
            familyMemberMapper.updateById(dataObject);
        }
        return familyMember;
    }

    @Override
    public List<FamilyMember> findAllByFamilyId(@NonNull String familyId) {
        Objects.requireNonNull(familyId, "familyId must not be null");
        LambdaQueryWrapper<FamilyMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMemberDO::getFamilyId, familyId);
        return familyMemberMapper.selectList(wrapper).stream()
                .map(FamilyMemberConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FamilyMember> findAllByUserId(@NonNull String userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        LambdaQueryWrapper<FamilyMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMemberDO::getUserId, userId);
        return familyMemberMapper.selectList(wrapper).stream()
                .map(FamilyMemberConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FamilyMember> findByFamilyIdAndUserId(@NonNull String familyId, @NonNull String userId) {
        Objects.requireNonNull(familyId, "familyId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        LambdaQueryWrapper<FamilyMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMemberDO::getFamilyId, familyId)
                .eq(FamilyMemberDO::getUserId, userId);
        FamilyMemberDO dataObject = familyMemberMapper.selectOne(wrapper);
        return Optional.ofNullable(FamilyMemberConverter.toDomain(dataObject));
    }

    @Override
    public long countByFamilyId(@NonNull String familyId) {
        Objects.requireNonNull(familyId, "familyId must not be null");
        LambdaQueryWrapper<FamilyMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMemberDO::getFamilyId, familyId);
        return familyMemberMapper.selectCount(wrapper);
    }

    @Override
    public long countByUserId(@NonNull String userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        LambdaQueryWrapper<FamilyMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMemberDO::getUserId, userId);
        return familyMemberMapper.selectCount(wrapper);
    }

    @Override
    public void deleteById(@NonNull String id) {
        Objects.requireNonNull(id, "id must not be null");
        familyMemberMapper.deleteById(id);
    }

    @Override
    public void deleteAllByFamilyId(@NonNull String familyId) {
        Objects.requireNonNull(familyId, "familyId must not be null");
        LambdaQueryWrapper<FamilyMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMemberDO::getFamilyId, familyId);
        familyMemberMapper.delete(wrapper);
    }

    @Override
    public void deleteAllByUserId(@NonNull String userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        LambdaQueryWrapper<FamilyMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMemberDO::getUserId, userId);
        familyMemberMapper.delete(wrapper);
    }

    @Override
    public boolean existsByFamilyIdAndUserId(@NonNull String familyId, @NonNull String userId) {
        Objects.requireNonNull(familyId, "familyId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        LambdaQueryWrapper<FamilyMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMemberDO::getFamilyId, familyId)
                .eq(FamilyMemberDO::getUserId, userId);
        return familyMemberMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByUserIdAndRoleOwner(@NonNull String userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return familyMemberMapper.countByUserIdAndRoleOwner(userId) > 0;
    }

    @Override
    public List<FamilyMember> findAllByUserIdAndRoleOwner(@NonNull String userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return familyMemberMapper.selectAllByUserIdAndRoleOwner(userId).stream()
                .map(FamilyMemberConverter::toDomain)
                .collect(Collectors.toList());
    }
}
