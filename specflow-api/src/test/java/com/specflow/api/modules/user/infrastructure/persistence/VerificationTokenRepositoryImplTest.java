package com.specflow.api.modules.user.infrastructure.persistence;

import com.specflow.api.modules.user.domain.entity.VerificationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VerificationTokenRepositoryImpl 单元测试
 *
 * <p>测试范围：
 * - save (insert/update)
 * - findByToken
 * - findLatestByUserIdAndType
 * - findByUserIdAndType
 * - findExpiredAndUnusedTokens
 * - deleteByIds
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationTokenRepositoryImpl 单元测试")
class VerificationTokenRepositoryImplTest {

    @Mock
    private VerificationTokenMapper verificationTokenMapper;

    @InjectMocks
    private VerificationTokenRepositoryImpl verificationTokenRepository;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_TOKEN = "test-token-value";

    @BeforeEach
    void setUp() {
        // MockitoExtension 自动初始化 @Mock 和 @InjectMocks
    }

    // ==================== save() 测试 ====================

    @Test
    @DisplayName("save() - 新令牌应执行 insert")
    void save_withNewToken_shouldInsert() {
        // Given
        VerificationToken token = createToken(null, TEST_TOKEN);
        when(verificationTokenMapper.updateById(any(VerificationTokenDO.class))).thenReturn(0);

        // When
        VerificationToken result = verificationTokenRepository.save(token);

        // Then
        assertThat(result).isNotNull();
        verify(verificationTokenMapper).updateById(any(VerificationTokenDO.class));
        verify(verificationTokenMapper).insert(any(VerificationTokenDO.class));
    }

    @Test
    @DisplayName("save() - 已有令牌应执行 update")
    void save_withExistingToken_shouldUpdate() {
        // Given
        VerificationToken token = createToken("token-id", TEST_TOKEN);
        when(verificationTokenMapper.updateById(any(VerificationTokenDO.class))).thenReturn(1);

        // When
        VerificationToken result = verificationTokenRepository.save(token);

        // Then
        assertThat(result).isNotNull();
        verify(verificationTokenMapper).updateById(any(VerificationTokenDO.class));
        verify(verificationTokenMapper, never()).insert(any(VerificationTokenDO.class));
    }

    // ==================== findByToken() 测试 ====================

    @Test
    @DisplayName("findByToken() - 存在的令牌应返回 Optional.of")
    void findByToken_withExistingToken_shouldReturnOptional() {
        // Given
        VerificationTokenDO tokenDO = createTokenDO("token-id", TEST_TOKEN);
        when(verificationTokenMapper.selectByToken(TEST_TOKEN)).thenReturn(tokenDO);

        // When
        Optional<VerificationToken> result = verificationTokenRepository.findByToken(TEST_TOKEN);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo(TEST_TOKEN);
    }

    @Test
    @DisplayName("findByToken() - 不存在的令牌应返回 Optional.empty")
    void findByToken_withNonExistingToken_shouldReturnEmpty() {
        // Given
        when(verificationTokenMapper.selectByToken("non-existent")).thenReturn(null);

        // When
        Optional<VerificationToken> result = verificationTokenRepository.findByToken("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findLatestByUserIdAndType() 测试 ====================

    @Test
    @DisplayName("findLatestByUserIdAndType() - 存在令牌应返回最新的")
    void findLatestByUserIdAndType_withExistingTokens_shouldReturnLatest() {
        // Given
        VerificationTokenDO tokenDO = createTokenDO("token-id", TEST_TOKEN);
        when(verificationTokenMapper.selectLatestByUserIdAndType(TEST_USER_ID, "EMAIL_VERIFICATION"))
                .thenReturn(tokenDO);

        // When
        Optional<VerificationToken> result = verificationTokenRepository.findLatestByUserIdAndType(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.get().getType()).isEqualTo(VerificationToken.Type.EMAIL_VERIFICATION);
    }

    @Test
    @DisplayName("findLatestByUserIdAndType() - 不存在应返回 Optional.empty")
    void findLatestByUserIdAndType_withNoTokens_shouldReturnEmpty() {
        // Given
        when(verificationTokenMapper.selectLatestByUserIdAndType(TEST_USER_ID, "PASSWORD_RESET"))
                .thenReturn(null);

        // When
        Optional<VerificationToken> result = verificationTokenRepository.findLatestByUserIdAndType(
                TEST_USER_ID, VerificationToken.Type.PASSWORD_RESET);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findByUserIdAndType() 测试 ====================

    @Test
    @DisplayName("findByUserIdAndType() - 应返回令牌列表")
    void findByUserIdAndType_withTokens_shouldReturnList() {
        // Given
        VerificationTokenDO token1 = createTokenDO("id-1", "token-1");
        VerificationTokenDO token2 = createTokenDO("id-2", "token-2");
        when(verificationTokenMapper.selectByUserIdAndType(TEST_USER_ID, "EMAIL_VERIFICATION"))
                .thenReturn(Arrays.asList(token1, token2));

        // When
        List<VerificationToken> result = verificationTokenRepository.findByUserIdAndType(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getToken()).isEqualTo("token-1");
        assertThat(result.get(1).getToken()).isEqualTo("token-2");
    }

    @Test
    @DisplayName("findByUserIdAndType() - 无令牌应返回空列表")
    void findByUserIdAndType_withNoTokens_shouldReturnEmptyList() {
        // Given
        when(verificationTokenMapper.selectByUserIdAndType(TEST_USER_ID, "EMAIL_CHANGE"))
                .thenReturn(Collections.emptyList());

        // When
        List<VerificationToken> result = verificationTokenRepository.findByUserIdAndType(
                TEST_USER_ID, VerificationToken.Type.EMAIL_CHANGE);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findExpiredAndUnusedTokens() 测试 ====================

    @Test
    @DisplayName("findExpiredAndUnusedTokens() - 应返回过期且未使用的令牌")
    void findExpiredAndUnusedTokens_withExpiredTokens_shouldReturnList() {
        // Given
        VerificationTokenDO expiredToken = createTokenDO("id-1", "token-1");
        expiredToken.setExpiredAt(LocalDateTime.now().minusHours(1));
        expiredToken.setUsed(false);

        when(verificationTokenMapper.selectExpiredAndUnusedTokens(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredToken));

        // When
        List<VerificationToken> result = verificationTokenRepository.findExpiredAndUnusedTokens();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("id-1");
    }

    @Test
    @DisplayName("findExpiredAndUnusedTokens() - 无过期令牌应返回空列表")
    void findExpiredAndUnusedTokens_withNoExpiredTokens_shouldReturnEmptyList() {
        // Given
        when(verificationTokenMapper.selectExpiredAndUnusedTokens(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<VerificationToken> result = verificationTokenRepository.findExpiredAndUnusedTokens();

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== deleteByIds() 测试 ====================

    @Test
    @DisplayName("deleteByIds() - 应批量删除")
    void deleteByIds_withValidIds_shouldDelete() {
        // Given
        List<String> ids = Arrays.asList("id-1", "id-2", "id-3");

        // When
        verificationTokenRepository.deleteByIds(ids);

        // Then
        verify(verificationTokenMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("deleteByIds() - 空列表不应调用删除")
    void deleteByIds_withEmptyList_shouldNotDelete() {
        // Given
        List<String> emptyList = Collections.emptyList();

        // When
        verificationTokenRepository.deleteByIds(emptyList);

        // Then
        verify(verificationTokenMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("deleteByIds() - null 列表不应调用删除")
    void deleteByIds_withNullList_shouldNotDelete() {
        // When
        verificationTokenRepository.deleteByIds(null);

        // Then
        verify(verificationTokenMapper, never()).deleteBatchIds(any());
    }

    // ==================== 辅助方法 ====================

    private VerificationToken createToken(String id, String token) {
        VerificationToken t = new VerificationToken();
        t.setId(id);
        t.setToken(token);
        t.setUserId(TEST_USER_ID);
        t.setType(VerificationToken.Type.EMAIL_VERIFICATION);
        t.setUsed(false);
        t.setExpiredAt(Instant.now().plusSeconds(3600));
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        return t;
    }

    private VerificationTokenDO createTokenDO(String id, String token) {
        VerificationTokenDO t = new VerificationTokenDO();
        t.setId(id);
        t.setToken(token);
        t.setUserId(TEST_USER_ID);
        t.setType("EMAIL_VERIFICATION");
        t.setUsed(false);
        t.setExpiredAt(LocalDateTime.now().plusHours(1));
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }
}
