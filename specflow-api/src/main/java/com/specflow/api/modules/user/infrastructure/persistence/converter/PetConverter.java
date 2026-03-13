package com.specflow.api.modules.user.infrastructure.persistence.converter;

import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.infrastructure.persistence.PetDO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Pet DO ↔ Entity 转换器
 *
 * <p>职责：
 * - 在 Infrastructure 层完成 DO 与 Entity 的双向转换
 * - 处理 LocalDateTime ↔ Instant 的时区转换
 * - 处理 String ↔ Enum 的类型转换
 * - 确保 Domain 层完全不知道 DO 的存在
 */
public class PetConverter {

    /**
     * DO 转 Domain Entity
     */
    public static Pet toDomain(PetDO petDO) {
        if (petDO == null) {
            return null;
        }
        return new Pet(
                petDO.getId(),
                petDO.getOwnerId(),
                petDO.getName(),
                toSpecies(petDO.getSpecies()),
                petDO.getBreed(),
                toGender(petDO.getGender()),
                petDO.getBirthday(),
                petDO.getAvatarUrl(),
                petDO.getDeleted() != null ? petDO.getDeleted() : false,
                toInstant(petDO.getDeletedAt()),
                toInstant(petDO.getCreatedAt()),
                toInstant(petDO.getUpdatedAt())
        );
    }

    /**
     * Domain Entity 转 DO
     */
    public static PetDO toDataObject(Pet pet) {
        if (pet == null) {
            return null;
        }
        return new PetDO(
                pet.getId(),
                pet.getOwnerId(),
                pet.getName(),
                pet.getSpecies() != null ? pet.getSpecies().name() : null,
                pet.getBreed(),
                pet.getGender() != null ? pet.getGender().name() : null,
                pet.getBirthday(),
                pet.getAvatarUrl(),
                pet.isDeleted(),
                toLocalDateTime(pet.getDeletedAt()),
                toLocalDateTime(pet.getCreatedAt()),
                toLocalDateTime(pet.getUpdatedAt())
        );
    }

    // ==================== 时间转换辅助方法 ====================

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }

    // ==================== 枚举转换辅助方法 ====================

    private static Pet.Species toSpecies(String species) {
        if (species == null) {
            return null;
        }
        try {
            return Pet.Species.valueOf(species);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Pet.Gender toGender(String gender) {
        if (gender == null) {
            return null;
        }
        try {
            return Pet.Gender.valueOf(gender);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
