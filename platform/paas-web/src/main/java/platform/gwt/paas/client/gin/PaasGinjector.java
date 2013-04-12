package platform.gwt.paas.client.gin;

import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.DefaultGatekeeper;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import platform.gwt.paas.client.login.LoggedInGatekeeper;
import platform.gwt.paas.client.pages.error.ErrorPagePresenter;
import platform.gwt.paas.client.pages.login.LoginPagePresenter;
import platform.gwt.paas.client.pages.project.ProjectPagePresenter;
import platform.gwt.paas.client.pages.projectlist.ProjectsListPagePresenter;

@GinModules({PaasGinModule.class})
public interface PaasGinjector extends Ginjector {

    EventBus getEventBus();

    @DefaultGatekeeper
    LoggedInGatekeeper getLoggedInGatekeeper();

    PlaceManager getPlaceManager();

    //LoginPage
    Provider<LoginPagePresenter> getLoginPagePresenter();

    //ErrorPage
    AsyncProvider<ErrorPagePresenter> getErrorPagePresenter();

    //Projects page
    AsyncProvider<ProjectsListPagePresenter> getProjectsListPagePresenter();

    AsyncProvider<ProjectPagePresenter> getProjectPagePresenter();
}
