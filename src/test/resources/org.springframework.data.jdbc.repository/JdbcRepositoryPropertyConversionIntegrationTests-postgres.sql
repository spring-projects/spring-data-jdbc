DROP TABLE ENTITYWITHCOLUMNSREQUIRINGCONVERSIONS;
CREATE TABLE ENTITYWITHCOLUMNSREQUIRINGCONVERSIONS ( idTimestamp TIMESTAMP PRIMARY KEY, bool boolean, SOMEENUM VARCHAR(100), bigDecimal DECIMAL(65), bigInteger BIGINT, date TIMESTAMP, localDateTime TIMESTAMP, zonedDateTime VARCHAR(30))
