package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.ChecklistRequest;
import org.example.zentrio.dto.request.StatusRequest;
import org.example.zentrio.enums.RoleName;
import org.example.zentrio.enums.Status;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.Board;
import org.example.zentrio.model.Checklist;
import org.example.zentrio.model.Task;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.AuthService;
import org.example.zentrio.service.ChecklistService;
import org.example.zentrio.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChecklistServiceImpl implements ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final MemberRepository memberRepository;
    private final AuthService authService;
    private final RoleRepository roleRepository;
    private final BoardRepository boardRepository;

    @Override
    public UUID checkExistedTaskId(UUID taskId, UUID currentUserId){

        Task taskById = taskRepository.getTaskByTaskId(taskId);
        if (taskById == null){
            throw new BadRequestException("Cannot find task by id!");
        }
        taskId = taskById.getTaskId();

        Board getBoardByTaskId = boardRepository.getBoardByTaskId(taskId);
        if (getBoardByTaskId == null){
            throw new NotFoundException("Cannot found board by task id!");
        }

        UUID existedMemberId = memberRepository.getMemberIdByUserIdAndBoardId(currentUserId, getBoardByTaskId.getBoardId());
        if (existedMemberId == null){
            throw new NotFoundException("User is not a member in this board!");
        }
        UUID roleIdOfExistedMember = memberRepository.getRoleIdByMemberId(existedMemberId);
        String roleOfExistedMember = roleRepository.getRoleNameByRoleId(roleIdOfExistedMember);
        if (roleOfExistedMember.isEmpty()){
            throw new NotFoundException("You are not a LEADER here!");
        }
        if (!roleOfExistedMember.equals(RoleName.ROLE_LEADER.toString())){
            throw new NotFoundException("You are not a LEADER here!");
        }
        if (roleOfExistedMember.equals(RoleName.ROLE_LEADER.toString())){
            return taskId;
        }

        return taskId;
    }

    @Override
    public UUID checkTaskIdToGetChecklist(UUID taskId){

        Task taskById = taskRepository.getTaskByTaskId(taskId);
        if (taskById == null){
            throw new BadRequestException("Cannot find task by id!");
        }
        taskId = taskById.getTaskId();

        Board getBoardByTaskId = boardRepository.getBoardByTaskId(taskId);
        if (getBoardByTaskId == null){
            throw new NotFoundException("Cannot found board by task id!");
        }
        return taskId;

    }

    //Check deadline checklist when create
    @Override
    public ChecklistRequest checkDateOfCreateChecklist(UUID taskId, ChecklistRequest checklistRequest) {

        Task task = taskService.getTaskById(taskId);
        if (task == null){
            throw new NotFoundException("Task not found!");
        }

        LocalDateTime startDateOfTask = task.getStartedAt();
        LocalDateTime finishDateOfTask = task.getFinishedAt();
        if (startDateOfTask == null || finishDateOfTask == null){
            throw new NotFoundException("Task deadline cannot found!");
        }

        if (checklistRequest.getStartedAt().isBefore(startDateOfTask)){
            throw new BadRequestException("Start date of checklist cannot be the past of start date of task!");
        }
        if (checklistRequest.getFinishedAt().isAfter(finishDateOfTask)){
            throw new BadRequestException("Finish date of checklist cannot be after of finish date of task!");
        }

        return checklistRequest;
    }

    @Override
    public Checklist updateChecklistStatusById(UUID checklistId, StatusRequest statusRequest) {
       Checklist checklist = getChecklistChecklistId(checklistId);
       if (checklist == null){
           throw new NotFoundException("Checklist not found!");
       }
       UUID taskId = checklist.getTaskId();
       if (taskId == null){
           throw new NotFoundException("Task id not found!");
       }
       checkTaskIdToGetChecklist(taskId);
       checklist.setUpdatedAt(LocalDateTime.now());
       LocalDateTime setUpdatedAtChecklist = checklist.getUpdatedAt();
       if (setUpdatedAtChecklist.isAfter(checklist.getFinishedAt())){
           checklist.setStatus(Status.OVERDUE.toString());
           checklistRepository.updateChecklistStatusById(checklist.getChecklistId(), statusRequest);
       }

       checklist.setStatus(Status.DONE.toString());
       return checklistRepository.updateChecklistStatusById(checklist.getChecklistId(), statusRequest);
    }

    @Override
    public Checklist createChecklist(UUID taskId, ChecklistRequest checklistRequest) {

        checkDateOfCreateChecklist(taskId, checklistRequest);
        UUID currentUserId = authService.getCurrentAppUserId();
        System.out.println("Current user : " + currentUserId);
        taskId = checkExistedTaskId(taskId, currentUserId);

        System.out.println("checklist title : " + checklistRequest.getTitle());

        return checklistRepository.createChecklist(taskId, checklistRequest);
    }

    @Override
    public List<Checklist> getAllChecklistByTaskId(UUID taskId) {

        taskId = checkTaskIdToGetChecklist(taskId);

        return checklistRepository.getAllChecklistByTaskId(taskId);

    }

    @Override
    public Checklist getChecklistChecklistId( UUID checklistId) {

        Checklist checklist = checklistRepository.getChecklistByChecklistId(checklistId);
        if (checklist == null){
            throw new NotFoundException("Checklist not found by task id and checklist id!!");
        }

        UUID taskId = checklist.getTaskId();
        if (taskId == null){
            throw new NotFoundException("Checklist not found by task id!");
        }

        UUID currentUserId = authService.getCurrentAppUserId();
        if (currentUserId == null){
            throw new NotFoundException("Current User Id not found!");
        }
        checkExistedTaskId(taskId, currentUserId);


        return checklist;

    }

    @Override
    public HashMap<String, Checklist> getChecklistByTaskIdAndTitle(UUID taskId, String title) {

        taskId = checkTaskIdToGetChecklist(taskId);
        taskId = checkTaskIdToGetChecklist(taskId);
        if (taskId == null){
            throw new NotFoundException("Checklist not found by task id!");
        }
        HashMap<String, Checklist> checklists = new HashMap<>();
        for (Checklist c : checklistRepository.getChecklistByTaskIdAndTitle(taskId, title)){
            checklists.put(c.getChecklistId().toString(), c);
        }

        return checklists;

    }

    @Override
    public Checklist updateChecklistById(UUID checklistId, ChecklistRequest checklistRequest) {

        UUID currentUserId = authService.getCurrentAppUserId();
        Checklist checklist = getChecklistChecklistId(checklistId);
        if (checklist == null){
            throw new NotFoundException("Checklist not found!");
        }
        UUID taskId = checklist.getTaskId();
        if (taskId == null){
            throw new NotFoundException("Checklist not found by task id!");
        }
        checkExistedTaskId(taskId, currentUserId);


        return checklistRepository.updateChecklistById(taskId, checklist.getChecklistId(), checklistRequest);
    }

    @Override
    public Checklist deleteChecklistByChecklist(UUID checklistId) {

        Checklist checklist = getChecklistChecklistId(checklistId);
        if (checklist == null){
            throw new NotFoundException("Checklist not found by task id and checklist id!");
        }
        UUID currentUserId = authService.getCurrentAppUserId();
        UUID taskId = checklist.getTaskId();
        if (taskId == null){
            throw new NotFoundException("Checklist not found by task id!");
        }
        checkExistedTaskId(taskId, currentUserId);

        return checklistRepository.deleteChecklistByChecklist(checklist.getChecklistId());
    }

    @Override
    public Checklist assignMemberToChecklist(UUID assignedByUserId, UUID assignToUserId, UUID checklistId, UUID taskId) {
        // Get the member ID of the user assigning (must be a leader of the task)
        UUID assignerMemberId = taskRepository.findMemberIdByUserIdAndTaskId(taskId, assignedByUserId);

        // Verify that the assigner has the ROLE_LEADER for this task
        String assignerRole = taskRepository.getRoleNameByUserIdAndTaskId(taskId, assignerMemberId);
        if (!assignerRole.contains(RoleName.ROLE_LEADER.toString())) {
            throw new BadRequestException("Only a leader can assign members to a checklist.");
        }

        // Get the member ID of the user to be assigned
        UUID assigneeMemberId = memberRepository.getMemberIdByUserId(assignToUserId);
        if(assigneeMemberId == null){
            throw new NotFoundException("Assign user with id " + assignToUserId + " not found!");
        }

        boolean isAlreadyAssigned = checklistRepository.isExistByUserIdAndTaskId(checklistId,assigneeMemberId);
        if (isAlreadyAssigned){
            throw new BadRequestException("Checklist with member id " + assigneeMemberId + " already assigned!");
        }

        // Perform the assignment
        memberRepository.insertIntoChecklistWithRoleMember(checklistId, assignerMemberId, assigneeMemberId);

        // Optionally return something meaningful, currently returns null
        return null;
    }



}