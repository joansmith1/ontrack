package net.ontrack.backend;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import net.ontrack.core.model.*;
import net.ontrack.core.security.GlobalFunction;
import net.ontrack.core.security.ProjectFunction;
import net.ontrack.service.EventService;
import net.ontrack.service.ManagementService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class ManagementServiceTest extends AbstractValidationTest {

    @Autowired
    private ManagementService service;
    @Autowired
    private EventService eventService;

    @Test
    public void createProject() throws Exception {
        final String projectName = uid("PRJ");
        ProjectSummary summary = asAdmin().call(new Callable<ProjectSummary>() {
            @Override
            public ProjectSummary call() throws Exception {
                return service.createProject(new ProjectCreationForm(projectName, "My description"));
            }
        });
        assertNotNull(summary);
        assertEquals(projectName, summary.getName());
        assertEquals("My description", summary.getDescription());
    }

    @Test
    public void createProject_name_format() throws Exception {
        validateNOK(" - Name: must match \"[A-Za-z0-9_.-]*\"\n",
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        return asAdmin().call(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                service.createProject(new ProjectCreationForm("Project 1", "My description"));
                                return null;
                            }
                        });
                    }
                });
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    public void createProject_anonymous_rejected() throws Exception {
        asAnonymous().call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.createProject(new ProjectCreationForm(uid("P"), "Cannot create project"));
                return null;
            }
        });
    }

    @Test(expected = AccessDeniedException.class)
    public void createProject_user_rejected() throws Exception {
        asUser().call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.createProject(new ProjectCreationForm(uid("P"), "Cannot create project"));
                return null;
            }
        });
    }

    @Test
    public void createProject_user_granted() throws Exception {
        final String projectName = uid("P");
        ProjectSummary project = asUser().withGlobalFn(GlobalFunction.PROJECT_CREATE).call(new Callable<ProjectSummary>() {
            @Override
            public ProjectSummary call() throws Exception {
                return service.createProject(new ProjectCreationForm(projectName, "Can create project"));
            }
        });
        assertNotNull(project);
        assertEquals(projectName, project.getName());
    }

    @Test
    public void loadProject() throws Exception {
        // Creates the project
        ProjectSummary project = doCreateProject();
        // Gets the project
        ProjectSummary loadedProject = service.getProject(project.getId());
        assertEquals(project.getName(), loadedProject.getName());
        assertEquals(project.getDescription(), loadedProject.getDescription());
        // Gets the audit
        List<ExpandedEvent> events = eventService.list(new EventFilter(0, 1));
        assertNotNull(events);
        assertFalse(events.isEmpty());
        ExpandedEvent event = events.get(0);
        assertEquals(EventType.PROJECT_CREATED, event.getEventType());
        assertEquals(
                Collections.singletonMap(
                        Entity.PROJECT,
                        new EntityStub(Entity.PROJECT, project.getId(), project.getName())),
                event.getEntities());
    }

    @Test
    public void deleteProject() throws Exception {
        // Creates the project
        final ProjectSummary project = doCreateProject();
        // Deletes the project
        Ack ack = asAdmin().call(new Callable<Ack>() {
            @Override
            public Ack call() throws Exception {
                return service.deleteProject(project.getId());
            }
        });
        assertTrue(ack.isSuccess());
        // Gets the audit
        List<ExpandedEvent> events = eventService.list(new EventFilter(0, 1));
        assertNotNull(events);
        assertFalse(events.isEmpty());
        // Deletion
        ExpandedEvent event = events.get(0);
        assertEquals(EventType.PROJECT_DELETED, event.getEventType());
        assertEquals(
                Collections.singletonMap(
                        "project",
                        project.getName()),
                event.getValues());
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    public void deleteProject_anonymous_denied() throws Exception {
        // Creates the project
        final ProjectSummary project = doCreateProject();
        // Tries to delete it
        asAnonymous().call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.deleteProject(project.getId());
                return null;
            }
        });
    }

    @Test(expected = AccessDeniedException.class)
    public void deleteProject_granted_for_another_project_denied() throws Exception {
        // Creates the project
        final ProjectSummary project = doCreateProject();
        // Tries to delete it
        asUser().withProjectFn(ProjectFunction.PROJECT_DELETE, project.getId() + 1).call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.deleteProject(project.getId());
                return null;
            }
        });
    }

    @Test
    public void deleteProject_granted_ok() throws Exception {
        // Creates the project
        final ProjectSummary project = doCreateProject();
        // Tries to delete it
        Ack ack = asUser().withProjectFn(ProjectFunction.PROJECT_DELETE, project.getId()).call(new Callable<Ack>() {
            @Override
            public Ack call() throws Exception {
                return service.deleteProject(project.getId());
            }
        });
        // Check
        assertTrue(ack.isSuccess());
    }

    @Test
    public void getBranchList() throws Exception {
        ProjectSummary p = doCreateProject();
        BranchSummary b1 = doCreateBranch(p.getId());
        BranchSummary b2 = doCreateBranch(p.getId());
        List<BranchSummary> branches = service.getBranchList(p.getId());
        assertNotNull(branches);
        assertEquals(2, branches.size());
        assertEquals(b1.getName(), branches.get(0).getName());
        assertEquals(p.getName(), branches.get(0).getProject().getName());
        assertEquals(b2.getName(), branches.get(1).getName());
        assertEquals(p.getName(), branches.get(1).getProject().getName());
    }

    @Test
    public void getBranch() throws Exception {
        BranchSummary branch = doCreateBranch();
        BranchSummary loadedBranch = service.getBranch(branch.getId());
        assertNotNull(loadedBranch);
        assertEquals(branch.getName(), loadedBranch.getName());
        assertEquals(branch.getDescription(), loadedBranch.getDescription());
        assertEquals(branch.getProject().getName(), loadedBranch.getProject().getName());
    }

    @Test
    public void createBranch() throws Exception {
        final ProjectSummary project = doCreateProject();
        final String branchName = uid("BCH");
        BranchSummary branch = asAdmin().call(new Callable<BranchSummary>() {
            @Override
            public BranchSummary call() throws Exception {
                return service.createBranch(project.getId(), new BranchCreationForm(branchName, "Project 2 branch 2"));
            }
        });
        assertNotNull(branch);
        assertEquals(branchName, branch.getName());
        assertEquals("Project 2 branch 2", branch.getDescription());
        assertEquals(project.getName(), branch.getProject().getName());
    }

    /**
     * Regression test for #276 - check that autopromotion level is kept when
     * cloning a branch.
     */
    @Test
    public void clone_autopromotion_level() throws Exception {
        // Creates a promotion level for a branch
        final PromotionLevelSummary pl = doCreatePromotionLevel();
        // Associates at least one validation stamp to this promotion level (for auto promotion to be eligible)
        final ValidationStampSummary stamp = doCreateValidationStamp(pl.getBranch().getId());
        asAdmin().call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.linkValidationStampToPromotionLevel(stamp.getId(), pl.getId());
                return null;
            }
        });
        // Sets the auto promotion to true
        asAdmin().call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                service.setPromotionLevelAutoPromote(pl.getId());
                return null;
            }
        });
        // Clones the branch
        BranchSummary clonedBranch = asAdmin().call(new Callable<BranchSummary>() {
            @Override
            public BranchSummary call() throws Exception {
                return service.cloneBranch(
                        pl.getBranch().getId(),
                        new BranchCloneForm(
                                uid("BCH"),
                                "Cloned branch",
                                Collections.<PropertyCreationForm>emptySet(),
                                Collections.<PropertyReplacement>emptySet(),
                                Collections.<PropertyReplacement>emptySet()
                        )
                );
            }
        });
        // Checks that the promotion level has been cloned and is auto promoted
        PromotionLevelSummary clonedPl = Iterables.find(
                service.getPromotionLevelList(clonedBranch.getId()),
                new Predicate<PromotionLevelSummary>() {
                    @Override
                    public boolean apply(PromotionLevelSummary o) {
                        return pl.getName().equals(o.getName());
                    }
                }
        );
        assertTrue("Cloned promotion level must be auto promoted", clonedPl.isAutoPromote());
    }

}
