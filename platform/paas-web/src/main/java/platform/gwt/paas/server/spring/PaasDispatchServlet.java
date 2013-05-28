package platform.gwt.paas.server.spring;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.LogicsAwareDispatchServlet;
import platform.gwt.paas.server.handlers.*;

public class PaasDispatchServlet extends LogicsAwareDispatchServlet<PaasRemoteInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new AddUserHandler(this));
        registry.addHandler(new RemindPasswordHandler(this));
        registry.addHandler(new LogoutHandler(this));

        registry.addHandler(new GetProjectsHandler(this));
        registry.addHandler(new AddNewProjectHandler(this));
        registry.addHandler(new RemoveProjectHandler(this));
        registry.addHandler(new UpdateProjectHandler(this));

        registry.addHandler(new GetModulesHandler(this));
        registry.addHandler(new GetModuleTextHandler(this));
        registry.addHandler(new UpdateModulesHandler(this));
        registry.addHandler(new AddModulesHandler(this));
        registry.addHandler(new AddNewModuleHandler(this));
        registry.addHandler(new GetAvailableModulesHandler(this));
        registry.addHandler(new RemoveModuleFromProjectHandler(this));

        registry.addHandler(new GetConfigurationsHandler(this));
        registry.addHandler(new AddNewConfigurationHandler(this));
        registry.addHandler(new RemoveConfigurationHandler(this));
        registry.addHandler(new UpdateConfigurationHandler(this));
        registry.addHandler(new StartConfigurationHandler(this));
        registry.addHandler(new StopConfigurationHandler(this));
        registry.addHandler(new RestartConfigurationHandler(this));
    }
}
