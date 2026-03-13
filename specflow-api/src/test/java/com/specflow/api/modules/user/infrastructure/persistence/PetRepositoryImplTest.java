package com.specflow.api.modules.user.infrastructure.persistence;

import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.infrastructure.persistence.converter.PetConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PetRepositoryImpl 单元测试
 *
 * <p>测试策略：
 * - Mock PetMapper
 * - 测试 DO ↔ Entity 转换逻辑
 * - 验证查询条件构建（含软删除过滤）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PetRepositoryImpl 单元测试")
class PetRepositoryImplTest {

    @Mock
    private PetMapper petMapper;

    @InjectMocks
    private PetRepositoryImpl petRepository;

    private static final String TEST_PET_ID = "pet-123";
    private static final String TEST_OWNER_ID = "owner-456";

    // ==================== save() 测试 ====================

    @Test
    @DisplayName("save() - 新宠物应执行插入")
    void save_withNewPet_shouldInsert() {
        // Given
        Pet pet = createPet(TEST_PET_ID, "Buddy", false);

        when(petMapper.updateById(any(PetDO.class))).thenReturn(0);

        // When
        petRepository.save(pet);

        // Then
        verify(petMapper).updateById(any(PetDO.class));
        verify(petMapper).insert(any(PetDO.class));
    }

    @Test
    @DisplayName("save() - 已存在宠物应执行更新")
    void save_withExistingPet_shouldUpdate() {
        // Given
        Pet pet = createPet(TEST_PET_ID, "Buddy", false);

        when(petMapper.updateById(any(PetDO.class))).thenReturn(1);

        // When
        petRepository.save(pet);

        // Then
        verify(petMapper).updateById(any(PetDO.class));
    }

    // ==================== findById() 测试 ====================

    @Test
    @DisplayName("findById() - 宠物存在应返回宠物")
    void findById_withExistingPet_shouldReturnPet() {
        // Given
        PetDO petDO = createPetDO(TEST_PET_ID, "Buddy", false);
        when(petMapper.selectById(TEST_PET_ID)).thenReturn(petDO);

        // When
        Optional<Pet> result = petRepository.findById(TEST_PET_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TEST_PET_ID);
        assertThat(result.get().getName()).isEqualTo("Buddy");
    }

    @Test
    @DisplayName("findById() - 宠物不存在应返回空")
    void findById_withNonExistentPet_shouldReturnEmpty() {
        // Given
        when(petMapper.selectById("non-existent")).thenReturn(null);

        // When
        Optional<Pet> result = petRepository.findById("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findByOwnerId() 测试 ====================

    @Test
    @DisplayName("findByOwnerId() - 返回未删除的宠物")
    void findByOwnerId_shouldReturnNonDeletedPets() {
        // Given
        PetDO petDO1 = createPetDO("pet-1", "Buddy", false);
        PetDO petDO2 = createPetDO("pet-2", "Kitty", false);
        when(petMapper.selectByOwnerIdAndDeleted(TEST_OWNER_ID, false))
                .thenReturn(List.of(petDO1, petDO2));

        // When
        List<Pet> result = petRepository.findByOwnerId(TEST_OWNER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Buddy");
        assertThat(result.get(0).isDeleted()).isFalse();
    }

    @Test
    @DisplayName("findByOwnerId() - 无宠物时返回空列表")
    void findByOwnerId_noPets_shouldReturnEmptyList() {
        // Given
        when(petMapper.selectByOwnerIdAndDeleted(TEST_OWNER_ID, false))
                .thenReturn(Collections.emptyList());

        // When
        List<Pet> result = petRepository.findByOwnerId(TEST_OWNER_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findAllByOwnerIdIncludingDeleted() 测试 ====================

    @Test
    @DisplayName("findAllByOwnerIdIncludingDeleted() - 包含已删除的宠物")
    void findAllByOwnerIdIncludingDeleted_shouldIncludeDeletedPets() {
        // Given
        PetDO activePet = createPetDO("pet-1", "Buddy", false);
        PetDO deletedPet = createPetDO("pet-2", "Kitty", true);
        when(petMapper.selectAllByOwnerId(TEST_OWNER_ID))
                .thenReturn(List.of(activePet, deletedPet));

        // When
        List<Pet> result = petRepository.findAllByOwnerIdIncludingDeleted(TEST_OWNER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).isDeleted()).isFalse();
        assertThat(result.get(1).isDeleted()).isTrue();
    }

    // ==================== findByOwnerIdAndDeleted() 测试 ====================

    @Test
    @DisplayName("findByOwnerIdAndDeleted() - 查询已删除的宠物")
    void findByOwnerIdAndDeleted_withDeletedTrue_shouldReturnDeletedPets() {
        // Given
        PetDO deletedPet = createPetDO("pet-1", "Buddy", true);
        when(petMapper.selectByOwnerIdAndDeleted(TEST_OWNER_ID, true))
                .thenReturn(List.of(deletedPet));

        // When
        List<Pet> result = petRepository.findByOwnerIdAndDeleted(TEST_OWNER_ID, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isDeleted()).isTrue();
    }

    // ==================== findDeletedByOwnerIdAndNameAndSpecies() 测试 ====================

    @Test
    @DisplayName("findDeletedByOwnerIdAndNameAndSpecies() - 找到匹配的已删除宠物")
    void findDeletedByOwnerIdAndNameAndSpecies_withMatches_shouldReturnList() {
        // Given
        PetDO deletedPet = createPetDO("pet-1", "Buddy", true);
        deletedPet.setSpecies("DOG");
        when(petMapper.selectDeletedByOwnerIdAndNameAndSpecies(TEST_OWNER_ID, "Buddy", "DOG"))
                .thenReturn(List.of(deletedPet));

        // When
        List<Pet> result = petRepository.findDeletedByOwnerIdAndNameAndSpecies(
                TEST_OWNER_ID, "Buddy", Pet.Species.DOG);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Buddy");
        assertThat(result.get(0).isDeleted()).isTrue();
    }

    @Test
    @DisplayName("findDeletedByOwnerIdAndNameAndSpecies() - 无匹配时返回空列表")
    void findDeletedByOwnerIdAndNameAndSpecies_noMatches_shouldReturnEmptyList() {
        // Given
        when(petMapper.selectDeletedByOwnerIdAndNameAndSpecies(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<Pet> result = petRepository.findDeletedByOwnerIdAndNameAndSpecies(
                TEST_OWNER_ID, "NonExistent", Pet.Species.CAT);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== countByOwnerId() 测试 ====================

    @Test
    @DisplayName("countByOwnerId() - 统计未删除的宠物数量")
    void countByOwnerId_shouldReturnNonDeletedCount() {
        // Given
        when(petMapper.countByOwnerId(TEST_OWNER_ID)).thenReturn(5L);

        // When
        long result = petRepository.countByOwnerId(TEST_OWNER_ID);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    // ==================== countAllByOwnerId() 测试 ====================

    @Test
    @DisplayName("countAllByOwnerId() - 统计所有宠物数量（含已删除）")
    void countAllByOwnerId_shouldReturnTotalCount() {
        // Given
        when(petMapper.countAllByOwnerId(TEST_OWNER_ID)).thenReturn(10L);

        // When
        long result = petRepository.countAllByOwnerId(TEST_OWNER_ID);

        // Then
        assertThat(result).isEqualTo(10L);
    }

    // ==================== 辅助方法 ====================

    private Pet createPet(String id, String name, boolean deleted) {
        Pet pet = Pet.create(TEST_OWNER_ID, name, Pet.Species.DOG, "Breed", Pet.Gender.MALE, null, null);
        try {
            var idField = Pet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pet, id);
            if (deleted) {
                pet.softDelete();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return pet;
    }

    private PetDO createPetDO(String id, String name, boolean deleted) {
        PetDO petDO = new PetDO();
        petDO.setId(id);
        petDO.setOwnerId(TEST_OWNER_ID);
        petDO.setName(name);
        petDO.setSpecies("DOG");
        petDO.setBreed("Golden Retriever");
        petDO.setGender("MALE");
        petDO.setBirthday(LocalDate.of(2020, 1, 1));
        petDO.setDeleted(deleted);
        return petDO;
    }
}
