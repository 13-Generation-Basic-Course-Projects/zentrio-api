package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.BoardFullData;
import org.example.zentrio.service.GuestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@Tag(name = "Guest Controller")
@RequiredArgsConstructor
@RequestMapping("api/v1/data")
public class GuestController {

    private final GuestService guestService;

    @Operation(summary = "Get all data in a board and workspace ID",description = "Get all data specific workspace ID and board ID")
    @GetMapping("/workspaces/{workspace-id}/boards/{board-id}")
    public ResponseEntity<ApiResponse<BoardFullData>> getFullDataInBoard(@PathVariable("workspace-id") UUID workspaceId, @PathVariable("board-id") UUID boardId) {
        BoardFullData boardFullData = guestService.getFullDataInBoard(workspaceId,boardId);
        if (boardFullData == null) {
            ApiResponse<BoardFullData> emptyResponse = ApiResponse.<BoardFullData>builder()
                    .success(false)
                    .message("No data found for the given workspace and board ID")
                    .status(HttpStatus.NOT_FOUND)
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(emptyResponse);
        }

        ApiResponse<BoardFullData> apiResponse = ApiResponse.<BoardFullData>builder()
                .success(true)
                .message("Get all data in a board and workspace ID and board ID successfully")
                .payload(boardFullData)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

}
