package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.specflow.api.modules.family.domain.entity.FamilyInvitation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FamilyInvitationRepositoryImpl 单元测试
 *
 * <p>测试策略：
 * - Mock FamilyInvitationMapper
 * - 测试 DO ↔ Entity 转换逻辑
 * - 验证查询条件构建
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyInvitationRepositoryImpl 单元测试")
class FamilyInvitationRepositoryImplTest {

    @Mock
    private FamilyInvitationMapper familyInvitationMapper;

    @InjectMocks
    private FamilyInvitationRepositoryImpl familyInvitationRepository;

    private static final String TEST_INVITATION_ID = "invitation-123";
    private static final String TEST_FAMILY_ID = "family-456";
    private static final String TEST_CODE = "ABC12345";
    private static final String TEST_CREATED_BY = "user-789";

    // ==================== save() 测试 ====================

    @Test
    @DisplayName("save() - 新邀请码应执行插入")
    void save_withNewInvitation_shouldInsert() {
        // Given
        FamilyInvitation invitation = createInvitation(TEST_INVITATION_ID, TEST_FAMILY_ID, TEST_CODE, false);

        when(familyInvitationMapper.selectById(TEST_INVITATION_ID)).thenReturn(null);

        // When
        FamilyInvitation result = familyInvitationRepository.save(invitation);

        // Then
        assertThat(result).isEqualTo(invitation);
        verify(familyInvitationMapper).insert(any(FamilyInvitationDO.class));
    }

    @Test
    @DisplayName("save() - 已存在邀请码应执行更新")
    void save_withExistingInvitation_shouldUpdate() {
        // Given
        FamilyInvitation invitation = createInvitation(TEST_INVITATION_ID, TEST_FAMILY_ID, TEST_CODE, false);
        FamilyInvitationDO existingDO = createInvitationDO(TEST_INVITATION_ID, TEST_FAMILY_ID, TEST_CODE, false);

        when(familyInvitationMapper.selectById(TEST_INVITATION_ID)).thenReturn(existingDO);

        // When
        FamilyInvitation result = familyInvitationRepository.save(invitation);

        // Then
        assertThat(result).isEqualTo(invitation);
        verify(familyInvitationMapper).updateById(any(FamilyInvitationDO.class));
    }

    // ==================== findById() 测试 ====================

    @Test
    @DisplayName("findById() - 邀请码存在时应返回")
    void findById_withExisting_shouldReturnInvitation() {
        // Given
        FamilyInvitationDO invitationDO = createInvitationDO(TEST_INVITATION_ID, TEST_FAMILY_ID, TEST_CODE, false);
        when(familyInvitationMapper.selectById(TEST_INVITATION_ID)).thenReturn(invitationDO);

        // When
        Optional<FamilyInvitation> result = familyInvitationRepository.findById(TEST_INVITATION_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TEST_INVITATION_ID);
        assertThat(result.get().getCode()).isEqualTo(TEST_CODE);
    }

    @Test
    @DisplayName("findById() - 邀请码不存在时应返回空")
    void findById_withNonExisting_shouldReturnEmpty() {
        // Given
        when(familyInvitationMapper.selectById("non-existent")).thenReturn(null);

        // When
        Optional<FamilyInvitation> result = familyInvitationRepository.findById("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findByCode() 测试 ====================

    @Test
    @DisplayName("findByCode() - 应转换为大小写并返回")
    void findByCode_shouldNormalizeAndReturn() {
        // Given
        String lowerCaseCode = "abc12345";
        FamilyInvitationDO invitationDO = createInvitationDO(TEST_INVITATION_ID, TEST_FAMILY_ID, "ABC12345", false);
        when(familyInvitationMapper.selectByCode("ABC12345")).thenReturn(invitationDO);

        // When
        Optional<FamilyInvitation> result = familyInvitationRepository.findByCode(lowerCaseCode);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("ABC12345");
    }

    @Test
    @DisplayName("findByCode() - 不存在时应返回空")
    void findByCode_withNonExisting_shouldReturnEmpty() {
        // Given
        when(familyInvitationMapper.selectByCode(any())).thenReturn(null);

        // When
        Optional<FamilyInvitation> result = familyInvitationRepository.findByCode("INVALID");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findAllByFamilyIdAndRevokedFalse() 测试 ====================

    @Test
    @DisplayName("findAllByFamilyIdAndRevokedFalse() - 应返回未撤销的邀请码")
    void findAllByFamilyIdAndRevokedFalse_shouldReturnNonRevoked() {
        // Given
        FamilyInvitationDO invitation1 = createInvitationDO("inv-1", TEST_FAMILY_ID, "CODE0001", false);
        FamilyInvitationDO invitation2 = createInvitationDO("inv-2", TEST_FAMILY_ID, "CODE0002", false);
        when(familyInvitationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(invitation1, invitation2));

        // When
        List<FamilyInvitation> result = familyInvitationRepository.findAllByFamilyIdAndRevokedFalse(TEST_FAMILY_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).isRevoked()).isFalse();
        assertThat(result.get(1).isRevoked()).isFalse();
    }

    // ==================== findAllByFamilyId() 测试 ====================

    @Test
    @DisplayName("findAllByFamilyId() - 应返回所有邀请码")
    void findAllByFamilyId_shouldReturnAllInvitations() {
        // Given
        FamilyInvitationDO invitation1 = createInvitationDO("inv-1", TEST_FAMILY_ID, "CODE0001", false);
        FamilyInvitationDO invitation2 = createInvitationDO("inv-2", TEST_FAMILY_ID, "CODE0002", true);
        when(familyInvitationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(invitation1, invitation2));

        // When
        List<FamilyInvitation> result = familyInvitationRepository.findAllByFamilyId(TEST_FAMILY_ID);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findAllByFamilyId() - 无数据时应返回空列表")
    void findAllByFamilyId_withNoData_shouldReturnEmptyList() {
        // Given
        when(familyInvitationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When
        List<FamilyInvitation> result = familyInvitationRepository.findAllByFamilyId(TEST_FAMILY_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== deleteById() 测试 ====================

    @Test
    @DisplayName("deleteById() - 应调用 Mapper 删除")
    void deleteById_shouldCallMapperDelete() {
        // When
        familyInvitationRepository.deleteById(TEST_INVITATION_ID);

        // Then
        verify(familyInvitationMapper).deleteById(TEST_INVITATION_ID);
    }

    // ==================== deleteAllByFamilyId() 测试 ====================

    @Test
    @DisplayName("deleteAllByFamilyId() - 应调用 Mapper 删除")
    void deleteAllByFamilyId_shouldCallMapperDelete() {
        // When
        familyInvitationRepository.deleteAllByFamilyId(TEST_FAMILY_ID);

        // Then
        verify(familyInvitationMapper).delete(any(LambdaQueryWrapper.class));
    }

    // ==================== existsByCode() 测试 ====================

    @Test
    @DisplayName("existsByCode() - 存在时返回 true")
    void existsByCode_whenExists_shouldReturnTrue() {
        // Given
        when(familyInvitationMapper.countByCode("ABC12345".toUpperCase())).thenReturn(1);

        // When
        boolean result = familyInvitationRepository.existsByCode("abc12345");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByCode() - 不存在时返回 false")
    void existsByCode_whenNotExists_shouldReturnFalse() {
        // Given
        when(familyInvitationMapper.countByCode(any())).thenReturn(0);

        // When
        boolean result = familyInvitationRepository.existsByCode("INVALID");

        // Then
        assertThat(result).isFalse();
    }

    // ==================== 转换验证测试 ====================

    @Test
    @DisplayName("转换验证 - revoked 布尔值应正确转换")
    void conversion_revokedBoolean_shouldConvertCorrectly() {
        // Given - revoked = true (Boolean.TRUE)
        FamilyInvitationDO invitationDO = createInvitationDO(TEST_INVITATION_ID, TEST_FAMILY_ID, TEST_CODE, true);
        when(familyInvitationMapper.selectById(TEST_INVITATION_ID)).thenReturn(invitationDO);

        // When
        Optional<FamilyInvitation> result = familyInvitationRepository.findById(TEST_INVITATION_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().isRevoked()).isTrue();
    }

    @Test
    @DisplayName("转换验证 - revoked 为 null 时应视为 false")
    void conversion_revokedNull_shouldTreatAsFalse() {
        // Given
        FamilyInvitationDO invitationDO = createInvitationDO(TEST_INVITATION_ID, TEST_FAMILY_ID, TEST_CODE, null);
        when(familyInvitationMapper.selectById(TEST_INVITATION_ID)).thenReturn(invitationDO);

        // When
        Optional<FamilyInvitation> result = familyInvitationRepository.findById(TEST_INVITATION_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("转换验证 - 时间字段应正确转换")
    void conversion_timeFields_shouldConvertCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
        LocalDateTime expired = now.plusDays(7);

        FamilyInvitationDO invitationDO = new FamilyInvitationDO();
        invitationDO.setId(TEST_INVITATION_ID);
        invitationDO.setFamilyId(TEST_FAMILY_ID);
        invitationDO.setCode(TEST_CODE);
        invitationDO.setCreatedBy(TEST_CREATED_BY);
        invitationDO.setRevoked(false);
        invitationDO.setCreatedAt(now);
        invitationDO.setUpdatedAt(now);
        invitationDO.setExpiredAt(expired);

        when(familyInvitationMapper.selectById(TEST_INVITATION_ID)).thenReturn(invitationDO);

        // When
        Optional<FamilyInvitation> result = familyInvitationRepository.findById(TEST_INVITATION_ID);

        // Then
        assertThat(result).isPresent();
        FamilyInvitation invitation = result.get();

        Instant expectedCreated = now.atZone(ZoneId.of("UTC")).toInstant();
        Instant expectedExpired = expired.atZone(ZoneId.of("UTC")).toInstant();

        assertThat(invitation.getCreatedAt()).isEqualTo(expectedCreated);
        assertThat(invitation.getExpiredAt()).isEqualTo(expectedExpired);
    }

    // ==================== 辅助方法 ====================

    private FamilyInvitation createInvitation(String id, String familyId, String code, boolean revoked) {
        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setId(id);
        invitation.setFamilyId(familyId);
        invitation.setCode(code);
        invitation.setCreatedBy(TEST_CREATED_BY);
        invitation.setRevoked(revoked);
        invitation.setCreatedAt(Instant.now());
        invitation.setUpdatedAt(Instant.now());
        invitation.setExpiredAt(Instant.now().plusSeconds(7 * 24 * 60 * 60));
        return invitation;
    }

    private FamilyInvitationDO createInvitationDO(String id, String familyId, String code, Boolean revoked) {
        FamilyInvitationDO invitationDO = new FamilyInvitationDO();
        invitationDO.setId(id);
        invitationDO.setFamilyId(familyId);
        invitationDO.setCode(code);
        invitationDO.setCreatedBy(TEST_CREATED_BY);
        invitationDO.setRevoked(revoked);
        invitationDO.setCreatedAt(LocalDateTime.now());
        invitationDO.setUpdatedAt(LocalDateTime.now());
        invitationDO.setExpiredAt(LocalDateTime.now().plusDays(7));
        return invitationDO;
    }
}
