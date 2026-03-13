package com.specflow.api.modules.family.interfaces.dto;

import com.specflow.api.modules.user.domain.entity.Pet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 家庭宠物响应 DTO（含主人信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyPetResponse {

    /**
     * 宠物 ID
     */
    private String id;

    /**
     * 宠物名字
     */
    private String name;

    /**
     * 种类（DOG/CAT）
     */
    private String species;

    /**
     * 品种
     */
    private String breed;

    /**
     * 性别（MALE/FEMALE）
     */
    private String gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 主人 ID
     */
    private String ownerId;

    /**
     * 主人昵称
     */
    private String ownerNickname;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 从领域实体和主人信息转换
     */
    public static FamilyPetResponse fromDomain(Pet pet, String ownerId, String ownerNickname) {
        if (pet == null) {
            return null;
        }
        return new FamilyPetResponse(
                pet.getId(),
                pet.getName(),
                pet.getSpecies() != null ? pet.getSpecies().name() : null,
                pet.getBreed(),
                pet.getGender() != null ? pet.getGender().name() : null,
                pet.getBirthday(),
                pet.getAvatarUrl(),
                ownerId,
                ownerNickname,
                pet.getCreatedAt()
        );
    }
}
