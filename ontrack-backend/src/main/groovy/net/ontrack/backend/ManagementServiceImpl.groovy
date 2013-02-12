package net.ontrack.backend

import net.ontrack.backend.db.SQL
import net.ontrack.backend.db.SQLUtils
import net.ontrack.core.model.*
import net.ontrack.core.security.SecurityRoles
import net.ontrack.core.validation.NameDescription
import net.ontrack.service.EventService
import net.ontrack.service.ManagementService
import net.ontrack.service.model.Event
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

import javax.sql.DataSource
import javax.validation.Validator
import java.sql.ResultSet
import java.sql.SQLException

@Service
class ManagementServiceImpl extends AbstractServiceImpl implements ManagementService {

	@Autowired
	public ManagementServiceImpl(DataSource dataSource, Validator validator, EventService auditService) {
		super(dataSource, validator, auditService);
	}
	
	// Project groups
	
	@Override
	@Transactional(readOnly = true)
	public List<ProjectGroupSummary> getProjectGroupList() {
		return dbList(SQL.PROJECT_GROUP_LIST, [:]) { rs ->
			new ProjectGroupSummary(rs.getInt("id"), rs.getString("name"), rs.getString("description"))
		}
	}

	@Override
	@Transactional
    @Secured(SecurityRoles.ADMINISTRATOR)
	public ProjectGroupSummary createProjectGroup(ProjectGroupCreationForm form) {
		// Validation
		validate(form, NameDescription.class);
		// Query
		int id = dbCreate (SQL.PROJECT_GROUP_CREATE, ["name": form.name, "description": form.description])
		// Audit
		event(Event.of(EventType.PROJECT_GROUP_CREATED).withProjectGroup(id))
		// OK
		new ProjectGroupSummary(id, form.name, form.description)
	}
	
	// Projects
	
	ProjectSummary readProjectSummary (ResultSet rs) {
		return new ProjectSummary(rs.getInt("id"), rs.getString("name"), rs.getString("description"))
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<ProjectSummary> getProjectList() {
		return dbList(SQL.PROJECT_LIST, [:]) { readProjectSummary(it) }
	}
	
	@Override
	@Transactional(readOnly = true)
	public ProjectSummary getProject(int id) {
		return dbLoad(SQL.PROJECT, id) {readProjectSummary(it) }
	}

	@Override
	@Transactional
    @Secured(SecurityRoles.ADMINISTRATOR)
	public ProjectSummary createProject(ProjectCreationForm form) {
		// Validation
		validate(form, NameDescription.class);
		// Query
		int id = dbCreate (SQL.PROJECT_CREATE, ["name": form.name, "description": form.description])
		// Audit
		event(Event.of(EventType.PROJECT_CREATED).withProject(id))
		// OK
		new ProjectSummary(id, form.name, form.description)
	}
	
	@Override
	@Transactional
    @Secured(SecurityRoles.ADMINISTRATOR)
	public Ack deleteProject(int id) {
		def name = getEntityName(Entity.PROJECT, id)
		def ack = dbDelete(SQL.PROJECT_DELETE, id)
		if (ack.success) {
			event(Event.of(EventType.PROJECT_DELETED).withValue("project", name))
		}
		return ack
	}
	
	// Branches
	
	BranchSummary readBranchSummary (ResultSet rs) {
		return new BranchSummary(rs.getInt("id"), rs.getString("name"), rs.getString("description"), getProject(rs.getInt("project")))
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<BranchSummary> getBranchList(int project) {
		return dbList(SQL.BRANCH_LIST, ["project": project]) { readBranchSummary(it) }
	}
	
	@Override
	@Transactional(readOnly = true)
	public BranchSummary getBranch(int id) {
		return dbLoad(SQL.BRANCH, id) { readBranchSummary(it) }
	}
	
	@Override
	@Transactional
    @Secured(SecurityRoles.ADMINISTRATOR)
	public BranchSummary createBranch(int project, BranchCreationForm form) {
		// Validation
		validate(form, NameDescription.class)
		// Query
		int id = dbCreate (SQL.BRANCH_CREATE, ["project": project, "name": form.name, "description": form.description])
		// Audit
		event(Event.of(EventType.BRANCH_CREATED).withProject(project).withBranch(id))
		// OK
		new BranchSummary(id, form.name, form.description, getProject(project))
	}
	
	// Validation stamps
	
	ValidationStampSummary readValidationStampSummary (ResultSet rs) {
		return new ValidationStampSummary(rs.getInt("id"), rs.getString("name"), rs.getString("description"), getBranch(rs.getInt("branch")))
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<ValidationStampSummary> getValidationStampList(int branch) {
		return dbList(SQL.VALIDATION_STAMP_LIST, ["branch": branch]) { readValidationStampSummary(it) }
	}
	
	@Override
	@Transactional(readOnly = true)
	public ValidationStampSummary getValidationStamp(int id) {
		return dbLoad(SQL.VALIDATION_STAMP, id) { readValidationStampSummary(it) }
	}
	
	@Override
	@Transactional
    @Secured(SecurityRoles.ADMINISTRATOR)
	public ValidationStampSummary createValidationStamp(int branch, ValidationStampCreationForm form) {
		// Validation
		validate(form, NameDescription.class)
		// Query
		int id = dbCreate (SQL.VALIDATION_STAMP_CREATE, ["branch": branch, "name": form.name, "description": form.description])
		// Branch summary
		def theBranch = getBranch(branch)
		// Audit
		event(Event.of(EventType.VALIDATION_STAMP_CREATED).withProject(theBranch.project.id).withBranch(theBranch.id).withValidationStamp(id))
		// OK
		new ValidationStampSummary(id, form.name, form.description, theBranch)
	}
	
	@Override
	@Transactional
    @Secured(SecurityRoles.ADMINISTRATOR)
	public Ack imageValidationStamp(int validationStampId, MultipartFile image) {
		// Checks the image type
		def contentType = image.getContentType();
		if (contentType != "image/png") {
			throw new ImageIncorrectMIMETypeException(contentType, "image/png");
		} 
		// Checks the size
		def imageSize = image.getSize()
		if (imageSize > SQL.VALIDATION_STAMP_IMAGE_MAXSIZE) {
			throw new ImageTooBigException(imageSize, SQL.VALIDATION_STAMP_IMAGE_MAXSIZE)
		}
		// Gets the bytes
		def content = image.getBytes()
		// Updates the content
		def count = getNamedParameterJdbcTemplate().update(
			SQL.VALIDATIONSTAMP_IMAGE_UPDATE,
			params("id", validationStampId).addValue("image", content))
		// OK
		return Ack.one(count)
	}
	
	@Override
	public byte[] imageValidationStamp(int validationStampId) {
		def list = getNamedParameterJdbcTemplate().query(
			SQL.VALIDATIONSTAMP_IMAGE,
			params("id", validationStampId),
			new RowMapper<byte[]>() {
				@Override
				byte[] mapRow(ResultSet rs, int row) throws SQLException ,DataAccessException {
					return rs.getBytes("image")
				}
			})
		if (list.isEmpty()) {
			return null
		} else {
			return list[0]
		}
	}
	
	// Builds
	
	BuildSummary readBuildSummary (ResultSet rs) {
		return new BuildSummary(rs.getInt("id"), rs.getString("name"), rs.getString("description"), getBranch(rs.getInt("branch")))
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<BuildCompleteStatus> getBuildList(int branch, int offset, int count) {
		List<BuildSummary> builds = dbList(SQL.BUILD_LIST, ["branch": branch, "offset": offset, "count": count]) { readBuildSummary(it) }
        builds.collect { summary ->
            def stamps = getBuildValidationStamps(summary.id)
            new BuildCompleteStatus(summary, stamps)
        }
	}
	
	@Override
	@Transactional(readOnly = true)
	public BuildSummary getBuild(int id) {
		return dbLoad(SQL.BUILD, id) { readBuildSummary(it) }
	}

    @Override
    @Transactional(readOnly = true)
    List<BuildValidationStamp> getBuildValidationStamps(int buildId) {
        // Gets the build details
        def build = getBuild(buildId)
        // Gets all the stamps for the branch
        def stamps = getValidationStampList(build.branch.id)
        // Collects information for all stamps
        return stamps.collect { stamp ->
            def buildStamp = BuildValidationStamp.of(stamp)
            // Gets the latest runs with their status for this build and this stamp
            def runStatuses = getValidationRuns(buildId, stamp.id)
            buildStamp = buildStamp.withRuns(runStatuses)
            // OK
            buildStamp
        }
    }

    // Validation runs
	
	ValidationRunSummary readValidationRunSummary (ResultSet rs) {
		def id = rs.getInt("id")
		return new ValidationRunSummary(
			id,
            rs.getInt("indexNb"),
			rs.getString("description"),
			getBuild(rs.getInt("build")),
			getValidationStamp(rs.getInt("validation_stamp")),
			getLastValidationRunStatus(id))
	}
	
	@Override
	@Transactional(readOnly = true)
	public ValidationRunSummary getValidationRun(int id) {
		return dbLoad(SQL.VALIDATION_RUN, id) { readValidationRunSummary(it) }
	}

    List<BuildValidationStampRun> getValidationRuns (int buildId, int validationStampId) {
        def runIds = dbList(
                SQL.VALIDATION_RUN_FOR_BUILD_AND_STAMP,
                [build: buildId, validationStamp: validationStampId])
                { it.getInt("ID") }
        runIds.collect { runId ->
            def runStatus = getLastValidationRunStatus(runId)
            new BuildValidationStampRun(runId, runStatus.status, runStatus.description)
        }
    }
	
	// Validation run status

    ValidationRunStatusStub readValidationRunStatusStub (ResultSet rs) {
        new ValidationRunStatusStub (rs.getInt("id"), SQLUtils.getEnum(Status.class, rs, "status"), rs.getString("description"))
        // TODO Author
        // TODO Timestamp
    }
	
	ValidationRunStatusStub getLastValidationRunStatus (int validationRunId) {
		return dbLoad(SQL.VALIDATION_RUN_STATUS_LAST, validationRunId) { readValidationRunStatusStub (it) }
	}
	
	// Common
	
	@Override
	@Transactional(readOnly = true)
	public int getEntityId(Entity entity, String name, Map<Entity, Integer> parentIds) {
		def sql = "SELECT ID FROM ${entity.name()} WHERE ${entity.nameColumn()} = :name"
		def sqlParams = params("name", name)
		entity.parents.eachWithIndex { parent, index ->
			def parentId = parentIds[parent]
			sql += " AND ${parent} = :parent${index}"
			sqlParams.addValue("parent${index}", parentId)
		}
		Integer id = getFirstItem(sql, sqlParams, Integer.class)
		if (id == null) {
			throw new EntityNameNotFoundException (entity, name)
		} else {
			return id
		}
	}
	
	protected String getEntityName (Entity entity, int id) {
		def sql = "SELECT ${entity.nameColumn()} FROM ${entity.name()} WHERE ID = :id"
		String name = getFirstItem(sql, params("id", id), String.class)
		if (name == null) {
			throw new EntityIdNotFoundException (entity, id)
		} else {
			return name
		}
	}
}
