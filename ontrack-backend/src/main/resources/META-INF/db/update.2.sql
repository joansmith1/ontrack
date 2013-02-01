-- A validation stamp may not be associated with any promotion level
ALTER TABLE VALIDATION_STAMP ALTER COLUMN PROMOTION_LEVEL INTEGER NULL;

-- Removes the status from the run level
ALTER TABLE VALIDATION_RUN DROP COLUMN STATUS;

-- Status for a run
CREATE TABLE VALIDATION_RUN_STATUS (
	ID INTEGER NOT NULL AUTO_INCREMENT,
	VALIDATION_RUN INTEGER NOT NULL,
	STATUS VARCHAR(20) NOT NULL,
	DESCRIPTION VARCHAR(1000) NULL,
	AUTHOR VARCHAR(80) NOT NULL,
	AUTHOR_ID INTEGER NULL,
	STATUS_TIMESTAMP TIMESTAMP NOT NULL,
	CONSTRAINT PK_VALIDATION_RUN_STATUS PRIMARY KEY (ID),
	CONSTRAINT FK_VALIDATION_RUN_STATUS_VALIDATION_RUN FOREIGN KEY (VALIDATION_RUN) REFERENCES VALIDATION_RUN (ID) ON DELETE CASCADE
);