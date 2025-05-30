package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.TaskRequest;
import org.example.zentrio.enums.RoleName;
import org.example.zentrio.enums.Stage;
import org.example.zentrio.enums.Status;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ConflictException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.*;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.*;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final BoardService boardService;
    private final RoleRepository roleRepository;
    private final BoardRepository boardRepository;
    private final GanttBarRepository ganttBarRepository;
    private final ChecklistRepository checklistRepository;
    private final MemberRepository memberRepository;
    private final GanttChartService ganttChartService;


    private void validateTaskTimeWithGanttBar(TaskRequest taskRequest, GanttBar ganttBar) {
        LocalDateTime now = LocalDateTime.now();

        //  Task start time cannot be in the past
        if (taskRequest.getStartedAt().isBefore(now)) {
            throw new BadRequestException("Task start time cannot be in the past.");
        }

        //  Task finish time must be after start time
        if (taskRequest.getFinishedAt().isBefore(taskRequest.getStartedAt())) {
            throw new BadRequestException("Task finish time must be after start time.");
        }

        //  Task finish time must not exceed GanttBar's finish time
        if (taskRequest.getFinishedAt().isAfter(ganttBar.getFinishedAt())) {
            throw new BadRequestException("Task finish time cannot exceed GanttBar's finish time.");
        }

        //  Task start time must not be before GanttBar's start time
        if (taskRequest.getStartedAt().isBefore(ganttBar.getStartedAt())) {
            throw new BadRequestException("Task start time cannot be before GanttBar's start time.");
        }
    }


    private void validateBoardAndGanttBar(UUID boardId, UUID ganttBarId) {
        boardService.getBoardByBoardId(boardId);
        GanttBar ganttBar = ganttBarRepository.getGanttBarByGanttBarId(ganttBarId);
        if(ganttBar == null) {
            throw new NotFoundException("GanttBar id " + ganttBarId + " not found");
        }
    }


    private void validateBoardAndGanttBarAndTaskTime(UUID boardId, UUID ganttBarId, TaskRequest taskRequest) {
        //  Validate if board exists
        boardService.getBoardByBoardId(boardId);

        //  Load GanttBar entity from DB
        GanttBar ganttBar = ganttBarRepository.getGanttBarByGanttBarId(ganttBarId);
        if (ganttBar == null) {
            throw new NotFoundException("Gantt bar with ID " + ganttBarId + " not found");
        }
        ganttChartService.getGanttChartById(ganttBar.getGanttChartId(),boardId);
        //  Validate task timing against GanttBar timing
        validateTaskTimeWithGanttBar(taskRequest, ganttBar);
    }


    private void validateRoleManageTask(UUID boardId, UUID userId) {
        List<String> roles = roleRepository.getRolesNameByBoardIdAndUserId(boardId, userId);

        if (roles == null || roles.isEmpty()) {
            throw new ForbiddenException("You are not a member of this board");
        }

        boolean hasAccess = roles.stream().anyMatch(role -> role.equals(RoleName.ROLE_MANAGER.toString()) || role.equals(RoleName.ROLE_LEADER.toString()));

        if (!hasAccess) {
            throw new ForbiddenException("Only Project Managers or Team Leaders can manage tasks");
        }
    }


    public void validateTaskIdWithBoardIdAndGanttBarId(UUID taskId, UUID boardId, UUID ganttBarId) {
        getTaskById(taskId);
        validateBoardAndGanttBar(boardId, ganttBarId);
    }

    public void validateCurrentUserRoles(UUID boardId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        List<String> roleName = roleRepository.getRolesNameByBoardIdAndUserId(boardId, userId);
        if (roleName == null || !roleName.contains(RoleName.ROLE_MANAGER.toString())) {
            throw new ForbiddenException("You're not manager in this board can't assign role");
        }
    }


    @Override
    public Task createTaskByBoardIdAndGanttBarId(TaskRequest taskRequest) {
        Board board = boardRepository.getBoardByBoardId(taskRequest.getBoardId());
        if (board == null) {
            throw new NotFoundException("Board with ID " + taskRequest.getBoardId() + " not found");
        }
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();


        // Validate user has permission (Manager or TL)
        validateRoleManageTask(board.getBoardId(), userId);

        // Determine the role of the user in the board
        List<String> roles = roleRepository.getRolesNameByBoardIdAndUserId(board.getBoardId(), userId); // returns "MANAGER" or "TEAM_LEADER"
        System.out.println("role: " + roles);

        UUID creatorMemberId;

        if (roles.contains(RoleName.ROLE_MANAGER.toString())) {
            creatorMemberId = boardRepository.getManagerMemberIdByUserIdAndBoardId(userId, board.getBoardId());
        } else if (roles.contains(RoleName.ROLE_LEADER.toString())) {
            creatorMemberId = boardRepository.getTeamLeaderMemberIdByUserIdAndBoardId(userId, board.getBoardId());
        } else {
            throw new ForbiddenException("You're not a Manager or Team Leader in this board, can't create task");
        }

        // Validate timing and references
        validateBoardAndGanttBarAndTaskTime(board.getBoardId(), taskRequest.getGanttBarId(), taskRequest);

        // Create the task with the correct "created_by" (manager or TL)
        Task task = taskRepository.createTaskByBoardIdAndGanttBarId(taskRequest, board.getBoardId(), taskRequest.getGanttBarId(), creatorMemberId);

        if (roles.contains(RoleName.ROLE_LEADER.toString())) {
//            UUID assigneeMemberId = creatorMemberId; // TL's member ID
            UUID assignByMemberId = boardRepository.getManagerMemberIdByBoardId(board.getBoardId()); // Manager's member ID

            taskRepository.insertTaskAssignment(task.getTaskId(), assignByMemberId, creatorMemberId);
        }

        return task;
    }


    //  taskRepository.insertTaskAssignment(task.getTaskId(),managerId,memberId);
    @Override
    public Task getTaskById(UUID taskId) {
        Task task = taskRepository.getTaskByTaskId(taskId);
        if (task == null) {
            throw new NotFoundException("Task with ID " + taskId + " not found");
        }
        return task;
    }

    @Override
    public HashSet<Task> getAllTasksByBoardIdAndGanttBarId(UUID boardId, UUID ganttBarId) {
        validateBoardAndGanttBar(boardId, ganttBarId);
        HashSet<Task> tasks= taskRepository.getAllTasksByBoardIdAndGanttBarId(boardId, ganttBarId);
        if (tasks.isEmpty()) {
            throw new NotFoundException("Task is empty nothing to updated");
        }
        return tasks;

    }

    @Override
    public Task updateTaskByIdWithBoardIdAndGanttBarId(TaskRequest taskRequest, UUID taskId) {
        Board board = boardRepository.getBoardByBoardId(taskRequest.getBoardId());
        if(board == null) {
            throw new NotFoundException("Board with ID " + taskRequest.getBoardId() + " not found");
        }
        GanttBar ganttBar = ganttBarRepository.getGanttBarByGanttBarId(taskRequest.getGanttBarId());
        if(ganttBar == null) {
            throw new NotFoundException("GanttBar with ID " + taskRequest.getGanttBarId() + " not found");
        }
        // Step 1: Fetch the task
        Task task = getTaskById(taskId);

        // Step 2: Get current user ID
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();

        // Step 3: Validate user has permission role (Manager or Team Leader)
        validateRoleManageTask(taskRequest.getBoardId(), userId);

        Task taskAfterUpdate = new Task();

        // Step 6: Validate timing and references
        validateBoardAndGanttBarAndTaskTime(taskRequest.getBoardId(), taskRequest.getGanttBarId(), taskRequest);
        String role = memberRepository.getRoleInTask(task.getBoardId(),userId,task.getTaskId());
        System.out.println("role: " + role);
        if (role == null) {
            throw  new  ForbiddenException ("Only manager or team leader can update in "+task.getTitle()+ " can update it" );
        }
        if (role.contains(RoleName.ROLE_MANAGER.toString()) || role.contains(RoleName.ROLE_LEADER.toString())) {
            taskAfterUpdate =  taskRepository.updateTaskByIdWithBoardIdAndGanttBarId(taskRequest, taskId, taskRequest.getBoardId(), taskRequest.getGanttBarId(),LocalDateTime.now());
        }

        // Step 7: Update the task in the DB
      //  return taskRepository.updateTaskByIdWithBoardIdAndGanttBarId(taskRequest, taskId, taskRequest.getBoardId(), taskRequest.getGanttBarId(),LocalDateTime.now());
        return taskAfterUpdate;
    }


    @Override
    public Task deleteTaskByIdWithBoardIdAndGanttBarId(UUID taskId, UUID boardId, UUID ganttBarId) {
        // Step 1: Fetch task to check creator
        Task task = getTaskById(taskId);

        // Step 2: Validate task association with board and Gantt bar
        validateTaskIdWithBoardIdAndGanttBarId(taskId, boardId, ganttBarId);

        // Step 3: Get current user ID
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();

        // Step 4: Validate user has at least Manager or Team Leader role
        validateRoleManageTask(boardId, userId);

        // Step 5: Get all roles of the user in the board
        List<String> roles = roleRepository.getRolesNameByBoardIdAndUserId(boardId, userId);

        // Step 6: Role-based permission logic
        if (roles.contains(RoleName.ROLE_MANAGER.toString())) {
            // Manager can delete any task — proceed
        } else if (roles.contains(RoleName.ROLE_LEADER.toString())) {
            // Team Leader can only delete tasks they created
            UUID leaderMemberId = boardRepository.getTeamLeaderMemberIdByUserIdAndBoardId(userId, boardId);
            if (!leaderMemberId.equals(task.getCreatedBy())) {
                throw new ForbiddenException("Team Leader can only delete tasks they created");
            }
        } else {
            throw new ForbiddenException("You do not have permission to delete this task");
        }

        // Step 7: Perform deletion
        return taskRepository.deleteTaskByIdWithBoardIdAndGanttBarId(taskId, boardId, ganttBarId);
    }


    @Override
    public HashSet<Task> getTaskByTitleWithBoardId(String title, UUID boardId) {
        boardService.getBoardByBoardId(boardId);
        List<Task> task = taskRepository.getTaskByTitleWithBoardId(title, boardId);
        return new HashSet<>(task);
    }

    @Override
    public void assignLeaderToTask(UUID taskId, UUID assigneeId) {

        Task task = getTaskById(taskId);

        validateCurrentUserRoles(task.getBoardId());
        if (taskRepository.isAlreadyAssigned(taskId)) {
            throw new ConflictException("Task's with ID " + taskId + " is already assigned Leader");
        }

        UUID currentUserId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        UUID assignerMemberId = boardRepository.getManagerMemberIdByUserIdAndBoardId(currentUserId, task.getBoardId());
        System.out.println("assignerId: " + assignerMemberId);
        Boolean userExistInBoard= boardRepository.getExistUserInBoard(assigneeId,task.getBoardId());
        UUID leaderId = null;
        if (userExistInBoard){
              leaderId = taskRepository.getLeaderIdByUserIdAndBoardId(assigneeId, task.getBoardId());
            System.out.println("leaderId: " + leaderId);
             if (leaderId == null) {
//                 throw new ForbiddenException("This assignee with " + assigneeId + " not a leader of this board");
                 UUID roleId = roleRepository.getRoleIdByRoleName(RoleName.ROLE_LEADER.name());
                 boardRepository.insertMember(assigneeId,task.getBoardId(),roleId);
                 leaderId = taskRepository.getLeaderIdByUserIdAndBoardId(assigneeId, task.getBoardId());
                 System.out.println("leaderId after assign: " + leaderId);
             }
         }else {
            throw new ForbiddenException("This user is not member in this board");
        }


        taskRepository.insertTaskAssignment(taskId, assignerMemberId, leaderId);
    }

    @Override
    public void moveTask(UUID taskId, Stage stage) {

        // 1. Get current user info and boardId by taskId
        UUID currentUserId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Task task = getTaskById(taskId);
        UUID boardId = task.getBoardId();
        if(task.getStatus() == null || !task.getStatus().equals(Status.COMPLETED.toString())) {
            throw new BadRequestException("You can't move tasks with status " + task.getStatus());
        }

        // 2. Get user roles for the board
        List<String> userRoles = roleRepository.getRolesNameByBoardIdAndUserId(boardId, currentUserId);

//        System.out.println("userRole" + userRoles);

        // 3. Validate user permission for this stage
        if (!canUserMoveToStage(userRoles, stage.toString())) {
            throw new ForbiddenException("You don't have permission to move task to " + stage);
        }

        // 4. Stage-specific validation
        switch (stage.toString()) {
            case "IN_PROGRESS":
                if (!taskRepository.isAlreadyAssigned(taskId)) {
                    throw new BadRequestException("Cannot move to IN_PROGRESS without an assigned user.");
                }
                break;

            case "UNDER_REVIEW":
                String currentStage = taskRepository.getTaskStage(taskId);
                if (!"IN_PROGRESS".equals(currentStage)) {
                    throw new BadRequestException("Task must be IN_PROGRESS to move to UNDER_REVIEW.");
                }
                break;

            case "COMPLETED":
                long incompleteChecklistCount = checklistRepository.countIncompleteByTaskId(taskId);
                if (incompleteChecklistCount > 0) {
                    throw new BadRequestException("Cannot move to COMPLETED until all checklist items are done.");
                }
                break;

            default:
                // For other stages, you can add rules or allow by default
                break;
        }

        // 5. Update task stage
        taskRepository.updateTaskStage(taskId, stage.toString());
    }

    @Override
    public void updateStatusOfTaskById(UUID taskId, Status status) {
       Task task = getTaskById(taskId);
       UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        String role = memberRepository.getRoleInTask(task.getBoardId(),userId,taskId);
        System.out.println("role : " + role);
        if(role == null) {
            throw new ForbiddenException("You do not have permission to update this task");
        }
        if(task.getStatus() == null ) {
            throw new BadRequestException("You can't update tasks with status " + status);
        }
        if(RoleName.ROLE_MANAGER.toString().equals(role) || RoleName.ROLE_LEADER.toString().equals(role)){
            taskRepository.updateStatusOfTaskById(taskId,status.toString());
        }
    }

    @Override
    public void updateProgressOfTaskById(UUID taskId) {
        Task task = getTaskById(taskId);
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        System.out.println("task" + task.getBoardId());
        String roleName = roleRepository.getRoleNameByBoardIdAndUserId(task.getBoardId(),userId);
        System.out.println("roleName: " + roleName);
        if(roleName == null) {
            throw new ForbiddenException("You do not have permission to update this task");
        }


        taskRepository.updateProgressOfTaskById(taskId,Stage.IN_PROGRESS.toString());
    }

    @Override
    public Task getTaskByIdAndUserId(UUID taskId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Task task = getTaskById(taskId);
        if(task == null){
            throw new NotFoundException("Task with ID " + taskId + " not found");
        }
        return taskRepository.getTaskByIdAndUserId(taskId,userId);
    }

    @Override
    public HashSet<Task> getAllTasksForCurrentUser() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return taskRepository.getAllTasksForCurrentUser(userId);
    }

    @Override
    public HashSet<Task> getAllTasks() {
        return taskRepository.getAllTasks();
    }

    private boolean canUserMoveToStage(List<String> roles, String stage) {
        return switch (stage) {
            case "IN_PROGRESS", "UNDER_REVIEW", "COMPLETED" ->
                    roles.contains("ROLE_LEADER") || roles.contains("ROLE_MANAGER");
            default -> roles.contains("ROLE_MANAGER");
        };
    }


}