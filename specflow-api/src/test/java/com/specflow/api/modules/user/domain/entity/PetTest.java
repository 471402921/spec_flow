package com.specflow.api.modules.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pet 领域实体单元测试
 *
 * <p>测试范围：
 * - 工厂方法 create()
 * - 领域行为：update, softDelete, restore, belongsTo
 * - 状态验证
 */
@DisplayName("Pet 领域实体测试")
class PetTest {

    @Test
    @DisplayName("create() - 使用所有参数创建宠物")
    void create_withAllParams_shouldCreatePet() {
        // Given
        String ownerId = "owner-123";
        String name = "Buddy";
        Pet.Species species = Pet.Species.DOG;
        String breed = "Golden Retriever";
        Pet.Gender gender = Pet.Gender.MALE;
        LocalDate birthday = LocalDate.of(2020, 1, 1);
        String avatarUrl = "https://example.com/pet.jpg";

        // When
        Pet pet = Pet.create(ownerId, name, species, breed, gender, birthday, avatarUrl);

        // Then
        assertThat(pet).isNotNull();
        assertThat(pet.getId()).isNotNull();
        assertThat(pet.getOwnerId()).isEqualTo(ownerId);
        assertThat(pet.getName()).isEqualTo(name);
        assertThat(pet.getSpecies()).isEqualTo(species);
        assertThat(pet.getBreed()).isEqualTo(breed);
        assertThat(pet.getGender()).isEqualTo(gender);
        assertThat(pet.getBirthday()).isEqualTo(birthday);
        assertThat(pet.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(pet.isDeleted()).isFalse();
        assertThat(pet.getDeletedAt()).isNull();
        assertThat(pet.getCreatedAt()).isNotNull();
        assertThat(pet.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("create() - 允许 birthday 和 avatarUrl 为 null")
    void create_withNullOptionalParams_shouldCreatePet() {
        // Given
        String ownerId = "owner-123";
        String name = "Kitty";
        Pet.Species species = Pet.Species.CAT;
        String breed = "Persian";
        Pet.Gender gender = Pet.Gender.FEMALE;

        // When
        Pet pet = Pet.create(ownerId, name, species, breed, gender, null, null);

        // Then
        assertThat(pet.getBirthday()).isNull();
        assertThat(pet.getAvatarUrl()).isNull();
        assertThat(pet.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("update() - 更新所有字段")
    void update_withAllParams_shouldUpdateAllFields() {
        // Given
        Pet pet = createDefaultPet();
        String newName = "NewBuddy";
        Pet.Species newSpecies = Pet.Species.CAT;
        String newBreed = "Siamese";
        Pet.Gender newGender = Pet.Gender.FEMALE;
        LocalDate newBirthday = LocalDate.of(2021, 6, 15);
        String newAvatar = "https://example.com/new.jpg";

        // When
        pet.update(newName, newSpecies, newBreed, newGender, newBirthday, newAvatar);

        // Then
        assertThat(pet.getName()).isEqualTo(newName);
        assertThat(pet.getSpecies()).isEqualTo(newSpecies);
        assertThat(pet.getBreed()).isEqualTo(newBreed);
        assertThat(pet.getGender()).isEqualTo(newGender);
        assertThat(pet.getBirthday()).isEqualTo(newBirthday);
        assertThat(pet.getAvatarUrl()).isEqualTo(newAvatar);
        assertThat(pet.getUpdatedAt()).isAfter(pet.getCreatedAt());
    }

    @Test
    @DisplayName("update() - null avatarUrl 应保持原值")
    void update_withNullAvatar_shouldKeepOriginalAvatar() {
        // Given
        Pet pet = createDefaultPet();
        String originalAvatar = pet.getAvatarUrl();

        // When
        pet.update("NewName", Pet.Species.DOG, "NewBreed", Pet.Gender.MALE, null, null);

        // Then
        assertThat(pet.getAvatarUrl()).isEqualTo(originalAvatar);
    }

    @Test
    @DisplayName("softDelete() - 软删除应设置标记和时间")
    void softDelete_shouldSetDeletedFlagAndTimestamp() {
        // Given
        Pet pet = createDefaultPet();

        // When
        pet.softDelete();

        // Then
        assertThat(pet.isDeleted()).isTrue();
        assertThat(pet.getDeletedAt()).isNotNull();
        assertThat(pet.getUpdatedAt()).isAfter(pet.getCreatedAt());
    }

    @Test
    @DisplayName("restore() - 恢复应清除删除标记")
    void restore_shouldClearDeletedFlag() {
        // Given
        Pet pet = createDefaultPet();
        pet.softDelete();

        // When
        pet.restore();

        // Then
        assertThat(pet.isDeleted()).isFalse();
        assertThat(pet.getDeletedAt()).isNull();
        assertThat(pet.getUpdatedAt()).isAfter(pet.getCreatedAt());
    }

    @Test
    @DisplayName("restore() - 对未删除的宠物调用应保持不变")
    void restore_onNonDeletedPet_shouldKeepUnchanged() {
        // Given
        Pet pet = createDefaultPet();

        // When
        pet.restore();

        // Then
        assertThat(pet.isDeleted()).isFalse();
        assertThat(pet.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("belongsTo() - 相同 ownerId 应返回 true")
    void belongsTo_withSameOwnerId_shouldReturnTrue() {
        // Given
        String ownerId = "owner-123";
        Pet pet = Pet.create(ownerId, "Buddy", Pet.Species.DOG, "Golden", Pet.Gender.MALE, null, null);

        // When & Then
        assertThat(pet.belongsTo(ownerId)).isTrue();
    }

    @Test
    @DisplayName("belongsTo() - 不同 ownerId 应返回 false")
    void belongsTo_withDifferentOwnerId_shouldReturnFalse() {
        // Given
        Pet pet = createDefaultPet();

        // When & Then
        assertThat(pet.belongsTo("different-owner")).isFalse();
    }

    @Test
    @DisplayName("软删除后再恢复可以再次删除")
    void softDeleteThenRestoreThenDeleteAgain_shouldWork() {
        // Given
        Pet pet = createDefaultPet();

        // When - 第一次删除
        pet.softDelete();
        assertThat(pet.isDeleted()).isTrue();

        // When - 恢复
        pet.restore();
        assertThat(pet.isDeleted()).isFalse();

        // When - 再次删除
        pet.softDelete();

        // Then
        assertThat(pet.isDeleted()).isTrue();
        assertThat(pet.getDeletedAt()).isNotNull();
    }

    private Pet createDefaultPet() {
        return Pet.create(
                "owner-123",
                "Buddy",
                Pet.Species.DOG,
                "Golden Retriever",
                Pet.Gender.MALE,
                LocalDate.of(2020, 1, 1),
                "https://example.com/pet.jpg"
        );
    }
}
