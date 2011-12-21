package platform.gwt.paas.server.spring;

import com.gwtplatform.dispatch.server.spring.HandlerModule;
import com.gwtplatform.dispatch.server.spring.configuration.DefaultModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import platform.gwt.paas.server.handlers.*;
import platform.gwt.paas.server.handlers.validators.AllowActionValidator;
import platform.gwt.paas.shared.actions.*;

@Configuration
@Import({DefaultModule.class})
public class ServerModule extends HandlerModule {

    public ServerModule() {
    }

//    @Bean
//    public ActionValidator getDefaultActionValidator() {
//        return new LoggedInActionValidator();
//    }

    protected void configureHandlers() {
        bindHandler(AddUserAction.class, AddUserHandler.class, AllowActionValidator.class);
        bindHandler(RemindPasswordAction.class, RemindPasswordHandler.class, AllowActionValidator.class);
        bindHandler(LogoutAction.class, LogoutHandler.class);

        bindHandler(GetProjectsAction.class, GetProjectsHandler.class);
        bindHandler(AddNewProjectAction.class, AddNewProjectHandler.class);
        bindHandler(RemoveProjectAction.class, RemoveProjectHandler.class);
        bindHandler(UpdateProjectAction.class, UpdateProjectHandler.class);

        bindHandler(GetModulesAction.class, GetModulesHandler.class);
        bindHandler(GetModuleTextAction.class, GetModuleTextHandler.class);
        bindHandler(UpdateModulesAction.class, UpdateModulesHandler.class);
        bindHandler(AddModulesAction.class, AddModulesHandler.class);
        bindHandler(AddNewModuleAction.class, AddNewModuleHandler.class);
        bindHandler(GetAvailableModulesAction.class, GetAvailableModulesHandler.class);
        bindHandler(RemoveModuleFromProjectAction.class, RemoveModuleFromProjectHandler.class);

        bindHandler(GetConfigurationsAction.class, GetConfigurationsHandler.class);
        bindHandler(AddNewConfigurationAction.class, AddNewConfigurationHandler.class);
        bindHandler(RemoveConfigurationAction.class, RemoveConfigurationHandler.class);
        bindHandler(UpdateConfigurationAction.class, UpdateConfigurationHandler.class);
        bindHandler(StartConfigurationAction.class, StartConfigurationHandler.class);
        bindHandler(StopConfigurationAction.class, StopConfigurationHandler.class);
        bindHandler(RestartConfigurationAction.class, RestartConfigurationHandler.class);
    }
}
