package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.StatusRequest;
import org.example.zentrio.dto.request.TaskRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.DeleteApiResponse;
import org.example.zentrio.model.Checklist;
import org.example.zentrio.model.Task;
import org.example.zentrio.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Task Controller")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create task by gantt bar id")
    @PostMapping("/gantt-bar-id/{gantt-bar-id}")
    public ResponseEntity<ApiResponse<Task>> createTask(
            @PathVariable("gantt-bar-id") UUID ganttBarId,
            @RequestBody @Valid TaskRequest taskRequest) {
        ApiResponse<Task> response = ApiResponse.<Task>builder()
                .success(true)
                .message("Created task by gantt bar id successfully!")
                .status(HttpStatus.CREATED)
                .payload(taskService.createTask( ganttBarId, taskRequest))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all tasks by gantt bar id")
    @GetMapping("/gantt-bar-id/{gantt-bar-id}")
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasksByGanttBarId(
            @PathVariable("gantt-bar-id") UUID ganttBarId,
            @RequestParam(defaultValue = "0")  Integer page,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        ApiResponse<List<Task>> response = taskService.getAllTasksByGanttBarId(ganttBarId,page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Get all tasks by board id")
    @GetMapping("boards/board-id/{board-id}")
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasksByBoardId(
            @PathVariable("board-id") UUID boardId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        ApiResponse<List<Task>> response = taskService.getAllTasksByBoardId(boardId,page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Get task by task id")
    @GetMapping("/task-id/{task-id}")
    public ResponseEntity<ApiResponse<Task>> getTaskById(@PathVariable("task-id") UUID taskId) {
        ApiResponse<Task> response = ApiResponse.<Task>builder()
                .success(true)
                .message("Get task by id successfully!")
                .status(HttpStatus.OK)
                .payload(taskService.getTaskById(taskId))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get task by board id task title")
    @GetMapping("boards/board-id/{board-id}/")
    public ResponseEntity<ApiResponse<List<Task>>> getTaskByTitle(
            @PathVariable("board-id") UUID boardId,
            @RequestParam String title) {
        ApiResponse<List<Task>> response = ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Get tasks by board id and title successfully!")
                .status(HttpStatus.OK)
                .payload(taskService.getTaskByTitle(boardId, title))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update task by task id")
    @PutMapping("/update/{task-id}")
    public ResponseEntity<ApiResponse<Task>> updateTaskById(
            @PathVariable("task-id") UUID taskId,
            @RequestBody @Valid TaskRequest taskRequest) {
        ApiResponse<Task> response = ApiResponse.<Task>builder()
                .success(true)
                .message("Update task by id successfully!")
                .status(HttpStatus.OK)
                .payload(taskService.updateTaskById(taskId, taskRequest))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update task title by task id")
    @PatchMapping("/updateTitle/{task-id}")
    public ResponseEntity<ApiResponse<Task>> updateTaskTitleByTaskId(
            @PathVariable("task-id") UUID taskId,
            @RequestBody String title) {
        ApiResponse<Task> response = ApiResponse.<Task>builder()
                .success(true)
                .message("Update task title by task id successfully!")
                .status(HttpStatus.OK)
                .payload(taskService.updateTaskTitleByTaskId(taskId, title))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update task description by task id")
    @PatchMapping("/{task-id}")
    public ResponseEntity<ApiResponse<Task>> updateTaskDescriptionByTaskId(
            @PathVariable("task-id") UUID taskId,
            @RequestBody String description) {
        ApiResponse<Task> response = ApiResponse.<Task>builder()
                .success(true)
                .message("Update task description by task id successfully!")
                .status(HttpStatus.OK)
                .payload(taskService.updateTaskDescriptionByTaskId(taskId, description))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete task by task id")
    @DeleteMapping("/delete/{task-id}")
    public ResponseEntity<DeleteApiResponse<Task>> deleteTaskByTaskId(
            @PathVariable("task-id") UUID taskId) {

        taskService.deleteTaskByTaskId(taskId);

        DeleteApiResponse<Task> response = DeleteApiResponse.<Task>builder()
                .success(true)
                .message("Delete task by task id successfully!")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update task status by task id")
    @PatchMapping("/task-id/{task-id}")
    public ResponseEntity<ApiResponse<Task>> updateTaskStatusById(
            @PathVariable("task-id") UUID taskId,
            @RequestBody StatusRequest statusRequest){
        ApiResponse<Task> response = ApiResponse.<Task> builder()
                .success(true)
                .message("Update task status by id successfully!")
                .status(HttpStatus.OK)
                .payload(taskService.updateTaskStatusById(taskId, statusRequest))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }


    //From Fanau


}