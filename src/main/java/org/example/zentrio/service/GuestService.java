package org.example.zentrio.service;

import org.example.zentrio.model.BoardFullData;

import java.util.UUID;

public interface GuestService {
    BoardFullData getFullDataInBoard(UUID workspaceId, UUID boardId);
}
