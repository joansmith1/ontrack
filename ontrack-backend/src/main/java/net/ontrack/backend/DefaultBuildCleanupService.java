package net.ontrack.backend;

import net.ontrack.backend.dao.BuildCleanupDao;
import net.ontrack.backend.dao.model.TBuildCleanup;
import net.ontrack.core.model.BranchSummary;
import net.ontrack.core.model.ProjectSummary;
import net.ontrack.service.ManagementService;
import net.ontrack.service.api.ScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DefaultBuildCleanupService implements ScheduledService, BuildCleanupService {

    private final Logger logger = LoggerFactory.getLogger(DefaultBuildCleanupService.class);

    private final BuildCleanupDao buildCleanupDao;
    private final ManagementService managementService;

    @Autowired
    public DefaultBuildCleanupService(BuildCleanupDao buildCleanupDao, ManagementService managementService) {
        this.buildCleanupDao = buildCleanupDao;
        this.managementService = managementService;
    }

    @Override
    public Runnable getTask() {
        return this;
    }

    /**
     * Every day
     */
    @Override
    public Trigger getTrigger() {
        return new PeriodicTrigger(1, TimeUnit.DAYS);
    }

    /**
     * Main task
     */
    @Override
    public void run() {
        logger.info("[build-cleanup] Running the clean-up task...");
        List<ProjectSummary> projectList = managementService.getProjectList();
        for (ProjectSummary project : projectList) {
            List<BranchSummary> branchList = managementService.getBranchList(project.getId());
            for (BranchSummary branch : branchList) {
                // Logging
                logger.info(
                        "[build-cleanup] Running the clean-up task for project={}, branch={} ...",
                        project.getName(), branch.getName()
                );
                // Clean-up for the branch
                buildCleanup(branch.getId());
                // Logging
                logger.info(
                        "[build-cleanup] End of the clean-up task for project={}, branch={} ...",
                        project.getName(), branch.getName()
                );
            }
        }
        logger.info("[build-cleanup] End of the clean-up task.");
    }

    private void buildCleanup(int branch) {
        // Gets the configuration
        TBuildCleanup conf = buildCleanupDao.findBuildCleanUp(branch);
        if (conf == null) {
            logger.info("[build-cleanup] No clean-up configuration defined");
        } else {
            logger.info("[build-cleanup] Retention={}, excluded promotion levels={}", conf.getRetention(), conf.getExcludedPromotionLevels());
            // TODO Actual clean-up
        }
    }
}