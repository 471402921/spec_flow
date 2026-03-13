package com.specflow.api.modules.user.infrastructure.persistence.converter;

import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.infrastructure.persistence.PetDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PetConverter 单元测试
 *
 * <p>测试范围：
 * - DO ↔ Entity 双向转换
 * - LocalDateTime ↔ Instant 时区转换
 * - String ↔ Enum 类型转换
 * - null 值处理
 */
@DisplayName("PetConverter 单元测试")
class PetConverterTest {

    private static final String TEST_PET_ID = "pet-123";
    private static final String TEST_OWNER_ID = "owner-456";
    private static final String TEST_NAME = "Buddy";
    private static final String TEST_BREED = "Golden Retriever";
    private static final String TEST_AVATAR_URL = "https://example.com/pet.jpg";

    // ==================== toDomain() 测试 ====================

    @Test
    @DisplayName("toDomain() - 完整 DO 应正确转换为 Entity")
    void toDomain_withCompleteDO_shouldConvertToEntity() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        LocalDate birthday = LocalDate.of(2020, 1, 15);

        PetDO petDO = new PetDO(
                TEST_PET_ID,
                TEST_OWNER_ID,
                TEST_NAME,
                "DOG",
                TEST_BREED,
                "MALE",
                birthday,
                TEST_AVATAR_URL,
                false,
                null,
                now,
                now
        );

        // When
        Pet pet = PetConverter.toDomain(petDO);

        // Then
        assertThat(pet).isNotNull();
        assertThat(pet.getId()).isEqualTo(TEST_PET_ID);
        assertThat(pet.getOwnerId()).isEqualTo(TEST_OWNER_ID);
        assertThat(pet.getName()).isEqualTo(TEST_NAME);
        assertThat(pet.getSpecies()).isEqualTo(Pet.Species.DOG);
        assertThat(pet.getBreed()).isEqualTo(TEST_BREED);
        assertThat(pet.getGender()).isEqualTo(Pet.Gender.MALE);
        assertThat(pet.getBirthday()).isEqualTo(birthday);
        assertThat(pet.getAvatarUrl()).isEqualTo(TEST_AVATAR_URL);
        assertThat(pet.isDeleted()).isFalse();
        assertThat(pet.getDeletedAt()).isNull();
        assertThat(pet.getCreatedAt()).isNotNull();
        assertThat(pet.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("toDomain() - null 输入应返回 null")
    void toDomain_withNull_shouldReturnNull() {
        // When
        Pet pet = PetConverter.toDomain(null);

        // Then
        assertThat(pet).isNull();
    }

    @Test
    @DisplayName("toDomain() - CAT 种类应正确转换")
    void toDomain_withCatSpecies_shouldConvertCorrectly() {
        // Given
        PetDO petDO = createMinimalPetDO();
        petDO.setSpecies("CAT");

        // When
        Pet pet = PetConverter.toDomain(petDO);

        // Then
        assertThat(pet.getSpecies()).isEqualTo(Pet.Species.CAT);
    }

    @Test
    @DisplayName("toDomain() - FEMALE 性别应正确转换")
    void toDomain_withFemaleGender_shouldConvertCorrectly() {
        // Given
        PetDO petDO = createMinimalPetDO();
        petDO.setGender("FEMALE");

        // When
        Pet pet = PetConverter.toDomain(petDO);

        // Then
        assertThat(pet.getGender()).isEqualTo(Pet.Gender.FEMALE);
    }

    @Test
    @DisplayName("toDomain() - 无效的 species 字符串应返回 null")
    void toDomain_withInvalidSpecies_shouldReturnNull() {
        // Given
        PetDO petDO = createMinimalPetDO();
        petDO.setSpecies("INVALID_SPECIES");

        // When
        Pet pet = PetConverter.toDomain(petDO);

        // Then
        assertThat(pet.getSpecies()).isNull();
    }

    @Test
    @DisplayName("toDomain() - 无效的 gender 字符串应返回 null")
    void toDomain_withInvalidGender_shouldReturnNull() {
        // Given
        PetDO petDO = createMinimalPetDO();
        petDO.setGender("INVALID_GENDER");

        // When
        Pet pet = PetConverter.toDomain(petDO);

        // Then
        assertThat(pet.getGender()).isNull();
    }

    @Test
    @DisplayName("toDomain() - 已删除宠物应正确转换")
    void toDomain_withDeletedPet_shouldConvertCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        PetDO petDO = new PetDO(
                TEST_PET_ID,
                TEST_OWNER_ID,
                TEST_NAME,
                "DOG",
                TEST_BREED,
                "MALE",
                null,
                null,
                true,
                now.minusDays(1),
                now.minusDays(7),
                now
        );

        // When
        Pet pet = PetConverter.toDomain(petDO);

        // Then
        assertThat(pet.isDeleted()).isTrue();
        assertThat(pet.getDeletedAt()).isNotNull();
        assertThat(pet.getBirthday()).isNull();
        assertThat(pet.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("toDomain() - deleted 字段为 null 时应默认为 false")
    void toDomain_withNullDeleted_shouldDefaultToFalse() {
        // Given
        PetDO petDO = createMinimalPetDO();
        petDO.setDeleted(null);

        // When
        Pet pet = PetConverter.toDomain(petDO);

        // Then
        assertThat(pet.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("toDomain() - 时间转换应正确处理 UTC 时区")
    void toDomain_shouldConvertTimeInUtc() {
        // Given
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        PetDO petDO = createMinimalPetDO();
        petDO.setCreatedAt(localDateTime);
        petDO.setUpdatedAt(localDateTime);

        // When
        Pet pet = PetConverter.toDomain(petDO);

        // Then
        assertThat(pet.getCreatedAt()).isEqualTo(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
        assertThat(pet.getUpdatedAt()).isEqualTo(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
    }

    // ==================== toDataObject() 测试 ====================

    @Test
    @DisplayName("toDataObject() - 完整 Entity 应正确转换为 DO")
    void toDataObject_withCompleteEntity_shouldConvertToDO() {
        // Given
        Instant now = Instant.now();
        LocalDate birthday = LocalDate.of(2020, 1, 15);

        Pet pet = new Pet(
                TEST_PET_ID,
                TEST_OWNER_ID,
                TEST_NAME,
                Pet.Species.DOG,
                TEST_BREED,
                Pet.Gender.MALE,
                birthday,
                TEST_AVATAR_URL,
                false,
                null,
                now,
                now
        );

        // When
        PetDO petDO = PetConverter.toDataObject(pet);

        // Then
        assertThat(petDO).isNotNull();
        assertThat(petDO.getId()).isEqualTo(TEST_PET_ID);
        assertThat(petDO.getOwnerId()).isEqualTo(TEST_OWNER_ID);
        assertThat(petDO.getName()).isEqualTo(TEST_NAME);
        assertThat(petDO.getSpecies()).isEqualTo("DOG");
        assertThat(petDO.getBreed()).isEqualTo(TEST_BREED);
        assertThat(petDO.getGender()).isEqualTo("MALE");
        assertThat(petDO.getBirthday()).isEqualTo(birthday);
        assertThat(petDO.getAvatarUrl()).isEqualTo(TEST_AVATAR_URL);
        assertThat(petDO.getDeleted()).isFalse();
        assertThat(petDO.getDeletedAt()).isNull();
        assertThat(petDO.getCreatedAt()).isNotNull();
        assertThat(petDO.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("toDataObject() - null 输入应返回 null")
    void toDataObject_withNull_shouldReturnNull() {
        // When
        PetDO petDO = PetConverter.toDataObject(null);

        // Then
        assertThat(petDO).isNull();
    }

    @Test
    @DisplayName("toDataObject() - CAT 种类应转换为字符串 CAT")
    void toDataObject_withCatSpecies_shouldConvertToString() {
        // Given
        Pet pet = createMinimalPet();
        pet.update(pet.getName(), Pet.Species.CAT, pet.getBreed(),
                pet.getGender(), pet.getBirthday(), pet.getAvatarUrl());

        // When
        PetDO petDO = PetConverter.toDataObject(pet);

        // Then
        assertThat(petDO.getSpecies()).isEqualTo("CAT");
    }

    @Test
    @DisplayName("toDataObject() - FEMALE 性别应转换为字符串 FEMALE")
    void toDataObject_withFemaleGender_shouldConvertToString() {
        // Given
        Pet pet = createMinimalPet();
        pet.update(pet.getName(), pet.getSpecies(), pet.getBreed(),
                Pet.Gender.FEMALE, pet.getBirthday(), pet.getAvatarUrl());

        // When
        PetDO petDO = PetConverter.toDataObject(pet);

        // Then
        assertThat(petDO.getGender()).isEqualTo("FEMALE");
    }

    @Test
    @DisplayName("toDataObject() - null species 应转换为 null 字符串")
    void toDataObject_withNullSpecies_shouldConvertToNull() {
        // Given
        Instant now = Instant.now();
        Pet pet = new Pet(
                TEST_PET_ID,
                TEST_OWNER_ID,
                TEST_NAME,
                null,
                TEST_BREED,
                Pet.Gender.MALE,
                null,
                null,
                false,
                null,
                now,
                now
        );

        // When
        PetDO petDO = PetConverter.toDataObject(pet);

        // Then
        assertThat(petDO.getSpecies()).isNull();
    }

    @Test
    @DisplayName("toDataObject() - null gender 应转换为 null 字符串")
    void toDataObject_withNullGender_shouldConvertToNull() {
        // Given
        Instant now = Instant.now();
        Pet pet = new Pet(
                TEST_PET_ID,
                TEST_OWNER_ID,
                TEST_NAME,
                Pet.Species.DOG,
                TEST_BREED,
                null,
                null,
                null,
                false,
                null,
                now,
                now
        );

        // When
        PetDO petDO = PetConverter.toDataObject(pet);

        // Then
        assertThat(petDO.getGender()).isNull();
    }

    @Test
    @DisplayName("toDataObject() - 时间转换应正确处理 UTC 时区")
    void toDataObject_shouldConvertTimeInUtc() {
        // Given
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        Pet pet = createMinimalPet();
        pet.update(pet.getName(), pet.getSpecies(), pet.getBreed(),
                pet.getGender(), pet.getBirthday(), pet.getAvatarUrl());

        // Use reflection to set timestamps
        try {
            var createdAtField = Pet.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(pet, instant);

            var updatedAtField = Pet.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(pet, instant);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // When
        PetDO petDO = PetConverter.toDataObject(pet);

        // Then
        assertThat(petDO.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")));
        assertThat(petDO.getUpdatedAt()).isEqualTo(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")));
    }

    // ==================== 双向转换一致性测试 ====================

    @Test
    @DisplayName("双向转换 - toDomain 后再 toDataObject 应保持数据一致")
    void roundTrip_shouldPreserveData() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        LocalDate birthday = LocalDate.of(2020, 1, 15);

        PetDO originalDO = new PetDO(
                TEST_PET_ID,
                TEST_OWNER_ID,
                TEST_NAME,
                "DOG",
                TEST_BREED,
                "MALE",
                birthday,
                TEST_AVATAR_URL,
                false,
                null,
                now,
                now
        );

        // When
        Pet pet = PetConverter.toDomain(originalDO);
        PetDO convertedDO = PetConverter.toDataObject(pet);

        // Then
        assertThat(convertedDO.getId()).isEqualTo(originalDO.getId());
        assertThat(convertedDO.getOwnerId()).isEqualTo(originalDO.getOwnerId());
        assertThat(convertedDO.getName()).isEqualTo(originalDO.getName());
        assertThat(convertedDO.getSpecies()).isEqualTo(originalDO.getSpecies());
        assertThat(convertedDO.getBreed()).isEqualTo(originalDO.getBreed());
        assertThat(convertedDO.getGender()).isEqualTo(originalDO.getGender());
        assertThat(convertedDO.getBirthday()).isEqualTo(originalDO.getBirthday());
        assertThat(convertedDO.getAvatarUrl()).isEqualTo(originalDO.getAvatarUrl());
        assertThat(convertedDO.getDeleted()).isEqualTo(originalDO.getDeleted());
    }

    @Test
    @DisplayName("双向转换 - toDataObject 后再 toDomain 应保持数据一致")
    void roundTripReverse_shouldPreserveData() {
        // Given
        Instant now = Instant.now();
        LocalDate birthday = LocalDate.of(2020, 1, 15);

        Pet originalPet = new Pet(
                TEST_PET_ID,
                TEST_OWNER_ID,
                TEST_NAME,
                Pet.Species.DOG,
                TEST_BREED,
                Pet.Gender.MALE,
                birthday,
                TEST_AVATAR_URL,
                false,
                null,
                now,
                now
        );

        // When
        PetDO petDO = PetConverter.toDataObject(originalPet);
        Pet convertedPet = PetConverter.toDomain(petDO);

        // Then
        assertThat(convertedPet.getId()).isEqualTo(originalPet.getId());
        assertThat(convertedPet.getOwnerId()).isEqualTo(originalPet.getOwnerId());
        assertThat(convertedPet.getName()).isEqualTo(originalPet.getName());
        assertThat(convertedPet.getSpecies()).isEqualTo(originalPet.getSpecies());
        assertThat(convertedPet.getBreed()).isEqualTo(originalPet.getBreed());
        assertThat(convertedPet.getGender()).isEqualTo(originalPet.getGender());
        assertThat(convertedPet.getBirthday()).isEqualTo(originalPet.getBirthday());
        assertThat(convertedPet.getAvatarUrl()).isEqualTo(originalPet.getAvatarUrl());
        assertThat(convertedPet.isDeleted()).isEqualTo(originalPet.isDeleted());
        assertThat(convertedPet.getCreatedAt()).isEqualTo(originalPet.getCreatedAt());
        assertThat(convertedPet.getUpdatedAt()).isEqualTo(originalPet.getUpdatedAt());
    }

    // ==================== 辅助方法 ====================

    private PetDO createMinimalPetDO() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        return new PetDO(
                TEST_PET_ID,
                TEST_OWNER_ID,
                TEST_NAME,
                "DOG",
                TEST_BREED,
                "MALE",
                null,
                null,
                false,
                null,
                now,
                now
        );
    }

    private Pet createMinimalPet() {
        return Pet.create(
                TEST_OWNER_ID,
                TEST_NAME,
                Pet.Species.DOG,
                TEST_BREED,
                Pet.Gender.MALE,
                null,
                null
        );
    }
}
