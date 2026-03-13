package com.specflow.api.modules.user.infrastructure.persistence;

import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.domain.repository.PetRepository;
import com.specflow.api.modules.user.infrastructure.persistence.converter.PetConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pet 仓库实现 - Infrastructure 层
 *
 * <p>职责：
 * - 实现 PetRepository 接口
 * - 使用 MyBatis-Plus Mapper 进行数据库操作
 * - 使用 Converter 进行 DO 与 Entity 的转换
 */
@Repository
@RequiredArgsConstructor
public class PetRepositoryImpl implements PetRepository {

    private final PetMapper petMapper;

    @Override
    public void save(Pet pet) {
        PetDO petDO = PetConverter.toDataObject(pet);
        // 使用 updateById 返回值判断记录是否存在，避免先查后写的竞态条件
        int affected = petMapper.updateById(petDO);
        if (affected == 0) {
            petMapper.insert(petDO);
        }
    }

    @Override
    public Optional<Pet> findById(String id) {
        PetDO petDO = petMapper.selectById(id);
        return Optional.ofNullable(PetConverter.toDomain(petDO));
    }

    @Override
    public List<Pet> findByOwnerId(String ownerId) {
        return petMapper.selectByOwnerIdAndDeleted(ownerId, false).stream()
                .map(PetConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Pet> findAllByOwnerIdIncludingDeleted(String ownerId) {
        return petMapper.selectAllByOwnerId(ownerId).stream()
                .map(PetConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Pet> findByOwnerIdAndDeleted(String ownerId, boolean deleted) {
        return petMapper.selectByOwnerIdAndDeleted(ownerId, deleted).stream()
                .map(PetConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Pet> findDeletedByOwnerIdAndNameAndSpecies(String ownerId, String name, Pet.Species species) {
        return petMapper.selectDeletedByOwnerIdAndNameAndSpecies(
                        ownerId, name, species.name()).stream()
                .map(PetConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByOwnerId(String ownerId) {
        return petMapper.countByOwnerId(ownerId);
    }

    @Override
    public long countAllByOwnerId(String ownerId) {
        return petMapper.countAllByOwnerId(ownerId);
    }
}
