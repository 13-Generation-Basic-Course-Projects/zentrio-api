package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.AssignedRoleRequest;
import org.example.zentrio.dto.request.BoardRequest;
import org.example.zentrio.dto.request.InviteRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.BoardResponse;
import org.example.zentrio.dto.response.MemberResponse;
import org.example.zentrio.enums.RoleRequest;
import org.example.zentrio.model.Board;
import org.example.zentrio.model.FileMetadata;
import org.example.zentrio.service.BoardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/boards")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Board Controller")
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "Created new board by workspace ID", description = "create a new board by specific workspace ID")
    @PostMapping
    public ResponseEntity<ApiResponse<Board>> createBoard(@Valid @RequestBody BoardRequest boardRequest) {
        ApiResponse<Board> apiResponse = ApiResponse.<Board>builder()
                .success(true)
                .message("Create board successfully")
                .payload(boardService.createBoard(boardRequest))
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Operation(summary = "Get all boards by workspace ID", description = "Get all boards by specific workspace ID")
    @GetMapping("workspaces/{workspace-id}")
    public ResponseEntity<ApiResponse<HashSet<Board>>> getAllBoardsByWorkspaceId(
            @PathVariable("workspace-id") UUID workspaceId,
            @RequestParam(defaultValue = "1") @Positive Integer page,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        ApiResponse<HashSet<Board>> response = boardService.getAllBoardsByWorkspaceId(workspaceId, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Update board by board ID And Workspace ID", description = "Update board by board ID And specific workspace ID")
    @PutMapping("/{board-id}")
    public ResponseEntity<ApiResponse<Board>> updateBoardByBoardId(
            @Valid @RequestBody BoardRequest boardRequest,
            @PathVariable("board-id") UUID boardId
    ) {
        ApiResponse<Board> apiResponse = ApiResponse.<Board>builder()
                .success(true)
                .message("Update board successfully")
                .payload(boardService.updateBoardByBoardId(boardRequest, boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Deleted board by ID AND workspace ID", description = "Deleted a board by board ID")
    @DeleteMapping("/{board-id}/workspaces/{workspace-id}")
    public ResponseEntity<?> deleteBoardByBoardId(
            @PathVariable("board-id") UUID boardId,
            @PathVariable("workspace-id") UUID workspaceId) {
        boardService.deleteBoardByBoardId(boardId,workspaceId);
        ApiResponse<Board> apiResponse = ApiResponse.<Board>builder()
                .success(true)
                .message("Deleted board successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get board by ID ", description = "Get a board by board ID")
    @GetMapping("/{board-id}")
    public ResponseEntity<ApiResponse<Board>> getBoardByBoardId(@PathVariable("board-id") UUID boardId) {
        ApiResponse<Board> apiResponse = ApiResponse.<Board>builder()
                .success(true)
                .message("Get board by ID successfully")
                .payload(boardService.getBoardByBoardId(boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get board by title ", description = "Get a board by board title")
    @GetMapping("/title/{board-title}")
    public ResponseEntity<ApiResponse<HashSet<Board>>> getBoardByBoardTitle(@PathVariable("board-title") String boardTitle) {
        ApiResponse<HashSet<Board>> apiResponse = ApiResponse.<HashSet<Board>>builder()
                .success(true)
                .message("Get board by title successfully")
                .payload(boardService.getBoardByBoardTitle(boardTitle))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Upload cover by board ID", description = "Upload board cover by specific board ID")
    @PostMapping(value = "/{board-id}/cover/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadata>> uploadCoverByBoardId(
            @PathVariable("board-id") UUID boardId,
            @RequestParam("file") MultipartFile file) {
        FileMetadata fileMetadata = boardService.uploadBoardImage(boardId, file);
        ApiResponse<FileMetadata> apiResponse = ApiResponse.<FileMetadata>builder()
                .success(true)
                .message("Board cover uploaded successfully")
                .payload(fileMetadata)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);

    }

    @Operation(summary = "Get cover board by board ID", description = "Get cover of boards by specific board ID")
    @GetMapping("/{board-id}/cover/{file-name}")
    public ResponseEntity<?> getCoverByBoardIdAndFileName(
            @PathVariable("board-id") UUID boardId,
            @PathVariable("file-name") String fileName
    ) throws IOException {
        InputStream inputStream = boardService.getFileByFileName(boardId, fileName);
        byte[] fileContent = inputStream.readAllBytes();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_PNG)
                .body(fileContent);
    }

    @Operation(summary = "Get board with members", description = "Get board with member information")
    @GetMapping("/{board-id}/members")
    public ResponseEntity<ApiResponse<HashSet<MemberResponse>>> getBoardWithMembers(@PathVariable("board-id") UUID boardId) {
        ApiResponse<HashSet<MemberResponse>> apiResponse = ApiResponse.<HashSet<MemberResponse>>builder()
                .success(true)
                .message("Get board with members successfully")
                .payload(boardService.getBoardByBoardIdWithMember(boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }



    @Operation(summary = "Assigned users role to board", description = "Assigned user role to the specific boards")
    @PostMapping("/assign")
    public ResponseEntity<?> assignRoleToBoard(@RequestBody AssignedRoleRequest assignedRoleRequest) {
        boardService.assignRoleToBoard(assignedRoleRequest);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Assigned role to board successfully")
                .status(HttpStatus.OK)
                .payload(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Updated favourite of board by ID", description = "Updated favourite of board by specific board ID")
    @PutMapping("/{board-id}/favourite")
    public ResponseEntity<?> updateBoardFavourite(
            @PathVariable("board-id") UUID boardId,
            @RequestParam boolean isFavourite) {
        boardService.updateIsFavourite(boardId,isFavourite);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Updated favourite of board successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Invite users to board", description = "Invite any users to specific board")
    @PostMapping("/{board-id}/workspaces/{workspace-id}/users/invite")
    public ResponseEntity<?> inviteMemberToBoard(
            @PathVariable("board-id") UUID boardId,
            @PathVariable("workspace-id") UUID workspaceId,
            @RequestBody List<InviteRequest> inviteRequests) {
        boardService.inviteMemberToBoard(workspaceId,boardId,inviteRequests);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Invited users to board successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Accept board invitation", description = "Accept invitation to join a board using email")
    @GetMapping("/{board-id}/workspaces/{workspace-id}/invitations/accept")
    public ResponseEntity<?> acceptInvitation(
            @PathVariable("board-id") UUID boardId,
            @PathVariable("workspace-id") UUID workspaceId,
            @RequestParam String email,
            @RequestParam RoleRequest roleRequest) {
        String redirectUrl = boardService.acceptBoardInvitation(workspaceId,boardId, email,roleRequest);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @Operation(summary = "Get board by ID with current user ID",description = "Get board by ID with current user ID")
    @GetMapping("/{board-id}/users")
    public ResponseEntity<ApiResponse<Board>> getBoardByIdWithCurrentUserId(
            @PathVariable("board-id") UUID boardId) {
        ApiResponse<Board> apiResponse = ApiResponse.<Board>builder()
                .success(true)
                .message("Get board by ID with current user ID successfully")
                .payload(boardService.getBoardByIdWithCurrentUserId(boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Get all board for current user")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<HashSet<Board>>> getAllBoardsForCurrentUser() {
        ApiResponse<HashSet<Board>> apiResponse = ApiResponse.<HashSet<Board>>builder()
                .success(true)
                .message("Get all boards for current user successfully")
                .payload(boardService.getAllBoardsForCurrentUser())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Get all boards")
    @GetMapping
    public ResponseEntity<ApiResponse<HashSet<Board>>> getAllBoards() {
        ApiResponse<HashSet<Board>> apiResponse = ApiResponse.<HashSet<Board>>builder()
                .success(true)
                .message("Get all boards successfully")
                .payload(boardService.getAllBoards())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get all data in board by board id for current user")
    @GetMapping("/all-data/{board-id}")
    public ResponseEntity<ApiResponse<BoardResponse>> getAllDataInBoard(@PathVariable("board-id") UUID boardId) {
        ApiResponse<BoardResponse> apiResponse = ApiResponse.<BoardResponse>builder()
                .success(true)
                .message("Get all data from  board successfully")
                .payload(boardService.getAllDataInBoard(boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }



    @Operation(summary = "Delete member in the board by user ID",description = "Deleted member in board by user ID")
    @DeleteMapping("/{board-id}/users/{user-id}")
    public ResponseEntity<?> deleteMember(@PathVariable("board-id") UUID boardId, @PathVariable("user-id") UUID userId) {
        boardService.deletedMember(boardId,userId);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Deleted member by board ID and user ID successfully")
                .status(HttpStatus.OK)
                .payload(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get favourite board by user ID",description = "Get all favourite board by user ID")
    @GetMapping("/favourite/users/{user-id}")
    public ResponseEntity<ApiResponse<HashSet<Board>>> getFavouriteBoardsByUserId(@PathVariable("user-id") UUID userId) {
        ApiResponse<HashSet<Board>> apiResponse = ApiResponse.<HashSet<Board>>builder()
                .success(true)
                .message("Get all favourite boards by user ID successfully")
                .status(HttpStatus.OK)
                .payload(boardService.getFavouriteBoardsByUserId(userId))
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);

    }


    @Operation(summary = "Leave a board by ID",description = "Leaving a board with a board ID and user ID")
    @DeleteMapping("/{board-id}")
    public ResponseEntity<?> leaveBoardByBoardIdAndUserId(
            @PathVariable("board-id") UUID boardId) {
        boardService.leaveBoard(boardId);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Leave a board by ID and user ID successfully")
                .status(HttpStatus.OK)
                .payload(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }



}
