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
            @Result(property = "taskAssignId", column = "task_assign_id"),
            @Result(property = "senderId", column = "sender_id"),
            @Result(property = "sender", column = "sender_id",
                    one = @One(select = "org.example.zentrio.repository.AppUserRepository.getUserByUserId")),
            @Result(property = "receiverId", column = "receiver_id"),
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "board", column = "board_id",
            one = @One(select = "org.example.zentrio.repository.BoardRepository.getBoardByBoardId")),
            @Result(property = "taskId",column = "task_id"),
            @Result(property = "task",column = "task_id",
            one = @One(select = "org.example.zentrio.repository.TaskRepository.getTaskByTaskId" )),
            @Result(property = "checklistId",column = "checklist_id")
    })

    @Select("""
                SELECT * FROM notifications
                WHERE receiver_id = #{userId}
                ORDER BY created_at DESC
            """)
    HashSet<Notification> getNotificationsByUserId(UUID userId);

    @Select("""
                INSERT INTO notifications(notification_id, title, content, type, is_read, created_at, board_id, task_id, checklist_id, sender_id, receiver_id)
                VALUES(#{notificationId}, #{title}, #{content}, #{type}, #{isRead}, #{createdAt}, #{boardId},#{taskId},#{checklistId}, #{senderId}, #{receiverId})
            """)
    @ResultMap("notificationMapper")
    void insertNotification(Notification notification);


    @Select("""
        DELETE FROM notifications
        WHERE notification_id = #{notificationId}
        AND receiver_id = #{userId}
    """)
    void deleteNotificationByIdAndByUserId(UUID notificationId, UUID userId);

    @Select("""
        SELECT * FROM notifications WHERE notification_id = #{notificationId}
    """)
    @ResultMap("notificationMapper")
    Notification getNotificationById(UUID notificationId);


    @Select("""
        DELETE FROM notifications
        WHERE receiver_id = #{userId}
    """)
    void deleteAllNotifications(UUID userId);


    @Select("""
        SELECT * FROM notifications n
        INNER JOIN members m ON n.receiver_id = m.user_id
        WHERE n.receiver_id = #{userId}
        AND m.board_id = #{boardId}
    """)
    @ResultMap("notificationMapper")
    HashSet<Notification> getNotificationByUserIdAndBoardId(UUID userId, UUID boardId);
}
