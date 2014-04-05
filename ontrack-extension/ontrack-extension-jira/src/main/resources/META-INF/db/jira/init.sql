-- DB versioning

CREATE TABLE EXT_JIRA_VERSION (
  VALUE INTEGER NOT NULL,
  UPDATED TIMESTAMP NOT NULL
);

-- Configuration table

CREATE TABLE EXT_JIRA_CONFIGURATION (
  ID INTEGER NOT NULL AUTO_INCREMENT,
	NAME VARCHAR(40) NOT NULL,
	URL VARCHAR(200) NOT NULL,
	USER VARCHAR(40) NULL,
	PASSWORD VARCHAR(40) NULL,
	EXCLUSIONS VARCHAR(500) NULL,
  CONSTRAINT EXT_JIRA_CONFIGURATION_PK PRIMARY KEY (ID),
  CONSTRAINT EXT_JIRA_CONFIGURATION_UQ_NAME UNIQUE (NAME)
);
