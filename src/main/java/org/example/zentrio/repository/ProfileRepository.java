package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.ProfileRequest;
import org.example.zentrio.model.AppUser;

import java.util.UUID;

@Mapper
public interface ProfileRepository {

    @Results(id = "profileMapper", value = {
            @Result(property = "userId", column = "user_id"),
            @Result(property = "username", column = "username"),
            @Result(property = "email", column = "email"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "password", column = "password"),
            @Result(property = "profileImage", column = "profile_image"),
            @Result(property = "isVerified", column = "is_verified"),
            @Result(property = "isReset", column = "is_reset"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "googleEmail", column = "google_email"),
            @Result(property = "location", column = "location"),
            @Result(property = "position", column = "position"),
            @Result(property = "contact", column = "contact"),
    }
    )
    @Select("""
                UPDATE users set username = #{req.username},
                                gender = #{req.gender},
                                position = #{req.position},
                                location = #{req.location},
                                contact = #{req.contact}
                WHERE user_id = #{appUserId}
                RETURNING *
            """)
    AppUser updateProfile(@Param("req") ProfileRequest request, UUID appUserId);

    @ResultMap("profileMapper")
    @Select("""
                DELETE FROM users WHERE user_id = #{appUserId}
            """)
    void deleteProfile(UUID appUserId);

    @Select("""
                SELECT u.user_id,u.username, u.gender, u.email, u.profile_image, u.position,u.contact, u.location, u.created_at, u.updated_at FROM users u
                WHERE user_id = #{userId}
            """)
    @ResultMap("profileMapper")
    AppUser getProfileByUserId(UUID userId);

    @Select("""
        UPDATE users set profile_image = #{profileImage}
        WHERE user_id = #{userId}
    """)
    void updateProfileCover(UUID userId, String profileImage);
}
