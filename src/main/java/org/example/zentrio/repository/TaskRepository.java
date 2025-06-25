package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.TaskRequest;
import org.example.zentrio.dto.response.*;
import org.example.zentrio.model.Task;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Mapper
public interface TaskRepository {


    @Results(id = "taskMapper", value = {
            @Result(property = "taskId", column = "task_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "description"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "taskOrder", column = "task_order"),
            @Result(property = "stage", column = "stage"),
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "ganttBarId", column = "gantt_bar_id"),
            @Result(property = "ganttBar", column = "gantt_bar_id",
            one = @One(select = "getGanttBarById")),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "creator", column = "created_by",
                    one = @One(select = "getDataOfUserCreator")),
            @Result(property = "teamLeader", column = "task_id",
                    one = @One(select = "getTeamLeadData")),
            @Result(property = "feedbacks", column = "task_id",
                    many = @Many(select = "org.example.zentrio.repository.FeedbackRepository.getAllFeedback")),
    })
    @Select("""
            INSERT INTO tasks(title, description,started_at, finished_at, stage, board_id, gantt_bar_id, created_by)
            VALUES(#{request.title}, #{request.description}, #{request.startedAt}, #{request.finishedAt}, 'TO DO',
                   #{boardId},
                   #{ganttBarId},
                   #{createdBy})
               RETURNING *
            """)
    Task createTaskByBoardIdAndGanttBarId(@Param("request") TaskRequest taskRequest, UUID boardId, UUID ganttBarId, UUID createdBy);

    @Select("""
                SELECT * FROM tasks WHERE task_id  = #{taskId}
            """)
    @ResultMap("taskMapper")
    Task getTaskByTaskId(UUID taskId);

    @Select("""
                SELECT * FROM tasks WHERE board_id  = #{boardId} AND gantt_bar_id  = #{ganttBarId}
            """)
    @ResultMap("taskMapper")
    HashSet<Task> getAllTasksByBoardIdAndGanttBarId(UUID boardId, UUID ganttBarId);

    @Select("""
                SELECT * FROM tasks WHERE board_id  = #{boardId}
            """)
    @ResultMap("taskMapper")
    List<Task> getAllTasksByBoard(UUID boardId);

    @Select("""
                UPDATE tasks SET
                                 title = #{request.title},
                                 description = #{request.description},
                                 started_at = #{request.startedAt},
                                 finished_at = #{request.finishedAt},
                                 updated_at = #{updatedAt}
                WHERE task_id = #{taskId}
                AND board_id = #{boardId}
                AND gantt_bar_id = #{ganttBarId}
                RETURNING *
            """)
    @ResultMap("taskMapper")
    Task updateTaskByIdWithBoardIdAndGanttBarId(@Param("request") TaskRequest taskRequest, UUID taskId, UUID boardId, UUID ganttBarId, LocalDateTime updatedAt);

    @Select("""
                DELETE FROM tasks
                WHERE task_id  = #{taskId}
                AND board_id = #{boardId}
                AND gantt_bar_id = #{ganttBarId}
            """)
    Task deleteTaskByIdWithBoardIdAndGanttBarId(UUID taskId, UUID boardId, UUID ganttBarId);

    @Select("""
                SELECT * FROM tasks
                WHERE board_id = #{boardId}
                AND title ILIKE CONCAT('%', #{title}, '%')
            """)
    @ResultMap("taskMapper")
    List<Task> getTaskByTitleWithBoardId(String title, UUID boardId);

    @Select("""
                INSERT INTO task_assignments(task_id, assigned_by, assigned_to)
                VALUES (#{taskId}, #{assignerId}, #{assignToUserId})
            """)
    void insertTaskAssignment(UUID taskId, UUID assignerId, UUID assignToUserId);


    @Select("""
            SELECT EXISTS (
            SELECT 1 FROM task_assignments ta
            WHERE ta.task_id = #{taskId})
            """)
    boolean isAlreadyAssigned(UUID taskId);


    @Select("""
                SELECT m.member_id FROM members m
                INNER JOIN task_assignments ta ON m.member_id = ta.assigned_to
                WHERE m.user_id = #{assignedBy}
                AND ta.task_id = #{taskId}
            """)
    UUID getMemberIdByUserIdAndTaskId(UUID assignedBy, UUID taskId);


    @Select("""
                SELECT m.member_id FROM members m
                INNER JOIN roles r ON m.role_id = r.role_id
                WHERE m.user_id = #{userId}
                  AND m.board_id = #{boardId}
                AND r.role_name = 'ROLE_LEADER'
                LIMIT 1
            """)
    UUID getLeaderIdByUserIdAndBoardId(UUID userId, UUID boardId);


    @Select("""
                SELECT stage FROM tasks WHERE task_id = #{taskId}
            """)
    String getTaskStage(UUID taskId);


    @Select("""
                UPDATE tasks
                SET stage = #{stage}
                WHERE task_id = #{taskId}
            """)
    void updateTaskStage(UUID taskId, String stage);


    @Select("""
                UPDATE tasks SET status = #{status}
                WHERE task_id = #{taskId}
            """)
    void updateStatusOfTaskById(UUID taskId, String status);


    @Select("""
                UPDATE tasks SET stage = #{stage}
                WHERE task_id = #{taskId}
            """)
    void updateProgressOfTaskById(UUID taskId, String stage);


    @Select("""
                SELECT r.role_name FROM members m
                INNER JOIN task_assignments ta ON m.member_id = ta.assigned_to
                INNER JOIN roles r ON m.role_id = r.role_id
                WHERE ta.task_id = #{taskId}
            """)
    String getRoleNameByTaskId(UUID taskId);


    @Select("""
                SELECT t.*
                FROM tasks t
                INNER JOIN members m ON t.created_by = m.member_id
                WHERE t.task_id = #{taskId}
                AND m.user_id = #{userId}
            """)
    @ResultMap("taskMapper")
    Task getTaskByIdAndUserId(UUID taskId, UUID userId);

    @Select("""
                SELECT t.*
                FROM tasks t
                INNER JOIN members m ON t.created_by = m.member_id
                WHERE m.user_id = #{userId}
            """)
    @ResultMap("taskMapper")
    HashSet<Task> getAllTasksForCurrentUser(UUID userId);

    @Select("""
                SELECT * FROM tasks
            """)
    @ResultMap("taskMapper")
    HashSet<Task> getAllTasks();

    @Select("""
                SELECT u.username, u.profile_image , u.user_id , u.gender FROM users u
                INNER JOIN members m ON u.user_id = m.user_id
                WHERE m.member_id = #{userId}
            """)
    @Results(id = "memberMapper", value = {

            @Result(property = "userId", column = "user_id"),
            @Result(property = "username", column = "username"),
            @Result(property = "imageUrl", column = "profile_image")
    })
    MemberResponseData getDataOfUserCreator(UUID userId);


    @Select("""
                SELECT board_id FROM tasks WHERE task_id = #{taskId}
            """)
    UUID getBoardIdByTaskId(UUID taskId);


    @Select("""
            SELECT m.member_id
        FROM members m
        INNER JOIN roles r ON r.role_id = m.role_id
        INNER JOIN task_assignments tk ON tk.assigned_to = m.member_id OR tk.assigned_by = m.member_id
        WHERE m.user_id = #{userId}
          AND m.board_id = #{boardId}
          AND tk.task_id = #{taskId}
        LIMIT 1;
        
        """)
    UUID getManagerOrLeader(UUID boardId, UUID userId, UUID taskId);


    @Select("""
        SELECT gb.title, gb.face FROM gantt_bars gb
        WHERE gantt_bar_id = #{ganttBarId}
    """)
    @Results(id = "ganttBarMapper", value = {
            @Result(property = "title",column = "title"),
            @Result(property = "face",column = "face")
    })
    GanttBarResponse getGanttBarById(UUID ganttBarId);


    @Results(id = "taskResponeMapper", value = {
            @Result(property = "taskId", column = "task_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "description"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "taskOrder", column = "task_order"),
            @Result(property = "stage", column = "stage"),
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "ganttBarId", column = "gantt_bar_id"),
            @Result(property = "ganttBar", column = "gantt_bar_id",
                    one = @One(select = "getGanttBarById")),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "creator", column = "created_by",
                    one = @One(select = "getDataOfUserCreator")),

            @Result(property = "teamLeader", column = "task_id",
                    one = @One(select = "getTeamLeadData")),
            @Result(property = "allChecklists", column = "task_id",
                    many = @Many(select = "org.example.zentrio.repository.ChecklistRepository.getAllDataInChecklistByTaskId")),
    })
    @Select("""
        SELECT  * FROM tasks WHERE board_id= #{boardId} 
        """)
    List<TaskResponse> getAllDataInTaskByBoardId(UUID boardId);

    @Select("""
                SELECT * FROM tasks WHERE board_id= #{boardId}
            """)
    @ResultMap("taskMapper")
    HashSet<Task> getTasksByBoardId(UUID boardId);


    @Select("""
        
            UPDATE tasks
        SET task_order = CASE
                             WHEN task_order = #{oldOrder}  THEN #{newOrder}                            
                             WHEN task_order BETWEEN #{newOrder} AND #{till} THEN task_order + 1    
                             ELSE task_order
            END
        WHERE board_id = #{boardId}
          AND task_order BETWEEN #{newOrder} AND #{oldOrder};
        """)
    void moveTaskOrderDown(int oldOrder, int newOrder, int till, UUID boardId);

    @Select("""
        
            UPDATE tasks
            SET task_order = CASE
                             WHEN task_order = #{oldOrder} THEN #{newOrder}                              
                             WHEN task_order BETWEEN #{till} AND #{newOrder} THEN task_order - 1     
                             ELSE task_order
            END
            WHERE board_id = #{boardId}
            AND task_order BETWEEN #{oldOrder} AND #{newOrder};
        """)
    void moveTaskOrderUp(int oldOrder, int newOrder, int till, UUID boardId);


    @Select("""
        
            SELECT
            u.profile_image AS image,
            u.username AS name,
            u.user_id,
            u.gender
        FROM
            task_assignments ta
                INNER JOIN
            members m ON ta.assigned_to = m.member_id
                INNER JOIN
            users u ON m.user_id = u.user_id
        WHERE
            ta.task_id = #{taskId};
        """)
    @Results(id = "teamLeadResponseMapper", value = {
            @Result(property = "userId", column = "user_id"),
            @Result(property = "imageUrl", column = "image"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "username", column = "name"),
    })
    MemberResponseData getTeamLeadData(UUID taskId);


    @Select("""
       SELECT COUNT(*) > 0
       FROM task_assignments
       WHERE task_id = #{taskId}
        AND assigned_to = #{userId}
    """)
    boolean isUserAssignedToTask(UUID userId, UUID taskId);

    @Select("""
        SELECT assigned_to FROM task_assignments
        WHERE assigned_to = #{userId}
        AND task_id = #{taskId}
    """)
    UUID getLeaderIdByUserIdAndTaskId(UUID userId, UUID taskId);

    @Select("""
        SELECT *  FROM tasks
        WHERE board_id = #{boardId}
    """)
    List<Task> getAllTasksByBoardId(UUID boardId);

}
