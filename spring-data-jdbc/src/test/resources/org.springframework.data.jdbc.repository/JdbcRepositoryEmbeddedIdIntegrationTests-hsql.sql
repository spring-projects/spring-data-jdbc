CREATE TABLE dummy_entity (
  DUMMY_ID1 BIGINT,
  DUMMY_ID2 BIGINT,
  DUMMY_ATTR VARCHAR(100),
  PRIMARY KEY( DUMMY_ID1, DUMMY_ID2 )
);
CREATE TABLE sub_entity (
  DUMMY_ID1 BIGINT,
  DUMMY_ID2 BIGINT,
  SUB_ID BIGINT,
  SUB_ATTR VARCHAR(100),
  PRIMARY KEY ( DUMMY_ID1, DUMMY_ID2, SUB_ID )
);
