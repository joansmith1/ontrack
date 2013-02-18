package net.ontrack.backend;

import net.ontrack.backend.db.SQL;
import net.ontrack.backend.db.SQLUtils;
import net.ontrack.core.model.*;
import net.ontrack.core.security.SecurityRoles;
import net.ontrack.core.security.SecurityUtils;
import net.ontrack.core.support.MapBuilder;
import net.ontrack.core.validation.NameDescription;
import net.ontrack.service.ControlService;
import net.ontrack.service.EventService;
import net.ontrack.service.ManagementService;
import net.ontrack.service.model.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import javax.validation.Validator;

@Service
public class ControlServiceImpl extends AbstractServiceImpl implements ControlService {

    private final ManagementService managementService;
    private final SecurityUtils securityUtils;

    @Autowired
    public ControlServiceImpl(DataSource dataSource, Validator validator, EventService auditService, ManagementService managementService, SecurityUtils securityUtils) {
        super(dataSource, validator, auditService);
        this.managementService = managementService;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional
    @Secured({SecurityRoles.CONTROLLER, SecurityRoles.ADMINISTRATOR})
    public BuildSummary createBuild(int branch, BuildCreationForm form) {
        // Validation
        validate(form, NameDescription.class);
        // Query
        int id = dbCreate(SQL.BUILD_CREATE,
                MapBuilder.params("branch", branch)
                        .with("name", form.getName())
                        .with("description", form.getDescription()).get());
        // Branch summary
        BranchSummary theBranch = managementService.getBranch(branch);
        // Audit
        event(Event.of(EventType.BUILD_CREATED).withProject(theBranch.getProject().getId()).withBranch(theBranch.getId()).withBuild(id));
        // OK
        return new BuildSummary(id, form.getName(), form.getDescription(), theBranch);
    }

    @Override
    @Transactional
    @Secured({SecurityRoles.CONTROLLER, SecurityRoles.ADMINISTRATOR})
    public ValidationRunSummary createValidationRun(int build, int validationStamp, ValidationRunCreationForm validationRun) {
        // Run itself
        int id = dbCreate(SQL.VALIDATION_RUN_CREATE,
                MapBuilder.params("build", build)
                        .with("validationStamp", validationStamp)
                        .with("description", validationRun.getDescription()).get());
        // First status
        createValidationRunStatus(id, new ValidationRunStatusCreationForm(validationRun.getStatus(), validationRun.getDescription()));
        // Summary
        ValidationRunSummary run = managementService.getValidationRun(id);
        // Event
        event(Event.of(EventType.VALIDATION_RUN_CREATED)
                .withProject(run.getBuild().getBranch().getProject().getId())
                .withBranch(run.getBuild().getBranch().getId())
                .withValidationStamp(validationStamp)
                .withBuild(build)
                .withValidationRun(id)
                .withValue("status", validationRun.getStatus().name())
        );
        // Gets the summary
        return run;
    }

    // TODO @Override
    @Transactional
    @Secured({SecurityRoles.USER, SecurityRoles.CONTROLLER, SecurityRoles.ADMINISTRATOR})
    public ValidationRunStatusSummary createValidationRunStatus(int validationRun, ValidationRunStatusCreationForm validationRunStatus) {
        // TODO Validation of the status
        // Author
        Signature signature = securityUtils.getCurrentSignature();
        // Creation
        int id = dbCreate(SQL.VALIDATION_RUN_STATUS_CREATE,
                MapBuilder.params("validationRun", validationRun)
                        .with("status", validationRunStatus.getStatus().name())
                        .with("description", validationRunStatus.getDescription())
                        .with("author", signature.getName())
                        .with("authorId", signature.getId())
                        .with("statusTimestamp", SQLUtils.toTimestamp(SQLUtils.now())).get());
        // OK
        return new ValidationRunStatusSummary(id, signature.getName(), validationRunStatus.getStatus(), validationRunStatus.getDescription());
    }

}