create table password (provider_key varchar(255) not null primary key,
hasher varchar(255) not null,
hash varchar(255) not null,
salt varchar(255));

create table users (user_id serial primary key,
first_name varchar(255),
last_name varchar(255),
email varchar(255),
provider_id varchar(255) not null,
provider_key varchar(255) not null
)

create table password_reset_token (
email varchar(255) primary key,
token varchar(64),
expiry varchar(64)
);