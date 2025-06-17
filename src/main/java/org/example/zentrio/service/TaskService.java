package org.example.zentrio.service;



import org.example.zentrio.dto.request.TaskRequest;
import org.example.zentrio.dto.response.TaskResponse;
import org.example.zentrio.enums.Stage;
import org.example.zentrio.enums.Status;
import org.example.zentrio.model.Task;

import java.util.HashSet;
import java.util.UUID;

public interface TaskService {
    Task createTaskByBoardIdAndGanttBarId(TaskRequest taskRequest);

    Task getTaskById(UUID taskId);

    HashSet<Task> getAllTasksByBoardIdAndGanttBarId(UUID boardId, UUID ganttBarId);

    Task updateTaskByIdWithBoardIdAndGanttBarId(TaskRequest taskRequest, UUID taskId);

    Task deleteTaskByIdWithBoardIdAndGanttBarId(UUID taskId, UUID boardId, UUID ganttBarId);

    HashSet<Task> getTaskByTitleWithBoardId(String title, UUID boardId);

    void assignLeaderToTask(UUID taskId, UUID assigneeId);

    void moveTask(UUID taskId, Stage stage);

    void updateStatusOfTaskById(UUID taskId, Status status);

    void updateProgressOfTaskById(UUID taskId);

    Task getTaskByIdAndUserId(UUID taskId);

    HashSet<Task> getAllTasksForCurrentUser();

    HashSet<Task> getAllTasks();

    HashSet<TaskResponse> getTasksByBoardId(UUID boardId);

    Void moveOrder( UUID boardId,int newOrder, int oldOrder);


//    void assignLeaderRoleToTask(UUID taskId, UUID assignedId);
}
