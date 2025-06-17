package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.Member;


import java.util.List;
import java.util.UUID;

@Mapper
public interface MemberRepository {

    @Select("""
                SELECT m.member_id
                FROM members m
                WHERE m.user_id = #{userId}
            """)
    UUID getMemberIdByUserId(UUID userId);

    @Select("""
                INSERT INTO checklist_assignments (checklist_id,assigned_by,member_id)
                VALUES (#{checklistId},#{assignedBy},#{memberId})
            """)
    void insertIntoChecklistWithRoleMember(UUID checklistId, UUID assignedBy, UUID memberId);


    @Select("""
                SELECT DISTINCT r.role_name
                FROM members m
                JOIN roles r ON m.role_id = r.role_id
                WHERE m.user_id = #{userId}
            """)
    @Results(id = "roleMapper", value = {
            @Result(property = "roleName", column = "role_name")
    })
    List<String> getRolesByUserId(UUID userId);

    @Select("""
                SELECT username, email FROM users
                WHERE user_id = #{userId}
            """)
    AppUser getUserByUserId(UUID userId);

    @Select("""
            
             SELECT r.role_name FROM roles r
                INNER JOIN  members m ON m.role_id = r.role_id
               WHERE  m.user_id= #{userId}
                AND  r.role_name='ROLE_MANAGER'
               AND  m.board_id= #{boardId} limit 1;
            
            """)
    String getRolePMByBoardIdAndUserId(UUID boardId, UUID userId);


    @Select("""
            
                SELECT  m.member_id   from members m
                  inner join task_assignments task on task.assigned_to =m.member_id
            where m.user_id= #{uuid}
              and  task.task_id= #{taskId}
            """)
    UUID getTeamleadUUID(UUID uuid, UUID taskId);


    @Select("""
              SELECT member_id FROM members m
                 JOIN roles r ON r.role_id= m.role_id
                  WHERE m.user_id = #{userId}
            AND m.board_id = #{boardId} AND r.role_name='ROLE_MEMBER'
            """)
//    @ResultMap("memberMapper")
    UUID getMemberId(UUID userId, UUID boardId);


    @Select("""
              SELECT member_id FROM members m
                inner join roles r on m.role_id= r.role_id
              WHERE user_id = #{userId}  AND board_id = #{boardId}
                AND r.role_name='ROLE_MANAGER'
            """)
    UUID getPmId(UUID userId, UUID boardId);


    @Select("""
            
            SELECT  roles.role_name from roles
            inner join public.members m on roles.role_id = m.role_id
            inner join  task_assignments tk on tk.assigned_to =m.member_id
            OR tk.assigned_by = member_id
            where m.user_id= #{userId}
            AND  tk.task_id= #{taskId}
            AND  m.board_id= #{boardId} limit  1;
            """)
    String getRoleInTask(UUID boardId, UUID userId, UUID taskId);

    @Select("""
            
             SELECT roles.role_name   FROM roles
            INNER JOIN members m ON m.role_id = roles.role_id
            WHERE m.board_id = #{boardId}
              AND m.user_id = #{userId}
              AND role_name IN (
                'ROLE_MANAGER', 'ROLE_LEADER');
            
            """)
    String getRolePmTeamLead(UUID boardId, UUID userId);


    @Select("""
            SELECT  m.member_id FROM members m
                INNER JOIN  roles r  ON r.role_id = m.role_id
            WHERE  r.role_name= 'ROLE_MANAGER' AND m.board_id= #{boardId};
            """)
    UUID getIdByBoardId(UUID boardId);


    @Select("""
            SELECT u.user_id FROM users u
            INNER JOIN   members m ON m.user_id= u.user_id
            WHERE  m.member_id= #{assignedTo} AND m.board_id= #{boardId}
            """)
    UUID getUserId(UUID assignedTo, UUID boardId);


    @Select("""
       
            SELECT count(m.member_id) >0
       FROM members m
       WHERE m.user_id = #{userId} AND m.board_id= #{boardId}
        """)
    Boolean existMemberId(UUID userId, UUID boardId);

    @Select("""
    SELECT  member_id FROM members WHERE user_id= #{userId} AND board_id=#{boardId} LIMIT 1;
    """)
    UUID getMemberIdInboard(UUID userId, UUID boardId);

    @Select("""
      
            SELECT  us.username FROM  users us
          INNER JOIN  members m  ON m.user_id= us.user_id
          INNER JOIN roles r ON  r.role_id = m.role_id
      WHERE r.role_name='ROLE_MANAGER' AND m.board_id=#{boardId}
      
        """)
    String getPmData(UUID boardId);
}