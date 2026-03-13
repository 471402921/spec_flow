package com.specflow.api.modules.user.interfaces;

import com.specflow.api.modules.user.application.PetService;
import com.specflow.api.modules.user.domain.entity.Pet;
import com.specflow.api.modules.user.interfaces.dto.AddPetRequest;
import com.specflow.api.modules.user.interfaces.dto.PetResponse;
import com.specflow.api.modules.user.interfaces.dto.RestorablePetResponse;
import com.specflow.api.modules.user.interfaces.dto.UpdatePetRequest;
import com.specflow.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 宠物管理控制器
 */
@Tag(name = "Pet Management", description = "宠物管理相关接口")
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /**
     * 添加宠物
     */
    @Operation(summary = "添加宠物", description = "添加新宠物，如有同名已删除宠物则返回恢复提示")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "200", description = "发现可恢复的同名宠物"),
        @ApiResponse(responseCode = "400", description = "参数无效或已达宠物上限"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping
    public ResponseEntity<Result<Object>> addPet(
            HttpServletRequest request,
            @Valid @RequestBody AddPetRequest addRequest) {
        String userId = getCurrentUserId(request);

        // 检查是否有可恢复的已删除宠物
        List<Pet> restorablePets = petService.findRestorablePets(
                userId,
                addRequest.getName(),
                Pet.Species.valueOf(addRequest.getSpecies().name())
        );

        if (!restorablePets.isEmpty()) {
            // 返回可恢复的宠物列表
            return ResponseEntity.ok(Result.success(
                    "PET_DELETED_MATCH_FOUND",
                    "发现已删除的同名宠物，是否恢复？",
                    RestorablePetResponse.fromDomainList(restorablePets)
            ));
        }

        // 创建新宠物
        Pet pet = petService.addPet(
                userId,
                addRequest.getName(),
                Pet.Species.valueOf(addRequest.getSpecies().name()),
                addRequest.getBreed(),
                Pet.Gender.valueOf(addRequest.getGender().name()),
                addRequest.getBirthday(),
                addRequest.getAvatarUrl()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(PetResponse.fromDomain(pet)));
    }

    /**
     * 获取宠物列表
     */
    @Operation(summary = "获取宠物列表", description = "获取当前用户的所有宠物（不含已删除）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping
    public Result<List<PetResponse>> getPets(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        List<Pet> pets = petService.getPetsByOwner(userId);
        List<PetResponse> responses = pets.stream()
                .map(PetResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 获取宠物详情
     */
    @Operation(summary = "获取宠物详情", description = "根据宠物 ID 获取详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "404", description = "宠物不存在")
    })
    @GetMapping("/{petId}")
    public Result<PetResponse> getPet(
            HttpServletRequest request,
            @Parameter(description = "宠物 ID") @PathVariable String petId) {
        String userId = getCurrentUserId(request);
        Pet pet = petService.getPet(userId, petId);
        return Result.success(PetResponse.fromDomain(pet));
    }

    /**
     * 编辑宠物
     */
    @Operation(summary = "编辑宠物", description = "修改宠物信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "参数无效"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "403", description = "无权操作此宠物"),
        @ApiResponse(responseCode = "404", description = "宠物不存在")
    })
    @PutMapping("/{petId}")
    public Result<PetResponse> updatePet(
            HttpServletRequest request,
            @Parameter(description = "宠物 ID") @PathVariable String petId,
            @Valid @RequestBody UpdatePetRequest updateRequest) {
        String userId = getCurrentUserId(request);
        var cmd = new PetService.UpdatePetCommand(
                updateRequest.getName(),
                Pet.Species.valueOf(updateRequest.getSpecies().name()),
                updateRequest.getBreed(),
                Pet.Gender.valueOf(updateRequest.getGender().name()),
                updateRequest.getBirthday(),
                updateRequest.getAvatarUrl()
        );
        Pet pet = petService.updatePet(userId, petId, cmd);
        return Result.success(PetResponse.fromDomain(pet));
    }

    /**
     * 删除宠物（软删除）
     */
    @Operation(summary = "删除宠物", description = "软删除宠物（可恢复）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "403", description = "无权操作此宠物"),
        @ApiResponse(responseCode = "404", description = "宠物不存在")
    })
    @DeleteMapping("/{petId}")
    public Result<Void> deletePet(
            HttpServletRequest request,
            @Parameter(description = "宠物 ID") @PathVariable String petId) {
        String userId = getCurrentUserId(request);
        petService.deletePet(userId, petId);
        return Result.success();
    }

    /**
     * 恢复已删除的宠物
     */
    @Operation(summary = "恢复宠物", description = "恢复已软删除的宠物")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "恢复成功"),
        @ApiResponse(responseCode = "400", description = "宠物未被删除或已达上限"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "403", description = "无权操作此宠物"),
        @ApiResponse(responseCode = "404", description = "宠物不存在")
    })
    @PostMapping("/{petId}/restore")
    public Result<PetResponse> restorePet(
            HttpServletRequest request,
            @Parameter(description = "宠物 ID") @PathVariable String petId) {
        String userId = getCurrentUserId(request);
        Pet pet = petService.restorePet(userId, petId);
        return Result.success(PetResponse.fromDomain(pet));
    }

    // ==================== 私有辅助方法 ====================

    private String getCurrentUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }
}
