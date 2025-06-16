package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.response.ChecklistResponse;
import org.example.zentrio.dto.response.StageTaskGroup;
import org.example.zentrio.dto.response.TaskRespone;
import org.example.zentrio.enums.RoleName;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.AllMember;
import org.example.zentrio.model.AllTasks;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.Report;
import org.example.zentrio.repository.MemberRepository;
import org.example.zentrio.repository.ReportRepository;
import org.example.zentrio.repository.TaskRepository;
import org.example.zentrio.service.BoardService;
import org.example.zentrio.service.ReportService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ReportServiceImpl implements ReportService {

    private final BoardService  boardService;
    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final TaskRepository taskRepository;

    public UUID userId (){
        AppUser appUser= (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUser.getUserId();
    }


    @Override
    public Report getReportByBoardId(UUID boardId) {
        boardService.getBoardByBoardId(boardId);
        String role= memberRepository.getRolePMByBoardIdAndUserId(boardId,userId());
        UUID pmID= memberRepository.getPmId(userId(),boardId);
        if (pmID==null){
            throw  new BadRequestException("You are not PM of this board");
        }
        if(role==null){
            throw new BadRequestException("You are not allow to get report by this board");
        }
        if (!role.equals(RoleName.ROLE_MANAGER.name())){
            throw new BadRequestException("Only manager can view report by this board");
        }
        Boolean existReport= reportRepository.getExistReport(boardId, pmID);
        if(!existReport){
            reportRepository.createReport(LocalDateTime.now(),boardId,pmID);
        }


        Report report = reportRepository.getReportByBoardId(boardId);
        if (report == null) {
            throw new NotFoundException("Report not found");
        }
        List<TaskRespone> rawTasks = Optional.ofNullable(taskRepository.getAllDataInTaskByBoardId(boardId))
                .orElse(Collections.emptyList());


        // ✅ Group tasks by stage
        Map<String, List<TaskRespone>> tasksByStage = rawTasks.stream()
                .collect(Collectors.groupingBy(TaskRespone::getStage));

        // ✅ Create StageTaskGroup list
        List<StageTaskGroup> groupedTasks = tasksByStage.entrySet().stream()
                .map(entry -> new StageTaskGroup(
                        entry.getKey(),            // stage name
                        entry.getValue().size(),   // count
                        entry.getValue()           // list of tasks
                ))
                .collect(Collectors.toList());

        report.setGroupedTasks(groupedTasks);
        return report;
    }


//    Report report = reportRepository.getReportByBoardId(boardId);
//    List<AllTasks> tasks = report.getAllTasks();
//
//    Map<String, Long> countByStage = tasks.stream()
//            .collect(Collectors.groupingBy(AllTasks::getTask, Collectors.counting()));
//
//
//for (AllTasks task : tasks) {
//        task.setTotal(countByStage.get(task.getTask()).intValue());
//    }




    @Override
    public List<AllMember> getMember(UUID boardId) {

        return reportRepository.getMember(boardId);
    }

    @Override
    public Map<String, Object> getAttachment(UUID checklistId) {
        return reportRepository.getAttachment(checklistId);
    }

    @Override
    public Set<ChecklistResponse> getChecklistById(UUID takId) {

        Set<ChecklistResponse>  response=new  HashSet<>(reportRepository.getChecklistById(takId));
        return response;
    }

    @Override
    public void updateVersionReport(Report report) {
        int version = report.getVersion() +1;
        reportRepository.updateVersion(report.getReportId(), version);

    }

    @Override
    public List<AllTasks> getTasks(UUID checklistId) {
        return reportRepository.getTasks(checklistId);
    }
}
