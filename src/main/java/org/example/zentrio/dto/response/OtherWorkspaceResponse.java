package org.example.zentrio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.example.zentrio.model.Board;
import org.example.zentrio.model.Workspace;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtherWorkspaceResponse {
    private UUID workspaceId;
    private String title;
    private String description;
    private MemberResponseData createdByDetails;
    private List<JoinBoardResponse> otherBoards;

}
