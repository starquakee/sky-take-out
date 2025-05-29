package com.sky.mapper;

import com.sky.entity.User;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    @Select("select * from user where openid = #{openid}")
    User getUserByOpenid(String openid);

    void insert(User user);

    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    Integer countByMap(Map map);
}
