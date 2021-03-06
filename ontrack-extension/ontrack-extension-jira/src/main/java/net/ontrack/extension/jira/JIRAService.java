package net.ontrack.extension.jira;

import net.ontrack.extension.issue.IssueService;
import net.ontrack.extension.jira.service.model.JIRAConfiguration;
import net.ontrack.extension.jira.service.model.JIRAIssue;

import java.util.Set;

public interface JIRAService extends IssueService {

    Set<String> extractIssueKeysFromMessage(int projectId, String message);

    String getIssueURL(JIRAConfiguration configuration, String key);

    JIRAIssue getIssue(JIRAConfiguration configuration, String key);

    boolean isIssue(JIRAConfiguration configuration, String token);

    String getJIRAURL(int projectId);

    JIRAConfiguration getConfigurationForProject(int projectId);
}
