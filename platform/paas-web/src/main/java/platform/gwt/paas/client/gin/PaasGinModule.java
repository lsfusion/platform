package platform.gwt.paas.client.gin;

import com.google.inject.Singleton;
import com.gwtplatform.mvp.client.RootPresenter;
import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import com.gwtplatform.mvp.client.gin.DefaultModule;
import platform.gwt.paas.client.PaasPlaceManager;
import platform.gwt.paas.client.login.LoggedInGatekeeper;
import platform.gwt.paas.client.pages.error.ErrorPagePresenter;
import platform.gwt.paas.client.pages.error.ErrorPageView;
import platform.gwt.paas.client.pages.login.LoginPagePresenter;
import platform.gwt.paas.client.pages.login.LoginPageView;
import platform.gwt.paas.client.pages.project.ProjectPagePresenter;
import platform.gwt.paas.client.pages.project.ProjectPageView;
import platform.gwt.paas.client.pages.projectlist.ProjectsListPagePresenter;
import platform.gwt.paas.client.pages.projectlist.ProjectsListPageView;
import platform.gwt.paas.client.widgets.SmartGwtRootView;

public class PaasGinModule extends AbstractPresenterModule {

    @Override
    protected void configure() {
        install(new DefaultModule(PaasPlaceManager.class));

//        install is equivalent to....
//        bind(EventBus.class).to(SimpleEventBus.class).in(Singleton.class);
//        bind(TokenFormatter.class).to(ParameterTokenFormatter.class).in(Singleton.class);
//        bind(RootPresenter.class).asEagerSingleton();
//        bind(PlaceManager.class).to(MyPlaceManager.class).in(Singleton.class);
//        bind(GoogleAnalytics.class).to(GoogleAnalyticsImpl.class).in(Singleton.class);

        bind(SmartGwtRootView.class).in(Singleton.class);
        bind(RootPresenter.RootView.class).to(SmartGwtRootView.class);

        bind(PaasPlaceManager.class).in(Singleton.class);

        bind(LoggedInGatekeeper.class).in(Singleton.class);

        bindPresenter(LoginPagePresenter.class, LoginPagePresenter.MyView.class, LoginPageView.class, LoginPagePresenter.MyProxy.class);

        bindPresenter(ErrorPagePresenter.class, ErrorPagePresenter.MyView.class, ErrorPageView.class, ErrorPagePresenter.MyProxy.class);

        bindPresenter(ProjectsListPagePresenter.class, ProjectsListPagePresenter.MyView.class, ProjectsListPageView.class, ProjectsListPagePresenter.MyProxy.class);

        bindPresenter(ProjectPagePresenter.class, ProjectPagePresenter.MyView.class, ProjectPageView.class, ProjectPagePresenter.MyProxy.class);
    }
}
