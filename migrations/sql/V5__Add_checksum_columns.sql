alter table file_status add column client_side_checksum varchar(64);
alter table file_status add column server_side_checksum varchar(64);
alter table file_status drop column checksum_verified;
alter table file_status drop column antivirus_passed;
alter table file_status add column antivirus_status varchar(64);
