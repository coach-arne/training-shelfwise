-- Domain schema for increments 1 and 2.

create table branch (
    id   bigserial primary key,
    code varchar(3)  not null unique,
    name varchar(60) not null
);

create table member (
    id                 bigserial primary key,
    card_number        varchar(20)  not null unique,
    first_name         varchar(80)  not null,
    last_name          varchar(80)  not null,
    birth_date         date         not null,
    street             varchar(120) not null,
    postal_code        varchar(12)  not null,
    city               varchar(80)  not null,
    email              varchar(160) not null,
    phone              varchar(40),
    sms_opt_in         boolean      not null default false,
    keep_history       boolean      not null default false,
    category           varchar(10)  not null,
    registration_date  date         not null,
    membership_start   date         not null,
    membership_end     date         not null,
    guardian_member_id bigint references member (id),
    guardian_name      varchar(160),
    guardian_email     varchar(160),
    password_hash      varchar(100) not null
);
create index idx_member_name on member (lower(last_name), lower(first_name));
create index idx_member_birth_date on member (birth_date);

create table staff_account (
    id               bigserial primary key,
    username         varchar(60)  not null unique,
    display_name     varchar(120) not null,
    password_hash    varchar(100) not null,
    role             varchar(20)  not null,
    branch_id        bigint references branch (id),
    linked_member_id bigint references member (id),
    active           boolean      not null default true
);

create table title (
    id               bigserial primary key,
    title            varchar(255) not null,
    authors          varchar(255) not null,
    publisher        varchar(160),
    publication_year integer,
    isbn             varchar(20),
    material_type    varchar(20)  not null
);
create index idx_title_isbn on title (isbn);

create table copy (
    id                     bigserial primary key,
    barcode                varchar(30)    not null unique,
    title_id               bigint         not null references title (id),
    home_branch_id         bigint         not null references branch (id),
    current_branch_id      bigint         not null references branch (id),
    transit_destination_id bigint references branch (id),
    shelf_mark             varchar(40),
    status                 varchar(20)    not null,
    replacement_cost       numeric(10, 2),
    acquisition_date       date           not null
);
create index idx_copy_title on copy (title_id);

create table loan (
    id                 bigserial primary key,
    copy_id            bigint      not null references copy (id),
    member_id          bigint references member (id),
    member_category    varchar(10) not null,
    checkout_date      date        not null,
    due_date           date        not null,
    return_date        date,
    status             varchar(10) not null,
    renewal_count      integer     not null default 0,
    checkout_branch_id bigint      not null references branch (id),
    return_branch_id   bigint references branch (id),
    lost_date          date,
    anonymised         boolean     not null default false
);
create index idx_loan_member on loan (member_id);
create index idx_loan_copy on loan (copy_id);
create index idx_loan_status_due on loan (status, due_date);

create table loan_event (
    id            bigserial primary key,
    loan_id       bigint      not null references loan (id),
    type          varchar(10) not null,
    event_date    date        not null,
    branch_id     bigint references branch (id),
    channel       varchar(10) not null,
    material_type varchar(20) not null
);
create index idx_loan_event_date on loan_event (event_date);

create table reservation (
    id                   bigserial primary key,
    title_id             bigint      not null references title (id),
    member_id            bigint      not null references member (id),
    pickup_branch_id     bigint      not null references branch (id),
    status               varchar(12) not null,
    placed_at            timestamptz not null,
    assigned_copy_id     bigint references copy (id),
    notified_on          date,
    hold_until           date,
    reminder_sent        boolean     not null default false,
    ended_on             date,
    cancelled_by_library boolean     not null default false
);
create index idx_reservation_title on reservation (title_id, status);
create index idx_reservation_member on reservation (member_id, status);

create table charge (
    id                 bigserial primary key,
    member_id          bigint         not null references member (id),
    type               varchar(20)    not null,
    original_amount    numeric(10, 2) not null,
    outstanding_amount numeric(10, 2) not null,
    charge_date        date           not null,
    status             varchar(10)    not null,
    loan_id            bigint references loan (id),
    reservation_id     bigint references reservation (id),
    description        varchar(255)
);
create index idx_charge_member on charge (member_id, status);
create index idx_charge_loan on charge (loan_id);

create table charge_transaction (
    id               bigserial primary key,
    charge_id        bigint         not null references charge (id),
    type             varchar(15)    not null,
    amount           numeric(10, 2) not null,
    transaction_date date           not null,
    note             varchar(255)
);
create index idx_charge_tx_charge on charge_transaction (charge_id);

create table waiver (
    id          bigserial primary key,
    charge_id   bigint         not null references charge (id),
    amount      numeric(10, 2) not null,
    reason      varchar(500)   not null,
    staff_id    bigint         not null references staff_account (id),
    staff_role  varchar(20)    not null,
    branch_id   bigint references branch (id),
    waiver_date date           not null
);
create index idx_waiver_charge on waiver (charge_id);

create table payment (
    id                 bigserial primary key,
    member_id          bigint         not null references member (id),
    amount             numeric(10, 2) not null,
    method             varchar(15)    not null,
    payment_date       date           not null,
    staff_id           bigint references staff_account (id),
    branch_id          bigint references branch (id),
    provider_reference varchar(60) unique
);

create table payment_allocation (
    id         bigserial primary key,
    payment_id bigint         not null references payment (id),
    charge_id  bigint         not null references charge (id),
    amount     numeric(10, 2) not null
);

create table online_payment_session (
    id         bigserial primary key,
    reference  varchar(60)    not null unique,
    member_id  bigint         not null references member (id),
    amount     numeric(10, 2) not null,
    status     varchar(12)    not null,
    created_on date           not null
);

create table online_payment_session_charge (
    session_id bigint not null references online_payment_session (id),
    charge_id  bigint not null
);

create table membership_fee_payment (
    id        bigserial primary key,
    member_id bigint         not null references member (id),
    amount    numeric(10, 2) not null,
    method    varchar(15),
    paid_on   date           not null,
    kind      varchar(15)    not null,
    staff_id  bigint references staff_account (id),
    branch_id bigint references branch (id)
);

create table notification (
    id                bigserial primary key,
    member_id         bigint references member (id),
    type              varchar(60)  not null,
    channel           varchar(10)  not null,
    recipient         varchar(160) not null,
    subject           varchar(200) not null,
    body              text         not null,
    notification_date date         not null,
    delivery_status   varchar(20)  not null,
    created_at        timestamptz  not null
);
create index idx_notification_date on notification (notification_date);

create table fine_rate (
    id            bigserial primary key,
    category      varchar(10)    not null,
    material_type varchar(20)    not null,
    daily_rate    numeric(10, 2) not null,
    valid_from    date           not null,
    unique (category, material_type, valid_from)
);

create table public_holiday (
    id           bigserial primary key,
    holiday_date date         not null unique,
    name         varchar(80)  not null
);

create table loan_limit (
    category  varchar(10) primary key,
    max_loans integer     not null
);
