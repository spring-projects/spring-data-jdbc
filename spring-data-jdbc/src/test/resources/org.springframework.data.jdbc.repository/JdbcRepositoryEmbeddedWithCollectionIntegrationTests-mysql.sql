CREATE TABLE dummy_entity (id BIGINT AUTO_INCREMENT PRIMARY KEY, TEST VARCHAR(100), PREFIX_TEST VARCHAR(100));
CREATE TABLE dummy_entity2 (id BIGINT, KEY BIGINT, TEST VARCHAR(100), PRIMARY KEY(id, KEY));
