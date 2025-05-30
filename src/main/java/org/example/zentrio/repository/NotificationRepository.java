package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.model.Notification;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Mapper
public interface NotificationRepository {

    @Results(id = "notificationMapper", value = {
            @Result(property = "notificationId", column = "notification_id"),
            @Result(property = "content", column = "content"),
            @Result(property = "type", column = "type"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "isRead", column = "is_read"),
            @Result(property = "taskId", column = "task_assign_id"),
            @Result(property = "senderId", column = "sender_id"),
            @Result(property = "receiverId", column = "receiver_id")
    })

    @Select("""
                SELECT * FROM notifications
                WHERE receiver_id = #{userId}
                ORDER BY created_at DESC
            """)
    HashSet<Notification> getNotificationsByUserId(UUID userId);

    @Select("""
                INSERT INTO notifications(notification_id, content, type, is_read, created_at, task_assign_id, sender_id, receiver_id)
                VALUES(#{notificationId}, #{content}, #{type}, #{isRead}, #{createdAt}, #{taskId}, #{senderId},#{receiverId})
            """)
    @ResultMap("notificationMapper")
    void insertNotification(Notification notification);
}
