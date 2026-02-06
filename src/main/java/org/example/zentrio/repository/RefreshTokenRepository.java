package org.example.zentrio.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.zentrio.model.RefreshToken;

import java.util.UUID;

@Mapper
public interface RefreshTokenRepository {

    @Select("""
                INSERT INTO refresh_tokens (id,user_id,token,expiry_date)
                VALUES (#{id}, #{userId}, #{token}, #{exiryDate})
            """)
    void insertToken(RefreshToken token);

    @Select("SELECT * FROM refresh_tokens WHERE token = #{token}")
    RefreshToken findByToken(String token);

    @Select("""
            DELETE FROM refresh_tokens WHERE user_id = #{userId}
            """)
    void deleteByUserId(UUID userId);
}
