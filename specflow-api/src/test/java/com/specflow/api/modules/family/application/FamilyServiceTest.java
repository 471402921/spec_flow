package com.specflow.api.modules.family.application;

import com.specflow.api.modules.family.domain.entity.Family;
import com.specflow.api.modules.family.domain.entity.FamilyInvitation;
import com.specflow.api.modules.family.domain.entity.FamilyMember;
import com.specflow.api.modules.family.domain.repository.FamilyInvitationRepository;
import com.specflow.api.modules.family.domain.repository.FamilyMemberRepository;
import com.specflow.api.modules.family.domain.repository.FamilyRepository;
import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.domain.repository.PetRepository;
import com.specflow.api.modules.user.domain.repository.UserRepository;
import com.specflow.common.exception.BusinessException;
import com.specflow.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FamilyService 单元测试
 *
 * <p>测试策略：
 * - 使用 Mockito 模拟所有 Repository
 * - 专注于业务逻辑测试，不涉及数据库
 * - 验证正常流程和异常场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyService 单元测试")
class FamilyServiceTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private FamilyInvitationRepository familyInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private FamilyService familyService;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_FAMILY_ID = "family-456";
    private static final String TEST_MEMBER_ID = "member-789";

    private Family mockFamily;
    private FamilyMember mockOwnerMember;
    private FamilyMember mockMember;

    @BeforeEach
    void setUp() {
        mockFamily = createMockFamily(TEST_FAMILY_ID, "Test Family", TEST_USER_ID);
        mockOwnerMember = createMockOwnerMember(TEST_MEMBER_ID, TEST_FAMILY_ID, TEST_USER_ID);
        mockMember = createMockMember("member-999", TEST_FAMILY_ID, "user-999");
    }

    // ==================== createFamily() 测试 ====================

    @Test
    @DisplayName("创建家庭 - 名称有效且未达上限时应成功")
    void createFamily_withValidNameAndUnderLimit_shouldSucceed() {
        // Given
        when(familyMemberRepository.countByUserId(TEST_USER_ID)).thenReturn(2L);
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(familyMemberRepository.save(any(FamilyMember.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Family result = familyService.createFamily("My Family", TEST_USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("My Family");
        assertThat(result.getOwnerId()).isEqualTo(TEST_USER_ID);
        verify(familyRepository).save(any(Family.class));
        verify(familyMemberRepository).save(any(FamilyMember.class));
    }

    @Test
    @DisplayName("创建家庭 - 名称为空时应抛出异常")
    void createFamily_withNullName_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> familyService.createFamily(null, TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("家庭名称长度需在2-20个字符之间");
    }

    @Test
    @DisplayName("创建家庭 - 名称过短时应抛出异常")
    void createFamily_withShortName_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> familyService.createFamily("A", TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("家庭名称长度需在2-20个字符之间");
    }

    @Test
    @DisplayName("创建家庭 - 名称过长时应抛出异常")
    void createFamily_withLongName_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> familyService.createFamily("A".repeat(21), TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("家庭名称长度需在2-20个字符之间");
    }

    @Test
    @DisplayName("创建家庭 - 已达上限时应抛出异常")
    void createFamily_whenAtLimit_shouldThrowException() {
        // Given
        when(familyMemberRepository.countByUserId(TEST_USER_ID)).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> familyService.createFamily("My Family", TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已达加入家庭数量上限");
    }

    // ==================== getFamily() 测试 ====================

    @Test
    @DisplayName("获取家庭 - 家庭存在时应返回")
    void getFamily_whenExists_shouldReturnFamily() {
        // Given
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // When
        Family result = familyService.getFamily(TEST_FAMILY_ID);

        // Then
        assertThat(result).isEqualTo(mockFamily);
    }

    @Test
    @DisplayName("获取家庭 - 家庭不存在时应抛出 NotFoundException")
    void getFamily_whenNotExists_shouldThrowNotFoundException() {
        // Given
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> familyService.getFamily(TEST_FAMILY_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("家庭不存在");
    }

    // ==================== updateFamilyName() 测试 ====================

    @Test
    @DisplayName("修改家庭名称 - 主人操作时成功")
    void updateFamilyName_withOwner_shouldSucceed() {
        // Given
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(mockOwnerMember));
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Family result = familyService.updateFamilyName(TEST_FAMILY_ID, TEST_USER_ID, "New Name");

        // Then
        assertThat(result.getName()).isEqualTo("New Name");
        verify(familyRepository).save(any(Family.class));
    }

    @Test
    @DisplayName("修改家庭名称 - 非主人操作时应抛出异常")
    void updateFamilyName_withNonOwner_shouldThrowException() {
        // Given
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, "user-999"))
                .thenReturn(Optional.of(mockMember));

        // When & Then
        assertThatThrownBy(() -> familyService.updateFamilyName(TEST_FAMILY_ID, "user-999", "New Name"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅家庭主人可执行此操作");
    }

    // ==================== dissolveFamily() 测试 ====================

    @Test
    @DisplayName("解散家庭 - 主人操作时成功")
    void dissolveFamily_withOwner_shouldSucceed() {
        // Given
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(mockOwnerMember));

        // When
        familyService.dissolveFamily(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        verify(familyInvitationRepository).deleteAllByFamilyId(TEST_FAMILY_ID);
        verify(familyMemberRepository).deleteAllByFamilyId(TEST_FAMILY_ID);
        verify(familyRepository).deleteById(TEST_FAMILY_ID);
    }

    @Test
    @DisplayName("解散家庭 - 非主人操作时应抛出异常")
    void dissolveFamily_withNonOwner_shouldThrowException() {
        // Given
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, "user-999"))
                .thenReturn(Optional.of(mockMember));

        // When & Then
        assertThatThrownBy(() -> familyService.dissolveFamily(TEST_FAMILY_ID, "user-999"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅家庭主人可执行此操作");
    }

    // ==================== removeMember() 测试 ====================

    @Test
    @DisplayName("移除成员 - 主人移除其他成员时应成功")
    void removeMember_withOwnerRemovingOther_shouldSucceed() {
        // Given
        String targetUserId = "user-999";
        FamilyMember targetMember = createMockMember("member-999", TEST_FAMILY_ID, targetUserId);

        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(mockOwnerMember));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, targetUserId))
                .thenReturn(Optional.of(targetMember));

        // When
        familyService.removeMember(TEST_FAMILY_ID, TEST_USER_ID, targetUserId);

        // Then
        verify(familyMemberRepository).deleteById(targetMember.getId());
    }

    @Test
    @DisplayName("移除成员 - 主人不能移除自己")
    void removeMember_withOwnerRemovingSelf_shouldThrowException() {
        // Given
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(mockOwnerMember));

        // When & Then
        assertThatThrownBy(() -> familyService.removeMember(TEST_FAMILY_ID, TEST_USER_ID, TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能移除家庭主人");
    }

    @Test
    @DisplayName("移除成员 - 非主人操作时应抛出异常")
    void removeMember_withNonOwner_shouldThrowException() {
        // Given
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, "user-999"))
                .thenReturn(Optional.of(mockMember));

        // When & Then
        assertThatThrownBy(() -> familyService.removeMember(TEST_FAMILY_ID, "user-999", TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅家庭主人可执行此操作");
    }

    // ==================== leaveFamily() 测试 ====================

    @Test
    @DisplayName("退出家庭 - 普通成员可以退出")
    void leaveFamily_withMember_shouldSucceed() {
        // Given
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, "user-999"))
                .thenReturn(Optional.of(mockMember));

        // When
        familyService.leaveFamily(TEST_FAMILY_ID, "user-999");

        // Then
        verify(familyMemberRepository).deleteById(mockMember.getId());
    }

    @Test
    @DisplayName("退出家庭 - 主人不能退出")
    void leaveFamily_withOwner_shouldThrowException() {
        // Given
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(mockOwnerMember));

        // When & Then
        assertThatThrownBy(() -> familyService.leaveFamily(TEST_FAMILY_ID, TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("家庭主人不能退出");
    }

    // ==================== transferOwnership() 测试 ====================

    @Test
    @DisplayName("转让主人 - 主人转让给成员时应成功")
    void transferOwnership_withOwnerToMember_shouldSucceed() {
        // Given
        String newOwnerId = "user-999";
        FamilyMember newOwnerMember = createMockMember("member-999", TEST_FAMILY_ID, newOwnerId);

        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(mockOwnerMember));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, newOwnerId))
                .thenReturn(Optional.of(newOwnerMember));
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(familyMemberRepository.save(any(FamilyMember.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        familyService.transferOwnership(TEST_FAMILY_ID, TEST_USER_ID, newOwnerId);

        // Then
        verify(familyRepository).save(any(Family.class));
        verify(familyMemberRepository, org.mockito.Mockito.times(2)).save(any(FamilyMember.class));
    }

    @Test
    @DisplayName("转让主人 - 非主人操作时应抛出异常")
    void transferOwnership_withNonOwner_shouldThrowException() {
        // Given
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, "user-999"))
                .thenReturn(Optional.of(mockMember));

        // When & Then
        assertThatThrownBy(() -> familyService.transferOwnership(TEST_FAMILY_ID, "user-999", TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅家庭主人可执行此操作");
    }

    @Test
    @DisplayName("转让主人 - 转让给非成员时应抛出异常")
    void transferOwnership_toNonMember_shouldThrowException() {
        // Given
        String nonMemberId = "user-non-member";

        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(mockOwnerMember));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, nonMemberId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> familyService.transferOwnership(TEST_FAMILY_ID, TEST_USER_ID, nonMemberId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目标用户不是该家庭成员");
    }

    // ==================== getUserFamilies() 测试 ====================

    @Test
    @DisplayName("获取用户家庭列表 - 应返回家庭列表")
    void getUserFamilies_shouldReturnFamilyList() {
        // Given
        FamilyMember membership = createMockMember("member-1", TEST_FAMILY_ID, TEST_USER_ID);
        when(familyMemberRepository.findAllByUserId(TEST_USER_ID)).thenReturn(List.of(membership));
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // When
        List<Family> result = familyService.getUserFamilies(TEST_USER_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(mockFamily);
    }

    @Test
    @DisplayName("获取用户家庭列表 - 家庭不存在时应过滤掉")
    void getUserFamilies_withMissingFamily_shouldFilterOut() {
        // Given
        FamilyMember membership = createMockMember("member-1", "missing-family", TEST_USER_ID);
        when(familyMemberRepository.findAllByUserId(TEST_USER_ID)).thenReturn(List.of(membership));
        when(familyRepository.findById("missing-family")).thenReturn(Optional.empty());

        // When
        List<Family> result = familyService.getUserFamilies(TEST_USER_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== getFamilyMembers() 测试 ====================

    @Test
    @DisplayName("获取家庭成员列表 - 应返回成员列表")
    void getFamilyMembers_shouldReturnMemberList() {
        // Given
        when(familyMemberRepository.findAllByFamilyId(TEST_FAMILY_ID))
                .thenReturn(List.of(mockOwnerMember, mockMember));

        // When
        List<FamilyMember> result = familyService.getFamilyMembers(TEST_FAMILY_ID);

        // Then
        assertThat(result).hasSize(2);
    }

    // ==================== generateInvitationCode() 测试 ====================

    @Test
    @DisplayName("生成邀请码 - 主人操作时应成功")
    void generateInvitationCode_withOwner_shouldSucceed() {
        // Given
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(mockOwnerMember));
        when(familyInvitationRepository.findAllByFamilyIdAndRevokedFalse(TEST_FAMILY_ID))
                .thenReturn(Collections.emptyList());
        when(familyInvitationRepository.existsByCode(anyString())).thenReturn(false);
        when(familyInvitationRepository.save(any(FamilyInvitation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        FamilyInvitation result = familyService.generateInvitationCode(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).hasSize(8);
        verify(familyInvitationRepository).save(any(FamilyInvitation.class));
    }

    @Test
    @DisplayName("生成邀请码 - 非主人操作时应抛出异常")
    void generateInvitationCode_withNonOwner_shouldThrowException() {
        // Given
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));
        when(familyMemberRepository.findByFamilyIdAndUserId(TEST_FAMILY_ID, "user-999"))
                .thenReturn(Optional.of(mockMember));

        // When & Then
        assertThatThrownBy(() -> familyService.generateInvitationCode(TEST_FAMILY_ID, "user-999"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅家庭主人可执行此操作");
    }

    // ==================== joinFamilyByInvitation() 测试 ====================

    @Test
    @DisplayName("通过邀请码加入 - 有效邀请码且满足条件时应成功")
    void joinFamilyByInvitation_withValidCode_shouldSucceed() {
        // Given
        String code = "ABC12345";
        FamilyInvitation invitation = FamilyInvitation.create(TEST_FAMILY_ID, code, TEST_USER_ID, 7);

        when(familyInvitationRepository.findByCode(code.toUpperCase()))
                .thenReturn(Optional.of(invitation));
        when(familyMemberRepository.existsByFamilyIdAndUserId(TEST_FAMILY_ID, "new-user"))
                .thenReturn(false);
        when(familyMemberRepository.countByUserId("new-user")).thenReturn(0L);
        when(familyMemberRepository.countByFamilyId(TEST_FAMILY_ID)).thenReturn(2L);
        when(familyMemberRepository.save(any(FamilyMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(familyRepository.findById(TEST_FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // When
        Family result = familyService.joinFamilyByInvitation(code, "new-user");

        // Then
        assertThat(result).isEqualTo(mockFamily);
        verify(familyMemberRepository).save(any(FamilyMember.class));
    }

    @Test
    @DisplayName("通过邀请码加入 - 邀请码不存在时应抛出异常")
    void joinFamilyByInvitation_withInvalidCode_shouldThrowException() {
        // Given
        when(familyInvitationRepository.findByCode(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> familyService.joinFamilyByInvitation("INVALID", "new-user"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码无效");
    }

    @Test
    @DisplayName("通过邀请码加入 - 邀请码已撤销时应抛出异常")
    void joinFamilyByInvitation_withRevokedCode_shouldThrowException() {
        // Given
        String code = "ABC12345";
        FamilyInvitation invitation = FamilyInvitation.create(TEST_FAMILY_ID, code, TEST_USER_ID, 7);
        invitation.revoke();

        when(familyInvitationRepository.findByCode(code.toUpperCase()))
                .thenReturn(Optional.of(invitation));

        // When & Then
        assertThatThrownBy(() -> familyService.joinFamilyByInvitation(code, "new-user"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码已过期");
    }

    @Test
    @DisplayName("通过邀请码加入 - 用户已是成员时应抛出异常")
    void joinFamilyByInvitation_whenAlreadyMember_shouldThrowException() {
        // Given
        String code = "ABC12345";
        FamilyInvitation invitation = FamilyInvitation.create(TEST_FAMILY_ID, code, TEST_USER_ID, 7);

        when(familyInvitationRepository.findByCode(code.toUpperCase()))
                .thenReturn(Optional.of(invitation));
        when(familyMemberRepository.existsByFamilyIdAndUserId(TEST_FAMILY_ID, "existing-user"))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> familyService.joinFamilyByInvitation(code, "existing-user"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("你已是该家庭成员");
    }

    @Test
    @DisplayName("通过邀请码加入 - 用户家庭数达上限时应抛出异常")
    void joinFamilyByInvitation_whenUserAtLimit_shouldThrowException() {
        // Given
        String code = "ABC12345";
        FamilyInvitation invitation = FamilyInvitation.create(TEST_FAMILY_ID, code, TEST_USER_ID, 7);

        when(familyInvitationRepository.findByCode(code.toUpperCase()))
                .thenReturn(Optional.of(invitation));
        when(familyMemberRepository.existsByFamilyIdAndUserId(TEST_FAMILY_ID, "limited-user"))
                .thenReturn(false);
        when(familyMemberRepository.countByUserId("limited-user")).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> familyService.joinFamilyByInvitation(code, "limited-user"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已达加入家庭数量上限");
    }

    @Test
    @DisplayName("通过邀请码加入 - 家庭成员数达上限时应抛出异常")
    void joinFamilyByInvitation_whenFamilyAtLimit_shouldThrowException() {
        // Given
        String code = "ABC12345";
        FamilyInvitation invitation = FamilyInvitation.create(TEST_FAMILY_ID, code, TEST_USER_ID, 7);

        when(familyInvitationRepository.findByCode(code.toUpperCase()))
                .thenReturn(Optional.of(invitation));
        when(familyMemberRepository.existsByFamilyIdAndUserId(TEST_FAMILY_ID, "new-user"))
                .thenReturn(false);
        when(familyMemberRepository.countByUserId("new-user")).thenReturn(0L);
        when(familyMemberRepository.countByFamilyId(TEST_FAMILY_ID)).thenReturn(10L);

        // When & Then
        assertThatThrownBy(() -> familyService.joinFamilyByInvitation(code, "new-user"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("家庭成员已满");
    }

    // ==================== getFamilyPets() 测试 ====================

    @Test
    @DisplayName("获取家庭宠物 - 成员查询时应返回宠物列表")
    void getFamilyPets_withMember_shouldReturnPetList() {
        // Given
        String memberUserId = "member-user-1";
        User mockUser = createMockUser(memberUserId, "User1");
        Pet mockPet = createMockPet("pet-1", "Pet1", memberUserId);
        FamilyMember member = createMockMember("member-1", TEST_FAMILY_ID, memberUserId);

        when(familyMemberRepository.existsByFamilyIdAndUserId(TEST_FAMILY_ID, TEST_USER_ID))
                .thenReturn(true);
        when(familyMemberRepository.findAllByFamilyId(TEST_FAMILY_ID))
                .thenReturn(List.of(member));
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(mockUser));
        when(petRepository.findByOwnerId(memberUserId)).thenReturn(List.of(mockPet));

        // When
        List<FamilyService.FamilyPetInfo> result = familyService.getFamilyPets(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).pet()).isEqualTo(mockPet);
        assertThat(result.get(0).ownerId()).isEqualTo(memberUserId);
    }

    @Test
    @DisplayName("获取家庭宠物 - 非成员查询时应抛出异常")
    void getFamilyPets_withNonMember_shouldThrowException() {
        // Given
        when(familyMemberRepository.existsByFamilyIdAndUserId(TEST_FAMILY_ID, "non-member"))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> familyService.getFamilyPets(TEST_FAMILY_ID, "non-member"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("你不是该家庭成员");
    }

    // ==================== 辅助方法 ====================

    private Family createMockFamily(String id, String name, String ownerId) {
        Family family = new Family();
        family.setId(id);
        family.setName(name);
        family.setOwnerId(ownerId);
        family.setCreatedAt(java.time.Instant.now());
        family.setUpdatedAt(java.time.Instant.now());
        return family;
    }

    private FamilyMember createMockOwnerMember(String id, String familyId, String userId) {
        FamilyMember member = new FamilyMember();
        member.setId(id);
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setRole(FamilyMember.FamilyRole.OWNER);
        member.setJoinedAt(java.time.Instant.now());
        member.setCreatedAt(java.time.Instant.now());
        member.setUpdatedAt(java.time.Instant.now());
        return member;
    }

    private FamilyMember createMockMember(String id, String familyId, String userId) {
        FamilyMember member = new FamilyMember();
        member.setId(id);
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setRole(FamilyMember.FamilyRole.MEMBER);
        member.setJoinedAt(java.time.Instant.now());
        member.setCreatedAt(java.time.Instant.now());
        member.setUpdatedAt(java.time.Instant.now());
        return member;
    }

    private User createMockUser(String id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }

    private Pet createMockPet(String id, String name, String ownerId) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName(name);
        pet.setOwnerId(ownerId);
        return pet;
    }
}
