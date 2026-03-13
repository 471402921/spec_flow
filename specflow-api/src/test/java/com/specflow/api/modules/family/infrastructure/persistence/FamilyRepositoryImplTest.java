package com.specflow.api.modules.family.infrastructure.persistence;

import com.specflow.api.modules.family.domain.entity.Family;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FamilyRepositoryImpl 单元测试
 *
 * <p>测试策略：
 * - Mock FamilyMapper
 * - 测试 DO ↔ Entity 转换逻辑
 * - 验证查询条件构建
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyRepositoryImpl 单元测试")
class FamilyRepositoryImplTest {

    @Mock
    private FamilyMapper familyMapper;

    @InjectMocks
    private FamilyRepositoryImpl familyRepository;

    private static final String TEST_FAMILY_ID = "family-123";
    private static final String TEST_FAMILY_NAME = "Test Family";
    private static final String TEST_OWNER_ID = "user-456";

    // ==================== save() 测试 ====================

    @Test
    @DisplayName("save() - 新家庭应执行插入")
    void save_withNewFamily_shouldInsert() {
        // Given
        Family family = createFamily(TEST_FAMILY_ID, TEST_FAMILY_NAME, TEST_OWNER_ID);

        when(familyMapper.selectById(TEST_FAMILY_ID)).thenReturn(null);

        // When
        Family result = familyRepository.save(family);

        // Then
        assertThat(result).isEqualTo(family);
        verify(familyMapper).insert(any(FamilyDO.class));
    }

    @Test
    @DisplayName("save() - 已存在家庭应执行更新")
    void save_withExistingFamily_shouldUpdate() {
        // Given
        Family family = createFamily(TEST_FAMILY_ID, TEST_FAMILY_NAME, TEST_OWNER_ID);
        FamilyDO existingDO = createFamilyDO(TEST_FAMILY_ID, TEST_FAMILY_NAME, TEST_OWNER_ID);

        when(familyMapper.selectById(TEST_FAMILY_ID)).thenReturn(existingDO);

        // When
        Family result = familyRepository.save(family);

        // Then
        assertThat(result).isEqualTo(family);
        verify(familyMapper).updateById(any(FamilyDO.class));
    }

    // ==================== findById() 测试 ====================

    @Test
    @DisplayName("findById() - 家庭存在时应返回家庭")
    void findById_withExistingFamily_shouldReturnFamily() {
        // Given
        FamilyDO familyDO = createFamilyDO(TEST_FAMILY_ID, TEST_FAMILY_NAME, TEST_OWNER_ID);
        when(familyMapper.selectById(TEST_FAMILY_ID)).thenReturn(familyDO);

        // When
        Optional<Family> result = familyRepository.findById(TEST_FAMILY_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(result.get().getName()).isEqualTo(TEST_FAMILY_NAME);
        assertThat(result.get().getOwnerId()).isEqualTo(TEST_OWNER_ID);
    }

    @Test
    @DisplayName("findById() - 家庭不存在时应返回空")
    void findById_withNonExistentFamily_shouldReturnEmpty() {
        // Given
        when(familyMapper.selectById("non-existent")).thenReturn(null);

        // When
        Optional<Family> result = familyRepository.findById("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById() - Mapper 返回 null 时 Converter 应返回 null")
    void findById_whenMapperReturnsNull_shouldReturnEmpty() {
        // Given
        when(familyMapper.selectById(TEST_FAMILY_ID)).thenReturn(null);

        // When
        Optional<Family> result = familyRepository.findById(TEST_FAMILY_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== deleteById() 测试 ====================

    @Test
    @DisplayName("deleteById() - 应调用 Mapper 删除")
    void deleteById_shouldCallMapperDelete() {
        // When
        familyRepository.deleteById(TEST_FAMILY_ID);

        // Then
        verify(familyMapper).deleteById(TEST_FAMILY_ID);
    }

    // ==================== deleteAllByIdInBatch() 测试 ====================

    @Test
    @DisplayName("deleteAllByIdInBatch() - 空列表时不应调用删除")
    void deleteAllByIdInBatch_withEmptyList_shouldNotCallDelete() {
        // When
        familyRepository.deleteAllByIdInBatch(List.of());

        // Then
        verify(familyMapper, org.mockito.Mockito.never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("deleteAllByIdInBatch() - 非空列表时应批量删除")
    void deleteAllByIdInBatch_withNonEmptyList_shouldBatchDelete() {
        // Given
        List<String> ids = List.of("id1", "id2", "id3");

        // When
        familyRepository.deleteAllByIdInBatch(ids);

        // Then
        verify(familyMapper).deleteBatchIds(ids);
    }

    // ==================== 转换验证测试 ====================

    @Test
    @DisplayName("转换验证 - 时间字段应正确转换")
    void conversion_timeFields_shouldConvertCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        FamilyDO familyDO = new FamilyDO();
        familyDO.setId(TEST_FAMILY_ID);
        familyDO.setName(TEST_FAMILY_NAME);
        familyDO.setOwnerId(TEST_OWNER_ID);
        familyDO.setCreatedAt(now);
        familyDO.setUpdatedAt(now);

        when(familyMapper.selectById(TEST_FAMILY_ID)).thenReturn(familyDO);

        // When
        Optional<Family> result = familyRepository.findById(TEST_FAMILY_ID);

        // Then
        assertThat(result).isPresent();
        Family family = result.get();

        Instant expectedInstant = now.atZone(ZoneId.of("UTC")).toInstant();
        assertThat(family.getCreatedAt()).isEqualTo(expectedInstant);
        assertThat(family.getUpdatedAt()).isEqualTo(expectedInstant);
    }

    @Test
    @DisplayName("转换验证 - null 时间字段应处理为 null")
    void conversion_nullTimeFields_shouldHandleAsNull() {
        // Given
        FamilyDO familyDO = new FamilyDO();
        familyDO.setId(TEST_FAMILY_ID);
        familyDO.setName(TEST_FAMILY_NAME);
        familyDO.setOwnerId(TEST_OWNER_ID);
        familyDO.setCreatedAt(null);
        familyDO.setUpdatedAt(null);

        when(familyMapper.selectById(TEST_FAMILY_ID)).thenReturn(familyDO);

        // When
        Optional<Family> result = familyRepository.findById(TEST_FAMILY_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCreatedAt()).isNull();
        assertThat(result.get().getUpdatedAt()).isNull();
    }

    // ==================== 辅助方法 ====================

    private Family createFamily(String id, String name, String ownerId) {
        Family family = new Family();
        family.setId(id);
        family.setName(name);
        family.setOwnerId(ownerId);
        family.setCreatedAt(Instant.now());
        family.setUpdatedAt(Instant.now());
        return family;
    }

    private FamilyDO createFamilyDO(String id, String name, String ownerId) {
        FamilyDO familyDO = new FamilyDO();
        familyDO.setId(id);
        familyDO.setName(name);
        familyDO.setOwnerId(ownerId);
        familyDO.setCreatedAt(LocalDateTime.now());
        familyDO.setUpdatedAt(LocalDateTime.now());
        return familyDO;
    }
}
