package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.ChecklistRequest;
import org.example.zentrio.dto.request.StatusRequest;
import org.example.zentrio.model.Checklist;
import org.example.zentrio.model.Task;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ChecklistRepository {

    @Select("""
        INSERT INTO checklists(title, started_at, finished_at, task_id) 
        VALUES (#{request.title}, #{request.startedAt}, #{request.finishedAt}, #{taskId})
        RETURNING *
    """)
    @Results(id = "checklistMapper", value = {
            @Result(property = "checklistId", column = "checklist_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "checklistOrder", column = "checklist_order"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "taskId", column = "task_id"),
    })
    Checklist createChecklist(UUID taskId, @Param("request") ChecklistRequest checklistRequest);

    Checklist getChecklistByTaskId(UUID taskId);

    @Select("""
        SELECT * FROM checklists WHERE task_id = #{taskId}
    """)
    @ResultMap("checklistMapper")
    List<Checklist> getAllChecklistByTaskId(UUID taskId);

    @Select("""
        SELECT * FROM checklists WHERE checklist_id = #{checklistId}
    """)
    @ResultMap("checklistMapper")
    Checklist getChecklistByChecklistId(UUID checklistId);

    @Select("""
        SELECT * FROM checklists WHERE task_id = #{taskId} AND title ILIKE '%' || #{title} || '%'
    """)
    @ResultMap("checklistMapper")
    List<Checklist> getChecklistByTaskIdAndTitle(UUID taskId, String title);

    @Select("""
        UPDATE checklists SET
                              title = #{request.title},
                              started_at = #{request.startedAt},
                              finished_at = #{request.finishedAt}
        WHERE task_id = #{taskId} AND checklist_id = #{checklistId}
        RETURNING *
    """)
    @ResultMap("checklistMapper")
    Checklist updateChecklistById(UUID taskId, UUID checklistId, @Param("request") ChecklistRequest checklistRequest);

    @Select("""
        DELETE FROM checklists WHERE checklist_id = #{checklistId}
    """)
    @ResultMap("checklistMapper")
    Checklist deleteChecklistByChecklist( UUID checklistId);

    @Select("""
                SELECT COUNT(*) FROM checklist_assignments
                WHERE checklist_id = #{checklistId}
                AND   member_id = #{memberId}
            """)
    boolean isExistByUserIdAndTaskId(UUID checklistId,UUID memberId);

    // get checklist by checklist id
    @Select("""
        SELECT * FROM checklists WHERE checklist_id = #{checklistId}
    """)
    @ResultMap("checklistMapper")
    Checklist getChecklistById(UUID checklistId);

    @Select("""
        UPDATE checklists SET status = #{request.status}, updated_at = now() WHERE checklist_id = #{checklistId}
        RETURNING *
    """)
    @ResultMap("checklistMapper")
    Checklist updateChecklistStatusById(UUID checklistId, @Param("request") StatusRequest statusRequest);
}