package org.example.zentrio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JoinBoardResponse {
    private UUID boardId;
    private String title;
    private String description;
    private Boolean isFavourite;
    private List<String> role;
}
