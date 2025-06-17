package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.TaskRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.TaskResponse;
import org.example.zentrio.enums.Stage;
import org.example.zentrio.enums.Status;
import org.example.zentrio.model.Task;
import org.example.zentrio.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Task Controller")
public class TaskController {


    private final TaskService taskService;

    @Operation(summary = "Create task by boards ID, and gantt bars ID", description = "Created task with boards ID and gantt bars ID")
    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTaskByBoardIdAndGanttBarId(
            @Valid @RequestBody TaskRequest taskRequest) {
        ApiResponse<Task> apiResponse = ApiResponse.<Task>builder()
                .success(true)
                .message("Created task successfully")
                .payload(taskService.createTaskByBoardIdAndGanttBarId(taskRequest))
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(201).body(apiResponse);
    }


    @Operation(summary = "Get task by task ID", description = "Get task by ID with current boards")
    @GetMapping("/{task-id}")
    public ResponseEntity<ApiResponse<Task>> getTaskById(@PathVariable("task-id") UUID taskId) {
        ApiResponse<Task> apiResponse = ApiResponse.<Task>builder()
                .success(true)
                .message("Get task by ID successfully")
                .payload(taskService.getTaskById(taskId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get all task by board ID, and gantt bar ID", description = "Get all task by specific board ID with gantt bar ID")
    @GetMapping("/boards/{board-id}/gantt-bars/{gantt-bar-id}")
    public ResponseEntity<ApiResponse<HashSet<Task>>> getTasksByBoardIdAndGanttBarId(
            @PathVariable("board-id") UUID boardId,
            @PathVariable("gantt-bar-id") UUID ganttBarId) {
        ApiResponse<HashSet<Task>> apiResponse = ApiResponse.<HashSet<Task>>builder()
                .success(true)
                .message("Get all task by gantt ID and board ID successfully")
                .payload(taskService.getAllTasksByBoardIdAndGanttBarId(boardId, ganttBarId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Get all task by board ID", description = "Get all task by specific board ID ")
    @GetMapping("/boards/{board-id}")
    public ResponseEntity<ApiResponse<HashSet<TaskResponse>>> getTasksByBoardId(
            @PathVariable("board-id") UUID boardId) {
        ApiResponse<HashSet<TaskResponse>> apiResponse = ApiResponse.<HashSet<TaskResponse>>builder()
                .success(true)
                .message("Get all task by board ID successfully")
                .payload(taskService.getTasksByBoardId(boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Update task by ID with board ID AND gantt bar ID", description = "Updated task by ID with boards ID and gantt bars ID")
    @PutMapping("/{task-id}")
    public ResponseEntity<ApiResponse<Task>> updateTaskByIdWithBoardIdAndGanttBarId(
            @Valid @RequestBody TaskRequest taskRequest,
            @PathVariable("task-id") UUID taskId) {
        ApiResponse<Task> apiResponse = ApiResponse.<Task>builder()
                .success(true)
                .message("Updated task by ID successfully")
                .payload(taskService.updateTaskByIdWithBoardIdAndGanttBarId(taskRequest, taskId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Deleted task by ID with board ID AND gantt bar ID", description = "Deleted task by ID with boards ID and gantt bars ID")
    @DeleteMapping("/{task-id}/boards/{board-id}/gantt-bars/{gantt-bar-id}")
    public ResponseEntity<ApiResponse<Task>> deleteTaskByIdWithBoardIdAndGanttBarId(
            @PathVariable("task-id") UUID taskId,
            @PathVariable("board-id") UUID boardId,
            @PathVariable("gantt-bar-id") UUID ganttBarId) {

        ApiResponse<Task> apiResponse = ApiResponse.<Task>builder()
                .success(true)
                .message("Deleted task by ID successfully")
                .payload(taskService.deleteTaskByIdWithBoardIdAndGanttBarId(taskId, boardId, ganttBarId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get task by title with board ID", description = "Get task by title with specific boards ID")
    @GetMapping("title/{title}/boards/{board-id}")
    public ResponseEntity<ApiResponse<HashSet<Task>>> getTaskByTitleWithBoardId(
            @PathVariable("title") String title,
            @PathVariable("board-id") UUID boardId) {
        ApiResponse<HashSet<Task>> apiResponse = ApiResponse.<HashSet<Task>>builder()
                .success(true)
                .message("Get task by ID successfully")
                .payload(taskService.getTaskByTitleWithBoardId(title, boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Assigned role with task ID", description = "Assigned role leader tasks with specific users")
    @PostMapping("/{task-id}/members/{assignee-id}")
    public ResponseEntity<?> assignLeaderToTask(
            @PathVariable("task-id") UUID taskId,
            @PathVariable("assignee-id") UUID assigneeId) {
        taskService.assignLeaderToTask(taskId, assigneeId);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Assigned role with task ID successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Move task with task ID", description = "Moved task to another stage with specific tasks ID")
    @PutMapping("/{task-id}/move")
    public ResponseEntity<?> moveTask(@PathVariable("task-id") UUID taskId, @RequestParam Stage stage) {
        taskService.moveTask(taskId, stage);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Moved task with task ID successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Update status of task by ID", description = "Update status of task by specific tasks ID")
    @PutMapping("/{task-id}/status")
    public ResponseEntity<?> updateStatusOfTaskById(
            @PathVariable("task-id") UUID taskId,
            @RequestParam Status status) {
        taskService.updateStatusOfTaskById(taskId,status);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Updated status task with task ID successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Update stage to progress by task ID after assigned Leaders", description = "Updated stage to IN_PROGRESS after assigned leaders")
    @PutMapping("/{task-id}/stage/in-progress")
    public ResponseEntity<?> updateProgressOfTaskById(@PathVariable("task-id") UUID taskId) {
        taskService.updateProgressOfTaskById(taskId);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Updated task to IN_PROGRESS with task ID successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get task by ID with current user",description = "Get task by ID with current user ID")
    @GetMapping("/{task-id}/users")
    public ResponseEntity<ApiResponse<Task>> getTaskByIdAndUserId(@PathVariable("task-id") UUID taskId) {
        ApiResponse<Task> apiResponse = ApiResponse.<Task>builder()
                .success(true)
                .message("Get task by ID with users ID successfully")
                .payload(taskService.getTaskByIdAndUserId(taskId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get all task for current users",description = "Get all task for current user ID")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<HashSet<Task>>> getAllTasksForCurrentUser() {
        ApiResponse<HashSet<Task>> apiResponse = ApiResponse.<HashSet<Task>>builder()
                .success(true)
                .message("Get all tasks for current users successfully")
                .payload(taskService.getAllTasksForCurrentUser())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get all task for all user",description = "Get all the tasks for all users")
    @GetMapping
    public ResponseEntity<ApiResponse<HashSet<Task>>> getAllTasks() {
        ApiResponse<HashSet<Task>> apiResponse = ApiResponse.<HashSet<Task>>builder()
                .success(true)
                .message("Get all tasks successfully")
                .payload(taskService.getAllTasks())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Move task in bord")
    @PutMapping("board/{board-id}")
    public ResponseEntity<ApiResponse<Void>> moveOrder(@PathVariable("board-id") UUID boardId ,
                                                      @RequestParam int newOrder,
                                                      @RequestParam int oldOrder) {
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(true)
                .message("Move tasks successfully")
                .payload(taskService.moveOrder(boardId,newOrder,oldOrder))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


}