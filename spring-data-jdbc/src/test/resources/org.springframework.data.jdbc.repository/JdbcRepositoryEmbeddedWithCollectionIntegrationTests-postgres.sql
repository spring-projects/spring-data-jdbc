DROP TABLE dummy_entity;
CREATE TABLE dummy_entity (id SERIAL PRIMARY KEY, TEST VARCHAR(100), PREFIX_TEST VARCHAR(100));
DROP TABLE dummy_entity2;
CREATE TABLE dummy_entity2 (id BIGINT, ORDER_KEY BIGINT, TEST VARCHAR(100), PRIMARY KEY (id, ORDER_KEY));
