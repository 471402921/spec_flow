package com.specflow.api.modules.user.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Pet Mapper - MyBatis-Plus
 */
@Mapper
public interface PetMapper extends BaseMapper<PetDO> {

    /**
     * 查询用户的所有宠物（含软删除的）
     *
     * @param ownerId 主人 ID
     * @return 宠物列表
     */
    default List<PetDO> selectAllByOwnerId(String ownerId) {
        LambdaQueryWrapper<PetDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PetDO::getOwnerId, ownerId);
        return selectList(wrapper);
    }

    /**
     * 根据主人 ID 和删除状态查询宠物
     *
     * @param ownerId 主人 ID
     * @param deleted 删除状态
     * @return 宠物列表
     */
    default List<PetDO> selectByOwnerIdAndDeleted(String ownerId, boolean deleted) {
        LambdaQueryWrapper<PetDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PetDO::getOwnerId, ownerId)
                .eq(PetDO::getDeleted, deleted);
        return selectList(wrapper);
    }

    /**
     * 查询用户已删除的同名同种类宠物
     *
     * @param ownerId 主人 ID
     * @param name    宠物名字
     * @param species 宠物种类
     * @return 已删除的宠物列表
     */
    @Select("SELECT * FROM pets WHERE owner_id = #{ownerId} AND LOWER(name) = LOWER(#{name}) " +
            "AND species = #{species} AND deleted = true")
    List<PetDO> selectDeletedByOwnerIdAndNameAndSpecies(@Param("ownerId") String ownerId,
                                                        @Param("name") String name,
                                                        @Param("species") String species);

    /**
     * 统计用户的宠物数量（不含软删除的）
     *
     * @param ownerId 主人 ID
     * @return 数量
     */
    default long countByOwnerId(String ownerId) {
        LambdaQueryWrapper<PetDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PetDO::getOwnerId, ownerId)
                .eq(PetDO::getDeleted, false);
        return selectCount(wrapper);
    }

    /**
     * 统计用户的宠物数量（含软删除的）
     *
     * @param ownerId 主人 ID
     * @return 数量
     */
    default long countAllByOwnerId(String ownerId) {
        LambdaQueryWrapper<PetDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PetDO::getOwnerId, ownerId);
        return selectCount(wrapper);
    }
}
