package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.specflow.api.modules.family.domain.entity.FamilyInvitation;
import com.specflow.api.modules.family.domain.repository.FamilyInvitationRepository;
import com.specflow.api.modules.family.infrastructure.persistence.converter.FamilyInvitationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * FamilyInvitation 仓库实现（Infrastructure 层）
 *
 * <p>职责：
 * - 实现 FamilyInvitationRepository 接口
 * - 使用 MyBatis-Plus 进行数据库操作
 * - 负责 DO 与 Entity 之间的转换
 */
@Repository
@RequiredArgsConstructor
public class FamilyInvitationRepositoryImpl implements FamilyInvitationRepository {

    private final FamilyInvitationMapper familyInvitationMapper;

    @Override
    public Optional<FamilyInvitation> findById(@NonNull String id) {
        Objects.requireNonNull(id, "id must not be null");
        FamilyInvitationDO dataObject = familyInvitationMapper.selectById(id);
        return Optional.ofNullable(FamilyInvitationConverter.toDomain(dataObject));
    }

    @Override
    public FamilyInvitation save(@NonNull FamilyInvitation invitation) {
        Objects.requireNonNull(invitation, "invitation must not be null");
        FamilyInvitationDO dataObject = FamilyInvitationConverter.toDataObject(invitation);
        if (familyInvitationMapper.selectById(invitation.getId()) == null) {
            familyInvitationMapper.insert(dataObject);
        } else {
            familyInvitationMapper.updateById(dataObject);
        }
        return invitation;
    }

    @Override
    public Optional<FamilyInvitation> findByCode(@NonNull String code) {
        Objects.requireNonNull(code, "code must not be null");
        String normalizedCode = code.toUpperCase();
        FamilyInvitationDO dataObject = familyInvitationMapper.selectByCode(normalizedCode);
        return Optional.ofNullable(FamilyInvitationConverter.toDomain(dataObject));
    }

    @Override
    public List<FamilyInvitation> findAllByFamilyIdAndRevokedFalse(@NonNull String familyId) {
        Objects.requireNonNull(familyId, "familyId must not be null");
        LambdaQueryWrapper<FamilyInvitationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyInvitationDO::getFamilyId, familyId)
                .eq(FamilyInvitationDO::getRevoked, false);
        return familyInvitationMapper.selectList(wrapper).stream()
                .map(FamilyInvitationConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FamilyInvitation> findAllByFamilyId(@NonNull String familyId) {
        Objects.requireNonNull(familyId, "familyId must not be null");
        LambdaQueryWrapper<FamilyInvitationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyInvitationDO::getFamilyId, familyId);
        return familyInvitationMapper.selectList(wrapper).stream()
                .map(FamilyInvitationConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(@NonNull String id) {
        Objects.requireNonNull(id, "id must not be null");
        familyInvitationMapper.deleteById(id);
    }

    @Override
    public void deleteAllByFamilyId(@NonNull String familyId) {
        Objects.requireNonNull(familyId, "familyId must not be null");
        LambdaQueryWrapper<FamilyInvitationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyInvitationDO::getFamilyId, familyId);
        familyInvitationMapper.delete(wrapper);
    }

    @Override
    public boolean existsByCode(@NonNull String code) {
        Objects.requireNonNull(code, "code must not be null");
        return familyInvitationMapper.countByCode(code.toUpperCase()) > 0;
    }
}
