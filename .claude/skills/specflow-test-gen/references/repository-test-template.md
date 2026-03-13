# RepositoryImpl 测试模板

本文档定义 RepositoryImpl 的单元测试标准结构和规范。

---

## 测试类结构

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("{Entity}RepositoryImpl 单元测试")
class {Entity}RepositoryImplTest {

    @Mock
    private {Entity}Mapper {entity}Mapper;

    @InjectMocks
    private {Entity}RepositoryImpl {entity}Repository;

    private static final String TEST_{ENTITY}_ID = "{entity}-123";
    private static final String TEST_OWNER_ID = "owner-456";

    // ==================== save() 测试 ====================

    @Test
    @DisplayName("save() - 新实体应执行插入")
    void save_withNewEntity_shouldInsert() {
        // Given
        {Entity} entity = createEntity(null); // id 为 null 表示新实体

        when({entity}Mapper.updateById(any({Entity}DO.class))).thenReturn(0);

        // When
        {entity}Repository.save(entity);

        // Then
        verify({entity}Mapper).updateById(any({Entity}DO.class));
        verify({entity}Mapper).insert(any({Entity}DO.class));
    }

    @Test
    @DisplayName("save() - 已存在实体应执行更新")
    void save_withExistingEntity_shouldUpdate() {
        // Given
        {Entity} entity = createEntity(TEST_{ENTITY}_ID);

        when({entity}Mapper.updateById(any({Entity}DO.class))).thenReturn(1);

        // When
        {entity}Repository.save(entity);

        // Then
        verify({entity}Mapper).updateById(any({Entity}DO.class));
        // 不应调用 insert
    }

    // ==================== findById() 测试 ====================

    @Test
    @DisplayName("findById() - 实体存在应返回实体")
    void findById_withExistingEntity_shouldReturnEntity() {
        // Given
        {Entity}DO entityDO = createEntityDO(TEST_{ENTITY}_ID);
        when({entity}Mapper.selectById(TEST_{ENTITY}_ID)).thenReturn(entityDO);

        // When
        Optional<{Entity}> result = {entity}Repository.findById(TEST_{ENTITY}_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TEST_{ENTITY}_ID);
        // 验证 Converter 被调用（通过结果字段值判断）
    }

    @Test
    @DisplayName("findById() - 实体不存在应返回空")
    void findById_withNonExistentEntity_shouldReturnEmpty() {
        // Given
        when({entity}Mapper.selectById("non-existent")).thenReturn(null);

        // When
        Optional<{Entity}> result = {entity}Repository.findById("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== 自定义查询方法测试（根据实际 Repository 接口）====================

    // 每个自定义查询方法需要 2+ 个测试：
    // 1. 正常返回数据
    // 2. 无数据返回（空列表/Optional.empty）
    // 3. 边界条件（如软删除过滤）

    // ==================== 辅助方法 ====================

    private {Entity} createEntity(String id) {
        // 使用工厂方法创建 Entity
        {Entity} entity = {Entity}.create(...);

        // 如果 id 不为 null，通过反射设置（因为 create 可能自动生成 id）
        if (id != null) {
            setField(entity, "id", id);
        }

        return entity;
    }

    private {Entity}DO createEntityDO(String id) {
        {Entity}DO entityDO = new {Entity}DO();
        entityDO.setId(id);
        // 设置其他必要字段...
        return entityDO;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

---

## 测试要点清单

### 基础 CRUD
- [ ] save() - 新实体走 insert
- [ ] save() - 已存在实体走 update
- [ ] findById() - 存在返回 Optional.of()
- [ ] findById() - 不存在返回 Optional.empty()
- [ ] deleteById() - 调用 mapper.deleteById()

### 转换验证
- [ ] DO → Entity 转换正确（字段值一一对应）
- [ ] Entity → DO 转换正确
- [ ] 时间类型转换正确（LocalDateTime ↔ Instant）
- [ ] 枚举类型转换正确（String ↔ Enum）

### 自定义查询方法
- [ ] 每个自定义查询方法至少有 2 个测试（有数据/无数据）
- [ ] 验证查询条件正确传递给 Mapper
- [ ] 验证软删除过滤（如适用）

### Mock 验证
- [ ] 写操作（save/delete）验证 Mapper 被调用
- [ ] 不验证 Converter 被调用（属于实现细节，只要结果正确即可）

---

## 常见场景示例

### 场景 1：带软删除的查询

```java
@Test
@DisplayName("findByOwnerId() - 只返回未删除的实体")
void findByOwnerId_shouldReturnNonDeletedEntities() {
    // Given
    {Entity}DO activeEntity = createEntityDO("entity-1", false);
    {Entity}DO deletedEntity = createEntityDO("entity-2", true);
    when({entity}Mapper.selectByOwnerIdAndDeleted(TEST_OWNER_ID, false))
            .thenReturn(List.of(activeEntity));

    // When
    List<{Entity}> result = {entity}Repository.findByOwnerId(TEST_OWNER_ID);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isDeleted()).isFalse();
}
```

### 场景 2：统计方法

```java
@Test
@DisplayName("countByOwnerId() - 返回正确数量")
void countByOwnerId_shouldReturnCorrectCount() {
    // Given
    when({entity}Mapper.countByOwnerId(TEST_OWNER_ID)).thenReturn(5L);

    // When
    long result = {entity}Repository.countByOwnerId(TEST_OWNER_ID);

    // Then
    assertThat(result).isEqualTo(5L);
}
```

### 场景 3：复杂条件查询

```java
@Test
@DisplayName("findDeletedByCriteria() - 根据多条件查询已删除实体")
void findDeletedByCriteria_withMatches_shouldReturnList() {
    // Given
    {Entity}DO deletedEntity = createDeletedEntityDO("entity-1");
    when({entity}Mapper.selectDeletedByCriteria(TEST_OWNER_ID, "name", "TYPE_A"))
            .thenReturn(List.of(deletedEntity));

    // When
    List<{Entity}> result = {entity}Repository.findDeletedByCriteria(
            TEST_OWNER_ID, "name", Entity.Type.TYPE_A);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isDeleted()).isTrue();
}
```

---

## 反模式清单

| 反模式 | 说明 | 正确做法 |
|--------|------|----------|
| 测试 Mapper | 测试 MyBatis Mapper 本身的行为 | 假设 Mapper 工作正常，只测试 Repository 逻辑 |
| Mock Converter | 对 Converter 进行 Mock | Converter 是静态工具类或已独立测试，Repository 测试关注整体流程 |
| 使用 @SpringBootTest | 在 RepositoryImpl 测试中使用 Spring 容器 | 使用 `@ExtendWith(MockitoExtension.class)`，纯单元测试 |
| 忽略 verify | 不验证 Mapper 是否被调用 | 写操作必须 `verify(mapper).save(...)` |
| 硬编码 Magic Values | 测试数据使用 "123", "abc" | 使用有意义的常量或字段，如 `TEST_USER_ID` |
