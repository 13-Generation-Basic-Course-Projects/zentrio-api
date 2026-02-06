package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.model.BoardFullData;
import org.example.zentrio.repository.GuestRepository;
import org.example.zentrio.service.GuestService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {
    private final GuestRepository guestRepository;
    @Override
    public BoardFullData getFullDataInBoard(UUID workspaceId, UUID boardId) {
        return guestRepository.getFullDataInBoard(workspaceId,boardId);
    }
}
