package org.example.zentrio.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.ChecklistRequest;
import org.example.zentrio.dto.response.MemberResponseData;
import org.example.zentrio.model.Checklist;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ChecklistRepository {


    @Results(id = "checklistMapper", value = {
            @Result(property = "checklistId", column = "checklist_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "status", column = "status"),
            @Result(property = "cover", column = "cover"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "checklistOrder", column = "checklist_order"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "members", column = "checklist_id",
                    many = @Many(select = "getMembersByChecklistId")),
            @Result(property = "taskId", column = "task_id"),
            @Result(property = "createdBy", column = "created_by")
    })
    @Select("""
                INSERT INTO checklists(title, started_at, finished_at, task_id, created_by)
                VALUES (#{checklistRequest.title}, #{checklistRequest.startedAt}, #{checklistRequest.finishedAt},
                        #{taskId},
                        #{createdBy})
                RETURNING *
            """)
    Checklist createChecklist(@Param("checklistRequest") ChecklistRequest checklistRequest, UUID taskId, UUID createdBy);


    @Select("""
                SELECT * FROM checklists WHERE checklist_id = #{checklistId}
            """)
    @ResultMap("checklistMapper")
    Checklist getChecklistById(UUID checklistId);


    @Select("""
                SELECT COUNT(*) FROM checklists WHERE task_id = #{taskId}
            """)
    Integer countAllChecklistByTaskId(UUID taskId);


    @Select("""
                SELECT * FROM checklists WHERE task_id = #{taskId}
            """)
    @ResultMap("checklistMapper")
    HashSet<Checklist> getAllChecklistsByTaskId(UUID taskId);


    @Select("""
                UPDATE checklists SET title = #{checklistRequest.title},
                                      started_at = #{checklistRequest.startedAt},
                                      finished_at = #{checklistRequest.finishedAt},
                                      updated_at = #{updatedAt}
                WHERE checklist_id = #{checklistId} AND task_id = #{taskId}
                RETURNING *
            """)
    @ResultMap("checklistMapper")
    Checklist updateChecklistByIdAndTaskId(@Param("checklistRequest") ChecklistRequest checklistRequest, UUID checklistId, UUID taskId, LocalDateTime updatedAt);

    @Select("""
                DELETE FROM checklists WHERE checklist_id = #{checklistId} AND task_id = #{taskId}
                RETURNING *
            """)
    @ResultMap("checklistMapper")
    Checklist deleteChecklistByIdAndTaskId(UUID checklistId, UUID taskId);


    @Select("""
             SELECT m.member_id FROM members m
            INNER JOIN  roles r on r.role_id=m.role_id
             WHERE m.board_id = #{boardId}
               AND m.user_id = #{assignedTo} AND r.role_name= 'ROLE_MEMBER'
            """)
    UUID findMemberIdByBoardIdAndUserId(UUID boardId, UUID assignedTo);


    @Select(""" 
                    INSERT INTO checklist_assignments (checklist_id,assigned_by, member_id)
                    VALUES (#{checklistId}, #{assignerId}, #{assigneeId})
            """)
    void insertToChecklistAssignment(UUID checklistId, UUID assignerId, UUID assigneeId);


    @Select("""
                    SELECT COUNT(*) > 0
                    FROM checklist_assignments
                    WHERE checklist_id = #{checklistId}
                    AND member_id = #{assigneeId}
            """)
    boolean checklistIsAssigned(UUID checklistId, UUID assigneeId);


    @Select("""
                UPDATE checklists
                SET cover = #{cover}
                WHERE checklist_id = #{checklistId}
                RETURNING *
            """)
    @ResultMap("checklistMapper")
    void updateCover(UUID checklistId, String cover);


    @Select("""
                SELECT COUNT(*) FROM checklists
                WHERE task_id = #{taskId}
                AND status != 'COMPLETED'
            """)
    long countIncompleteByTaskId(UUID taskId);


    @Select("""
        UPDATE checklists SET status = #{status}
        WHERE checklist_id = #{checklistId}
    """)
    void updateStatusOfChecklistById(UUID checklistId,String status);

    @Select("""
        SELECT * FROM checklists
    """)
    @ResultMap("checklistMapper")
    HashSet<Checklist> getAllChecklists();

    @Select("""
        SELECT c.* FROM members m
        INNER JOIN tasks ON m.member_id = tasks.created_by
        INNER JOIN checklists c ON tasks.task_id = c.task_id
        WHERE m.user_id = #{userId}
    """)
    @ResultMap("checklistMapper")
    HashSet<Checklist> getAllChecklistsByCurrentUser(UUID userId);

    @Select("""
            SELECT
                u.username AS userName,
                u.profile_image AS image
            FROM users u
                     INNER JOIN members m ON u.user_id = m.user_id
                  inner join checklist_assignments ch on m.member_id = ch.member_id
            WHERE ch.checklist_id = #{checklistId};
            """)
    @Results(id = "memberChecklistMapper", value = {
            @Result(property = "imageUrl", column = "image"),
            @Result(property = "userName", column = "name"),
    })
    List<MemberResponseData> getMembersByChecklistId(UUID checklistId);




}