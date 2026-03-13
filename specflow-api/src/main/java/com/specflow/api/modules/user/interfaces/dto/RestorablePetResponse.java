package com.specflow.api.modules.user.interfaces.dto;

import com.specflow.api.modules.user.domain.entity.Pet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 可恢复宠物响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestorablePetResponse {

    private String id;
    private String name;
    private Species species;
    private String breed;
    private Instant deletedAt;

    /**
     * 从领域实体转换为响应
     */
    public static RestorablePetResponse fromDomain(Pet pet) {
        if (pet == null) {
            return null;
        }
        return RestorablePetResponse.builder()
                .id(pet.getId())
                .name(pet.getName())
                .species(pet.getSpecies() != null ? Species.valueOf(pet.getSpecies().name()) : null)
                .breed(pet.getBreed())
                .deletedAt(pet.getDeletedAt())
                .build();
    }

    /**
     * 转换列表
     */
    public static List<RestorablePetResponse> fromDomainList(List<Pet> pets) {
        return pets.stream()
                .map(RestorablePetResponse::fromDomain)
                .collect(Collectors.toList());
    }

    public enum Species {
        DOG, CAT
    }
}
