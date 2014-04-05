package net.ontrack.extension.jira.dao;

public interface JIRASQL {
    String JIRA_CONFIGURATION_ALL = "SELECT * FROM EXT_JIRA_CONFIGURATION ORDER BY NAME";
    String JIRA_CONFIGURATION_CREATE = "INSERT INTO EXT_JIRA_CONFIGURATION (NAME, URL, USER, PASSWORD, EXCLUSIONS) VALUES (:name, :url, :user, :password, :exclusions)";
    String JIRA_CONFIGURATION_UPDATE = "UPDATE EXT_JIRA_CONFIGURATION SET NAME = :name, URL = :url, USER = :user, PASSWORD = :password, EXCLUSIONS = :exclusions WHERE ID = :id";
    String JIRA_CONFIGURATION_BY_ID = "SELECT * FROM EXT_JIRA_CONFIGURATION WHERE ID = :id";
    String JIRA_CONFIGURATION_BY_NAME = "SELECT * FROM EXT_JIRA_CONFIGURATION WHERE NAME = :name";
    String JIRA_CONFIGURATION_DELETE = "DELETE FROM EXT_JIRA_CONFIGURATION WHERE ID = :id";
    String JIRA_CONFIGURATION_PASSWORD = "SELECT PASSWORD FROM EXT_JIRA_CONFIGURATION WHERE ID = :id";
}
