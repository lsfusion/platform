package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetProjectsResult;
import platform.gwt.paas.shared.actions.UpdateProjectAction;

import java.rmi.RemoteException;

@Component
public class UpdateProjectHandler extends SimpleActionHandlerEx<UpdateProjectAction, GetProjectsResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public UpdateProjectHandler() {
        super(UpdateProjectAction.class);
    }

    @Override
    public GetProjectsResult executeEx(final UpdateProjectAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetProjectsResult(
                blProvider.getLogics().updateProject(
                        getAuthentication().getName(), action.project));
    }
}

