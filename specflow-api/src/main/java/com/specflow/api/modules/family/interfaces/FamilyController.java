package com.specflow.api.modules.family.interfaces;

import com.specflow.api.modules.family.application.FamilyService;
import com.specflow.api.modules.family.domain.entity.Family;
import com.specflow.api.modules.family.domain.entity.FamilyInvitation;
import com.specflow.api.modules.family.domain.entity.FamilyMember;
import com.specflow.api.modules.family.interfaces.dto.CreateFamilyRequest;
import com.specflow.api.modules.family.interfaces.dto.FamilyDetailResponse;
import com.specflow.api.modules.family.interfaces.dto.FamilyInvitationResponse;
import com.specflow.api.modules.family.interfaces.dto.FamilyPetResponse;
import com.specflow.api.modules.family.interfaces.dto.FamilyResponse;
import com.specflow.api.modules.family.interfaces.dto.JoinFamilyRequest;
import com.specflow.api.modules.family.interfaces.dto.TransferOwnershipRequest;
import com.specflow.api.modules.family.interfaces.dto.UpdateFamilyRequest;
import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.domain.repository.UserRepository;
import com.specflow.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 家庭管理控制器
 */
@Tag(name = "Family Management", description = "家庭管理相关接口")
@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final UserRepository userRepository;

    /**
     * 创建家庭（API-13）
     */
    @Operation(summary = "创建家庭", description = "创建新家庭，创建者自动成为主人")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效或已达家庭上限")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<FamilyResponse> createFamily(
            @Valid @RequestBody CreateFamilyRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        Family family = familyService.createFamily(request.getName(), userId);
        return Result.success(FamilyResponse.fromDomain(family));
    }

    /**
     * 查看我加入的家庭列表（API-14）
     */
    @Operation(summary = "查看我的家庭列表", description = "获取当前用户加入的所有家庭")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping
    public Result<List<FamilyResponse>> getMyFamilies(HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        List<Family> families = familyService.getUserFamilies(userId);
        return Result.success(families.stream()
                .map(FamilyResponse::fromDomain)
                .toList());
    }

    /**
     * 查看家庭详情（API-15）
     */
    @Operation(summary = "查看家庭详情", description = "获取家庭详情及成员列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "家庭不存在"),
            @ApiResponse(responseCode = "403", description = "不是家庭成员")
    })
    @GetMapping("/{familyId}")
    public Result<FamilyDetailResponse> getFamilyDetail(
            @PathVariable String familyId,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);

        // 检查成员权限
        familyService.getFamilyMembers(familyId).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new com.specflow.common.exception.BusinessException(
                        "NOT_FAMILY_MEMBER", "你不是该家庭成员"));

        Family family = familyService.getFamily(familyId);
        List<FamilyMember> members = familyService.getFamilyMembers(familyId);

        // 组装成员信息（含昵称）
        List<FamilyDetailResponse.FamilyMemberInfo> memberInfos = members.stream()
                .map(m -> {
                    User user = userRepository.findById(m.getUserId()).orElse(null);
                    return new FamilyDetailResponse.FamilyMemberInfo(
                            m.getUserId(),
                            user != null ? user.getNickname() : "未知用户",
                            m.getRole().name(),
                            m.getJoinedAt()
                    );
                })
                .toList();

        FamilyDetailResponse response = new FamilyDetailResponse(
                family.getId(),
                family.getName(),
                family.getOwnerId(),
                memberInfos,
                family.getCreatedAt(),
                family.getUpdatedAt()
        );
        return Result.success(response);
    }

    /**
     * 修改家庭名称（API-16）
     */
    @Operation(summary = "修改家庭名称", description = "仅家庭主人可修改")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效"),
            @ApiResponse(responseCode = "403", description = "无权操作"),
            @ApiResponse(responseCode = "404", description = "家庭不存在")
    })
    @PutMapping("/{familyId}")
    public Result<FamilyResponse> updateFamily(
            @PathVariable String familyId,
            @Valid @RequestBody UpdateFamilyRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        Family family = familyService.updateFamilyName(familyId, userId, request.getName());
        return Result.success(FamilyResponse.fromDomain(family));
    }

    /**
     * 解散家庭（API-17）
     */
    @Operation(summary = "解散家庭", description = "仅家庭主人可解散，物理删除所有关联数据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "解散成功"),
            @ApiResponse(responseCode = "403", description = "无权操作"),
            @ApiResponse(responseCode = "404", description = "家庭不存在")
    })
    @DeleteMapping("/{familyId}")
    public Result<Void> dissolveFamily(
            @PathVariable String familyId,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        familyService.dissolveFamily(familyId, userId);
        return Result.success();
    }

    /**
     * 生成邀请码（API-18）
     */
    @Operation(summary = "生成邀请码", description = "生成新的家庭邀请码，旧码自动失效")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "生成成功"),
            @ApiResponse(responseCode = "403", description = "无权操作"),
            @ApiResponse(responseCode = "404", description = "家庭不存在")
    })
    @PostMapping("/{familyId}/invitations")
    public Result<FamilyInvitationResponse> generateInvitation(
            @PathVariable String familyId,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        FamilyInvitation invitation = familyService.generateInvitationCode(familyId, userId);
        return Result.success(FamilyInvitationResponse.fromDomain(invitation));
    }

    /**
     * 通过邀请码加入家庭（API-19）
     */
    @Operation(summary = "加入家庭", description = "使用邀请码加入家庭")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "加入成功"),
            @ApiResponse(responseCode = "400", description = "邀请码无效、已过期或已达上限")
    })
    @PostMapping("/join")
    public Result<FamilyResponse> joinFamily(
            @Valid @RequestBody JoinFamilyRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        Family family = familyService.joinFamilyByInvitation(request.getCode(), userId);
        return Result.success(FamilyResponse.fromDomain(family));
    }

    /**
     * 移除成员（API-20）
     */
    @Operation(summary = "移除成员", description = "仅家庭主人可移除其他成员")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "移除成功"),
            @ApiResponse(responseCode = "400", description = "不能移除主人"),
            @ApiResponse(responseCode = "403", description = "无权操作"),
            @ApiResponse(responseCode = "404", description = "家庭或成员不存在")
    })
    @DeleteMapping("/{familyId}/members/{targetUserId}")
    public Result<Void> removeMember(
            @PathVariable String familyId,
            @PathVariable String targetUserId,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        familyService.removeMember(familyId, userId, targetUserId);
        return Result.success();
    }

    /**
     * 退出家庭（API-21）
     */
    @Operation(summary = "退出家庭", description = "退出当前家庭，主人不能退出")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "退出成功"),
            @ApiResponse(responseCode = "400", description = "主人不能退出"),
            @ApiResponse(responseCode = "403", description = "不是家庭成员")
    })
    @PostMapping("/{familyId}/members/leave")
    public Result<Void> leaveFamily(
            @PathVariable String familyId,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        familyService.leaveFamily(familyId, userId);
        return Result.success();
    }

    /**
     * 转让主人身份（API-22）
     */
    @Operation(summary = "转让主人身份", description = "将家庭主人身份转让给其他成员")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "转让成功"),
            @ApiResponse(responseCode = "400", description = "目标用户不是成员"),
            @ApiResponse(responseCode = "403", description = "无权操作"),
            @ApiResponse(responseCode = "404", description = "家庭不存在")
    })
    @PostMapping("/{familyId}/transfer")
    public Result<Void> transferOwnership(
            @PathVariable String familyId,
            @Valid @RequestBody TransferOwnershipRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        familyService.transferOwnership(familyId, userId, request.getNewOwnerId());
        return Result.success();
    }

    /**
     * 查看家庭内宠物（API-23）
     */
    @Operation(summary = "查看家庭宠物", description = "获取家庭内所有成员的宠物列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "不是家庭成员")
    })
    @GetMapping("/{familyId}/pets")
    public Result<List<FamilyPetResponse>> getFamilyPets(
            @PathVariable String familyId,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        List<FamilyService.FamilyPetInfo> petInfos = familyService.getFamilyPets(familyId, userId);
        return Result.success(petInfos.stream()
                .map(info -> FamilyPetResponse.fromDomain(info.pet(), info.ownerId(), info.ownerNickname()))
                .toList());
    }

    private String getCurrentUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }
}
