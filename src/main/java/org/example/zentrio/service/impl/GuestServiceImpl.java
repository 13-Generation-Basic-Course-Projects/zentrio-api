package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.Board;
import org.example.zentrio.model.BoardFullData;
import org.example.zentrio.model.Workspace;
import org.example.zentrio.repository.BoardRepository;
import org.example.zentrio.repository.GuestRepository;
import org.example.zentrio.repository.WorkspaceRepository;
import org.example.zentrio.service.GuestService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {
    private final GuestRepository guestRepository;
    private final WorkspaceRepository workspaceRepository;
    private final BoardRepository boardRepository;

    @Override
    public BoardFullData getFullDataInBoard(UUID workspaceId, UUID boardId) {
        Workspace workspace = workspaceRepository.getWorkspaceByWorkspaceId(workspaceId);
        if (workspace == null) {
            throw new NotFoundException("Workspace " + workspaceId + " not found");
        }
        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board " + boardId + " not found");
        }
        return guestRepository.getFullDataInBoard(workspaceId,boardId);
    }
}
