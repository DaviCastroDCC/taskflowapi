-- TaskFlow initial schema.
--
-- UUID primary keys are assigned by the application (Hibernate @UuidGenerator), so there is
-- no need for a pgcrypto / uuid-ossp default here. Timestamps are stored with time zone and
-- written by JPA auditing in UTC.

create table users (
    id            uuid         primary key,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    email         varchar(254) not null,
    password_hash varchar(255) not null,
    full_name     varchar(120) not null,
    enabled       boolean      not null default true
);

-- Logins are matched case-insensitively, so uniqueness has to be enforced the same way.
create unique index uq_users_email on users (lower(email));

create table refresh_tokens (
    id         uuid         primary key,
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    user_id    uuid         not null references users (id) on delete cascade,
    token_hash varchar(128) not null,
    expires_at timestamptz  not null,
    revoked    boolean      not null default false,
    constraint uq_refresh_tokens_hash unique (token_hash)
);

create index ix_refresh_tokens_user on refresh_tokens (user_id);

create table workspaces (
    id         uuid         primary key,
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    name       varchar(120) not null,
    slug       varchar(60)  not null,
    owner_id   uuid         not null references users (id),
    constraint uq_workspaces_slug unique (slug)
);

create index ix_workspaces_owner on workspaces (owner_id);

create table workspace_members (
    id           uuid        primary key,
    created_at   timestamptz not null,
    updated_at   timestamptz not null,
    workspace_id uuid        not null references workspaces (id) on delete cascade,
    user_id      uuid        not null references users (id) on delete cascade,
    role         varchar(20) not null,
    constraint uq_workspace_members unique (workspace_id, user_id),
    constraint ck_workspace_members_role check (role in ('VIEWER', 'MEMBER', 'ADMIN', 'OWNER'))
);

create index ix_workspace_members_user on workspace_members (user_id);

create table projects (
    id            uuid         primary key,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    workspace_id  uuid         not null references workspaces (id) on delete cascade,
    name          varchar(120) not null,
    project_key   varchar(10)  not null,
    description   varchar(1000),
    archived      boolean      not null default false,
    task_sequence bigint       not null default 0,
    version       bigint       not null default 0,
    constraint uq_projects_key unique (workspace_id, project_key)
);

create index ix_projects_workspace on projects (workspace_id);

create table tasks (
    id          uuid         primary key,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null,
    project_id  uuid         not null references projects (id) on delete cascade,
    task_number bigint       not null,
    title       varchar(200) not null,
    description varchar(4000),
    status      varchar(20)  not null,
    priority    varchar(20)  not null,
    assignee_id uuid         references users (id),
    reporter_id uuid         not null references users (id),
    due_date    date,
    version     bigint       not null default 0,
    constraint uq_tasks_number unique (project_id, task_number),
    constraint ck_tasks_status check (status in ('BACKLOG', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    constraint ck_tasks_priority check (priority in ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

create index ix_tasks_project on tasks (project_id);
create index ix_tasks_assignee on tasks (assignee_id);
create index ix_tasks_project_status on tasks (project_id, status);

create table comments (
    id         uuid          primary key,
    created_at timestamptz   not null,
    updated_at timestamptz   not null,
    task_id    uuid          not null references tasks (id) on delete cascade,
    author_id  uuid          not null references users (id),
    body       varchar(2000) not null
);

create index ix_comments_task on comments (task_id);
