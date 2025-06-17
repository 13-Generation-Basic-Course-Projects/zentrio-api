package org.example.zentrio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Document {

    private UUID documentId;
    private LocalDateTime creationDate;
    private boolean is_public;
    private String documentType;
    private UUID ownerId;
    private UUID boardId;
    private String drive_url;
    private String description;
    private String folderId;
}
