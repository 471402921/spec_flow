package com.specflow.api.modules.user.application;

import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.domain.repository.PetRepository;
import com.specflow.common.exception.BusinessException;
import com.specflow.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Pet 应用服务
 *
 * <p>职责：
 * - 定义业务用例（Use Cases）
 * - 声明事务边界
 * - 编排 Domain 对象和 Repository
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;

    // 宠物数量上限
    private static final int PET_LIMIT = 20;

    /**
     * 添加宠物
     *
     * @param ownerId   主人 ID
     * @param name      名字
     * @param species   种类
     * @param breed     品种
     * @param gender    性别
     * @param birthday  生日（可为 null）
     * @param avatarUrl 头像 URL（可为 null）
     * @return 创建的宠物
     */
    public Pet addPet(String ownerId, String name, Pet.Species species,
                      String breed, Pet.Gender gender, LocalDate birthday, String avatarUrl) {
        log.info("Adding pet for user: {}, name: {}", ownerId, name);

        // 检查宠物数量上限
        long currentCount = petRepository.countByOwnerId(ownerId);
        if (currentCount >= PET_LIMIT) {
            throw new BusinessException("PET_LIMIT_EXCEEDED", "已达宠物数量上限（最多20只）");
        }

        // 校验生日
        validateBirthday(birthday);

        // 创建宠物
        Pet pet = Pet.create(ownerId, name, species, breed, gender, birthday, avatarUrl);
        petRepository.save(pet);

        log.info("Pet created: id={}, name={}", pet.getId(), name);
        return pet;
    }

    /**
     * 检查是否有可恢复的已删除宠物
     *
     * @param ownerId 主人 ID
     * @param name    名字
     * @param species 种类
     * @return 可恢复的宠物列表
     */
    @Transactional(readOnly = true)
    public List<Pet> findRestorablePets(String ownerId, String name, Pet.Species species) {
        return petRepository.findDeletedByOwnerIdAndNameAndSpecies(ownerId, name, species);
    }

    /**
     * 恢复已删除的宠物
     *
     * @param ownerId 主人 ID
     * @param petId   宠物 ID
     * @return 恢复后的宠物
     */
    public Pet restorePet(String ownerId, String petId) {
        log.info("Restoring pet: {} for user: {}", petId, ownerId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new NotFoundException("宠物不存在"));

        // 验证主人身份
        if (!pet.belongsTo(ownerId)) {
            throw new BusinessException("PET_ACCESS_DENIED", "仅宠物主人可执行此操作");
        }

        // 验证是否已删除
        if (!pet.isDeleted()) {
            throw new BusinessException("PET_NOT_DELETED", "宠物未被删除");
        }

        // 检查恢复后是否超过上限
        long currentCount = petRepository.countByOwnerId(ownerId);
        if (currentCount >= PET_LIMIT) {
            throw new BusinessException("PET_LIMIT_EXCEEDED", "已达宠物数量上限（最多20只）");
        }

        pet.restore();
        petRepository.save(pet);

        log.info("Pet restored: {}", petId);
        return pet;
    }

    /**
     * 编辑宠物
     *
     * @param ownerId 主人 ID
     * @param petId   宠物 ID
     * @param cmd     更新命令
     * @return 更新后的宠物
     */
    public Pet updatePet(String ownerId, String petId, UpdatePetCommand cmd) {
        log.info("Updating pet: {} for user: {}", petId, ownerId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new NotFoundException("宠物不存在"));

        // 验证主人身份
        if (!pet.belongsTo(ownerId)) {
            throw new BusinessException("PET_ACCESS_DENIED", "仅宠物主人可执行此操作");
        }

        // 校验生日
        validateBirthday(cmd.birthday());

        pet.update(cmd.name(), cmd.species(), cmd.breed(),
                cmd.gender(), cmd.birthday(), cmd.avatarUrl());
        petRepository.save(pet);

        log.info("Pet updated: {}", petId);
        return pet;
    }

    /**
     * 编辑宠物命令
     */
    public record UpdatePetCommand(
            String name,
            Pet.Species species,
            String breed,
            Pet.Gender gender,
            LocalDate birthday,
            String avatarUrl
    ) {
    }

    /**
     * 删除宠物（软删除）
     *
     * @param ownerId 主人 ID
     * @param petId   宠物 ID
     */
    public void deletePet(String ownerId, String petId) {
        log.info("Deleting pet: {} for user: {}", petId, ownerId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new NotFoundException("宠物不存在"));

        // 验证主人身份
        if (!pet.belongsTo(ownerId)) {
            throw new BusinessException("PET_ACCESS_DENIED", "仅宠物主人可执行此操作");
        }

        pet.softDelete();
        petRepository.save(pet);

        log.info("Pet soft deleted: {}", petId);
    }

    /**
     * 获取宠物详情
     *
     * @param ownerId 主人 ID
     * @param petId   宠物 ID
     * @return 宠物
     */
    @Transactional(readOnly = true)
    public Pet getPet(String ownerId, String petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new NotFoundException("宠物不存在"));

        // 验证主人身份且未删除
        if (!pet.belongsTo(ownerId) || pet.isDeleted()) {
            throw new NotFoundException("宠物不存在");
        }

        return pet;
    }

    /**
     * 获取用户的宠物列表
     *
     * @param ownerId 主人 ID
     * @return 宠物列表
     */
    @Transactional(readOnly = true)
    public List<Pet> getPetsByOwner(String ownerId) {
        return petRepository.findByOwnerId(ownerId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验生日不能晚于今天（UTC）
     *
     * @param birthday 生日
     * @throws BusinessException 如果生日晚于今天
     */
    private void validateBirthday(LocalDate birthday) {
        if (birthday != null && birthday.isAfter(LocalDate.now(ZoneId.of("UTC")))) {
            throw new BusinessException("INVALID_BIRTHDAY", "生日不能晚于今天");
        }
    }
}
