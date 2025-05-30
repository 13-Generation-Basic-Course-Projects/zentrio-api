package org.example.zentrio.service.impl;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.zentrio.dto.request.AssignedRoleRequest;
import org.example.zentrio.dto.request.BoardRequest;
import org.example.zentrio.dto.request.InviteRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.MemberResponse;
import org.example.zentrio.enums.ImageExtension;
import org.example.zentrio.enums.RoleName;
import org.example.zentrio.enums.RoleRequest;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ConflictException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.jwt.JwtService;
import org.example.zentrio.model.*;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.BoardService;
import org.example.zentrio.service.EmailService;
import org.example.zentrio.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final RoleRepository roleRepository;
    private final AppUserRepository appUserRepository;
    private final MinioClient minioClient;
    private final WorkspaceService workspaceService;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public Board createBoard(BoardRequest boardRequest) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Workspace workspace = workspaceRepository.getWorkspaceById(boardRequest.getWorkspaceId(), userId);
        if (workspace == null) {
            throw new NotFoundException("Workspace id " + boardRequest.getWorkspaceId() + " not found");
        }
        Board board = boardRepository.createBoard(boardRequest, boardRequest.getWorkspaceId());
        UUID boardId = board.getBoardId();
        UUID roleId = roleRepository.getRoleIdByRoleName(RoleName.ROLE_MANAGER.toString());
        boardRepository.insertMember(userId, boardId, roleId);
        return board;
    }

    @Override
    public ApiResponse<HashSet<Board>> getAllBoardsByWorkspaceId(UUID workspaceId, Integer page, Integer size) {
        workspaceService.getWorkspaceById(workspaceId);

        Integer offset = (page - 1) * size;

        List<Board> boardList = boardRepository.getAllBoardsByWorkspaceId(workspaceId, size, offset);

        HashSet<Board> boards = new HashSet<>(boardList);

        int totalElements = boardRepository.getBoardCountByWorkspaceId(workspaceId);
        int totalPages = (int) Math.ceil(totalElements / (double) size);

        return ApiResponse.<HashSet<Board>>builder()
                .success(true)
                .message("Get all tasks successfully")
                .payload(boards)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .pagination(new Pagination(page, totalElements, totalPages))
                .build();
    }


    @Override
    public Board updateBoardByBoardId(BoardRequest boardRequest, UUID boardId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Board board =  getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board id " + boardId + " not found");
        }
        if(!board.getWorkspaceId().equals(boardRequest.getWorkspaceId())) {
            throw new NotFoundException("workspace id " + boardRequest.getWorkspaceId() + " not found");
        }
        Workspace workspace = workspaceRepository.getWorkspaceByWorkspaceId(boardRequest.getWorkspaceId());
        if (workspace == null) {
            throw new NotFoundException("Workspace id " + boardRequest.getWorkspaceId() + " not found");
        }
        String roleName = roleRepository.getRoleNameByBoardIdAndUserId(boardId, userId);

        if (roleName == null) {
            throw new NotFoundException("You're doesn't have a ROLE MANGER to update this board");
        }

        if (!roleName.equals(RoleName.ROLE_MANAGER.toString())) {
            throw new NotFoundException("You're doesn't have permission to update this board");
        }

        return boardRepository.updateBoardByBoardId(boardRequest, boardId, LocalDateTime.now());
    }

    @Override
    public void deleteBoardByBoardId(UUID boardId, UUID workspaceId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        getBoardByBoardId(boardId);
        Workspace workspace = workspaceRepository.getWorkspaceById(workspaceId, userId);
        if (workspace == null) {
            throw new NotFoundException("Workspace id " + workspaceId + " not found");
        }
        String roleName = roleRepository.getRoleNameByBoardIdAndUserId(boardId, userId);

        if (roleName == null) {
            throw new NotFoundException("You're doesn't have a ROLE MANGER to deleted this board");
        }

        if (!roleName.equals(RoleName.ROLE_MANAGER.toString())) {
            throw new NotFoundException("You're doesn't have permission to deleted this board");
        }
        boardRepository.deleteBoardByBoardId(boardId, workspaceId);
    }

    @Override
    public Board getBoardByBoardId(UUID boardId) {
        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board id " + boardId + " not found");
        }
        return board;
    }

    @Override
    public HashSet<Board> getBoardByBoardTitle(@RequestBody String boardTitle) {
        List<Board> boardList = boardRepository.getBoardByTitle(boardTitle);
        if (boardList == null) {
            throw new NotFoundException("Board title " + boardTitle + " not found");
        }
        return new HashSet<>(boardList);
    }


    @Override
    public HashSet<MemberResponse> getBoardByBoardIdWithMember(UUID boardId) {
        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board id " + boardId + " not found");
        }
        return boardRepository.getBoardByBoardIdWithMember(boardId);
    }

    @Override
    public void assignRoleToBoard(AssignedRoleRequest assignedRoleRequest) {
        // Validate board existence and user permissions
        getBoardByBoardId(assignedRoleRequest.getBoardId());
        validateCurrentUserRoles(assignedRoleRequest.getBoardId());

        // Check workspace existence
        Workspace workspace = workspaceRepository.getWorkspaceByWorkspaceId(assignedRoleRequest.getWorkspaceId());
        if (workspace == null) {
            throw new NotFoundException("Workspace ID not found");
        }

        // Check if assignee user exists
        AppUser assignee = appUserRepository.getUserById(assignedRoleRequest.getAssigneeId());
        if (assignee == null) {
            throw new NotFoundException("Assignee ID not found");
        }

        if(assignedRoleRequest.getRoleName() == null){
            throw new NotFoundException("Role name cannot be null or empty");
        }

        UUID roleId = roleRepository.getRoleIdByRoleName(assignedRoleRequest.getRoleName().toString());

        if (roleId == null) {
            throw new NotFoundException("Invalid role name " + assignedRoleRequest.getRoleName());
        }
        // Get all existing roles of the user on this board
        List<String> existingRoles = roleRepository.getRolesNameByBoardIdAndUserId(
                assignedRoleRequest.getBoardId(),
                assignedRoleRequest.getAssigneeId()
        );
        System.out.println(existingRoles);

        final String MANAGER_ROLE = RoleName.ROLE_MANAGER.toString();
        String newRoleName = assignedRoleRequest.getRoleName().toString();

        // Prevent assigning more than one Manager role
        if (MANAGER_ROLE.equals(newRoleName) || existingRoles.contains(MANAGER_ROLE)) {
            throw new ConflictException("User already has Manager role on this board.");
        }

        // Prevent assigning duplicate roles
        if (existingRoles.contains(newRoleName)) {
            throw new ConflictException("User already has the role " + newRoleName);
        }

        // Assign the new role
        roleRepository.insertToMember(assignedRoleRequest.getAssigneeId(), assignedRoleRequest.getBoardId(), roleId);
    }


    @SneakyThrows
    @Override
    public FileMetadata uploadBoardImage(UUID boardId, MultipartFile file) {
        getBoardByBoardId(boardId);
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
        boardRepository.updateCover(boardId, fileName);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .contentType(file.getContentType())
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .build()
        );

        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/boards/" + fileName)
                .toUriString();

        return FileMetadata.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }

    @SneakyThrows
    @Override
    public InputStream getFileByFileName(UUID boardId, String fileName) {
        getBoardByBoardId(boardId);
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
    public void updateIsFavourite(UUID boardId, boolean isFavourite) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Board board = boardRepository.getBoardByBoardId(boardId);
        if(board == null) {
            throw new NotFoundException("Board ID " + boardId + " not found");
        }
        List<String> roleName = roleRepository.getRolesNameByBoardIdAndUserId(boardId, userId);
        if(roleName == null) {
            throw new NotFoundException("You doesn't have the role to update the favourite status of this board");
        }
        if(!roleName.contains(RoleName.ROLE_MANAGER.toString())) {
            throw new ForbiddenException("You do not have the role to update the favourite status only manager can update the favourite status");
        }
        boardRepository.updateIsFavourite(boardId, isFavourite);
    }

    @Override
    public void inviteMemberToBoard(UUID boardId, List<InviteRequest> inviteRequests) {
        getBoardByBoardId(boardId);
        for(InviteRequest inviteRequest : inviteRequests) {
            emailService.sendInvitations(boardId, inviteRequest.getEmail(),inviteRequest.getRoleName());
        }
    }

    @Override
    public Board getBoardByIdWithCurrentUserId(UUID boardId) {
        Board board = getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board id not found");
        }
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return boardRepository.getBoardByIdWithCurrentUserId(boardId,userId);
    }

    @Override
    public HashSet<Board> getAllBoardsForCurrentUser() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return boardRepository.getAllBoardsForCurrentUser(userId);
    }

    @Override
    public HashSet<Board> getAllBoards() {
        return boardRepository.getAllBoards();
    }

    @Override
    public String acceptBoardInvitation(UUID boardId, String email, RoleRequest roleRequest) {

        log.info("acceptBoardInvitation: boardId: {}, email: {}", boardId, email);
            getBoardByBoardId(boardId);
            AppUser appUser = appUserRepository.getUserByEmail(email);
            if (appUser == null) {
                // If the user is not found, redirect to the login/sign-in page.
                // Note: This URL is just an example; the actual URL should be
                // provided dynamically by the frontend.
                return "https://www.youtube.com/watch?v=Pzi-VuPjcII";
            }
            UUID userId = appUser.getUserId();
            UUID roleId = roleRepository.getRoleIdByRoleName(roleRequest.toString());
            if (roleId == null) {
                // If the user is not found, redirect to the login/sign-in page.
                // Note: This URL is just an example; the actual URL should be
                // provided dynamically by the frontend.
                return "https://www.youtube.com/watch?v=Pzi-VuPjcII";
            }
            UUID existingMemberId = boardRepository.getMemberIdByUserIdAndBoardId(userId, boardId);
            if (existingMemberId != null) {
                // If the user exists, redirect them to the board page.
                // Note: This URL is just an example; the actual URL should be
                // obtained dynamically from the frontend.
                return "http://localhost:8080/swagger-ui/index.html";
            }

        log.info("Returning redirect URL: {}", "http://localhost:8080/swagger-ui/index.html");


        boardRepository.insertMember(userId, boardId, roleId);
            // If a user is found and is not yet assigned to a board,
            // upon acceptance, they should be redirected to the board page.
            // Note: The URL shown here is just an example; the real URL
            // should be dynamically obtained from the frontend.
            return "http://localhost:8080/swagger-ui/index.html";
    }

    public void validateCurrentUserRoles(UUID boardId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        String roleName = roleRepository.getRoleNameByBoardIdAndUserId(boardId, userId);
        if (roleName == null || !roleName.equals(RoleName.ROLE_MANAGER.toString())) {
            throw new ForbiddenException("You're not manager in this board can't assign role");
        }
    }


}
