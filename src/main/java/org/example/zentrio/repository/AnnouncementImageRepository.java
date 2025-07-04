package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.model.Announcement;
import org.example.zentrio.model.AnnouncementImage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AnnouncementImageRepository {


    @Select("""
        INSERT INTO announcement_images(image_url,uploaded_at,created_by,announcement_id)
        VALUES (#{imageUrl}, #{creationDate}, #{authorId},#{announcementId})
        """)
    void createAnnouncementImage(String imageUrl, LocalDateTime creationDate, UUID authorId, UUID announcementId);


    @Select("""
        UPDATE announcement_images SET announcement_id= #{announcementId}
        WHERE announcement_id IS NULL AND created_by= #{authorId}
        """)
    void insertAnnouncementImage(UUID announcementId , UUID authorId);


    @Select("""
            SELECT * FROM  announcement_images WHERE  announcement_id= #{announcementId}
            """)
    @Results(id = "AnnouncementImagesMapper", value = {
            @Result(property = "announcementImageId", column = "announcement_images_id"),
            @Result(property = "imageUrl", column = "image_url"),
            @Result(property = "creationDate", column = "uploaded_at"),
            @Result(property = "authorId", column = "created_by"),
            @Result(property = "announcementId", column = "announcement_id"),
    })
    List<AnnouncementImage> getALLAnnouncementImagesByAnnouncementId( UUID announcementId);


}
