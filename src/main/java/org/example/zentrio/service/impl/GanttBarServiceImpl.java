package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;

import org.example.zentrio.dto.request.GanttBarRequest;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.GanttBar;
import org.example.zentrio.model.GanttChart;
import org.example.zentrio.repository.BoardRepository;
import org.example.zentrio.repository.GanttBarRepository;
import org.example.zentrio.repository.GanttChartRepository;
import org.example.zentrio.service.GanttBarService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GanttBarServiceImpl implements GanttBarService {

    private final GanttBarRepository ganttBarRepository;
    private final GanttChartRepository ganttChartRepository;
    private final BoardRepository boardRepository;

    @Override
    public List<GanttBar> createGanttBarByGanttChartId(List<GanttBarRequest> ganttBarRequest) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        List<GanttBar> list= new ArrayList<>();
        for (GanttBarRequest ganttBarRequest1 : ganttBarRequest) {
            GanttChart ganttChart = ganttChartRepository.getGanttChartByGanttChartId(ganttBarRequest1.getGanttChartId());
            if (ganttChart == null) {
                throw new BadRequestException("GanttChart not found");
            }
            UUID memberId = boardRepository.getManagerMemberIdByUserIdAndBoardId(userId, ganttChart.getBoardId());
            System.out.println("memberId : " +memberId);
            if(memberId == null) {
                throw new ForbiddenException("You're not a manager of this board can't create Gantt bar");
            }

            if(ganttBarRequest1.getFinishedAt().isBefore(ganttBarRequest1.getStartedAt())){
                throw new BadRequestException("Finished time should be after started time");
            }

//            LocalDateTime now = LocalDateTime.now();

            LocalDateTime updatedStartedTime = ganttBarRequest1.getStartedAt().withHour(00)
                    .withMinute(00)
                    .withSecond(00)
                    .withNano(000_000_000);

            LocalDateTime updatedFinishedTime = ganttBarRequest1.getFinishedAt().withHour(23)
                    .withMinute(00)
                    .withSecond(00)
                    .withNano(000_000_000);

            ganttBarRequest1.setStartedAt(updatedStartedTime);
            ganttBarRequest1.setFinishedAt(updatedFinishedTime);
          list.add( ganttBarRepository.createGanttBarByGanttChartId(ganttBarRequest1, ganttBarRequest1.getGanttChartId()));

        }


        return list ;
    }

    @Override
    public GanttBar getGanttBarById(UUID ganttBarId, UUID ganttChartId) {
        GanttBar ganttBar = ganttBarRepository.getGanttBarById(ganttBarId,ganttChartId);
        if (ganttBar == null) {
            throw new BadRequestException("Gantt bar with id " + ganttBarId + " not found");
        }
        return ganttBar;
    }

    @Override
    public HashSet<GanttBar> getAllGanttBarsByGanttChartId(UUID ganttChartId) {
        GanttChart ganttChart = ganttChartRepository.getGanttChartByGanttChartId(ganttChartId);
        if (ganttChart == null) {
            throw new BadRequestException("Gantt chart not found");
        }
        return ganttBarRepository.getAllGanttBarsByGanttChartId(ganttChartId);
    }

    @Override
    public GanttBar updateGanttBarByGanttBarId(GanttBarRequest ganttBarRequest, UUID ganttBarId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        getGanttBarById(ganttBarId,ganttBarRequest.getGanttChartId());
        GanttChart ganttChart = ganttChartRepository.getGanttChartByGanttChartId(ganttBarRequest.getGanttChartId());
        if (ganttChart == null) {
            throw new BadRequestException("Gantt chart not found");
        }
        UUID memberId = boardRepository.getManagerMemberIdByUserIdAndBoardId(userId,ganttChart.getBoardId());
        if(memberId == null) {
            throw new ForbiddenException("You're not a manager of this board can't updated Gantt bar");
        }
        if(ganttBarRequest.getFinishedAt().isBefore(ganttBarRequest.getStartedAt())){
            throw new BadRequestException("Finished time should be after started time");
        }
        return ganttBarRepository.updateGanttBarByGanttBarId(ganttBarRequest,ganttBarId,ganttBarRequest.getGanttChartId());
    }

    @Override
    public void deleteGanttBarByGanttBarId(UUID ganttBarId,UUID ganttChartId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        GanttChart ganttChart = ganttChartRepository.getGanttChartByGanttChartId(ganttChartId);
        if (ganttChart == null) {
            throw new BadRequestException("Gantt chart not found");
        }
        getGanttBarById(ganttBarId,ganttChartId);
        UUID memberId = boardRepository.getManagerMemberIdByUserIdAndBoardId(userId,ganttChart.getBoardId());
        if(memberId == null) {
            throw new ForbiddenException("You're not a manager of this board can't deleted Gantt bar");
        }
        ganttBarRepository.deleteGanttBarByGanttBarId(ganttBarId,ganttChartId);
    }

    @Override
    public HashSet<GanttBar> getAllGanttBarsForCurrentUser() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return ganttBarRepository.getAllGanttBarsForCurrentUser(userId);
    }

    @Override
    public HashSet<GanttBar> getAllGanttBars() {
        return ganttBarRepository.getAllGanttBars();
    }
}
