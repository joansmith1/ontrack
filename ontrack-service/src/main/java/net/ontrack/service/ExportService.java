package net.ontrack.service;

import net.ontrack.core.model.Ack;
import net.ontrack.core.model.ExportData;
import net.ontrack.core.model.ProjectSummary;

import java.util.Collection;
import java.util.List;

public interface ExportService {

    String exportLaunch(Collection<Integer> projectIds);

    Ack exportCheck(String uuid);

    ExportData exportDownload(String uuid);

    /**
     * Launches the import of
     *
     * @param importData Data to import
     * @return UUID to be used in {@link #importCheck(String)}
     */
    String importLaunch(ExportData importData);

    /**
     * Checks if an import is finished or not.
     *
     * @param uuid UUID of the import task (generated by {@link #importLaunch(net.ontrack.core.model.ExportData)})
     * @return The result of the check
     */
    Ack importCheck(String uuid);

    /**
     * Gets the result for an import task.
     *
     * @param uuid UUID of the import task (generated by {@link #importLaunch(net.ontrack.core.model.ExportData)})
     * @return List of imported projects
     */
    Collection<ProjectSummary> importResults(String uuid);
}
