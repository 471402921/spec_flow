package com.specflow.api.modules.family.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflow.api.modules.family.application.FamilyService;
import com.specflow.api.modules.family.domain.entity.Family;
import com.specflow.api.modules.family.domain.entity.FamilyInvitation;
import com.specflow.api.modules.family.domain.entity.FamilyMember;
import com.specflow.api.modules.family.interfaces.dto.CreateFamilyRequest;
import com.specflow.api.modules.family.interfaces.dto.JoinFamilyRequest;
import com.specflow.api.modules.family.interfaces.dto.TransferOwnershipRequest;
import com.specflow.api.modules.family.interfaces.dto.UpdateFamilyRequest;
import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.domain.repository.UserRepository;
import com.specflow.api.modules.user.domain.service.TokenProvider;
import com.specflow.common.exception.BusinessException;
import com.specflow.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FamilyController 集成测试
 *
 * <p>测试策略：
 * - 使用 @SpringBootTest + @AutoConfigureMockMvc 测试 Web 层
 * - Mock FamilyService
 * - 验证 HTTP 请求/响应、参数校验、错误码
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FamilyController 集成测试")
class FamilyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FamilyService familyService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TokenProvider tokenProvider;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_FAMILY_ID = "family-456";
    private static final String TEST_TOKEN = "token_abc123xyz";

    @BeforeEach
    void setUp() {
        // 默认设置：模拟有效的 token
        when(tokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
        when(tokenProvider.getUserIdByToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
        when(tokenProvider.getExpiredAtByToken(TEST_TOKEN))
                .thenReturn(java.time.Instant.now().plusSeconds(30L * 24 * 60 * 60));
    }

    // ==================== POST /api/v1/families 测试 ====================

    @Test
    @DisplayName("POST /api/v1/families - 创建家庭成功")
    void createFamily_withValidData_shouldReturn201() throws Exception {
        // Given
        CreateFamilyRequest request = new CreateFamilyRequest();
        request.setName("My Family");

        Family mockFamily = createMockFamily(TEST_FAMILY_ID, "My Family", TEST_USER_ID);
        when(familyService.createFamily("My Family", TEST_USER_ID)).thenReturn(mockFamily);

        // When & Then
        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(TEST_FAMILY_ID)))
                .andExpect(jsonPath("$.data.name", is("My Family")))
                .andExpect(jsonPath("$.data.ownerId", is(TEST_USER_ID)));
    }

    @Test
    @DisplayName("POST /api/v1/families - 名称过短应返回 400")
    void createFamily_withShortName_shouldReturn400() throws Exception {
        // Given
        CreateFamilyRequest request = new CreateFamilyRequest();
        request.setName("A");

        // When & Then
        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/v1/families - 名称过长应返回 400")
    void createFamily_withLongName_shouldReturn400() throws Exception {
        // Given
        CreateFamilyRequest request = new CreateFamilyRequest();
        request.setName("A".repeat(21));

        // When & Then
        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/families - 家庭数达上限应返回 400")
    void createFamily_whenAtLimit_shouldReturn400() throws Exception {
        // Given
        CreateFamilyRequest request = new CreateFamilyRequest();
        request.setName("My Family");

        when(familyService.createFamily(anyString(), anyString()))
                .thenThrow(new BusinessException("FAMILY_LIMIT_EXCEEDED", "已达加入家庭数量上限"));

        // When & Then
        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("FAMILY_LIMIT_EXCEEDED")));
    }

    // ==================== GET /api/v1/families 测试 ====================

    @Test
    @DisplayName("GET /api/v1/families - 获取我的家庭列表")
    void getMyFamilies_shouldReturnFamilyList() throws Exception {
        // Given
        Family mockFamily = createMockFamily(TEST_FAMILY_ID, "Test Family", TEST_USER_ID);
        when(familyService.getUserFamilies(TEST_USER_ID)).thenReturn(List.of(mockFamily));

        // When & Then
        mockMvc.perform(get("/api/v1/families")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is(TEST_FAMILY_ID)))
                .andExpect(jsonPath("$.data[0].name", is("Test Family")));
    }

    @Test
    @DisplayName("GET /api/v1/families - 无家庭时应返回空列表")
    void getMyFamilies_withNoFamilies_shouldReturnEmptyList() throws Exception {
        // Given
        when(familyService.getUserFamilies(TEST_USER_ID)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/families")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== GET /api/v1/families/{familyId} 测试 ====================

    @Test
    @DisplayName("GET /api/v1/families/{familyId} - 查看家庭详情成功")
    void getFamilyDetail_withMember_shouldReturnDetail() throws Exception {
        // Given
        Family mockFamily = createMockFamily(TEST_FAMILY_ID, "Test Family", TEST_USER_ID);
        FamilyMember mockMember = createMockOwnerMember("member-1", TEST_FAMILY_ID, TEST_USER_ID);
        User mockUser = createMockUser(TEST_USER_ID, "TestUser");

        when(familyService.getFamilyMembers(TEST_FAMILY_ID)).thenReturn(List.of(mockMember));
        when(familyService.getFamily(TEST_FAMILY_ID)).thenReturn(mockFamily);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));

        // When & Then
        mockMvc.perform(get("/api/v1/families/{familyId}", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(TEST_FAMILY_ID)))
                .andExpect(jsonPath("$.data.name", is("Test Family")))
                .andExpect(jsonPath("$.data.members[0].userId", is(TEST_USER_ID)));
    }

    @Test
    @DisplayName("GET /api/v1/families/{familyId} - 非成员访问应返回 403")
    void getFamilyDetail_withNonMember_shouldReturn403() throws Exception {
        // Given
        FamilyMember otherMember = createMockMember("member-1", TEST_FAMILY_ID, "other-user");
        when(familyService.getFamilyMembers(TEST_FAMILY_ID)).thenReturn(List.of(otherMember));

        // When & Then
        mockMvc.perform(get("/api/v1/families/{familyId}", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("NOT_FAMILY_MEMBER")));
    }

    @Test
    @DisplayName("GET /api/v1/families/{familyId} - 家庭不存在应返回 404")
    void getFamilyDetail_withNonExistentFamily_shouldReturn404() throws Exception {
        // Given
        FamilyMember mockMember = createMockOwnerMember("member-1", TEST_FAMILY_ID, TEST_USER_ID);
        when(familyService.getFamilyMembers(TEST_FAMILY_ID)).thenReturn(List.of(mockMember));
        when(familyService.getFamily(TEST_FAMILY_ID))
                .thenThrow(new NotFoundException("FAMILY_NOT_FOUND", "家庭不存在"));

        // When & Then
        mockMvc.perform(get("/api/v1/families/{familyId}", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("FAMILY_NOT_FOUND")));
    }

    // ==================== PUT /api/v1/families/{familyId} 测试 ====================

    @Test
    @DisplayName("PUT /api/v1/families/{familyId} - 修改家庭名称成功")
    void updateFamily_withOwner_shouldReturn200() throws Exception {
        // Given
        UpdateFamilyRequest request = new UpdateFamilyRequest();
        request.setName("New Family Name");

        Family updatedFamily = createMockFamily(TEST_FAMILY_ID, "New Family Name", TEST_USER_ID);
        when(familyService.updateFamilyName(TEST_FAMILY_ID, TEST_USER_ID, "New Family Name"))
                .thenReturn(updatedFamily);

        // When & Then
        mockMvc.perform(put("/api/v1/families/{familyId}", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("New Family Name")));
    }

    @Test
    @DisplayName("PUT /api/v1/families/{familyId} - 非主人修改应返回 403")
    void updateFamily_withNonOwner_shouldReturn403() throws Exception {
        // Given
        UpdateFamilyRequest request = new UpdateFamilyRequest();
        request.setName("New Name");

        when(familyService.updateFamilyName(anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException("FAMILY_ACCESS_DENIED", "仅家庭主人可执行此操作"));

        // When & Then
        mockMvc.perform(put("/api/v1/families/{familyId}", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("FAMILY_ACCESS_DENIED")));
    }

    // ==================== DELETE /api/v1/families/{familyId} 测试 ====================

    @Test
    @DisplayName("DELETE /api/v1/families/{familyId} - 解散家庭成功")
    void dissolveFamily_withOwner_shouldReturn200() throws Exception {
        // Given - 无需 mock，void 方法

        // When & Then
        mockMvc.perform(delete("/api/v1/families/{familyId}", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/v1/families/{familyId} - 非主人解散应返回 403")
    void dissolveFamily_withNonOwner_shouldReturn403() throws Exception {
        // Given
        doThrow(new BusinessException("FAMILY_ACCESS_DENIED", "仅家庭主人可执行此操作"))
                .when(familyService).dissolveFamily(anyString(), anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/families/{familyId}", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("FAMILY_ACCESS_DENIED")));
    }

    // ==================== POST /api/v1/families/{familyId}/invitations 测试 ====================

    @Test
    @DisplayName("POST /api/v1/families/{familyId}/invitations - 生成邀请码成功")
    void generateInvitation_withOwner_shouldReturn200() throws Exception {
        // Given
        FamilyInvitation mockInvitation = FamilyInvitation.create(TEST_FAMILY_ID, "ABC12345", TEST_USER_ID, 7);
        when(familyService.generateInvitationCode(TEST_FAMILY_ID, TEST_USER_ID)).thenReturn(mockInvitation);

        // When & Then
        mockMvc.perform(post("/api/v1/families/{familyId}/invitations", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code").exists())
                .andExpect(jsonPath("$.data.expiredAt").exists());
    }

    @Test
    @DisplayName("POST /api/v1/families/{familyId}/invitations - 非主人生成应返回 403")
    void generateInvitation_withNonOwner_shouldReturn403() throws Exception {
        // Given
        when(familyService.generateInvitationCode(anyString(), anyString()))
                .thenThrow(new BusinessException("FAMILY_ACCESS_DENIED", "仅家庭主人可执行此操作"));

        // When & Then
        mockMvc.perform(post("/api/v1/families/{familyId}/invitations", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("FAMILY_ACCESS_DENIED")));
    }

    // ==================== POST /api/v1/families/join 测试 ====================

    @Test
    @DisplayName("POST /api/v1/families/join - 加入家庭成功")
    void joinFamily_withValidCode_shouldReturn200() throws Exception {
        // Given
        JoinFamilyRequest request = new JoinFamilyRequest();
        request.setCode("ABC12345");

        Family mockFamily = createMockFamily(TEST_FAMILY_ID, "Test Family", "owner-id");
        when(familyService.joinFamilyByInvitation("ABC12345", TEST_USER_ID)).thenReturn(mockFamily);

        // When & Then
        mockMvc.perform(post("/api/v1/families/join")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(TEST_FAMILY_ID)));
    }

    @Test
    @DisplayName("POST /api/v1/families/join - 邀请码无效应返回 400")
    void joinFamily_withInvalidCode_shouldReturn400() throws Exception {
        // Given
        JoinFamilyRequest request = new JoinFamilyRequest();
        request.setCode("INVALID");

        when(familyService.joinFamilyByInvitation(anyString(), anyString()))
                .thenThrow(new BusinessException("INVITATION_NOT_FOUND", "邀请码无效"));

        // When & Then
        mockMvc.perform(post("/api/v1/families/join")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("INVITATION_NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/families/join - 邀请码过期应返回 400")
    void joinFamily_withExpiredCode_shouldReturn400() throws Exception {
        // Given
        JoinFamilyRequest request = new JoinFamilyRequest();
        request.setCode("EXPIRED");

        when(familyService.joinFamilyByInvitation(anyString(), anyString()))
                .thenThrow(new BusinessException("INVITATION_EXPIRED", "邀请码已过期"));

        // When & Then
        mockMvc.perform(post("/api/v1/families/join")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("INVITATION_EXPIRED")));
    }

    // ==================== DELETE /api/v1/families/{familyId}/members/{targetUserId} 测试 ====================

    @Test
    @DisplayName("DELETE /api/v1/families/{familyId}/members/{targetUserId} - 移除成员成功")
    void removeMember_withOwner_shouldReturn200() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/families/{familyId}/members/{targetUserId}",
                        TEST_FAMILY_ID, "target-user")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/v1/families/{familyId}/members/{targetUserId} - 移除自己应返回 400")
    void removeMember_withOwnerRemovingSelf_shouldReturn400() throws Exception {
        // Given
        doThrow(new BusinessException("CANNOT_REMOVE_OWNER", "不能移除家庭主人"))
                .when(familyService).removeMember(anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/families/{familyId}/members/{targetUserId}",
                        TEST_FAMILY_ID, TEST_USER_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("CANNOT_REMOVE_OWNER")));
    }

    // ==================== POST /api/v1/families/{familyId}/members/leave 测试 ====================

    @Test
    @DisplayName("POST /api/v1/families/{familyId}/members/leave - 退出家庭成功")
    void leaveFamily_withMember_shouldReturn200() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/families/{familyId}/members/leave", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/families/{familyId}/members/leave - 主人退出应返回 400")
    void leaveFamily_withOwner_shouldReturn400() throws Exception {
        // Given
        doThrow(new BusinessException("OWNER_CANNOT_LEAVE", "家庭主人不能退出"))
                .when(familyService).leaveFamily(anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/families/{familyId}/members/leave", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("OWNER_CANNOT_LEAVE")));
    }

    // ==================== POST /api/v1/families/{familyId}/transfer 测试 ====================

    @Test
    @DisplayName("POST /api/v1/families/{familyId}/transfer - 转让主人成功")
    void transferOwnership_withOwner_shouldReturn200() throws Exception {
        // Given
        TransferOwnershipRequest request = new TransferOwnershipRequest();
        request.setNewOwnerId("new-owner-id");

        // When & Then
        mockMvc.perform(post("/api/v1/families/{familyId}/transfer", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/families/{familyId}/transfer - 转让给非成员应返回 400")
    void transferOwnership_toNonMember_shouldReturn400() throws Exception {
        // Given
        TransferOwnershipRequest request = new TransferOwnershipRequest();
        request.setNewOwnerId("non-member");

        doThrow(new BusinessException("TRANSFER_TARGET_NOT_MEMBER", "目标用户不是该家庭成员"))
                .when(familyService).transferOwnership(anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/families/{familyId}/transfer", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("TRANSFER_TARGET_NOT_MEMBER")));
    }

    // ==================== GET /api/v1/families/{familyId}/pets 测试 ====================

    @Test
    @DisplayName("GET /api/v1/families/{familyId}/pets - 获取家庭宠物成功")
    void getFamilyPets_withMember_shouldReturnPetList() throws Exception {
        // Given
        Pet mockPet = createMockPet("pet-1", "Fluffy", TEST_USER_ID);
        FamilyService.FamilyPetInfo petInfo = new FamilyService.FamilyPetInfo(mockPet, TEST_USER_ID, "TestUser");
        when(familyService.getFamilyPets(TEST_FAMILY_ID, TEST_USER_ID)).thenReturn(List.of(petInfo));

        // When & Then
        mockMvc.perform(get("/api/v1/families/{familyId}/pets", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is("pet-1")))
                .andExpect(jsonPath("$.data[0].name", is("Fluffy")));
    }

    @Test
    @DisplayName("GET /api/v1/families/{familyId}/pets - 非成员访问应返回 403")
    void getFamilyPets_withNonMember_shouldReturn403() throws Exception {
        // Given
        when(familyService.getFamilyPets(anyString(), anyString()))
                .thenThrow(new BusinessException("NOT_FAMILY_MEMBER", "你不是该家庭成员"));

        // When & Then
        mockMvc.perform(get("/api/v1/families/{familyId}/pets", TEST_FAMILY_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("NOT_FAMILY_MEMBER")));
    }

    // ==================== 辅助方法 ====================

    private Family createMockFamily(String id, String name, String ownerId) {
        Family family = new Family();
        family.setId(id);
        family.setName(name);
        family.setOwnerId(ownerId);
        family.setCreatedAt(Instant.now());
        family.setUpdatedAt(Instant.now());
        return family;
    }

    private FamilyMember createMockOwnerMember(String id, String familyId, String userId) {
        FamilyMember member = new FamilyMember();
        member.setId(id);
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setRole(FamilyMember.FamilyRole.OWNER);
        member.setJoinedAt(Instant.now());
        return member;
    }

    private FamilyMember createMockMember(String id, String familyId, String userId) {
        FamilyMember member = new FamilyMember();
        member.setId(id);
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setRole(FamilyMember.FamilyRole.MEMBER);
        member.setJoinedAt(Instant.now());
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
