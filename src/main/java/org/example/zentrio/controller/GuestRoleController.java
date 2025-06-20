package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.BoardResponse;
import org.example.zentrio.service.BoardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/board-guest-role")
@Tag(name = "For Guest Role")
@RequiredArgsConstructor
public class GuestRoleController {

    private final BoardService boardService;

    @Operation(summary = "Get from workspace for guest role", description = "Get all data from workspace for guest role")
    @GetMapping("boards/{board-id}")
    ResponseEntity<ApiResponse<BoardResponse>> getAllDataForGuestRole(@PathVariable("board-id") UUID boardId) {
        ApiResponse<BoardResponse> apiResponse = ApiResponse.<BoardResponse>builder()
                .success(true)
                .message("Get all data for guest role successfully")
                .payload(boardService.getAllDataInBoard(boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

}
