package com.specflow.api.modules.family.infrastructure.persistence;

import com.specflow.api.modules.family.domain.entity.Family;
import com.specflow.api.modules.family.domain.repository.FamilyRepository;
import com.specflow.api.modules.family.infrastructure.persistence.converter.FamilyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Family 仓库实现（Infrastructure 层）
 *
 * <p>职责：
 * - 实现 FamilyRepository 接口
 * - 使用 MyBatis-Plus 进行数据库操作
 * - 负责 DO 与 Entity 之间的转换
 */
@Repository
@RequiredArgsConstructor
public class FamilyRepositoryImpl implements FamilyRepository {

    private final FamilyMapper familyMapper;

    @Override
    public Optional<Family> findById(@NonNull String id) {
        Objects.requireNonNull(id, "id must not be null");
        FamilyDO dataObject = familyMapper.selectById(id);
        return Optional.ofNullable(FamilyConverter.toDomain(dataObject));
    }

    @Override
    public Family save(@NonNull Family family) {
        Objects.requireNonNull(family, "family must not be null");
        FamilyDO dataObject = FamilyConverter.toDataObject(family);
        if (familyMapper.selectById(family.getId()) == null) {
            familyMapper.insert(dataObject);
        } else {
            familyMapper.updateById(dataObject);
        }
        return family;
    }

    @Override
    public void deleteById(@NonNull String id) {
        Objects.requireNonNull(id, "id must not be null");
        familyMapper.deleteById(id);
    }

    @Override
    public void deleteAllByIdInBatch(@NonNull List<String> ids) {
        Objects.requireNonNull(ids, "ids must not be null");
        if (ids.isEmpty()) {
            return;
        }
        familyMapper.deleteBatchIds(ids);
    }
}
