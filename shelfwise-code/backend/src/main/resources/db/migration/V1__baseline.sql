-- Baseline: global system state and number sequences.

create table system_state (
    id                  integer primary key,
    business_date       date not null,
    last_processed_date date not null
);

create sequence card_number_seq start with 1 increment by 1;
