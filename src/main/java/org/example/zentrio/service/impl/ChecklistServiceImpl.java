package org.example.zentrio.service.impl;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.zentrio.dto.request.ChecklistRequest;
import org.example.zentrio.enums.ChecklistStatus;
import org.example.zentrio.enums.ImageExtension;
import org.example.zentrio.enums.RoleName;
import org.example.zentrio.enums.Status;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.*;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.ChecklistService;
import org.example.zentrio.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class
ChecklistServiceImpl implements ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final RoleRepository roleRepository;
    private final BoardRepository boardRepository;
    private final MinioClient minioClient;
    private final MemberRepository memberRepository;

    @Value("${minio.bucket.name}")
    private String bucketName;


    private void validateChecklistWithTaskTime(ChecklistRequest checklistRequest, Task task) {
//        LocalDateTime now = LocalDateTime.now();

        //  Checklist start time cannot be in the past
//        if (checklistRequest.getStartedAt().isBefore(now)) {
//            throw new BadRequestException("Checklist start time cannot be in the past.");
//        }

        // Checklist finish time must be the same or after start time
        if (checklistRequest.getFinishedAt().isBefore(checklistRequest.getStartedAt())) {
            throw new BadRequestException("Checklist finish time cannot be before start time.");
        }

        // Checklist finish time must not exceed Task's finish time
        if (checklistRequest.getFinishedAt().isAfter(task.getFinishedAt())) {
            throw new BadRequestException("Checklist finish time cannot exceed Task's finish time.");
        }

        // Checklist start time must not be before Task's start time
        if (checklistRequest.getStartedAt().isBefore(task.getStartedAt())) {
            throw new BadRequestException("Checklist start time cannot be before Task's start time.");
        }
    }

    void validateChecklistIdAndTaskId(UUID checklistId, UUID taskId) {
        Checklist checklist = getChecklistChecklistId(checklistId);
        if (checklist == null) {
            throw new NotFoundException("Checklist with id " + checklistId + " not found.");
        }
        Task task = taskService.getTaskById(taskId);
        if (task == null) {
            throw new NotFoundException("Task with ID " + taskId + " not found.");
        }
    }

    private void validateChecklistAccess(UUID taskId, UUID boardId, UUID userId) {
//      Fetch all roles assigned to the user for the board
        List<String> roles = roleRepository.getRolesNameByBoardIdAndUserId(boardId, userId);
        roles = filterRoles(roles);

//      Manager — full access
        if (roles.contains(RoleName.ROLE_MANAGER.toString())) {
            return;
        }

        System.out.println("userId" + userId);

//      Leader — limited access (only to their own tasks)
        if (roles.contains(RoleName.ROLE_LEADER.toString())) {
            UUID leaderMemberId = boardRepository.getTeamLeaderMemberIdByUserIdAndBoardId(userId, boardId);
            System.out.println("roleLeader" + leaderMemberId);
            // Fetch the task
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                throw new NotFoundException("Task with ID " + taskId + " not found.");
            }

            // Validate that the leader created the task
//            if (!task.getCreatedBy().equals(leaderMemberId)) {
//                throw new ForbiddenException("Team Leader can only manage checklists for tasks they created.");
//            }

            return;
        }

//      Member — no permission
        if (roles.contains(RoleName.ROLE_MEMBER.toString())) {
            throw new ForbiddenException("Members are not allowed to create or manage checklists.");
        }

//      No valid role
        throw new ForbiddenException("You do not have permission to manage checklists.");
    }


    public List<String> filterRoles(List<String> roles) {
        // If manager is present, return only manager
        if (roles.contains(RoleName.ROLE_MANAGER.toString())) {
            return List.of(RoleName.ROLE_MANAGER.toString());
        }

        // Otherwise, return unique leader and member roles only
        return roles.stream()
                .filter(role -> role.equals(RoleName.ROLE_LEADER.toString()) || role.equals(RoleName.ROLE_MEMBER.toString()))
                .distinct()
                .collect(Collectors.toList());
    }


    @Override
    public Checklist createChecklist(ChecklistRequest checklistRequest) {

//        LocalDateTime now = LocalDateTime.now();

        LocalDateTime updatedStartedTime = checklistRequest.getStartedAt().withHour(00)
                .withMinute(00)
                .withSecond(00)
                .withNano(000_000_000);

        LocalDateTime updatedFinishedTime = checklistRequest.getFinishedAt().withHour(23)
                .withMinute(00)
                .withSecond(00)
                .withNano(000_000_000);

        checklistRequest.setStartedAt(updatedStartedTime);
        checklistRequest.setFinishedAt(updatedFinishedTime);

        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Task task = taskService.getTaskById(checklistRequest.getTaskId());
        validateChecklistAccess(checklistRequest.getTaskId(), task.getBoardId(), userId);
        validateChecklistWithTaskTime(checklistRequest, task);
        List<String> roles = roleRepository.getRolesNameByBoardIdAndUserId(task.getBoardId(), userId);
        roles = filterRoles(roles);
        UUID memberId;
        if (roles.contains(RoleName.ROLE_MANAGER.toString())) {
            memberId = boardRepository.getManagerMemberIdByUserIdAndBoardId(userId, task.getBoardId());
        } else if (roles.contains(RoleName.ROLE_LEADER.toString())) {
            memberId = boardRepository.getTeamLeaderMemberIdByUserIdAndBoardId(userId, task.getBoardId());
        } else {
            throw new ForbiddenException("You do not have permission to create checklist.");
        }


        if(memberId == null) {
            throw new ForbiddenException("You're not a Manger or Leader in this board, can't create checklist");
        }

        Checklist checklist =  checklistRepository.createChecklist(checklistRequest, task.getTaskId(), memberId);

        boolean isHasLeader = checklistRepository.getTaskAssignIdByTaskId(task.getTaskId()) > 0;
        if (isHasLeader) {
            checklistRepository.updateStatusOfChecklistById(checklist.getChecklistId(),ChecklistStatus.IN_PROGRESS.toString());
        }
        return checklist;

    }

    @Override
    public Checklist getChecklistChecklistId(UUID checklistId) {
        Checklist checklist = checklistRepository.getChecklistById(checklistId);
        if (checklist == null) {
            throw new BadRequestException("Checklist with ID " + checklistId + " not found!");
        }
        return checklist;
    }

    @Override
    public HashSet<Checklist> getAllChecklistsByTaskId(UUID taskId) {
        Task task = taskRepository.getTaskByTaskId(taskId);
        if (task == null) {
            throw new NotFoundException("Task with ID " + taskId + " not found.");
        }
        return checklistRepository.getAllChecklistsByTaskId(taskId);
    }

    @Override
    public Checklist updateChecklistByIdAndTaskId(ChecklistRequest checklistRequest, UUID checklistId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Task task = taskService.getTaskById(checklistRequest.getTaskId());
        validateChecklistIdAndTaskId(checklistId, checklistRequest.getTaskId());
        validateChecklistWithTaskTime(checklistRequest, task);
        validateChecklistAccess(checklistRequest.getTaskId(), task.getBoardId(), userId);
        return checklistRepository.updateChecklistByIdAndTaskId(checklistRequest, checklistId, checklistRequest.getTaskId(),LocalDateTime.now());
    }

    @Override
    public Checklist deleteChecklistByIdAndTaskId(UUID checklistId, UUID taskId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Task task = taskService.getTaskById(taskId);
        validateChecklistAccess(taskId, task.getBoardId(), userId);
        validateChecklistIdAndTaskId(checklistId, taskId);
        return checklistRepository.deleteChecklistByIdAndTaskId(checklistId, taskId);
    }

    @Override
    public void assignMemberToChecklist(UUID checklistId, UUID taskId, UUID assignedBy, UUID assignedTo) {
        validateChecklistIdAndTaskId(checklistId, taskId);
        Task task = taskService.getTaskById(taskId);

        System.out.println("assignBy " + assignedBy);

        // can replace taskRepository to memberRepository

        UUID assignerId = taskRepository.getManagerOrLeader(task.getBoardId(),assignedBy,task.getTaskId());
        System.out.println("assignTo " + assignerId);
        UUID assignTo=null;
        String role= memberRepository.getRoleInTask(task.getBoardId(),assignedBy,task.getTaskId());
        System.out.println("role: " + role);
        if (role == null) {
            throw new ForbiddenException("You don't have permission to assign member to checklist.");
        }
        if (role.equals(RoleName.ROLE_MANAGER.toString()) ||  role.equals(RoleName.ROLE_LEADER.toString())) {
            Boolean userExistInBoard = boardRepository.getExistUserInBoard(assignedTo, task.getBoardId());
            if (userExistInBoard) {
                assignTo = checklistRepository.findMemberIdByBoardIdAndUserId(task.getBoardId(), assignedTo);
//                System.out.println("assignto1: " + assignto);
                if (assignTo == null) {
                    UUID roleId = roleRepository.getRoleIdByRoleName(RoleName.ROLE_MEMBER.toString());
                    boardRepository.insertMember(assignedTo, task.getBoardId(), roleId);
                    assignTo = checklistRepository.findMemberIdByBoardIdAndUserId(task.getBoardId(), assignedTo);
//                    System.out.println("assignto2: " + assignto);
                }

                if (checklistRepository.checklistIsAssigned(checklistId, assignTo)) {
                    throw new BadRequestException("Member with ID " + assignTo + " is already assigned to this checklist");
                }
            } else {
                throw new ForbiddenException("Assign to user does not have in this board");
            }

            checklistRepository.insertToChecklistAssignment(checklistId, assignerId, assignTo);
        }
    }



    @SneakyThrows
    @Override
    public FileMetadata uploadChecklistCoverImage(UUID checklistId, MultipartFile file) {
        getChecklistChecklistId(checklistId);
        List<String> imageExtensions = new ArrayList<>();

        for (ImageExtension imageExtension : ImageExtension.values()) {
            imageExtensions.add(imageExtension.getExtension());
        }

        if (!imageExtensions.contains(StringUtils.getFilenameExtension(file.getOriginalFilename()))) {
            throw new BadRequestException("Profile image must be a valid image URL ending with .png, .svg, .jpg, .jpeg, or .gif");
        }

        boolean bucketExits = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());

        if (!bucketExits) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        String fileName = file.getOriginalFilename();
        fileName = UUID.randomUUID() + "." + StringUtils.getFilenameExtension(fileName);
        checklistRepository.updateCover(checklistId, fileName);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .contentType(file.getContentType())
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .build()
        );

        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/preview-file/" + fileName)
                .toUriString();

        return FileMetadata.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }


    @Override
    public InputStream getFileByFileName(UUID checklistId,String fileName) {
        getChecklistChecklistId(checklistId);
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new NotFoundException("File not found: " + fileName);
            }
            throw new RuntimeException("MinIO error", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error accessing MinIO", e);
        }
    }

    @Override
    public void updateStatusOfChecklistById(UUID checklistId, ChecklistStatus status) {

        getChecklistChecklistId(checklistId);
        checklistRepository.updateStatusOfChecklistById(checklistId,status.toString());
    }

    @Override
    public HashSet<Checklist> getAllChecklists() {
        return checklistRepository.getAllChecklists();
    }

    @Override
    public HashSet<Checklist> getAllChecklistsByCurrentUser() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return checklistRepository.getAllChecklistsByCurrentUser(userId);
    }

    @Override
    public Void moveChecklistOrder(UUID taskId, int newOrder, int oldOrder) {
        Task task= taskService.getTaskById(taskId);
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        String teamLead= memberRepository.getRoleInTask(task.getBoardId(), userId , task.getTaskId());
        System.out.println(teamLead);
        if (teamLead == null || !teamLead.equals(RoleName.ROLE_LEADER.toString())) {
            throw new ForbiddenException("You do not have the leader of this task");
        }
        if (newOrder == oldOrder){
            return null ;
        }
        HashSet<Checklist> checklists = checklistRepository.getAllChecklistsByTaskId(task.getTaskId());
        if (checklists.isEmpty()){
            throw new NotFoundException("No checklists found for task " + task.getTaskId());
        }
        boolean oldExists = false;
        boolean newExists = false;

        for (Checklist checklist : checklists) {
            if (checklist.getChecklistOrder() == oldOrder) {
                oldExists = true;
            }
            if (checklist.getChecklistOrder() == newOrder) {
                newExists = true;
            }
        }

        if (!oldExists) {
            throw new NotFoundException("No task with order: " + oldOrder);
        }

        if (!newExists) {
            throw new NotFoundException("No task with order: " + newOrder);
        }

        // move down
        if (oldOrder > newOrder){
            int till= oldOrder -1;
            checklistRepository.moveChecklistOrderDown(oldOrder,newOrder, till,task.getTaskId());
            System.out.println("move down"+till);
        }else {
            int till= oldOrder +1;
            checklistRepository.moveChecklistOrderUp(oldOrder,newOrder, till,task.getTaskId());
            System.out.println("move up"+till);
        }
        return  null;
    }


}