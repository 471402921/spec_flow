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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Family 应用服务
 *
 * <p>职责：
 * - 定义家庭相关的业务用例
 * - 声明事务边界
 * - 编排 Domain 对象和 Repository
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FamilyService {

    private static final int MAX_FAMILY_COUNT = 5;
    private static final int MAX_FAMILY_MEMBER_COUNT = 10;
    private static final int INVITATION_CODE_LENGTH = 8;
    private static final int INVITATION_VALID_DAYS = 7;
    private static final int INVITATION_GENERATION_MAX_RETRY = 10;

    // 邀请码字符集：排除 0, O, I, 1 避免混淆
    private static final String INVITATION_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyInvitationRepository familyInvitationRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;

    // ==================== 家庭管理 ====================

    /**
     * 创建家庭
     *
     * @param name   家庭名称
     * @param userId 创建者用户 ID
     * @return 创建的家庭
     */
    public Family createFamily(String name, String userId) {
        log.info("Creating family: name={}, userId={}", name, userId);

        // 校验家庭名称
        if (name == null || name.length() < 2 || name.length() > 20) {
            throw new BusinessException("FAMILY_NAME_INVALID", "家庭名称长度需在2-20个字符之间");
        }

        // 检查用户家庭数量上限
        long userFamilyCount = familyMemberRepository.countByUserId(userId);
        if (userFamilyCount >= MAX_FAMILY_COUNT) {
            throw new BusinessException("FAMILY_LIMIT_EXCEEDED", "已达加入家庭数量上限（最多5个）");
        }

        // 创建家庭和成员关系（同一事务）
        Family family = Family.create(name, userId);
        familyRepository.save(family);

        FamilyMember ownerMember = FamilyMember.createOwner(family.getId(), userId);
        familyMemberRepository.save(ownerMember);

        log.info("Family created successfully: id={}, owner={}", family.getId(), userId);
        return family;
    }

    /**
     * 获取用户加入的所有家庭列表
     *
     * @param userId 用户 ID
     * @return 家庭列表
     */
    @Transactional(readOnly = true)
    public List<Family> getUserFamilies(String userId) {
        List<FamilyMember> memberships = familyMemberRepository.findAllByUserId(userId);
        return memberships.stream()
                .map(membership -> familyRepository.findById(membership.getFamilyId()).orElse(null))
                .filter(family -> family != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取家庭详情
     *
     * @param familyId 家庭 ID
     * @return 家庭实体
     */
    @Transactional(readOnly = true)
    public Family getFamily(String familyId) {
        return familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("FAMILY_NOT_FOUND", "家庭不存在"));
    }

    /**
     * 获取家庭成员列表
     *
     * @param familyId 家庭 ID
     * @return 成员关系列表
     */
    @Transactional(readOnly = true)
    public List<FamilyMember> getFamilyMembers(String familyId) {
        return familyMemberRepository.findAllByFamilyId(familyId);
    }

    /**
     * 修改家庭名称
     *
     * @param familyId 家庭 ID
     * @param userId   操作用户 ID
     * @param newName  新名称
     * @return 更新后的家庭
     */
    public Family updateFamilyName(String familyId, String userId, String newName) {
        log.info("Updating family name: familyId={}, userId={}", familyId, userId);

        // 校验名称
        if (newName == null || newName.length() < 2 || newName.length() > 20) {
            throw new BusinessException("FAMILY_NAME_INVALID", "家庭名称长度需在2-20个字符之间");
        }

        // 检查权限
        Family family = getFamily(familyId);
        FamilyMember member = getFamilyMember(familyId, userId);
        if (!member.isOwner()) {
            throw new BusinessException("FAMILY_ACCESS_DENIED", "仅家庭主人可执行此操作");
        }

        // 更新名称
        family.updateName(newName);
        familyRepository.save(family);

        log.info("Family name updated: familyId={}", familyId);
        return family;
    }

    /**
     * 解散家庭
     *
     * @param familyId 家庭 ID
     * @param userId   操作用户 ID
     */
    public void dissolveFamily(String familyId, String userId) {
        log.info("Dissolving family: familyId={}, userId={}", familyId, userId);

        // 检查家庭存在
        Family family = getFamily(familyId);

        // 检查权限
        FamilyMember member = getFamilyMember(familyId, userId);
        if (!member.isOwner()) {
            throw new BusinessException("FAMILY_ACCESS_DENIED", "仅家庭主人可执行此操作");
        }

        // 物理删除所有关联数据（同一事务）
        familyInvitationRepository.deleteAllByFamilyId(familyId);
        familyMemberRepository.deleteAllByFamilyId(familyId);
        familyRepository.deleteById(familyId);

        log.info("Family dissolved: familyId={}", familyId);
    }

    // ==================== 邀请码管理 ====================

    /**
     * 生成家庭邀请码
     *
     * @param familyId 家庭 ID
     * @param userId   操作用户 ID
     * @return 生成的邀请码
     */
    public FamilyInvitation generateInvitationCode(String familyId, String userId) {
        log.info("Generating invitation code: familyId={}, userId={}", familyId, userId);

        // 检查家庭存在
        Family family = getFamily(familyId);

        // 检查权限
        FamilyMember member = getFamilyMember(familyId, userId);
        if (!member.isOwner()) {
            throw new BusinessException("FAMILY_ACCESS_DENIED", "仅家庭主人可执行此操作");
        }

        // 撤销该家庭所有未过期的邀请码
        List<FamilyInvitation> existingInvitations =
                familyInvitationRepository.findAllByFamilyIdAndRevokedFalse(familyId);
        for (FamilyInvitation invitation : existingInvitations) {
            invitation.revoke();
            familyInvitationRepository.save(invitation);
        }

        // 生成新邀请码（带重试机制）
        String code = generateUniqueInvitationCode();
        FamilyInvitation invitation = FamilyInvitation.create(familyId, code, userId, INVITATION_VALID_DAYS);
        familyInvitationRepository.save(invitation);

        log.info("Invitation code generated: familyId={}, code={}", familyId, code);
        return invitation;
    }

    /**
     * 通过邀请码加入家庭
     *
     * @param code   邀请码
     * @param userId 用户 ID
     * @return 加入的家庭
     */
    public Family joinFamilyByInvitation(String code, String userId) {
        log.info("Joining family by invitation: userId={}", userId);

        String normalizedCode = code.toUpperCase();
        FamilyInvitation invitation = familyInvitationRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new BusinessException("INVITATION_NOT_FOUND", "邀请码无效"));

        // 校验顺序（严格按 PRD 定义）
        // 1. 邀请码是否存在 - 已在上面处理

        // 2. 邀请码是否过期或已撤销
        if (!invitation.isValid()) {
            if (invitation.isRevoked()) {
                throw new BusinessException("INVITATION_EXPIRED", "邀请码已过期，请联系家庭主人重新生成");
            }
            throw new BusinessException("INVITATION_EXPIRED", "邀请码已过期，请联系家庭主人重新生成");
        }

        // 3. 用户是否已是该家庭成员
        if (familyMemberRepository.existsByFamilyIdAndUserId(invitation.getFamilyId(), userId)) {
            throw new BusinessException("ALREADY_FAMILY_MEMBER", "你已是该家庭成员");
        }

        // 4. 用户家庭数是否已达上限
        long userFamilyCount = familyMemberRepository.countByUserId(userId);
        if (userFamilyCount >= MAX_FAMILY_COUNT) {
            throw new BusinessException("FAMILY_LIMIT_EXCEEDED", "已达加入家庭数量上限（最多5个）");
        }

        // 5. 家庭成员数是否已达上限
        long familyMemberCount = familyMemberRepository.countByFamilyId(invitation.getFamilyId());
        if (familyMemberCount >= MAX_FAMILY_MEMBER_COUNT) {
            throw new BusinessException("FAMILY_MEMBER_LIMIT_EXCEEDED", "家庭成员已满（最多10人）");
        }

        // 加入家庭
        FamilyMember newMember = FamilyMember.createMember(invitation.getFamilyId(), userId);
        familyMemberRepository.save(newMember);

        Family family = familyRepository.findById(invitation.getFamilyId()).orElseThrow();
        log.info("User joined family: familyId={}, userId={}", family.getId(), userId);
        return family;
    }

    // ==================== 成员管理 ====================

    /**
     * 家庭主人移除成员
     *
     * @param familyId 家庭 ID
     * @param ownerId  主人用户 ID
     * @param targetUserId 要移除的用户 ID
     */
    public void removeMember(String familyId, String ownerId, String targetUserId) {
        log.info("Removing member: familyId={}, ownerId={}, targetUserId={}", familyId, ownerId, targetUserId);

        // 检查权限
        FamilyMember ownerMember = getFamilyMember(familyId, ownerId);
        if (!ownerMember.isOwner()) {
            throw new BusinessException("FAMILY_ACCESS_DENIED", "仅家庭主人可执行此操作");
        }

        // 不能移除自己
        if (ownerId.equals(targetUserId)) {
            throw new BusinessException("CANNOT_REMOVE_OWNER", "不能移除家庭主人");
        }

        // 查找目标成员
        FamilyMember targetMember = familyMemberRepository.findByFamilyIdAndUserId(familyId, targetUserId)
                .orElseThrow(() -> new BusinessException("NOT_FAMILY_MEMBER", "目标用户不是该家庭成员"));

        // 移除成员
        familyMemberRepository.deleteById(targetMember.getId());
        log.info("Member removed: familyId={}, userId={}", familyId, targetUserId);
    }

    /**
     * 退出家庭
     *
     * @param familyId 家庭 ID
     * @param userId   用户 ID
     */
    public void leaveFamily(String familyId, String userId) {
        log.info("Leaving family: familyId={}, userId={}", familyId, userId);

        // 查找成员关系
        FamilyMember member = getFamilyMember(familyId, userId);

        // 主人不能退出
        if (member.isOwner()) {
            throw new BusinessException("OWNER_CANNOT_LEAVE", "家庭主人不能退出，请先转让主人身份");
        }

        // 退出家庭
        familyMemberRepository.deleteById(member.getId());
        log.info("Member left family: familyId={}, userId={}", familyId, userId);
    }

    /**
     * 转让主人身份
     *
     * @param familyId     家庭 ID
     * @param currentOwnerId 当前主人 ID
     * @param newOwnerId   新主人 ID
     */
    public void transferOwnership(String familyId, String currentOwnerId, String newOwnerId) {
        log.info("Transferring ownership: familyId={}, currentOwnerId={}, newOwnerId={}",
                familyId, currentOwnerId, newOwnerId);

        // 检查当前主人权限
        FamilyMember currentOwner = getFamilyMember(familyId, currentOwnerId);
        if (!currentOwner.isOwner()) {
            throw new BusinessException("FAMILY_ACCESS_DENIED", "仅家庭主人可执行此操作");
        }

        // 检查新主人是否是家庭成员
        FamilyMember newOwner = familyMemberRepository.findByFamilyIdAndUserId(familyId, newOwnerId)
                .orElseThrow(() -> new BusinessException("TRANSFER_TARGET_NOT_MEMBER", "目标用户不是该家庭成员"));

        // 同一事务内更新：原主人降级，新主人升级，更新 family.owner_id
        Family family = getFamily(familyId);
        family.transferOwnership(newOwnerId);
        familyRepository.save(family);

        currentOwner.demoteToMember();
        familyMemberRepository.save(currentOwner);

        newOwner.promoteToOwner();
        familyMemberRepository.save(newOwner);

        log.info("Ownership transferred: familyId={}, newOwnerId={}", familyId, newOwnerId);
    }

    // ==================== 宠物查询 ====================

    /**
     * 获取家庭内所有成员的宠物列表
     *
     * @param familyId 家庭 ID
     * @param userId   查询用户 ID（用于权限检查）
     * @return 宠物列表（带主人信息）
     */
    @Transactional(readOnly = true)
    public List<FamilyPetInfo> getFamilyPets(String familyId, String userId) {
        // 检查权限
        if (!familyMemberRepository.existsByFamilyIdAndUserId(familyId, userId)) {
            throw new BusinessException("NOT_FAMILY_MEMBER", "你不是该家庭成员");
        }

        // 获取所有成员
        List<FamilyMember> members = familyMemberRepository.findAllByFamilyId(familyId);

        // 获取所有成员的宠物
        return members.stream()
                .flatMap(member -> {
                    User user = userRepository.findById(member.getUserId()).orElse(null);
                    if (user == null) {
                        return java.util.stream.Stream.empty();
                    }
                    return petRepository.findByOwnerId(member.getUserId()).stream()
                            .map(pet -> new FamilyPetInfo(pet, user.getId(), user.getNickname()));
                })
                .collect(Collectors.toList());
    }

    // ==================== 内部方法 ====================

    private FamilyMember getFamilyMember(String familyId, String userId) {
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                .orElseThrow(() -> new BusinessException("NOT_FAMILY_MEMBER", "你不是该家庭成员"));
    }

    /**
     * 生成唯一的邀请码（带重试机制）
     */
    private String generateUniqueInvitationCode() {
        for (int i = 0; i < INVITATION_GENERATION_MAX_RETRY; i++) {
            String code = generateRandomCode();
            if (!familyInvitationRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BusinessException("INVITATION_GENERATION_FAILED", "邀请码生成失败，请稍后重试");
    }

    /**
     * 生成随机邀请码
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(INVITATION_CODE_LENGTH);
        for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
            code.append(INVITATION_CODE_CHARS.charAt(SECURE_RANDOM.nextInt(INVITATION_CODE_CHARS.length())));
        }
        return code.toString();
    }

    // ==================== DTO ====================

    /**
     * 家庭宠物信息（包含宠物和主人信息）
     */
    public record FamilyPetInfo(Pet pet, String ownerId, String ownerNickname) {}
}
