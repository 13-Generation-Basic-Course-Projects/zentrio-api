package org.example.zentrio.service;


import jakarta.validation.Valid;
import org.example.zentrio.dto.request.ChecklistRequest;
import org.example.zentrio.dto.request.ChecklistTimeRequest;
import org.example.zentrio.dto.request.ChecklistTitleRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.enums.ChecklistStatus;
import org.example.zentrio.model.Checklist;
import org.example.zentrio.model.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashSet;
import java.util.UUID;

public interface ChecklistService {

    Checklist createChecklist(ChecklistRequest checklistRequest);

    Checklist getChecklistChecklistId(UUID checklistId);

    HashSet<Checklist> getAllChecklistsByTaskId(UUID taskId);

    Checklist updateChecklistByIdAndTaskId(ChecklistTitleRequest checklistRequest, UUID checklistId);

    Checklist deleteChecklistByIdAndTaskId(UUID checklistId, UUID taskId);

    void assignMemberToChecklist(UUID checklistId, UUID taskId, UUID assignedBy, UUID assignedTo);

    FileMetadata uploadChecklistCoverImage(UUID checklistId, MultipartFile file);

    InputStream getFileByFileName(UUID checklistId,String fileName);

    void updateStatusOfChecklistById(UUID checklistId, ChecklistStatus status);

    HashSet<Checklist> getAllChecklists();

    HashSet<Checklist> getAllChecklistsByCurrentUser();

    Void moveChecklistOrder(UUID taskId, int newOrder, int oldOrder);

    Checklist updateChecklistDateLineByIdAndTaskId( ChecklistTimeRequest checklistRequest);
}
