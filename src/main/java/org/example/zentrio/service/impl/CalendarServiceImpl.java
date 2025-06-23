package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.CalendarRequest;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.*;
import org.example.zentrio.repository.CalendarRepository;
import org.example.zentrio.repository.MemberRepository;
import org.example.zentrio.service.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CalendarRepository calendarRepository;
    private final ChecklistService checklistService;
    private final BoardService boardService;
    private final CommentService commentService;
    private final TaskService taskService;
    private final MemberRepository memberRepository;

    UUID userId() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return userId;
    }

    // update update
    private void validateNoteWithTask(CalendarRequest calendarRequest, Task task) {
        LocalDateTime startDate = calendarRequest.getStartDate();
        LocalDateTime tillDate = calendarRequest.getTillDate();
        LocalDateTime taskStart = task.getStartedAt();
        LocalDateTime taskEnd = task.getFinishedAt();

        // Start time must not be before task's start time
        if (startDate.isBefore(taskStart)) {
            throw new BadRequestException("Start time cannot be before task's start time.");
        }

        // Allow any time on the same day as taskEnd
        LocalDate taskEndDate = taskEnd.toLocalDate();
        LocalDate tillDateDate = tillDate.toLocalDate();

        if (tillDateDate.isAfter(taskEndDate)) {
            throw new BadRequestException("Finish time cannot exceed task's finish date.");
        }

        // Optional: Uncomment if you want to enforce chronological order
        if (tillDate.isBefore(startDate)) {
            throw new BadRequestException("Finish time cannot be before start time.");
        }
    }





    @Override
    public HashSet<Task> getCalendarForCurrentUser(UUID boardId) {
        boardService.getBoardByBoardId(boardId);
        HashSet<Task> calendar = new HashSet<>(calendarRepository.getCalendarTasksForUser(userId(), boardId));
        if (calendar.isEmpty()) {
            throw new NotFoundException("No calendar found for userId: " + userId());

        }
        return calendar;

    }

    @Override
    public Calendar CreateNoteInCalendar(CalendarRequest calendarRequest) {
        Task task = taskService.getTaskById(calendarRequest.getTaskId());
        validateNoteWithTask(calendarRequest, task);
        Calendar calendar = calendarRepository.createCalendar(calendarRequest, LocalDateTime.now(), userId());

        return calendar;
    }

    @Override
    public Calendar getCalendarById(UUID notedId) {
        Calendar calendar = calendarRepository.getCalendarById(notedId);
        if (calendar == null) {
            throw new NotFoundException("No calendar found for userId: " + userId());
        }
        return calendar;
    }

    @Override
    public HashSet<Calendar> getAllCalendarByTaskId(UUID taskId) {
        taskService.getTaskById(taskId);
        HashSet<Calendar> calendars = new HashSet<>(calendarRepository.getAllNoteByTaskId(taskId, userId()));
        if (calendars.isEmpty()) {
            throw new NotFoundException("No calendar found for userId: " + userId());
        }
        return calendars;
    }

    @Override
    public Calendar UpdateCalendarByNoteId(UUID noteId, CalendarRequest calendarRequest) {
        Calendar calendar = getCalendarById(noteId);
        if (calendar == null) {
            throw new NotFoundException("No calendar found for userId: " + userId());
        }
        Task task = taskService.getTaskById(calendarRequest.getTaskId());
        validateNoteWithTask(calendarRequest, task);

        Calendar calendarUpdate = calendarRepository.UpdateCalendarByNoteId(calendarRequest, noteId, userId());
        if (calendarUpdate == null) {
            throw new NotFoundException("No calendar updated for userId: " + userId());
        }

        return calendarUpdate;
    }

    @Override
    public Void deleteCalendarByNoteId(UUID noteId, UUID taskId) {
        Calendar calendar = getCalendarById(noteId);
        System.out.println(calendar);
        if (calendar == null) {
            throw new NotFoundException("No calendar found ");
        }
        if (!calendar.getUserId().equals(userId())) {
            throw new ForbiddenException("You are not allowed to delete this calendar");
        }
        calendarRepository.deleteCalendarByNoteId(noteId, taskId, userId());

        return null;
    }
}
