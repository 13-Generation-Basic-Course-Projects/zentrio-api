package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.response.ChecklistResponse;
import org.example.zentrio.model.AllMember;
import org.example.zentrio.model.AllTasks;
import org.example.zentrio.model.Report;
import org.example.zentrio.utility.JsonbTypeHandler;
import org.w3c.dom.stylesheets.LinkStyle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface ReportRepository {

    @Select("""
    SELECT r.role_name AS role_name, COUNT(*) AS total
    FROM roles r
    INNER JOIN members m ON r.role_id = m.role_id
    INNER JOIN users u ON u.user_id = m.user_id
    WHERE m.board_id = #{boardId}
    GROUP BY r.role_name
""")
    @Results(id = "roleCountMapper", value = {
            @Result(property = "roleName", column = "role_name"),
            @Result(property = "total", column = "total")
    })
    List<AllMember> getMember(UUID boardId);


    @Select("""
     SELECT task_id, stage, started_at, finished_at
                                   FROM tasks
                                   WHERE board_id = #{boardId}
""")
    @Results(id = "tasksCountMapper", value = {
            @Result(property = "task", column = "stage"),
            @Result(property = "taskId", column = "task_id"),
            @Result(property = "startAt", column = "started_at"),
            @Result(property = "finishAt", column = "finished_at"),
            @Result(property = "checklist", column = "task_id",
                    many = @Many(select = "getChecklistById"))
    })
    List<AllTasks> getTasks(UUID boardId);



    @Select("""
        SELECT * FROM  reports WHERE board_id=#{boardId}  
            ORDER BY version DESC
            LIMIT 1
        
        """)
    @Results(id = "reportMapping", value = {
            @Result(property = "reportId" , column = "report_id"),
            @Result(property = "creationDate" , column = "created_at"),
            @Result(property = "boardId" , column = "board_id"),
            @Result(property = "version" , column = "version"),
            @Result(property = "boardData" , column = "board_id",
            one = @One(select = "org.example.zentrio.repository.BoardRepository.getBoardByBoardId")),
            @Result(property = "allMembers" , column = "board_id",
            many =@Many (select = "getMember") ),
            @Result(property = "boardName" , column = "board_id",
            one = @One(select = "boardName")),
//            @Result(property = "allTasks" , column = "board_id",
//                    many= @Many(select = "org.example.zentrio.repository.TaskRepository.getAllDataInTaskByBoardId")),
    })
    Report getReportByBoardId(UUID boardId);

    @Select("""
        SELECT title FROM boards WHERE board_id= #{boardId}
        """)
    String boardName(UUID boardId);


    @Select("""
        select details from attachments where checklist_id= #{checklistId}
        """)
    @Result(column = "details", property = "details", typeHandler = JsonbTypeHandler.class)
    Map<String, Object> getAttachment(UUID checklistId);

    @Select("""
        select details from attachments where checklist_id= #{checklistId}
        """)
    @Result(column = "details", property = "details", typeHandler = JsonbTypeHandler.class)
    Map<String, Object> getAttachmentBYId(UUID checklistId);

    @Select("""
        
            select  u.username from
                               users u
        inner join  members m on  m.user_id= u.user_id
        inner join checklist_assignments ch on ch.member_id = m.member_id
        where ch.checklist_id= #{checklistId};
        """)
    List<String> allMemberUsernames( UUID checklistId);

    @Select("""
            select count(*) from comments where checklist_id= #{checklistId};
    """)
    Integer countComments(UUID checklistId);


    @Select("""
        SELECT  * FROM  checklists where task_id= #{checklistId}
        """)
    @Results(id = "checkListResponse", value = {
            @Result(property = "checklistId", column = "checklist_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "members", column = "checklist_id",
            one = @One(select = "allMemberUsernames")),
            @Result(property = "comments", column = "checklist_id",
            one = @One(select = "countComments")),
            @Result(property = "attachments", column = "checklist_id",
            one = @One(select = "getAttachmentBYId")),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "taskId", column = "task_id"),

    })
    List<ChecklistResponse> getChecklistById(UUID checklistId);

    @Select("""
        INSERT  INTO reports(created_at,created_by,board_id)
        VALUES (#{time}, #{pmID}, #{boardId})
        """)
    void createReport(LocalDateTime time, UUID boardId, UUID pmID);


    @Select("""
        
            SELECT  EXISTS(
            SELECT  1 FROM  reports rp WHERE
            rp.board_id= #{boardId}
            AND rp.created_by= #{pmID})
        """)
    Boolean getExistReport(UUID boardId, UUID pmID);

    @Select("""
        
            UPDATE  reports rp SET version= #{version}
        WHERE  report_id= #{reportId}     
        """)
    void updateVersion(UUID reportId, int version);
}
