package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.GanttChartRequest;
import org.example.zentrio.dto.response.GanttChartResponse;
import org.example.zentrio.model.GanttChart;


import java.time.LocalDateTime;
import java.util.UUID;

@Mapper
public interface GanttChartRepository {

    @Results(id = "ganttChartMapper", value = {
            @Result(property = "ganttChartId", column = "gantt_chart_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "createdBy", column = "created_by")
    })
    @Select("""
                INSERT INTO gantt_charts (title,board_id,created_by)
                VALUES (#{request.title},#{boardId},#{userId})
                RETURNING *
            """)
    GanttChart createGanttChartByBoardId(@Param("request") GanttChartRequest ganttChartRequest, UUID boardId, UUID userId);

    @Select("""
                SELECT * FROM gantt_charts WHERE board_id = #{boardId}
            """)
    @ResultMap("ganttChartMapper")
    GanttChart getAllGanttChartByBoardId(UUID boardId);

    @Select("""
                UPDATE gantt_charts SET title = #{request.title}, updated_at = #{updatedAt}
                WHERE gantt_chart_id = #{ganttChartId}
                AND board_id = #{boardId}
                RETURNING *
            """)
    @ResultMap("ganttChartMapper")
    GanttChart updateGanttChartByGanttChartId(@Param("request") GanttChartRequest ganttChartRequest, UUID ganttChartId, UUID boardId, LocalDateTime updatedAt);

    @Select("""
                SELECT * FROM gantt_charts
                WHERE gantt_chart_id = #{ganttChartId}
                AND board_id = #{boardId}
            """)
    @ResultMap("ganttChartMapper")
    GanttChart getGanttChartByIdAndBoardId(UUID ganttChartId, UUID boardId);

    @Select("""
                DELETE FROM gantt_charts
                WHERE gantt_chart_id = #{ganttChartId}
                AND board_id = #{boardId}
            """)
    void deleteGanttChartById(UUID ganttChartId, UUID boardId);


    @Select("""
                SELECT * FROM gantt_charts
                WHERE board_id = #{boardId}
                LIMIT 1
            """)
    @ResultMap("ganttChartMapper")
    GanttChart getGanttChartByBoardId(UUID boardId);

    @Select("""
                SELECT * FROM gantt_charts
                WHERE gantt_chart_id = #{ganttChartId}
            """)
    @ResultMap("ganttChartMapper")
    GanttChart getGanttChartByGanttChartId(UUID ganttChartId);

    @Select("""
                SELECT ch.*
                FROM gantt_charts ch
                         INNER JOIN boards b on b.board_id = ch.board_id
                         INNER JOIN members m ON m.board_id = b.board_id
                WHERE user_id = #{userId}
            """)
    @ResultMap("ganttChartMapper")
    GanttChart getGanttChartByCurrentUser(UUID userId);

    @Select("""
        SELECT * FROM gantt_charts
                WHERE board_id = #{boardId}
    """)
    @Results(id = "aGanttChartMapper", value = {
            @Result(property = "ganttChartId", column = "gantt_chart_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "allGanttBars", column = "gantt_chart_id",
                    many = @Many(select = "org.example.zentrio.repository.GanttBarRepository.getAllGanttBarsByGanttChart")

            )
    })
    GanttChartResponse gatAGanttChartByBoardId(UUID boardId);
}
