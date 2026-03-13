package com.specflow.api.modules.user.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflow.api.modules.user.application.PetService;
import com.specflow.api.modules.user.domain.service.TokenProvider;
import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.interfaces.dto.AddPetRequest;
import com.specflow.api.modules.user.interfaces.dto.UpdatePetRequest;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
 * PetController 集成测试
 *
 * <p>测试策略：
 * - 使用 @SpringBootTest + @AutoConfigureMockMvc 测试 Web 层
 * - Mock PetService
 * - 验证 HTTP 请求/响应、参数校验、错误码
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PetController 集成测试")
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PetService petService;

    @MockBean
    private TokenProvider tokenProvider;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_PET_ID = "pet-456";
    private static final String TEST_TOKEN = "token_abc123xyz";

    @BeforeEach
    void setUp() {
        when(tokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
        when(tokenProvider.getUserIdByToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
    }

    // ==================== addPet() 测试 ====================

    @Test
    @DisplayName("POST /api/v1/pets - 成功添加宠物")
    void addPet_withValidData_shouldReturn201() throws Exception {
        // Given
        AddPetRequest request = new AddPetRequest();
        request.setName("Buddy");
        request.setSpecies(AddPetRequest.Species.DOG);
        request.setBreed("Golden Retriever");
        request.setGender(AddPetRequest.Gender.MALE);
        request.setBirthday(LocalDate.of(2020, 1, 1));

        when(petService.findRestorablePets(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        Pet mockPet = createMockPet(TEST_PET_ID, "Buddy", Pet.Species.DOG);
        when(petService.addPet(anyString(), anyString(), any(), anyString(), any(), any(), any()))
                .thenReturn(mockPet);

        // When & Then
        mockMvc.perform(post("/api/v1/pets")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(TEST_PET_ID)))
                .andExpect(jsonPath("$.data.name", is("Buddy")))
                .andExpect(jsonPath("$.data.species", is("DOG")));
    }

    @Test
    @DisplayName("POST /api/v1/pets - 发现可恢复的已删除宠物")
    void addPet_withRestorablePets_shouldReturn200WithMatches() throws Exception {
        // Given
        AddPetRequest request = new AddPetRequest();
        request.setName("Buddy");
        request.setSpecies(AddPetRequest.Species.DOG);
        request.setBreed("Golden Retriever");
        request.setGender(AddPetRequest.Gender.MALE);

        Pet deletedPet = createMockPet("deleted-pet-789", "Buddy", Pet.Species.DOG);
        deletedPet.softDelete();
        when(petService.findRestorablePets(TEST_USER_ID, "Buddy", Pet.Species.DOG))
                .thenReturn(List.of(deletedPet));

        // When & Then
        mockMvc.perform(post("/api/v1/pets")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("PET_DELETED_MATCH_FOUND")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Buddy")));
    }

    @Test
    @DisplayName("POST /api/v1/pets - 宠物数量已达上限")
    void addPet_whenLimitReached_shouldReturn400() throws Exception {
        // Given
        AddPetRequest request = new AddPetRequest();
        request.setName("Buddy");
        request.setSpecies(AddPetRequest.Species.DOG);
        request.setBreed("Golden Retriever");
        request.setGender(AddPetRequest.Gender.MALE);

        when(petService.findRestorablePets(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());
        when(petService.addPet(anyString(), anyString(), any(), anyString(), any(), any(), any()))
                .thenThrow(new BusinessException("PET_LIMIT_EXCEEDED", "已达宠物数量上限"));

        // When & Then
        mockMvc.perform(post("/api/v1/pets")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PET_LIMIT_EXCEEDED")));
    }

    @Test
    @DisplayName("POST /api/v1/pets - 必填字段缺失")
    void addPet_withMissingRequiredFields_shouldReturn400() throws Exception {
        // Given - 缺少 name
        AddPetRequest request = new AddPetRequest();
        request.setSpecies(AddPetRequest.Species.DOG);
        request.setBreed("Golden Retriever");
        request.setGender(AddPetRequest.Gender.MALE);

        // When & Then
        mockMvc.perform(post("/api/v1/pets")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==================== getPets() 测试 ====================

    @Test
    @DisplayName("GET /api/v1/pets - 获取宠物列表")
    void getPets_withPets_shouldReturnList() throws Exception {
        // Given
        Pet pet1 = createMockPet("pet-1", "Buddy", Pet.Species.DOG);
        Pet pet2 = createMockPet("pet-2", "Kitty", Pet.Species.CAT);
        when(petService.getPetsByOwner(TEST_USER_ID)).thenReturn(List.of(pet1, pet2));

        // When & Then
        mockMvc.perform(get("/api/v1/pets")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name", is("Buddy")))
                .andExpect(jsonPath("$.data[1].name", is("Kitty")));
    }

    @Test
    @DisplayName("GET /api/v1/pets - 无宠物时返回空列表")
    void getPets_noPets_shouldReturnEmptyList() throws Exception {
        // Given
        when(petService.getPetsByOwner(TEST_USER_ID)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/pets")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==================== getPet() 测试 ====================

    @Test
    @DisplayName("GET /api/v1/pets/{petId} - 获取宠物详情")
    void getPet_withValidId_shouldReturn200() throws Exception {
        // Given
        Pet mockPet = createMockPet(TEST_PET_ID, "Buddy", Pet.Species.DOG);
        when(petService.getPet(TEST_USER_ID, TEST_PET_ID)).thenReturn(mockPet);

        // When & Then
        mockMvc.perform(get("/api/v1/pets/{petId}", TEST_PET_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(TEST_PET_ID)))
                .andExpect(jsonPath("$.data.name", is("Buddy")))
                .andExpect(jsonPath("$.data.species", is("DOG")));
    }

    @Test
    @DisplayName("GET /api/v1/pets/{petId} - 宠物不存在")
    void getPet_withNonExistentId_shouldReturn404() throws Exception {
        // Given
        when(petService.getPet(anyString(), anyString()))
                .thenThrow(new NotFoundException("宠物不存在"));

        // When & Then
        mockMvc.perform(get("/api/v1/pets/{petId}", "non-existent")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    // ==================== updatePet() 测试 ====================

    @Test
    @DisplayName("PUT /api/v1/pets/{petId} - 成功更新宠物")
    void updatePet_withValidData_shouldReturn200() throws Exception {
        // Given
        UpdatePetRequest request = new UpdatePetRequest();
        request.setName("NewName");
        request.setSpecies(UpdatePetRequest.Species.CAT);
        request.setBreed("Siamese");
        request.setGender(UpdatePetRequest.Gender.FEMALE);

        Pet updatedPet = createMockPet(TEST_PET_ID, "NewName", Pet.Species.CAT);
        when(petService.updatePet(anyString(), anyString(), any())).thenReturn(updatedPet);

        // When & Then
        mockMvc.perform(put("/api/v1/pets/{petId}", TEST_PET_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("NewName")))
                .andExpect(jsonPath("$.data.species", is("CAT")));
    }

    @Test
    @DisplayName("PUT /api/v1/pets/{petId} - 非主人操作")
    void updatePet_withDifferentOwner_shouldReturn403() throws Exception {
        // Given
        UpdatePetRequest request = new UpdatePetRequest();
        request.setName("NewName");
        request.setSpecies(UpdatePetRequest.Species.DOG);
        request.setBreed("Breed");
        request.setGender(UpdatePetRequest.Gender.MALE);

        when(petService.updatePet(anyString(), anyString(), any()))
                .thenThrow(new BusinessException("PET_ACCESS_DENIED", "无权限"));

        // When & Then
        mockMvc.perform(put("/api/v1/pets/{petId}", TEST_PET_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PET_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PUT /api/v1/pets/{petId} - 宠物不存在")
    void updatePet_withNonExistentPet_shouldReturn404() throws Exception {
        // Given
        UpdatePetRequest request = new UpdatePetRequest();
        request.setName("NewName");
        request.setSpecies(UpdatePetRequest.Species.DOG);
        request.setBreed("Breed");
        request.setGender(UpdatePetRequest.Gender.MALE);

        when(petService.updatePet(anyString(), anyString(), any()))
                .thenThrow(new NotFoundException("宠物不存在"));

        // When & Then
        mockMvc.perform(put("/api/v1/pets/{petId}", "non-existent")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    // ==================== deletePet() 测试 ====================

    @Test
    @DisplayName("DELETE /api/v1/pets/{petId} - 成功删除宠物")
    void deletePet_withValidId_shouldReturn200() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/pets/{petId}", TEST_PET_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/v1/pets/{petId} - 非主人操作")
    void deletePet_withDifferentOwner_shouldReturn403() throws Exception {
        // Given
        doThrow(new BusinessException("PET_ACCESS_DENIED", "无权限"))
                .when(petService).deletePet(anyString(), anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/pets/{petId}", TEST_PET_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PET_ACCESS_DENIED")));
    }

    // ==================== restorePet() 测试 ====================

    @Test
    @DisplayName("POST /api/v1/pets/{petId}/restore - 成功恢复宠物")
    void restorePet_withValidId_shouldReturn200() throws Exception {
        // Given
        Pet restoredPet = createMockPet(TEST_PET_ID, "Buddy", Pet.Species.DOG);
        when(petService.restorePet(TEST_USER_ID, TEST_PET_ID)).thenReturn(restoredPet);

        // When & Then
        mockMvc.perform(post("/api/v1/pets/{petId}/restore", TEST_PET_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(TEST_PET_ID)));
    }

    @Test
    @DisplayName("POST /api/v1/pets/{petId}/restore - 宠物不存在")
    void restorePet_withNonExistentPet_shouldReturn404() throws Exception {
        // Given
        when(petService.restorePet(anyString(), anyString()))
                .thenThrow(new NotFoundException("宠物不存在"));

        // When & Then
        mockMvc.perform(post("/api/v1/pets/{petId}/restore", "non-existent")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/pets/{petId}/restore - 非主人操作")
    void restorePet_withDifferentOwner_shouldReturn403() throws Exception {
        // Given
        when(petService.restorePet(anyString(), anyString()))
                .thenThrow(new BusinessException("PET_ACCESS_DENIED", "无权限"));

        // When & Then
        mockMvc.perform(post("/api/v1/pets/{petId}/restore", TEST_PET_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PET_ACCESS_DENIED")));
    }

    // ==================== 辅助方法 ====================

    private Pet createMockPet(String id, String name, Pet.Species species) {
        Pet pet = Pet.create("owner-123", name, species, "Breed", Pet.Gender.MALE, null, null);
        try {
            var idField = Pet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pet, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return pet;
    }
}
