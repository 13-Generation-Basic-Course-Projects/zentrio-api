package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.AttachmentRequest;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.Attachment;
import org.example.zentrio.model.Checklist;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.AttachmentService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ChecklistRepository checklistRepository;
    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;


    private void validateAttachmentDetails(AttachmentRequest attachment) {
        Map<String, String> details = attachment.getDetails();
        if (details == null || details.isEmpty()) {
            throw new BadRequestException("Attachment details cannot be empty.");
        }

        String fileName = details.get("fileName");

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BadRequestException("Attachment fileName is required in details.");
        }

        if (!fileName.matches(".*\\.(pdf|docx|png|jpg|jpeg|xlsx)$")) {
            throw new BadRequestException("Invalid file type for: " + fileName);
        }

        String fileSizeStr = details.get("fileSize");
        if (fileSizeStr != null) {
            try {
                long fileSize = Long.parseLong(fileSizeStr);
                if (fileSize > 10 * 1024 * 1024) {
                    throw new BadRequestException("Attachment " + fileName + " exceeds max allowed size of 10MB.");
                }
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid fileSize format in attachment " + fileName);
            }
        }
    }

    void validateChecklistAndAttachment(UUID checklistId, UUID attachmentId) {
        Checklist checklist = checklistRepository.getChecklistById(checklistId);
        if (checklist == null) {
            throw new BadRequestException("Checklist with id " + checklistId + " does not exist.");
        }
        Attachment attachment = attachmentRepository.getAttachmentById(attachmentId);
        if (attachment == null) {
            throw new BadRequestException("Attachment with id " + attachmentId + " does not exist.");
        }
    }


    // note to ask
    @Override
    public Attachment createAttachment(AttachmentRequest attachmentRequest, UUID checklistId) {
        // Get current authenticated user's ID
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();

        // Fetch the checklist by ID, throw if not found
        Checklist checklist = checklistRepository.getChecklistById(checklistId);
        if (checklist == null) {
            throw new BadRequestException("Checklist with id " + checklistId + " not found");
        }

        // Get taskId from checklist
        UUID taskId = checklist.getTaskId();
        if (taskId == null) {
            throw new BadRequestException("Checklist with id " + checklistId + " is not associated with any task");
        }

        // Get boardId from task
        UUID boardId = taskRepository.getBoardIdByTaskId(taskId);
        if (boardId == null) {
            throw new BadRequestException("Task with id " + taskId + " is not associated with any board");
        }

        // Get memberId for current user in the board
        UUID userMemberId = boardRepository.getMemberIdByUserIdAndBoardId(userId, boardId);
        if (userMemberId == null) {
            throw new ForbiddenException("You are not a member of this board");
        }

        // Verify checklist creator matches current user's memberId
        if (!checklist.getCreatedBy().equals(userMemberId)) {
            throw new ForbiddenException("You are not authorized to add attachments to this checklist.");
        }

        // Optional: if you want to limit only one attachment per checklist
//        int existingAttachments = attachmentRepository.countByChecklistId(checklistId);
//        if (existingAttachments > 0) {
//            throw new BadRequestException("Checklist already has attachments.");
//        }

        // Validate the attachment details
        validateAttachmentDetails(attachmentRequest);

        // Create and return the new attachment with the current user's memberId as creator
        return attachmentRepository.createAttachment(attachmentRequest, checklistId, userMemberId);
    }



    @Override
    public Attachment updateAttachment(AttachmentRequest attachmentRequest, UUID checklistId, UUID attachmentId) {
        validateChecklistAndAttachment(checklistId, attachmentId);
        return attachmentRepository.updateAttachment(attachmentRequest, attachmentId, LocalDateTime.now());
    }

    @Override
    public Attachment getAttachmentByChecklistId(UUID checklistId) {
        Attachment attachment = attachmentRepository.getAttachmentByChecklistId(checklistId);
        if (attachment == null) {
            throw new BadRequestException("Attachment with checklist ID " + checklistId + " not found");
        }
        return attachment;
    }

    @Override
    public void deleteAttachmentById(UUID checklistId, UUID attachmentId) {
        validateChecklistAndAttachment(checklistId, attachmentId);
        attachmentRepository.deleteAttachmentById(checklistId, attachmentId);
    }

}



