package org.example.zentrio.enums;

public enum NotificationTypes {
    // Access & Role Changes
    BOARD_INVITATION,
    ROLE_ASSIGNED,
    ACCESS_REVOKED,

    // Task & Checklist Updates
    TASK_ASSIGNED,
    TASK_UPDATED,
    TASK_COMMENTED,
    TASK_REMINDER,
    TASK_STATUS_CHANGED,
    CHECKLIST_ITEM_ADDED,
    CHECKLIST_COMPLETED,


    // Project / Board Updates
    BOARD_CREATED,
    BOARD_UPDATED,
    BOARD_DELETED,
    PROJECT_REPORT_SHARED,

    // Collaboration Activity
    DIRECT_MESSAGE,
    USER_MENTIONED,
    NEW_COMMENT,

    // Alerts & Critical Issues
    TASK_OVERDUE,
    TASK_BLOCKED,

    // File Changes
    FILE_UPLOADED,
    FILE_UPDATED
}
