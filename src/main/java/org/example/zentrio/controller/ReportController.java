package org.example.zentrio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.response.*;
import org.example.zentrio.model.AllMember;
import org.example.zentrio.model.AllTasks;
import org.example.zentrio.model.Report;
import org.example.zentrio.model.VersionReport;
import org.example.zentrio.repository.MemberRepository;
import org.example.zentrio.repository.ReportRepository;
import org.example.zentrio.repository.TaskRepository;
import org.example.zentrio.service.ReportService;
import org.example.zentrio.service.impl.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Report Controller")
public class ReportController {
    private final ReportService reportService;
    private final PdfService   pdfService;
    private  final MemberRepository memberRepository;
    private final ReportRepository reportRepository;


    @GetMapping("/boards/{board-id}")
    @Operation(summary = "Get report by board id")
    public ResponseEntity<ApiResponse<Report>> getReportByBoardId(@PathVariable("board-id") UUID boardId) {
        ApiResponse<Report> apiResponse = ApiResponse.<Report>builder()
                .success(true)
                .message("Get report successfully")
                .payload(reportService.getReportByBoardId(boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

//    @GetMapping("all-user-and-role")
//    public ResponseEntity<ApiResponse <List<AllMember>>> getMember(@RequestParam UUID boardId) {
//        ApiResponse<List<AllMember>> apiResponse = ApiResponse.< List<AllMember>>builder()
//                .success(true)
//                .message("Get profile successfully")
//                .payload(reportService.getMember(boardId))
//                .status(HttpStatus.OK)
//                .timestamp(LocalDateTime.now())
//                .build();
//        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
//    }
//
//
//    @GetMapping("get-attachment-by-checklist-Id")
//    public ResponseEntity<ApiResponse <Map<String, Object>>>  getAttachment (@RequestParam UUID checklistId) {
//        System.out.println(reportService.getAttachment(checklistId));
//
//        ApiResponse<Map<String, Object>> apiResponse = ApiResponse.<Map<String, Object>>builder()
//                .success(true)
//                .message("Get profile successfully")
//                .payload(reportService.getAttachment(checklistId))
//                .status(HttpStatus.OK)
//                .timestamp(LocalDateTime.now())
//                .build();
//        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
//    }
//
//
//    @GetMapping("/String")
//    public Map<String, Object>  get (@RequestParam UUID checklistId) {
//        return reportService.getAttachment(checklistId);
//
////        ApiResponse<Map<String, String>> apiResponse = ApiResponse.<Map<String, String>>builder()
////                .success(true)
////                .message("Get profile successfully")
////                .payload(reportService.getAttachment(checklistId))
////                .status(HttpStatus.OK)
////                .timestamp(LocalDateTime.now())
////                .build();
////        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
//    }
//
//
//
//
//
//    @GetMapping("get-checklist-by-Task-Id")
//    public ResponseEntity<ApiResponse <Set<ChecklistResponse>>>  getChecklistById (@RequestParam UUID checklistId) {
//        ApiResponse<Set<ChecklistResponse>> apiResponse = ApiResponse.<Set<ChecklistResponse>>builder()
//                .success(true)
//                .message("Get profile successfully")
//                .payload(reportService.getChecklistById(checklistId))
//                .status(HttpStatus.OK)
//                .timestamp(LocalDateTime.now())
//                .build();
//        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
//    }

@GetMapping("/pdf/boards/{board-id}")
@Operation(summary = "Generate report as pdf file by board id")
public ResponseEntity<byte[]> generateProjectReport(@PathVariable("board-id") UUID boardId) {
    Report report = reportService.getReportByBoardId(boardId);
    String teamLead = memberRepository.getPmData(boardId);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    Map<String, Object> reportMap = objectMapper.convertValue(report, Map.class);

    reportRepository.saveVersionReport(reportMap,report.getBoardId(),report.getVersion(),LocalDateTime.now(), report.getReportId());

    List<StageSummary> stageSummaries = new ArrayList<>();
    List<StageTaskGroup> groupedTasks = report.getGroupedTasks();
    int taskCount = 0;

    for (StageTaskGroup group : groupedTasks) {
        taskCount += group.getCount();

        int checklistCount = 0;
        for (TaskResponse task : group.getTasks()) {
            if (task.getAllChecklists() != null) {
                checklistCount += task.getAllChecklists().size();
            }
        }

        stageSummaries.add(new StageSummary(
                group.getStage(),
                group.getTasks().size(),
                checklistCount
        ));
    }

    // Monthly summaries
    List<MonthlyTaskSummary> monthlySummaries = new ArrayList<>();
    Map<Month, List<TaskResponse>> tasksByMonth = groupedTasks.stream()
            .flatMap(group -> group.getTasks().stream())
            .filter(task -> task.getCreatedAt() != null)
            .collect(Collectors.groupingBy(task -> task.getCreatedAt().getMonth()));

    for (Month month : Month.values()) {
        List<TaskResponse> tasks = tasksByMonth.getOrDefault(month, new ArrayList<>());
        int taskCountPerMonth = tasks.size();
        int subtaskCount = 0;
        int completedCount = 0;

        for (TaskResponse task : tasks) {
            List<ChecklistRespone> checklists = task.getAllChecklists();
            if (checklists != null) {
                subtaskCount += checklists.size();
                completedCount += (int) checklists.stream()
                        .filter(ChecklistRespone::isCompleted)
                        .count();
            }
        }

        double completionRateValue = (subtaskCount == 0) ? 0 : (completedCount * 100.0 / subtaskCount);
        String completionRate = String.format("%.0f%%", completionRateValue);

        monthlySummaries.add(new MonthlyTaskSummary(
                month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                taskCountPerMonth,
                subtaskCount,
                completionRate,
                report.getVersion()
        ));
    }

    int totalTaskCount = monthlySummaries.stream()
            .mapToInt(MonthlyTaskSummary::getTaskCount)
            .sum();

    int totalSubTaskCount = monthlySummaries.stream()
            .mapToInt(MonthlyTaskSummary::getSubTaskCount)
            .sum();

    double avgCompletionRateValue = monthlySummaries.isEmpty() ? 0 :
            monthlySummaries.stream()
                    .mapToInt(ms -> Integer.parseInt(ms.getCompletionRate().replace("%", "")))
                    .average()
                    .orElse(0.0);
    String avgCompletionRate = String.format("%.0f%%", avgCompletionRateValue);

    Map<String, Object> data = new HashMap<>();
    data.put("teamLead", teamLead);
    data.put("boardName", report.getBoardName());
    data.put("creationDate", report.getBoardData().getCreatedAt());
    data.put("version", report.getVersion());
    data.put("allMembers", report.getAllMembers());
    data.put("groupedTasks", groupedTasks);
    data.put("taskCount", taskCount);
    data.put("stageSummaries", stageSummaries);
    data.put("monthlySummaries", monthlySummaries);
    data.put("totalTaskCount", totalTaskCount);
    data.put("totalSubTaskCount", totalSubTaskCount);
    data.put("avgCompletionRate", avgCompletionRate);

    byte[] pdf = pdfService.generatePdf("project-report", data);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", report.getBoardName() + "-report.pdf");
    reportService.updateVersionReport(report);

    return ResponseEntity.ok()
            .headers(headers)
            .body(pdf);
}


    @GetMapping("/version/{report_id}")
    @Operation(summary = "Get version report by board id")
    public ResponseEntity<ApiResponse<List<VersionReport>>> getVersionReportByReportId(@PathVariable("report_id") UUID reportId) {
        ApiResponse<List<VersionReport>> apiResponse = ApiResponse.<List<VersionReport>>builder()
                .success(true)
                .message("Get version report successfully")
                .payload(reportService.getVersionReportByReportId(reportId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);

    }



}
