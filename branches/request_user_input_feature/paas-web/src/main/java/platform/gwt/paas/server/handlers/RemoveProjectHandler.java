package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetProjectsResult;
import platform.gwt.paas.shared.actions.RemoveProjectAction;

import java.rmi.RemoteException;

@Component
public class RemoveProjectHandler extends SimpleActionHandlerEx<RemoveProjectAction, GetProjectsResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public RemoveProjectHandler() {
        super(RemoveProjectAction.class);
    }

    @Override
    public GetProjectsResult executeEx(final RemoveProjectAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetProjectsResult(
                blProvider.getLogics().removeProject(
                        getAuthentication().getName(), action.projectId));
    }
}

