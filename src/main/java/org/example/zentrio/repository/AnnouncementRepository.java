package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.AnnouncementRequest;
import org.example.zentrio.model.Announcement;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AnnouncementRepository {



    @Select("""
        INSERT INTO announcements(text_content,title,created_by,bord_id)
        VALUES (#{request.description},#{request.title
        }, #{creator}, #{request.boardId})
        RETURNING *
        """)
    @Results(id="announcementMapper", value = {
            @Result(property = "announcementId", column = "announcement_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "text_content"),
            @Result(property = "isPinned", column = "is_pinned"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "update_at"),
            @Result(property = "authorId", column = "created_by"),
            @Result(property = "imageUrl", column = "announcement_id",
                    many = @Many(select = "getImageUrl")),
            @Result(property = "createdBy", column = "created_by",
                    one = @One(select = "org.example.zentrio.repository.CommentRepository.getMemberByUserId")),
            @Result(property = "boardId", column = "bord_id"),
            @Result(property = "reacts", column = "announcement_id",
            many = @Many(select = "org.example.zentrio.repository.ReactRepository.GetAllReactAnnouncementIdById")),
            @Result(property = "comments", column = "announcement_id",
            many = @Many(select = "org.example.zentrio.repository.CommentRepository.getAllAnnouncementComments")),
    })
    Announcement createAnnouncement(@Param("request") AnnouncementRequest request, UUID creator);


    @Select("""
        SELECT * FROM  announcements WHERE announcement_id= #{announcementId}
        """)
    @ResultMap("announcementMapper")
    Announcement getAnnouncementById(UUID announcementId);

    @Select("""
        UPDATE announcements SET text_content= #{re.description},
                                 title= #{re.title}  ,      
                                 update_at= #{now}
        WHERE announcement_id =#{announcementId} AND bord_id= #{bordId}
       RETURNING * 
       """)
    @ResultMap("announcementMapper")
    Announcement updateAnnouncementById(UUID announcementId,@Param("re") AnnouncementRequest request, LocalDateTime now, UUID bordId);

    @Select("""
        UPDATE announcements SET is_pinned= #{b}
        WHERE announcement_id= #{announcementId}
       RETURNING * 
       """)
    @ResultMap("announcementMapper")
    Announcement updateAnnouncementPinnedById(boolean b, UUID announcementId );


    @Select("""
            DELETE FROM announcements WHERE announcement_id= #{announcementId} AND  created_by= #{pmId}
            """)
    void deletedAnnouncementPinnedById(UUID announcementId, UUID pmId);

    @Select("""
            SELECT * FROM announcements WHERE bord_id= #{boardId}
            """)
    @ResultMap("announcementMapper")
    List<Announcement> getAnnouncementsByBoardId(UUID boardId);


    @Select("""
                SELECT  image_url FROM announcement_images WHERE announcement_id= #{announcementId}
                """)
    List<String> getImageUrl(UUID announcementId);
}
