package platform.gwt.paas.client.pages.projectlist;

import com.google.web.bindery.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.dispatch.shared.DispatchAsync;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.*;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import paas.api.gwt.shared.dto.ProjectDTO;
import platform.gwt.paas.client.NameTokens;
import platform.gwt.paas.client.PaasPlaceManager;
import platform.gwt.paas.client.common.ErrorHandlingCallback;
import platform.gwt.paas.client.data.ProjectRecord;
import platform.gwt.paas.client.pages.projectlist.edit.EditProjectDialog;
import platform.gwt.paas.client.pages.projectlist.edit.EditProjectUIHandlers;
import platform.gwt.paas.shared.actions.*;

import static platform.gwt.paas.client.Parameters.PROJECT_ID_PARAM;

public class ProjectsListPagePresenter extends Presenter<ProjectsListPagePresenter.MyView, ProjectsListPagePresenter.MyProxy> implements ProjectsListPageUIHandlers {

    @Inject
    private DispatchAsync dispatcher;
    @Inject
    private PaasPlaceManager placeManager;

    @ProxyCodeSplit
    @NameToken(NameTokens.projectsListPage)
    public interface MyProxy extends Proxy<ProjectsListPagePresenter>, Place {
    }

    public interface MyView extends View, HasUiHandlers<ProjectsListPageUIHandlers> {
        void setProjects(ProjectDTO[] projects);

        ProjectInfoPane getProjectInfoPane();
    }

    @Inject
    public ProjectsListPagePresenter(EventBus eventBus, MyView view, MyProxy proxy) {
        super(eventBus, view, proxy);

        getView().setUiHandlers(this);
    }

    public void projectSelected(ProjectRecord record) {
        getView().getProjectInfoPane().setProject(record);
    }

    @Override
    public void refreshButtonClicked(boolean refreshContent) {
        updateProjectList();
    }

    @Override
    public void openProject(ProjectRecord selectedProject) {
        placeManager.revealPlace(
                new PlaceRequest(NameTokens.projectPage)
                        .with(PROJECT_ID_PARAM, "" + selectedProject.getId())
        );
    }

    @Override
    public void deleteProject(final ProjectRecord selectedProject) {
        SC.confirm("Are you sure, you want to delete this project?", new BooleanCallback() {
            @Override
            public void execute(Boolean value) {
                if (value != null && value) {
                    dispatcher.execute(new RemoveProjectAction(selectedProject.getId()), new GetProjectsCallback());
                }
            }
        });
    }

    @Override
    public void addNewProject() {
        EditProjectDialog.showAddDialog(new EditProjectUIHandlers() {
            @Override
            public void onOk(ProjectDTO project) {
                dispatcher.execute(new AddNewProjectAction(project), new GetProjectsCallback());
            }
        });
    }

    @Override
    public void editProject(ProjectRecord selectedProject) {
        EditProjectDialog.showEditDialog(selectedProject, new EditProjectUIHandlers() {
            @Override
            public void onOk(ProjectDTO project) {
                dispatcher.execute(new UpdateProjectAction(project), new GetProjectsCallback());
            }
        });
    }

    @Override
    protected void onReset() {
        super.onReset();

        updateProjectList();
    }

    private void updateProjectList() {
        dispatcher.execute(new GetProjectsAction(), new GetProjectsCallback());
    }

    @Override
    protected void revealInParent() {
        RevealRootContentEvent.fire(this, this);
    }

    private class GetProjectsCallback extends ErrorHandlingCallback<GetProjectsResult> {
        @Override
        public void success(GetProjectsResult result) {
            getView().setProjects(result.projects);
        }
    }
}