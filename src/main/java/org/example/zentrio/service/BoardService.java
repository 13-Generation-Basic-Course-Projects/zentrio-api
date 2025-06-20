package org.example.zentrio.service;

import org.example.zentrio.dto.request.AssignedRoleRequest;
import org.example.zentrio.dto.request.BoardRequest;
import org.example.zentrio.dto.request.InviteRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.BoardAllResponse;
import org.example.zentrio.dto.response.BoardResponse;
import org.example.zentrio.dto.response.MemberResponse;
import org.example.zentrio.enums.RoleRequest;
import org.example.zentrio.model.Board;
import org.example.zentrio.model.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public interface BoardService {

    Board createBoard(BoardRequest boardRequest);

    ApiResponse<HashSet<Board>> getAllBoardsByWorkspaceId(UUID workspaceId, Integer page, Integer size);

    Board updateBoardByBoardId(BoardRequest boardRequest, UUID boardId);

    void deleteBoardByBoardId(UUID workspaceId, UUID boardId);

    Board getBoardByBoardId(UUID boardId);

    HashSet<Board> getBoardByBoardTitle(String boardTitle);

    HashSet<MemberResponse> getBoardByBoardIdWithMember(UUID boardId);

    void assignRoleToBoard(AssignedRoleRequest assignedRoleRequest);

    FileMetadata uploadBoardImage(UUID boardId, MultipartFile file);

    InputStream getFileByFileName(UUID boardId, String fileName);

    void updateIsFavourite(UUID boardId, boolean isFavourite);

    void inviteMemberToBoard(UUID workspaceId,UUID boardId, List<InviteRequest> inviteRequests);

    Board getBoardByIdWithCurrentUserId(UUID boardId);

    HashSet<Board> getAllBoardsForCurrentUser();

    HashSet<Board> getAllBoards();

    String acceptBoardInvitation(UUID workspaceId, UUID boardId, String email, RoleRequest roleRequest);

    BoardResponse getAllDataInBoardForReport(UUID boardId);

    BoardAllResponse getAllDataInBoard(UUID boardId);

    void deletedMember(UUID boardId, UUID userId);

    HashSet<Board> getFavouriteBoardsByUserId(UUID userId);
}