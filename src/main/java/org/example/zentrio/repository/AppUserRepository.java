package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.response.AppUserResponse;
import org.example.zentrio.model.AppUser;

import java.util.UUID;

@Mapper
public interface AppUserRepository {
    @Select("""
            SELECT * FROM users
            WHERE email= #{email}
            """)
    @Results(id = "UserMapper", value = {
            @Result(property = "userId", column = "user_id"),
            @Result(property = "username", column = "username"),
            @Result(property = "email", column = "email"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "password", column = "password"),
            @Result(property = "profileImage", column = "profile_image"),
            @Result(property = "isVerified", column = "is_verified"),
            @Result(property = "isReset", column = "is_reset"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    }
    )
    AppUser getUserByEmail(String email);

    @Select("""
        SELECT user_id FROM users WHERE email = #{email}
    """)
    UUID getUserIdByEmail(String email);


    @Select("""
        SELECT * FROM users WHERE user_id = #{userId}
    """)
    @ResultMap("UserMapper")
    AppUser getUserById( UUID userId);

    @Select("""
        UPDATE users SET is_verified = true
        WHERE user_id = #{userId}
    """)
    void updateVerify(UUID userId);

    @Select("""
        SELECT users.user_id FROM users WHERE email = #{email}
    """)
    UUID getCurrentUserId(String email);

    @Select("""
        SELECT user_id, username,email,gender,profile_image,created_at,updated_at
        FROM users WHERE user_id =  #{userId}
    """)
   @Results(id = "userResponse", value = {
           @Result(property = "userId",column = "user_id"),
           @Result(property = "profileImage", column = "profile_image"),
           @Result(property = "createdAt", column = "created_at"),
           @Result(property = "updatedAt", column = "updated_at")
   })
    AppUserResponse getUserByUserId(UUID userId);

    @Select("""
        UPDATE users SET is_reset = true
        WHERE user_id = #{userId}
    """)
    void updatedIsResetToTrue(UUID userId);

    @Select("""
        UPDATE users SET is_reset = false
        WHERE user_id = #{userId}
    """)
    void updatedIsResetToFalse(UUID userId);
}
