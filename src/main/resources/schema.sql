/* drop table accounts, sessions, invites, ranks; */
create table if not exists accounts (id SERIAL NOT NULL, username VARCHAR(32), rank VARCHAR(32), email VARCHAR(32), password VARCHAR, invitor INT, data TEXT, PRIMARY KEY (id));
create table if not exists sessions (token VARCHAR(32), username VARCHAR(32), data TEXT, PRIMARY KEY (token));
create table if not exists invites (code VARCHAR(32), creator INT, data TEXT, PRIMARY KEY (code));
create table if not exists ranks (id SERIAL NOT NULL, name VARCHAR(32), level INT, PRIMARY KEY (id));