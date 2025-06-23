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

    UUID userId(){
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return userId;
    }


    private void validateNoteWithTask(CalendarRequest calendarRequest, Task task) {
//        LocalDateTime now = LocalDateTime.now();



            // Checklist finish time must be the same or after start time
            if (calendarRequest.getTillDate().isBefore(calendarRequest.getStartDate())) {
                throw new BadRequestException("Task finish time cannot be before start time.");
            }

            // Checklist finish time must not exceed Task's finish time
            if (calendarRequest.getTillDate().isAfter(task.getFinishedAt())) {
                throw new BadRequestException("Task finish time cannot exceed Task's finish time.");
            }

//        // Checklist start time must not be before Task's start time
            if (calendarRequest.getStartDate().isBefore(task.getStartedAt())) {
                throw new BadRequestException("Task start time cannot be before Task's start time.");
            }
        }



    @Override
    public HashSet<Task> getCalendarForCurrentUser(UUID boardId) {
        boardService.getBoardByBoardId(boardId);
        HashSet<Task>  calendar=new HashSet<>(calendarRepository.getCalendarTasksForUser(userId(), boardId))  ;
        if (calendar.isEmpty()){
            throw new NotFoundException("No calendar found for userId: " + userId());

        }
        return calendar;

    }

    @Override
    public Calendar CreateNoteInCalendar( CalendarRequest calendarRequest) {

//        LocalDateTime now = LocalDateTime.now();

        LocalDateTime updatedStartedTime = calendarRequest.getStartDate().withHour(00)
                .withMinute(00)
                .withSecond(00)
                .withNano(000_000_000);

        LocalDateTime updatedFinishedTime = calendarRequest.getTillDate().withHour(23)
                .withMinute(00)
                .withSecond(00)
                .withNano(000_000_000);

        calendarRequest.setStartDate(updatedStartedTime);
        calendarRequest.setTillDate(updatedFinishedTime);

    Task   task= taskService.getTaskById(calendarRequest.getTaskId());
        validateNoteWithTask(calendarRequest,task);
    Calendar calendar= calendarRepository.createCalendar(calendarRequest,LocalDateTime.now(), userId());

        return calendar;
    }

    @Override
    public Calendar getCalendarById(UUID notedId) {
        Calendar  calendar= calendarRepository.getCalendarById(notedId);
        if (calendar == null){
            throw new NotFoundException("No calendar found for userId: " + userId());
        }
        return calendar;
    }

    @Override
    public HashSet<Calendar> getAllCalendarByTaskId(UUID taskId) {
          taskService.getTaskById(taskId);
        HashSet<Calendar> calendars= new HashSet<>(calendarRepository.getAllNoteByTaskId(taskId,userId()));
        if (calendars.isEmpty()){
            throw new NotFoundException("No calendar found for userId: " + userId());
        }
        return calendars;
    }

    @Override
    public Calendar UpdateCalendarByNoteId(UUID noteId, CalendarRequest calendarRequest) {
      Calendar calendar=  getCalendarById(noteId);
      if (calendar == null){
          throw new NotFoundException("No calendar found for userId: " + userId());
      }
      Task  task= taskService.getTaskById(calendarRequest.getTaskId());
      validateNoteWithTask(calendarRequest,task);

        Calendar calendarUpdate= calendarRepository.UpdateCalendarByNoteId(calendarRequest,noteId, userId());
            if (calendarUpdate == null){
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
      calendarRepository.deleteCalendarByNoteId(noteId, taskId,userId());

        return null;
    }
}
