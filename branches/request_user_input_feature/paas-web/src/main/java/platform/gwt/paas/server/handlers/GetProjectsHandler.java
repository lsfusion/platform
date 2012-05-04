package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetProjectsAction;
import platform.gwt.paas.shared.actions.GetProjectsResult;

import java.rmi.RemoteException;

@Component
public class GetProjectsHandler extends SimpleActionHandlerEx<GetProjectsAction, GetProjectsResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public GetProjectsHandler() {
        super(GetProjectsAction.class);
    }

    @Override
    public GetProjectsResult executeEx(final GetProjectsAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetProjectsResult(
                blProvider.getLogics().getProjects(
                        getAuthentication().getName()));
    }
}

