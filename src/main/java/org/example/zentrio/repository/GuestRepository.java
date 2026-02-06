package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.model.BoardFullData;

import java.util.UUID;

@Mapper
public interface GuestRepository {


    @Select("""
                SELECT * FROM boards
                WHERE board_id = #{boardId} AND workspace_id=#{workspaceId}
            """)
    @Results(id = "fullBoardMapper", value = {
            @Result(property = "board", column = "board_id",
                    one = @One(select = "org.example.zentrio.repository.BoardRepository.getBoardByBoardId")),
            @Result(property = "tasks",column = "board_id",
            many = @Many(select = ("org.example.zentrio.repository.TaskRepository.getAllDataInTaskByBoardId")))
    })
    BoardFullData getFullDataInBoard(UUID workspaceId, UUID boardId);


}
