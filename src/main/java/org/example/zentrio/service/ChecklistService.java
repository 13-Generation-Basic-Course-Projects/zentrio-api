package org.example.zentrio.service;

import org.example.zentrio.dto.request.ChecklistRequest;
import org.example.zentrio.dto.request.StatusRequest;
import org.example.zentrio.model.Checklist;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public interface ChecklistService {
    Checklist createChecklist(UUID taskId, ChecklistRequest checklistRequest);

    UUID checkExistedTaskId(UUID taskId, UUID currentUserId);

    UUID checkTaskIdToGetChecklist(UUID taskId);

    List<Checklist> getAllChecklistByTaskId(UUID taskId);

    Checklist getChecklistChecklistId( UUID checklistId);

    HashMap<String, Checklist> getChecklistByTaskIdAndTitle(UUID taskId, String title);

    Checklist updateChecklistById(UUID checklistId, ChecklistRequest checklistRequest);

    Checklist deleteChecklistByChecklist(UUID checklistId);

    Checklist assignMemberToChecklist(UUID assignedByUserId, UUID assignToUserId, UUID checklistId, UUID taskId);

    ChecklistRequest checkDateOfCreateChecklist(UUID taskId, ChecklistRequest checklistRequest);

    Checklist updateChecklistStatusById(UUID checklistId, StatusRequest statusRequest);
}
