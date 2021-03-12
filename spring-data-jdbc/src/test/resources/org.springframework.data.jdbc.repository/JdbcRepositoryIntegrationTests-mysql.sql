SET SQL_MODE='ALLOW_INVALID_DATES';

CREATE TABLE dummy_entity
(
    id_Prop BIGINT AUTO_INCREMENT PRIMARY KEY,
    NAME    VARCHAR(100),
    POINT_IN_TIME TIMESTAMP(3) default null,
    OFFSET_DATE_TIME TIMESTAMP(3) default null
);
