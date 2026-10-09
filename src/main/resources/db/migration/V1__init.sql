create table inbox (
    id          bigserial primary key,
    token       varchar(40)  not null unique,
    name        varchar(120) not null,
    description varchar(500),
    active      boolean      not null default true,
    created_at  timestamptz  not null default now()
);

create table webhook_request (
    id           bigserial primary key,
    inbox_id     bigint       not null references inbox (id) on delete cascade,
    method       varchar(10)  not null,
    path         text         not null,
    query_string text,
    headers      jsonb        not null default '{}'::jsonb,
    content_type varchar(255),
    body         text,
    body_size    integer      not null default 0,
    source_ip    varchar(64),
    received_at  timestamptz  not null default now()
);

create index idx_webhook_request_inbox_received_at on webhook_request (inbox_id, received_at desc);
create index idx_webhook_request_inbox_method on webhook_request (inbox_id, method);
