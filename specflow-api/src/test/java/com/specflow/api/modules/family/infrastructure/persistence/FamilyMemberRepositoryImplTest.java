package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.specflow.api.modules.family.domain.entity.FamilyMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FamilyMemberRepositoryImpl 单元测试
 *
 * <p>测试策略：
 * - Mock FamilyMemberMapper
 * - 测试 DO ↔ Entity 转换逻辑
 * - 验证查询条件构建
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyMemberRepositoryImpl 单元测试")
class FamilyMemberRepositoryImplTest {

    @Mock
    private FamilyMemberMapper familyMemberMapper;

    @InjectMocks
    private FamilyMemberRepositoryImpl familyMemberRepository;

    private static final String TEST_MEMBER_ID = "member-123";
    private static final String TEST_FAMILY_ID = "family-456";
    private static final String TEST_USER_ID = "user-789";

    // ==================== save() 测试 ====================

    @Test
    @DisplayName("save() - 新成员应执行插入")
    void save_withNewMember_shouldInsert() {
        // Given
        FamilyMember member = createMember(TEST_MEMBER_ID, TEST_FAMILY_ID, TEST_USER_ID, FamilyMember.FamilyRole.MEMBER);

        when(familyMemberMapper.selectById(TEST_MEMBER_ID)).thenReturn(null);

        // When
        FamilyMember result = familyMemberRepository.save(member);

        // Then
        assertThat(result).isEqualTo(member);
        verify(familyMemberMapper).insert(any(FamilyMemberDO.class));
    }

    @Test
    @DisplayName("save() - 已存在成员应执行更新")
    void save_withExistingMember_shouldUpdate() {
        // Given
        FamilyMember member = createMember(TEST_MEMBER_ID, TEST_FAMILY_ID, TEST_USER_ID, FamilyMember.FamilyRole.MEMBER);
        FamilyMemberDO existingDO = createMemberDO(TEST_MEMBER_ID, TEST_FAMILY_ID, TEST_USER_ID, "MEMBER");

        when(familyMemberMapper.selectById(TEST_MEMBER_ID)).thenReturn(existingDO);

        // When
        FamilyMember result = familyMemberRepository.save(member);

        // Then
        assertThat(result).isEqualTo(member);
        verify(familyMemberMapper).updateById(any(FamilyMemberDO.class));
    }

    // ==================== findById() 测试 ====================

    @Test
    @DisplayName("findById() - 成员存在时应返回成员")
    void findById_withExistingMember_shouldReturnMember() {
        // Given
        FamilyMemberDO memberDO = createMemberDO(TEST_MEMBER_ID, TEST_FAMILY_ID, TEST_USER_ID, "OWNER");
        when(familyMemberMapper.selectById(TEST_MEMBER_ID)).thenReturn(memberDO);

        // When
        Optional<FamilyMember> result = familyMemberRepository.findById(TEST_MEMBER_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TEST_MEMBER_ID);
        assertThat(result.get().getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(result.get().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.get().getRole()).isEqualTo(FamilyMember.FamilyRole.OWNER);
    }

    @Test
    @DisplayName("findById() - 成员不存在时应返回空")
    void findById_withNonExistentMember_shouldReturnEmpty() {
        // Given
        when(familyMemberMapper.selectById("non-existent")).thenReturn(null);

        // When
        Optional<FamilyMember> result = familyMemberRepository.findById("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findAllByFamilyId() 测试 ====================

    @Test
    @DisplayName("findAllByFamilyId() - 应返回成员列表")
    void findAllByFamilyId_shouldReturnMemberList() {
        // Given
        FamilyMemberDO member1 = createMemberDO("member-1", TEST_FAMILY_ID, "user-1", "OWNER");
        FamilyMemberDO member2 = createMemberDO("member-2", TEST_FAMILY_ID, "user-2", "MEMBER");
        when(familyMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member1, member2));

        // When
        List<FamilyMember> result = familyMemberRepository.findAllByFamilyId(TEST_FAMILY_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(result.get(1).getFamilyId()).isEqualTo(TEST_FAMILY_ID);
    }

    @Test
    @DisplayName("findAllByFamilyId() - 无数据时应返回空列表")
    void findAllByFamilyId_withNoData_shouldReturnEmptyList() {
        // Given
        when(familyMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When
        List<FamilyMember> result = familyMemberRepository.findAllByFamilyId(TEST_FAMILY_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findAllByUserId() 测试 ====================

    @Test
    @DisplayName("findAllByUserId() - 应返回成员关系列表")
    void findAllByUserId_shouldReturnMembershipList() {
        // Given
        FamilyMemberDO member1 = createMemberDO("member-1", "family-1", TEST_USER_ID, "MEMBER");
        FamilyMemberDO member2 = createMemberDO("member-2", "family-2", TEST_USER_ID, "MEMBER");
        when(familyMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member1, member2));

        // When
        List<FamilyMember> result = familyMemberRepository.findAllByUserId(TEST_USER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.get(1).getUserId()).isEqualTo(TEST_USER_ID);
    }

    // ==================== findByFamilyIdAndUserId() 测试 ====================

    @Test
    @DisplayName("findByFamilyIdAndUserId() - 存在时应返回成员")
    void findByFamilyIdAndUserId_withExisting_shouldReturnMember() {
        // Given
        FamilyMemberDO memberDO = createMemberDO(TEST_MEMBER_ID, TEST_FAMILY_ID, TEST_USER_ID, "OWNER");
        when(familyMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberDO);

        // When
        Optional<FamilyMember> result = familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(result.get().getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("findByFamilyIdAndUserId() - 不存在时应返回空")
    void findByFamilyIdAndUserId_withNonExisting_shouldReturnEmpty() {
        // Given
        when(familyMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When
        Optional<FamilyMember> result = familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== countByFamilyId() 测试 ====================

    @Test
    @DisplayName("countByFamilyId() - 应返回正确数量")
    void countByFamilyId_shouldReturnCorrectCount() {
        // Given
        when(familyMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

        // When
        long result = familyMemberRepository.countByFamilyId(TEST_FAMILY_ID);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    // ==================== countByUserId() 测试 ====================

    @Test
    @DisplayName("countByUserId() - 应返回正确数量")
    void countByUserId_shouldReturnCorrectCount() {
        // Given
        when(familyMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        // When
        long result = familyMemberRepository.countByUserId(TEST_USER_ID);

        // Then
        assertThat(result).isEqualTo(3L);
    }

    // ==================== deleteById() 测试 ====================

    @Test
    @DisplayName("deleteById() - 应调用 Mapper 删除")
    void deleteById_shouldCallMapperDelete() {
        // When
        familyMemberRepository.deleteById(TEST_MEMBER_ID);

        // Then
        verify(familyMemberMapper).deleteById(TEST_MEMBER_ID);
    }

    // ==================== deleteAllByFamilyId() 测试 ====================

    @Test
    @DisplayName("deleteAllByFamilyId() - 应调用 Mapper 删除")
    void deleteAllByFamilyId_shouldCallMapperDelete() {
        // When
        familyMemberRepository.deleteAllByFamilyId(TEST_FAMILY_ID);

        // Then
        verify(familyMemberMapper).delete(any(LambdaQueryWrapper.class));
    }

    // ==================== deleteAllByUserId() 测试 ====================

    @Test
    @DisplayName("deleteAllByUserId() - 应调用 Mapper 删除")
    void deleteAllByUserId_shouldCallMapperDelete() {
        // When
        familyMemberRepository.deleteAllByUserId(TEST_USER_ID);

        // Then
        verify(familyMemberMapper).delete(any(LambdaQueryWrapper.class));
    }

    // ==================== existsByFamilyIdAndUserId() 测试 ====================

    @Test
    @DisplayName("existsByFamilyIdAndUserId() - 存在时返回 true")
    void existsByFamilyIdAndUserId_whenExists_shouldReturnTrue() {
        // Given
        when(familyMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When
        boolean result = familyMemberRepository.existsByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByFamilyIdAndUserId() - 不存在时返回 false")
    void existsByFamilyIdAndUserId_whenNotExists_shouldReturnFalse() {
        // Given
        when(familyMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean result = familyMemberRepository.existsByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== existsByUserIdAndRoleOwner() 测试 ====================

    @Test
    @DisplayName("existsByUserIdAndRoleOwner() - 存在时返回 true")
    void existsByUserIdAndRoleOwner_whenExists_shouldReturnTrue() {
        // Given
        when(familyMemberMapper.countByUserIdAndRoleOwner(TEST_USER_ID)).thenReturn(2);

        // When
        boolean result = familyMemberRepository.existsByUserIdAndRoleOwner(TEST_USER_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndRoleOwner() - 不存在时返回 false")
    void existsByUserIdAndRoleOwner_whenNotExists_shouldReturnFalse() {
        // Given
        when(familyMemberMapper.countByUserIdAndRoleOwner(TEST_USER_ID)).thenReturn(0);

        // When
        boolean result = familyMemberRepository.existsByUserIdAndRoleOwner(TEST_USER_ID);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== findAllByUserIdAndRoleOwner() 测试 ====================

    @Test
    @DisplayName("findAllByUserIdAndRoleOwner() - 应返回主人角色列表")
    void findAllByUserIdAndRoleOwner_shouldReturnOwnerList() {
        // Given
        FamilyMemberDO owner1 = createMemberDO("member-1", "family-1", TEST_USER_ID, "OWNER");
        FamilyMemberDO owner2 = createMemberDO("member-2", "family-2", TEST_USER_ID, "OWNER");
        when(familyMemberMapper.selectAllByUserIdAndRoleOwner(TEST_USER_ID)).thenReturn(List.of(owner1, owner2));

        // When
        List<FamilyMember> result = familyMemberRepository.findAllByUserIdAndRoleOwner(TEST_USER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo(FamilyMember.FamilyRole.OWNER);
        assertThat(result.get(1).getRole()).isEqualTo(FamilyMember.FamilyRole.OWNER);
    }

    // ==================== 转换验证测试 ====================

    @Test
    @DisplayName("转换验证 - 角色枚举应正确转换")
    void conversion_roleEnum_shouldConvertCorrectly() {
        // Given - OWNER
        FamilyMemberDO ownerDO = createMemberDO(TEST_MEMBER_ID, TEST_FAMILY_ID, TEST_USER_ID, "OWNER");
        when(familyMemberMapper.selectById(TEST_MEMBER_ID)).thenReturn(ownerDO);

        // When
        Optional<FamilyMember> ownerResult = familyMemberRepository.findById(TEST_MEMBER_ID);

        // Then
        assertThat(ownerResult).isPresent();
        assertThat(ownerResult.get().getRole()).isEqualTo(FamilyMember.FamilyRole.OWNER);
    }

    @Test
    @DisplayName("转换验证 - 无效角色字符串应返回 null")
    void conversion_invalidRole_shouldReturnNull() {
        // Given
        FamilyMemberDO memberDO = createMemberDO(TEST_MEMBER_ID, TEST_FAMILY_ID, TEST_USER_ID, "INVALID_ROLE");
        when(familyMemberMapper.selectById(TEST_MEMBER_ID)).thenReturn(memberDO);

        // When
        Optional<FamilyMember> result = familyMemberRepository.findById(TEST_MEMBER_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isNull();
    }

    // ==================== 辅助方法 ====================

    private FamilyMember createMember(String id, String familyId, String userId, FamilyMember.FamilyRole role) {
        FamilyMember member = new FamilyMember();
        member.setId(id);
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(Instant.now());
        member.setCreatedAt(Instant.now());
        member.setUpdatedAt(Instant.now());
        return member;
    }

    private FamilyMemberDO createMemberDO(String id, String familyId, String userId, String role) {
        FamilyMemberDO memberDO = new FamilyMemberDO();
        memberDO.setId(id);
        memberDO.setFamilyId(familyId);
        memberDO.setUserId(userId);
        memberDO.setRole(role);
        memberDO.setJoinedAt(LocalDateTime.now());
        memberDO.setCreatedAt(LocalDateTime.now());
        memberDO.setUpdatedAt(LocalDateTime.now());
        return memberDO;
    }
}
