package com.specflow.api.modules.user.interfaces.dto;

import com.specflow.api.modules.user.domain.entity.Pet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 宠物响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponse {

    private String id;
    private String ownerId;
    private String name;
    private Species species;
    private String breed;
    private Gender gender;
    private LocalDate birthday;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 从领域实体转换为响应
     */
    public static PetResponse fromDomain(Pet pet) {
        if (pet == null) {
            return null;
        }
        return PetResponse.builder()
                .id(pet.getId())
                .ownerId(pet.getOwnerId())
                .name(pet.getName())
                .species(pet.getSpecies() != null ? Species.valueOf(pet.getSpecies().name()) : null)
                .breed(pet.getBreed())
                .gender(pet.getGender() != null ? Gender.valueOf(pet.getGender().name()) : null)
                .birthday(pet.getBirthday())
                .avatarUrl(pet.getAvatarUrl())
                .createdAt(pet.getCreatedAt())
                .updatedAt(pet.getUpdatedAt())
                .build();
    }

    public enum Species {
        DOG, CAT
    }

    public enum Gender {
        MALE, FEMALE
    }
}
