package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.ReactRequest;
import org.example.zentrio.model.React;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ReactRepository {

    @Select("""
        INSERT INTO reacts(reaction_type,created_at,created_by,announcement_id)
        VALUES (#{request.reactType}, #{now}, #{authorId}, #{request.announcementId})
        RETURNING *
        """)
    @Results(id = "reactMapper",value = {
            @Result(property = "reactId", column = "react_id"),
            @Result(property = "reactType", column = "reaction_type"),
            @Result(property = "reactDate", column = "created_at"),
            @Result(property = "authorId", column = "created_by"),
            @Result(property = "commentBy", column = "created_by",
            one = @One(select = "org.example.zentrio.repository.CommentRepository.getMemberByUserId")),
            @Result(property = "announcementId", column = "announcement_id"),
    })
    React createReact(@Param("request") ReactRequest reactRequest, LocalDateTime now, UUID authorId);


    @Select("""
    SELECT  EXISTS(
        SELECT 1 FROM reacts WHERE announcement_id= #{announcementId} AND created_by= #{userId} 
    )
    """)
    Boolean getExistingReact(UUID announcementId, UUID userId);

    @Select("""
                UPDATE reacts SET reaction_type= #{request.reactType} 
                WHERE react_id = #{reactId} AND created_by= #{authorId} AND announcement_id= #{request.announcementId}
                RETURNING *
                """)
    @ResultMap("reactMapper")
    React UpdateReact(UUID reactId,@Param("request") ReactRequest reactRequest, UUID authorId);


    @Select("""
            SELECT * FROM reacts WHERE react_id = #{reactId}
            """)
    @ResultMap("reactMapper")
    React GetReactById(UUID reactId);


    @Select("""
            DELETE  FROM reacts WHERE  react_id =#{reactId} AND created_by= #{authorId}
            """)
    Void DeleteReactById(UUID reactId, UUID authorId);




    @Select("""
            SELECT * FROM reacts WHERE announcement_id= #{announcementId}
            """)
    @ResultMap("reactMapper")
    List<React> GetAllReactAnnouncementIdById(UUID announcementId);
}
