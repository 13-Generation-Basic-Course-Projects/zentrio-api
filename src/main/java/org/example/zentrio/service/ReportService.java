package org.example.zentrio.service;

import org.example.zentrio.dto.response.ChecklistResponse;
import org.example.zentrio.model.AllMember;
import org.example.zentrio.model.AllTasks;
import org.example.zentrio.model.Report;
import org.example.zentrio.model.VersionReport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ReportService {
    Report getReportByBoardId(UUID boardId);

    List<AllMember> getMember(UUID boardId);

    Map<String, Object> getAttachment(UUID checklistId);

    Set<ChecklistResponse> getChecklistById(UUID checklistId);

    void updateVersionReport(Report report);

    List<AllTasks> getTasks(UUID checklistId);

    List<VersionReport> getVersionReportByReportId(UUID reportId);
}
