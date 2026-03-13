package com.specflow.api.modules.user.application;

import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.domain.repository.PetRepository;
import com.specflow.common.exception.BusinessException;
import com.specflow.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PetService 单元测试
 *
 * <p>测试策略：
 * - Mock PetRepository
 * - 验证业务规则：宠物上限、主人权限、软删除/恢复
 * - 验证生日校验（UTC时区）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PetService 单元测试")
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private PetService petService;

    private static final String OWNER_ID = "owner-123";
    private static final String PET_ID = "pet-456";

    // ==================== addPet() 测试 ====================

    @Test
    @DisplayName("addPet() - 成功添加宠物")
    void addPet_withValidData_shouldCreatePet() {
        // Given
        when(petRepository.countByOwnerId(OWNER_ID)).thenReturn(5L); // 当前有5只

        // When
        Pet result = petService.addPet(
                OWNER_ID,
                "Buddy",
                Pet.Species.DOG,
                "Golden Retriever",
                Pet.Gender.MALE,
                LocalDate.of(2020, 1, 1),
                "https://example.com/pet.jpg"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(result.getName()).isEqualTo("Buddy");
        assertThat(result.getSpecies()).isEqualTo(Pet.Species.DOG);
        assertThat(result.getBreed()).isEqualTo("Golden Retriever");
        assertThat(result.getGender()).isEqualTo(Pet.Gender.MALE);
        assertThat(result.isDeleted()).isFalse();
        verify(petRepository).save(any(Pet.class));
    }

    @Test
    @DisplayName("addPet() - 达到宠物上限应抛出异常")
    void addPet_whenLimitReached_shouldThrowException() {
        // Given
        when(petRepository.countByOwnerId(OWNER_ID)).thenReturn(20L); // 已达上限

        // When & Then
        assertThatThrownBy(() -> petService.addPet(
                OWNER_ID, "Buddy", Pet.Species.DOG, "Golden", Pet.Gender.MALE, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PET_LIMIT_EXCEEDED");

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    @DisplayName("addPet() - 生日晚于今天应抛出异常")
    void addPet_withFutureBirthday_shouldThrowException() {
        // Given
        LocalDate futureDate = LocalDate.now(ZoneId.of("UTC")).plusDays(1);
        when(petRepository.countByOwnerId(OWNER_ID)).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> petService.addPet(
                OWNER_ID, "Buddy", Pet.Species.DOG, "Golden", Pet.Gender.MALE, futureDate, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_BIRTHDAY");

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    @DisplayName("addPet() - 生日为null应允许")
    void addPet_withNullBirthday_shouldCreatePet() {
        // Given
        when(petRepository.countByOwnerId(OWNER_ID)).thenReturn(5L);

        // When
        Pet result = petService.addPet(
                OWNER_ID, "Buddy", Pet.Species.CAT, "Persian", Pet.Gender.FEMALE, null, null);

        // Then
        assertThat(result.getBirthday()).isNull();
        verify(petRepository).save(any(Pet.class));
    }

    // ==================== findRestorablePets() 测试 ====================

    @Test
    @DisplayName("findRestorablePets() - 找到可恢复的宠物")
    void findRestorablePets_withMatches_shouldReturnList() {
        // Given
        Pet deletedPet = createDeletedPet("Buddy", Pet.Species.DOG);
        when(petRepository.findDeletedByOwnerIdAndNameAndSpecies(OWNER_ID, "Buddy", Pet.Species.DOG))
                .thenReturn(List.of(deletedPet));

        // When
        List<Pet> result = petService.findRestorablePets(OWNER_ID, "Buddy", Pet.Species.DOG);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Buddy");
    }

    @Test
    @DisplayName("findRestorablePets() - 无匹配时返回空列表")
    void findRestorablePets_noMatches_shouldReturnEmptyList() {
        // Given
        when(petRepository.findDeletedByOwnerIdAndNameAndSpecies(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<Pet> result = petService.findRestorablePets(OWNER_ID, "NonExistent", Pet.Species.CAT);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== restorePet() 测试 ====================

    @Test
    @DisplayName("restorePet() - 成功恢复已删除宠物")
    void restorePet_withValidData_shouldRestore() {
        // Given
        Pet deletedPet = createDeletedPet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(deletedPet, "id", PET_ID);
        ReflectionTestUtils.setField(deletedPet, "ownerId", OWNER_ID);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(deletedPet));
        when(petRepository.countByOwnerId(OWNER_ID)).thenReturn(5L);

        // When
        Pet result = petService.restorePet(OWNER_ID, PET_ID);

        // Then
        assertThat(result.isDeleted()).isFalse();
        assertThat(result.getDeletedAt()).isNull();
        verify(petRepository).save(deletedPet);
    }

    @Test
    @DisplayName("restorePet() - 宠物不存在应抛出异常")
    void restorePet_withNonExistentPet_shouldThrowException() {
        // Given
        when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> petService.restorePet(OWNER_ID, PET_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("宠物不存在");
    }

    @Test
    @DisplayName("restorePet() - 非主人操作应抛出异常")
    void restorePet_withDifferentOwner_shouldThrowException() {
        // Given
        Pet pet = createDeletedPet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", "different-owner");

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        // When & Then
        assertThatThrownBy(() -> petService.restorePet(OWNER_ID, PET_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PET_ACCESS_DENIED");
    }

    @Test
    @DisplayName("restorePet() - 未删除的宠物应抛出异常")
    void restorePet_withNonDeletedPet_shouldThrowException() {
        // Given
        Pet pet = createActivePet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", OWNER_ID);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        // When & Then
        assertThatThrownBy(() -> petService.restorePet(OWNER_ID, PET_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PET_NOT_DELETED");
    }

    @Test
    @DisplayName("restorePet() - 恢复后超过上限应抛出异常")
    void restorePet_whenLimitReached_shouldThrowException() {
        // Given
        Pet deletedPet = createDeletedPet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(deletedPet, "id", PET_ID);
        ReflectionTestUtils.setField(deletedPet, "ownerId", OWNER_ID);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(deletedPet));
        when(petRepository.countByOwnerId(OWNER_ID)).thenReturn(20L); // 已达上限

        // When & Then
        assertThatThrownBy(() -> petService.restorePet(OWNER_ID, PET_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PET_LIMIT_EXCEEDED");
    }

    // ==================== updatePet() 测试 ====================

    @Test
    @DisplayName("updatePet() - 成功更新宠物")
    void updatePet_withValidData_shouldUpdate() {
        // Given
        Pet pet = createActivePet("OldName", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", OWNER_ID);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        PetService.UpdatePetCommand cmd = new PetService.UpdatePetCommand(
                "NewName", Pet.Species.CAT, "NewBreed", Pet.Gender.FEMALE,
                LocalDate.of(2021, 1, 1), "https://new-avatar.jpg"
        );

        // When
        Pet result = petService.updatePet(OWNER_ID, PET_ID, cmd);

        // Then
        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getSpecies()).isEqualTo(Pet.Species.CAT);
        assertThat(result.getBreed()).isEqualTo("NewBreed");
        verify(petRepository).save(pet);
    }

    @Test
    @DisplayName("updatePet() - 宠物不存在应抛出异常")
    void updatePet_withNonExistentPet_shouldThrowException() {
        // Given
        when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

        PetService.UpdatePetCommand cmd = new PetService.UpdatePetCommand(
                "Name", Pet.Species.DOG, "Breed", Pet.Gender.MALE, null, null);

        // When & Then
        assertThatThrownBy(() -> petService.updatePet(OWNER_ID, PET_ID, cmd))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updatePet() - 非主人操作应抛出异常")
    void updatePet_withDifferentOwner_shouldThrowException() {
        // Given
        Pet pet = createActivePet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", "different-owner");

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        PetService.UpdatePetCommand cmd = new PetService.UpdatePetCommand(
                "NewName", Pet.Species.DOG, "Breed", Pet.Gender.MALE, null, null);

        // When & Then
        assertThatThrownBy(() -> petService.updatePet(OWNER_ID, PET_ID, cmd))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PET_ACCESS_DENIED");
    }

    @Test
    @DisplayName("updatePet() - 生日晚于今天应抛出异常")
    void updatePet_withFutureBirthday_shouldThrowException() {
        // Given
        Pet pet = createActivePet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", OWNER_ID);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        LocalDate futureDate = LocalDate.now(ZoneId.of("UTC")).plusDays(1);
        PetService.UpdatePetCommand cmd = new PetService.UpdatePetCommand(
                "Name", Pet.Species.DOG, "Breed", Pet.Gender.MALE, futureDate, null);

        // When & Then
        assertThatThrownBy(() -> petService.updatePet(OWNER_ID, PET_ID, cmd))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_BIRTHDAY");
    }

    // ==================== deletePet() 测试 ====================

    @Test
    @DisplayName("deletePet() - 成功软删除宠物")
    void deletePet_withValidData_shouldSoftDelete() {
        // Given
        Pet pet = createActivePet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", OWNER_ID);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        // When
        petService.deletePet(OWNER_ID, PET_ID);

        // Then
        assertThat(pet.isDeleted()).isTrue();
        assertThat(pet.getDeletedAt()).isNotNull();
        verify(petRepository).save(pet);
    }

    @Test
    @DisplayName("deletePet() - 宠物不存在应抛出异常")
    void deletePet_withNonExistentPet_shouldThrowException() {
        // Given
        when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> petService.deletePet(OWNER_ID, PET_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deletePet() - 非主人操作应抛出异常")
    void deletePet_withDifferentOwner_shouldThrowException() {
        // Given
        Pet pet = createActivePet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", "different-owner");

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        // When & Then
        assertThatThrownBy(() -> petService.deletePet(OWNER_ID, PET_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PET_ACCESS_DENIED");
    }

    // ==================== getPet() 测试 ====================

    @Test
    @DisplayName("getPet() - 成功获取宠物")
    void getPet_withValidData_shouldReturnPet() {
        // Given
        Pet pet = createActivePet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", OWNER_ID);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        // When
        Pet result = petService.getPet(OWNER_ID, PET_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PET_ID);
    }

    @Test
    @DisplayName("getPet() - 宠物不存在应抛出异常")
    void getPet_withNonExistentPet_shouldThrowException() {
        // Given
        when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> petService.getPet(OWNER_ID, PET_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getPet() - 已删除的宠物应抛出异常")
    void getPet_withDeletedPet_shouldThrowException() {
        // Given
        Pet pet = createDeletedPet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", OWNER_ID);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        // When & Then
        assertThatThrownBy(() -> petService.getPet(OWNER_ID, PET_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getPet() - 非主人操作应抛出异常")
    void getPet_withDifferentOwner_shouldThrowException() {
        // Given
        Pet pet = createActivePet("Buddy", Pet.Species.DOG);
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "ownerId", "different-owner");

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        // When & Then
        assertThatThrownBy(() -> petService.getPet(OWNER_ID, PET_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== getPetsByOwner() 测试 ====================

    @Test
    @DisplayName("getPetsByOwner() - 返回宠物列表")
    void getPetsByOwner_withPets_shouldReturnList() {
        // Given
        Pet pet1 = createActivePet("Buddy", Pet.Species.DOG);
        Pet pet2 = createActivePet("Kitty", Pet.Species.CAT);
        when(petRepository.findByOwnerId(OWNER_ID)).thenReturn(List.of(pet1, pet2));

        // When
        List<Pet> result = petService.getPetsByOwner(OWNER_ID);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getPetsByOwner() - 无宠物时返回空列表")
    void getPetsByOwner_noPets_shouldReturnEmptyList() {
        // Given
        when(petRepository.findByOwnerId(OWNER_ID)).thenReturn(Collections.emptyList());

        // When
        List<Pet> result = petService.getPetsByOwner(OWNER_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== 辅助方法 ====================

    private Pet createActivePet(String name, Pet.Species species) {
        return Pet.create(OWNER_ID, name, species, "Breed", Pet.Gender.MALE, null, null);
    }

    private Pet createDeletedPet(String name, Pet.Species species) {
        Pet pet = Pet.create(OWNER_ID, name, species, "Breed", Pet.Gender.MALE, null, null);
        pet.softDelete();
        return pet;
    }
}
