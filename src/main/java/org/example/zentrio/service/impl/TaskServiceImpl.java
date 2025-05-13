package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.StatusRequest;
import org.example.zentrio.dto.request.TaskRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.enums.RoleName;
import org.example.zentrio.enums.Status;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.*;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final BoardService boardService;
    private final GanttChartService ganttChartService;
    private final GanttBarService ganttBarService;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final AuthService authService;
    private final ChecklistRepository checklistRepository;

    public List<UUID> checkExistedGanttBarId( UUID ganttBarId){

        GanttBar getGanttBarById = ganttBarService.getGanttBarByGanttBarID(ganttBarId);
        if (getGanttBarById == null){
            throw new NotFoundException("Gantt Bar not found!");
        }
        if (!ganttBarId.equals(getGanttBarById.getGanttBarId())){
            throw new NotFoundException("Gantt Bar not found!");
        }
        UUID ganttChartId = getGanttBarById.getGanttChartId();
        if (ganttChartId == null){
            throw new NotFoundException("Gantt Bar Id not found!");
        }
        GanttChart getGanttChartById = ganttChartService.getGanttChartByID(ganttChartId);
        if (getGanttChartById == null){
            throw new NotFoundException("Gantt Chart not found!");
        }

        UUID boardId = getGanttChartById.getBoard_id();
        if (boardId != null) {
            boardService.checkExistedBoardId(boardId);
        } else {
            throw new NotFoundException("Board Id not found! Cannot create task!");
        }

        ganttBarId = getGanttBarById.getGanttBarId();

        return List.of(boardId, ganttBarId);
    }

    public UUID checkExistedBoardByUserId(UUID currentUserId, UUID boardId){

        if (boardId != null) {
            boardService.checkExistedBoardId(boardId);
        } else {
            throw new NotFoundException("Board Id not found! Cannot create task!");
        }

        Member checkMember = memberRepository.getMemberByUserIdAndBoardId(currentUserId, boardId);
        if (checkMember == null){
            throw new BadRequestException("You are not a member here!");
        }
//        System.out.println("checkMember : " + checkMember);
//        System.out.println("checkMember id : " + checkMember.getMemberId());

        UUID roleIdOfExistedMember = memberRepository.getRoleIdByMemberId(checkMember.getMemberId());
//        System.out.println("roleIdOfExistedMember : " + roleIdOfExistedMember);
        String roleOfExistedMember = roleRepository.getRoleNameByRoleId(roleIdOfExistedMember);
//        System.out.println("roleOfExistedMember : " + roleOfExistedMember);
        String managerRole = RoleName.ROLE_MANAGER.toString();
        String leaderRole = RoleName.ROLE_LEADER.toString();
        if (roleOfExistedMember.isEmpty()){
            throw new BadRequestException("You are not a member here!");
        }
        if (!roleOfExistedMember.equals(managerRole)){
            if (!roleOfExistedMember.equals(leaderRole)){
                throw new BadRequestException("You are not a MANAGER or a LEADER here!");
            }
        }

        return boardId;
    }

    @Override
    public TaskRequest checkDateOfCreateTask(UUID ganttBarId, TaskRequest taskRequest) {

        GanttBar ganttBar = ganttBarService.getGanttBarByGanttBarID(ganttBarId);
        if (ganttBar == null){
            throw new NotFoundException("Gantt Bar not found!");
        }

        LocalDateTime startDateOfGanttBar = ganttBar.getStartAt();
        LocalDateTime finishDateOfGanttBar = ganttBar.getFinishedAt();
        if (startDateOfGanttBar == null || finishDateOfGanttBar == null){
            throw new BadRequestException("Deadline of Gantt Bar not found");
        }

        if (taskRequest.getStartedAt().isBefore(startDateOfGanttBar)){
            throw new BadRequestException("Start date of task cannot be the past of start date of gantt bar!");
        }
        if (taskRequest.getFinishedAt().isAfter(finishDateOfGanttBar)){
            throw new BadRequestException("Finish date of task cannot be after of finish date of gantt bar!");
        }

        return taskRequest;
    }

    public UUID checkExistedTaskByTaskId(UUID taskId){
        Task task = taskRepository.getTaskByTaskId(taskId);
        if (task == null){
            throw new NotFoundException("Task Id not found!");
        }
        taskId = task.getTaskId();

        checkExistedGanttBarId( task.getGanttBarId());
        UUID currentUserId = authService.getCurrentAppUserId();
        UUID boardId = checkExistedBoardByUserId(currentUserId, task.getBoardId());
        Task existedTask = taskRepository.getTaskById(boardId, task.getGanttBarId(), taskId);
        if (!taskId.equals(existedTask.getTaskId())){
            throw new NotFoundException("Task Id not found!");
        }
        return taskId;
    }

    @Override
    public Task createTask( UUID ganttBarId, TaskRequest taskRequest) {

        checkDateOfCreateTask(ganttBarId, taskRequest);
        List<UUID> existedIds = checkExistedGanttBarId(ganttBarId);
        UUID currentUserId = authService.getCurrentAppUserId();
        UUID boardId = existedIds.get(0);
        boardId = checkExistedBoardByUserId(currentUserId, boardId);

        return taskRepository.createTask(boardId, ganttBarId, taskRequest);
    }

    @Override
    public ApiResponse<List<Task>> getAllTasksByGanttBarId(UUID ganttBarId, Integer page, Integer size) {


        Integer offset = page * size;

        List<UUID> existedIds = checkExistedGanttBarId(ganttBarId);
        UUID existedBoardId = existedIds.get(0);
        System.out.println("board id : " + existedBoardId);
        ganttBarId = existedIds.get(1);
        System.out.println("gantt bar id : " + ganttBarId);

        List<Task> taskList = taskRepository.getAllTasksByGanttBarId( ganttBarId, size, offset);


        Integer totalElements = taskRepository.countTasksByGanttBarId( ganttBarId);
        Integer totalPages = (int) Math.ceil(totalElements / (double) size);

        return ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Get all tasks by gantt bar id successfully")
                .payload(taskList)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .pagination(new Pagination(page, totalElements, totalPages))
                .build();
    }

    @Override
    public ApiResponse<List<Task>> getAllTasksByBoardId(UUID boardId, Integer page, Integer size) {
        Integer offset = page * size;

        if (boardId != null) {
            boardService.checkExistedBoardId(boardId);
        } else {
            throw new NotFoundException("Board Id not found! Cannot create task!");
        }

        List<Task> taskList = taskRepository.getAllTasksByBoardId( boardId, size, offset);


        Integer totalElements = taskRepository.countTasksByBoardId( boardId);
        Integer totalPages = (int) Math.ceil(totalElements / (double) size);

        return ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Get all tasks by board id successfully")
                .payload(taskList)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .pagination(new Pagination(page, totalElements, totalPages))
                .build();
    }

    @Override
    public Task updateTaskStatusById(UUID taskId, StatusRequest statusRequest) {

        Task task = getTaskById(taskId);
        if (task == null){
            throw new NotFoundException("Task not found!");
        }

        taskId = task.getTaskId();
        List<UUID> existedIds = checkExistedGanttBarId(task.getGanttBarId());
        if (existedIds == null){
            throw new NotFoundException("Gantt bar not found!");
        }
        UUID ganttBarId = existedIds.get(1);
        if (ganttBarId == null){
            throw new NotFoundException("Gantt bar not found!");
        }
        GanttBar ganttBar = ganttBarService.getGanttBarByGanttBarID(ganttBarId);
        if (ganttBar == null){
            throw new NotFoundException("Gantt bar not found!");
        }

        List<Checklist> allChecklistsById = checklistRepository.getAllChecklistByTaskId(taskId);

        task.setUpdatedAt(LocalDateTime.now());
        LocalDateTime setUpdatedAtTask = task.getUpdatedAt();


            if (setUpdatedAtTask.isBefore(task.getFinishedAt())){
                Boolean isDone = false;
                for (Checklist checklist : allChecklistsById) {
                    if (checklist.getStatus().equals(Status.DONE.toString()) || checklist.getStatus().equals(Status.OVERDUE.toString())){
                        isDone = true;
//                        statusRequest.setStatus(Status.DONE);
//                        taskRepository.updateTaskStatusById(taskId, statusRequest);
                    }

                    if (checklist.getStatus().equals(Status.PENDING.toString())){
                        isDone = false;
                    }
                }
                if (isDone.equals(true)){
                        statusRequest.setStatus(Status.DONE);
                        taskRepository.updateTaskStatusById(taskId, statusRequest);

                }
                if (isDone.equals(false)){
                    statusRequest.setStatus(Status.PENDING);
                    taskRepository.updateTaskStatusById(taskId, statusRequest);
                    throw new BadRequestException("Cannot update status! Checklists in this task still 'PENDING'!");
                }
            }

            if (setUpdatedAtTask.isAfter(task.getFinishedAt())){
                Boolean isDone = false;
                for (Checklist checklist : allChecklistsById) {
                    if (checklist.getStatus().equals(Status.DONE.toString()) || checklist.getStatus().equals(Status.OVERDUE.toString())){
                        isDone = true;
                    }

                    if (checklist.getStatus().equals(Status.PENDING.toString())){
                        isDone = false;
                    }
                }
                if (isDone.equals(true)){
                    statusRequest.setStatus(Status.OVERDUE);
                    taskRepository.updateTaskStatusById(taskId, statusRequest);
                }
                if (isDone.equals(false)){
                    statusRequest.setStatus(Status.PENDING);
                    taskRepository.updateTaskStatusById(taskId, statusRequest);
                    throw new BadRequestException("Cannot update status! Checklists in this task still 'PENDING'!");
                }
            }

        return task;
    }

    @Override
    public Task getTaskById( UUID taskId) {
        Task task = taskRepository.getTaskByTaskId(taskId);
        if (task == null){
            throw new NotFoundException("Task with this id not found!");
        }

        checkExistedGanttBarId(task.getGanttBarId());
        if (taskId == null){
            throw new NotFoundException("Task Id not found!");
        }
            return taskRepository.getTaskById(task.getBoardId(), task.getGanttBarId(), taskId);

    }

    @Override
    public List<Task> getTaskByTitle(UUID boardId, String title) {

        if (boardId != null) {
            boardService.checkExistedBoardId(boardId);
        } else {
            throw new NotFoundException("Board Id not found! Cannot create task!");
        }

        if (title == null){
            throw new NotFoundException("Task title not found!");
        }
        return taskRepository.getAllTaskByBoardIdAndTitle(boardId, title);
    }

    @Override
    public Task updateTaskById(UUID taskId, TaskRequest taskRequest) {

        checkExistedTaskByTaskId(taskId);
        return taskRepository.updateTaskById(taskId, taskRequest);
    }

    @Override
    public Task updateTaskTitleByTaskId( UUID taskId, String title) {

        checkExistedTaskByTaskId(taskId);
        return taskRepository.updateTaskTitleByTaskId(taskId, title);

    }

    @Override
    public Task updateTaskDescriptionByTaskId(UUID taskId, String description) {

        checkExistedTaskByTaskId(taskId);
        return taskRepository.updateTaskDescriptionByTaskId(taskId, description);

    }

    @Override
    public Task deleteTaskByTaskId( UUID taskId) {

        checkExistedTaskByTaskId(taskId);
        return taskRepository.deleteTaskByTaskId(taskId);

    }

    //From Fanau
    @Override
    public Task assignRole(UUID boardId, UUID assignToUserId) {
        Board boardByBoardId = boardRepository.getBoardByBoardId(boardId);


        if (boardByBoardId == null){
            throw new NotFoundException("Board cannot found by board id!");
        }
        UUID getBoardId = boardByBoardId.getBoardId();
        if (!boardId.equals(getBoardId)){
            throw new BadRequestException("Board id is not existed!");
        }

        boolean isAlreadyAssignedRole = boardRepository.isMemberAlreadyAssignedRoleToBoard(boardId, assignToUserId);
        if(isAlreadyAssignedRole) {
            throw new BadRequestException("You are already member of this board");
        }

        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        System.out.println("userid : " + userId);
        String roleName = roleRepository.getRoleNameByUserIdAndBoardId(boardId, userId);
        if (roleName == null) {
            throw new BadRequestException("You are doesn't have a role to assign this task");
        }

        if (!roleName.equals(RoleName.ROLE_MANAGER.toString())) {
            throw new BadRequestException("You are not PM can't assign role for member in this task");
        }

//        load roleId by passing roleName;
        UUID roleId = roleRepository.getRoleIdByRoleName(RoleName.ROLE_LEADER.toString());
//        System.out.println("targetBoardId = " + getBoardId);
//        input role for member that has invite
//        roleRepository.insertUserRoles(userIdFromEmailInput, roleId);

//        insert userId that have roleId = ROLE_LEADER into boardId
        roleRepository.insertToMember(assignToUserId, getBoardId, roleId);
        return null;
    }


    @Override
    public Task assignUserToTaskWithRole(UUID assignedToUserId, UUID taskId) {

        System.out.println("assignedToUserId = " + assignedToUserId);

        Task task = taskRepository.getTaskByTaskId(taskId);
        UUID boardId = task.getBoardId();

        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();

        // load user id input for sure id of the user are correct in current board
        UUID assignUserIdInMemberTable = roleRepository.getMemberIdByUserIdAndBoardId(boardId, userId); // pm id assign

        String managerRole = roleRepository.getRoleNameByUserIdAndBoardId(boardId, userId);
//        System.out.println("managerRole = " + managerRole);

        if (managerRole == null) {
            throw new BadRequestException("You are doesn't have a role to assign this task");
        }
        if (!managerRole.equals(RoleName.ROLE_MANAGER.toString())) {
            throw new BadRequestException("You are not PM can't assign member in this task");
        }


        System.out.println("assignUserIdInMemberTable = " + assignUserIdInMemberTable);
        if (assignUserIdInMemberTable == null) {
            throw new BadRequestException("Assigned user does not exist");
        }

        // load role of the user that are assign in this task doesn't have role leader -> exist
        UUID assignTo = roleRepository.getMemberIdByUserIdAndBoardId(boardId, assignedToUserId); // assign to
        System.out.println("assignTo = " + assignTo);

        if(assignTo == null) {
            throw new BadRequestException("User id that u was assign was " + assignedToUserId + " not exist");
        }

        // load role for user using id of user input
        String roleName = roleRepository.getRoleNameByUserIdAndBoardId(boardId, assignedToUserId);
        System.out.println("roleName = " + roleName);


        if (roleName == null) {
            throw new BadRequestException("You doesn't have role as a leader you should ");
        }
        // if user doesn't have any role in board with table member
        if (!roleName.equals(RoleName.ROLE_LEADER.toString())) {
            throw new BadRequestException("You should let's manager assign role first ");
        }

        boolean alreadyAssigned = taskRepository.isMemberAlreadyAssignedToTask(taskId, assignTo);
        if(alreadyAssigned) {
            throw new BadRequestException("This team leader is already assigned to this task.");
        }


        // insert assignerId , assignToId , and taskId , with role as a Leader
        taskRepository.assignMemberToTaskWithRole(assignUserIdInMemberTable, assignTo, taskId);
        return task;
    }




}