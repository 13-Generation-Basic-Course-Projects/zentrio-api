package org.example.zentrio.service;

import org.example.zentrio.dto.request.StatusRequest;
import org.example.zentrio.dto.request.TaskRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.Task;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    Task createTask(UUID ganttBarId, TaskRequest taskRequest);

    ApiResponse<List<Task>> getAllTasksByGanttBarId(UUID ganttBarId,Integer page,Integer size);

    Task getTaskById(UUID taskId);

    List<Task> getTaskByTitle(UUID boardId,String title);

    Task updateTaskById(UUID taskId, TaskRequest taskRequest);

    Task updateTaskTitleByTaskId(UUID taskId, String title);

    Task updateTaskDescriptionByTaskId(UUID taskId, String description);

    Task deleteTaskByTaskId( UUID taskId);

    UUID checkExistedBoardByUserId(UUID currentUserId, UUID boardId);

    TaskRequest checkDateOfCreateTask(UUID ganttBarId, TaskRequest taskRequest);

    Task assignRole(UUID boardId,UUID assignToUserId);

    Task assignUserToTaskWithRole(UUID assignedToUserId, UUID taskId);

    ApiResponse<List<Task>> getAllTasksByBoardId(UUID boardId, Integer page, Integer size);

    Task updateTaskStatusById(UUID taskId, StatusRequest statusRequest);
}
