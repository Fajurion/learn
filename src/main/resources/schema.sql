drop table members, groups;
/* Create tables for account management */
create table if not exists accounts (id SERIAL NOT NULL, username VARCHAR(32), rank VARCHAR(32), email VARCHAR(32), password VARCHAR(512), invitor INT, data TEXT, PRIMARY KEY (id));
create table if not exists sessions (id SERIAL NOT NULL, token VARCHAR(100), account INT, creation BIGINT, PRIMARY KEY (token));
create table if not exists invites (id SERIAL NOT NULL, code VARCHAR(64), creator INT, data TEXT, PRIMARY KEY (id));
create table if not exists ranks (id SERIAL NOT NULL, name VARCHAR(32), level INT, PRIMARY KEY (id));

/* Create tables for Topics and Subtopics */
create table if not exists topics (id SERIAL NOT NULL, parent INT, name VARCHAR(50), creator INT, locked BOOLEAN, category BOOLEAN, PRIMARY KEY (id));

/* Create tables for Image Storage, Comments and Post Storage */
create table if not exists images (id SERIAL NOT NULL, creator INT, image bytea, PRIMARY KEY (id));
create table if not exists posts (id SERIAL NOT NULL, topic INT, creator INT, likes INT, date BIGINT, title VARCHAR(50), content TEXT, PRIMARY KEY (id));
create table if not exists likes (id SERIAL NOT NULL, account INT, post INT, PRIMARY KEY (id));
create table if not exists comments (id SERIAL NOT NULL, topic INT, post INT, creator INT, date BIGINT, content VARCHAR(512), PRIMARY KEY (id));

/* Create tables for Classes, Members and Tests */
create table if not exists groups (id SERIAL NOT NULL, name VARCHAR(32), description VARCHAR(200), creator INT, PRIMARY KEY (id));
create table if not exists tests (id SERIAL NOT NULL, name VARCHAR(32), description VARCHAR(200), date BIGINT, creator INT, PRIMARY KEY (id));
create table if not exists members (id SERIAL NOT NULL, class INT, account INT, joined BIGINT, PRIMARY KEY (id));