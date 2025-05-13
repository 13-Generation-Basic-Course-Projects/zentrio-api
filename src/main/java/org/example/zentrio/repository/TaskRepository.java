package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.StatusRequest;
import org.example.zentrio.dto.request.TaskRequest;
import org.example.zentrio.model.Task;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TaskRepository {

    @Select("""
                INSERT INTO tasks(title, description, started_at, finished_at, stage, board_id, gantt_bar_id) VALUES(#{request.title}, #{request.description}, #{request.startedAt}, #{request.finishedAt}, 'TO DO', #{boardId}, #{ganttBarId})
                RETURNING *
            """)
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
            @Result(property = "ganttBarTitle", column = "gantt_bar_id", one = @One(select = "org.example.zentrio.repository.GanttBarRepository.getGanttBarName")),
    })
    Task createTask(UUID boardId, UUID ganttBarId, @Param("request") TaskRequest taskRequest);

    @Select("""
                SELECT * FROM tasks WHERE gantt_bar_id = #{ganttBarId}
                LIMIT #{limit} OFFSET #{offset}
            """)
    @ResultMap("taskMapper")
    List<Task> getAllTasksByGanttBarId(UUID ganttBarId, Integer limit, Integer offset);

    @Select("""
                SELECT * FROM tasks WHERE board_id = #{boardId}
                LIMIT #{limit} OFFSET #{offset}
            """)
    @ResultMap("taskMapper")
    List<Task> getAllTasksByBoardId(UUID boardId, Integer limit, Integer offset);

    @Select("""
                SELECT * FROM tasks WHERE board_id = #{boardId} AND gantt_bar_id = #{ganttBarId} AND task_id = #{taskId}
            """)
    @ResultMap("taskMapper")
    Task getTaskById(UUID boardId, UUID ganttBarId, UUID taskId);

    @Select("""
                SELECT * FROM tasks WHERE  title ILIKE '%' || #{title} || '%'
            """)
    @ResultMap("taskMapper")
    List<Task> getTaskByTitle(String title);

    @Select("""
                UPDATE tasks SET 
                                 title = #{request.title},
                                 description = #{request.description},
                                 started_at = #{request.startedAt},
                                 finished_at = #{request.finishedAt}
                WHERE task_id = #{taskId}
                RETURNING *
            """)
    @ResultMap("taskMapper")
    Task updateTaskById(UUID taskId, @Param("request") TaskRequest taskRequest);

    @Select("""
                UPDATE tasks SET title = #{title} WHERE task_id = #{taskId}
                RETURNING *
            """)
    @ResultMap("taskMapper")
    Task updateTaskTitleByTaskId(UUID taskId, String title);

    @Select("""
                UPDATE tasks SET description = #{description} WHERE task_id = #{taskId}
                RETURNING *
            """)
    @ResultMap("taskMapper")
    Task updateTaskDescriptionByTaskId(UUID taskId, String description);

    @Select("""
                DELETE FROM tasks WHERE task_id = #{taskId}
            """)
    @ResultMap("taskMapper")
    Task deleteTaskByTaskId(UUID taskId);

    @Select("""
                INSERT INTO task_assignment(assigned_by, assigned_to, task_id)
                VALUES (#{assignById}, #{assignedToId}, #{taskId})
            """)
    void assignMemberToTaskWithRole(UUID assignById, UUID assignedToId, UUID taskId);

    @Select("""
                SELECT * FROM tasks WHERE task_id = #{taskId}
            """)
    @ResultMap("taskMapper")
    Task getTaskByTaskId(UUID taskId);

    @Select("""
                SELECT * FROM tasks WHERE board_id = #{boardId} AND  title ILIKE '%' || #{title} || '%'
            """)
    @ResultMap("taskMapper")
    List<Task> getAllTaskByBoardIdAndTitle(UUID boardId, String title);

    @Select("""
                UPDATE tasks SET status = #{request.status}, updated_at = now()  WHERE task_id = #{taskId}
                RETURNING *
            """)
    @ResultMap("taskMapper")
    Task updateTaskStatusById(UUID taskId, @Param("request") StatusRequest statusRequest);


    @Select("""
                SELECT COUNT(*) FROM task_assignment
                WHERE task_id = #{taskId}
                AND assigned_to = #{memberId}
            """)
    boolean isMemberAlreadyAssignedToTask(UUID taskId, UUID memberId);

    //From Fanau

    @Select("""
                SELECT m.member_id FROM members m
                INNER JOIN task_assignment tk ON m.member_id = tk.assigned_to
                WHERE tk.task_id = #{taskId}
                AND m.user_id = #{userId}
            """)
    UUID findMemberIdByUserIdAndTaskId(UUID taskId, UUID userId);

    @Select("""
                SELECT r.role_name FROM task_assignment tk
                INNER JOIN members m ON tk.assigned_to = m.member_id
                INNER JOIN roles r ON m.role_id = r.role_id
                WHERE task_id = #{taskId}
                AND m.member_id = #{assignedById}
            """)
    String getRoleNameByUserIdAndTaskId(UUID taskId, UUID assignedById);


    @Select("""
                SELECT COUNT(*) FROM tasks
                WHERE gantt_bar_id = #{ganttBarId}
            """)
    Integer countTasksByGanttBarId( UUID ganttBarId);

    @Select("""
                SELECT COUNT(*) FROM tasks
                WHERE board_id = #{boardId}
            """)
    Integer countTasksByBoardId( UUID boardId);

}
