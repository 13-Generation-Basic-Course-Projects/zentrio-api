-- DATABASE SCRIPT
-- Create database
CREATE DATABASE zentrio_db;

--Create extension for uuid
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create the users table
CREATE TABLE users
(
    user_id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username      VARCHAR(50)  NOT NULL,
    gender        VARCHAR(100),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR      NOT NULL,
    profile_image VARCHAR          DEFAULT 'https://i.pinimg.com/736x/d0/7b/a6/d07ba6dcf05fa86c0a61855bc722cb7a.jpg',
    created_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    is_verified   BOOLEAN          DEFAULT false,
    is_reset      BOOLEAN          DEFAULT false
);

-- Create the roles table
CREATE TABLE roles
(
    role_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_name VARCHAR(50) NOT NULL
);

INSERT INTO roles(role_name)
VALUES ('ROLE_MANAGER'),
       ('ROLE_LEADER'),
       ('ROLE_MEMBER');

-- Create the workspaces table
CREATE TABLE workspaces
(
    workspace_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    created_by   UUID REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE
);


-- Create the boards table
CREATE TABLE boards
(
    board_id     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    is_favourite BOOLEAN          DEFAULT false,
    cover        VARCHAR(255),
    image_url    VARCHAR(255),
    workspace_id UUID REFERENCES workspaces (workspace_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the members table
CREATE TABLE members
(
    member_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id   UUID REFERENCES roles (role_id) ON DELETE CASCADE ON UPDATE CASCADE,
    user_id   UUID REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    board_id  UUID REFERENCES boards (board_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the gantt_charts table
CREATE TABLE gantt_charts
(
    gantt_chart_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title          VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    created_by     UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    board_id       UUID UNIQUE REFERENCES boards (board_id) ON DELETE CASCADE ON UPDATE CASCADE

);

-- Create the gantt_bars table
CREATE TABLE gantt_bars
(
    gantt_bar_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title          VARCHAR(255) NOT NULL,
    started_at     TIMESTAMP    NOT NULL,
    finished_at    TIMESTAMP    NOT NULL,
    gantt_chart_id UUID REFERENCES gantt_charts (gantt_chart_id) ON DELETE CASCADE ON UPDATE CASCADE,
    face           varchar(100)
);


-- Create the tasks table
CREATE TABLE tasks
(
    task_id      UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(50)           DEFAULT 'PENDING',
    created_at   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    started_at   TIMESTAMP    NOT NULL,
    finished_at  TIMESTAMP    NOT NULL,
    task_order   SERIAL,
    stage        VARCHAR(50)  NOT NULL DEFAULT 'TO DO',
    created_by   UUID REFERENCES members (member_id) ON UPDATE CASCADE ON DELETE CASCADE,
    board_id     UUID REFERENCES boards (board_id) ON DELETE CASCADE ON UPDATE CASCADE,
    gantt_bar_id UUID REFERENCES gantt_bars (gantt_bar_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the task_assignments table
CREATE TABLE task_assignments
(
    task_assign_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assigned_to    UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    assigned_by    UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    task_id        UUID UNIQUE REFERENCES tasks (task_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the checklists table
CREATE TABLE checklists
(
    checklist_id    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(50)      DEFAULT 'PENDING',
    cover           VARCHAR(255),
    created_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    checklist_order SERIAL,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    created_by      UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    task_id         UUID REFERENCES tasks (task_id) ON DELETE CASCADE ON UPDATE CASCADE
);


-- Create the checklist_assignments table
CREATE TABLE checklist_assignments
(
    checklist_assign_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    member_id           UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    assigned_by         UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    checklist_id        UUID REFERENCES checklists (checklist_id) ON DELETE CASCADE ON UPDATE CASCADE
);


-- Create the calendars table
CREATE TABLE calendars
(
    calendar_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    noted       TEXT,
    noted_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    till_date   TIMESTAMP,
    user_id     UUID REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    task_id     UUID REFERENCES tasks (task_id) ON DELETE CASCADE ON UPDATE CASCADE
);


-- Create the comments table
CREATE TABLE comments
(
    comment_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content      TEXT NOT NULL,
    comment_date TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    commented_by UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    checklist_id UUID REFERENCES checklists (checklist_id) ON DELETE CASCADE ON UPDATE CASCADE
);


-- Create the documents table
CREATE TABLE documents
(
    document_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  TIMESTAMP,
    is_public   BOOLEAN          DEFAULT false,
    doc_type    VARCHAR(100),
    drive_url   text,
    description text,
    folder_id   text,
    user_id     UUID REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    board_id    UUID REFERENCES boards (board_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the attachments table
CREATE TABLE attachments
(
    attachment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    details       JSONB NOT NULL,
    created_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    created_by    UUID REFERENCES members (member_id) ON UPDATE CASCADE ON DELETE CASCADE,
    checklist_id  UUID UNIQUE REFERENCES checklists (checklist_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the achievements table
CREATE TABLE achievements
(
    achievement_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    details        JSONB NOT NULL,
    created_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    user_id        UUID REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the notifications table
CREATE TABLE notifications
(
    notification_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content         TEXT NOT NULL,
    type            VARCHAR(50),
    is_read         BOOLEAN          DEFAULT FALSE,
    created_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    task_assign_id  UUID REFERENCES task_assignments (task_assign_id) ON DELETE CASCADE ON UPDATE CASCADE,
    sender_id       UUID REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    receiver_id     UUID REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the reports table
CREATE TABLE reports
(
    report_id  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version    SERIAL,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    board_id   UUID REFERENCES boards (board_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the feedbacks table
CREATE TABLE feedbacks
(
    feedback_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    comment     TEXT NOT NULL,
    task_id     UUID REFERENCES tasks (task_id) ON DELETE CASCADE ON UPDATE CASCADE,
    feedback_by UUID REFERENCES members (member_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- trigger for report version
CREATE OR REPLACE FUNCTION override_version_per_board()
    RETURNS TRIGGER AS
$$
DECLARE
    max_version INTEGER;
BEGIN
    SELECT COALESCE(MAX(version), 0)
    INTO max_version
    FROM reports
    WHERE board_id = NEW.board_id;

    NEW.version := max_version + 1;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_version_per_board
    BEFORE INSERT
    ON reports
    FOR EACH ROW
EXECUTE FUNCTION override_version_per_board();


---task_order
CREATE OR REPLACE FUNCTION set_task_order_per_board()
    RETURNS TRIGGER AS
$$
DECLARE
    max_order INTEGER;
BEGIN
    SELECT COALESCE(MAX(task_order), 0)
    INTO max_order
    FROM tasks
    WHERE board_id = NEW.board_id;

    NEW.task_order := max_order + 1;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_set_task_order ON tasks;

CREATE TRIGGER trigger_set_task_order
    BEFORE INSERT
    ON tasks
    FOR EACH ROW
EXECUTE FUNCTION set_task_order_per_board();
CREATE OR REPLACE FUNCTION reorder_task_order_after_delete()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Shift down all tasks in the same board with higher order
    UPDATE tasks
    SET task_order = task_order - 1
    WHERE board_id = OLD.board_id
      AND task_order > OLD.task_order;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_reorder_after_task_delete ON tasks;

CREATE TRIGGER trigger_reorder_after_task_delete
    AFTER DELETE
    ON tasks
    FOR EACH ROW
EXECUTE FUNCTION reorder_task_order_after_delete();



--- trigger for checklist_order
CREATE OR REPLACE FUNCTION set_checklist_order_per_task()
    RETURNS TRIGGER AS
$$
DECLARE
    max_order INTEGER;
BEGIN
    SELECT COALESCE(MAX(checklist_order), 0)
    INTO max_order
    FROM checklists
    WHERE task_id = NEW.task_id;

    NEW.checklist_order := max_order + 1;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trigger_set_checklist_order ON checklists;

CREATE TRIGGER trigger_set_checklist_order
    BEFORE INSERT
    ON checklists
    FOR EACH ROW
EXECUTE FUNCTION set_checklist_order_per_task();

CREATE OR REPLACE FUNCTION reorder_checklist_order_after_delete()
    RETURNS TRIGGER AS
$$
BEGIN
    UPDATE checklists
    SET checklist_order = checklist_order - 1
    WHERE task_id = OLD.task_id
      AND checklist_order > OLD.checklist_order;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trigger_reorder_checklist_after_delete ON checklists;

CREATE TRIGGER trigger_reorder_checklist_after_delete
    AFTER DELETE
    ON checklists
    FOR EACH ROW
EXECUTE FUNCTION reorder_checklist_order_after_delete();
