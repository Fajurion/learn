/* Create tables for account management */
create table if not exists accounts (id SERIAL NOT NULL, username VARCHAR(32), rank VARCHAR(32), email VARCHAR(32), password VARCHAR, invitor INT, data TEXT, PRIMARY KEY (id));
create table if not exists sessions (token VARCHAR(32), id INT, data TEXT, PRIMARY KEY (token));
create table if not exists invites (code VARCHAR(32), creator INT, data TEXT, PRIMARY KEY (code));
create table if not exists ranks (id SERIAL NOT NULL, name VARCHAR(32), level INT, PRIMARY KEY (id));

/* Create tables for Topics and Subtopics */
create table if not exists topics (id SERIAL NOT NULL, parent INT, name VARCHAR(50), creator INT, locked BOOLEAN, PRIMARY KEY (id));

/* Create tables for Image Storage, Comments and Post Storage */
create table if not exists images (id SERIAL NOT NULL, creator INT, image bytea, PRIMARY KEY (id));
create table if not exists posts (id SERIAL NOT NULL, topic INT, creator INT, likes INT, date BIGINT, content TEXT, PRIMARY KEY (id));
create table if not exists likes (account INT, post INT, PRIMARY KEY (account));
create table if not exists comments (id SERIAL NOT NULL, creator INT, content TEXT, PRIMARY KEY (id));