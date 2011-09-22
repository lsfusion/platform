package platform.gwt.paas.client.pages.login;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealRootLayoutContentEvent;
import platform.gwt.base.client.ui.login.LoginBox;
import platform.gwt.base.client.ui.login.SpringLoginBoxUiHandlers;
import platform.gwt.paas.client.NameTokens;
import platform.gwt.paas.client.PaasPlaceManager;
import platform.gwt.paas.client.login.LoginAuthenticatedEvent;

import static platform.gwt.paas.client.PaasPlaceManager.TARGET_PARAM;

public class LoginPagePresenter extends Presenter<LoginPagePresenter.MyView, LoginPagePresenter.MyProxy> {
    private final PaasPlaceManager placeManager;

    @ProxyStandard
    @NameToken(NameTokens.loginPage)
    @NoGatekeeper
    public interface MyProxy extends Proxy<LoginPagePresenter>, Place {
    }

    public interface MyView extends View {
        LoginBox getLoginBox();

        void resetAndFocus();
    }

    @Inject
    public LoginPagePresenter(EventBus eventBus, MyView view, MyProxy proxy, PaasPlaceManager placeManager) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;

        getView().getLoginBox().setUIHandlers(new MySpringLoginBoxUiHandlers());
    }

    @Override
    protected void onReset() {
        super.onReset();
        getView().resetAndFocus();
    }

    @Override
    protected void revealInParent() {
        RevealRootLayoutContentEvent.fire(this, this);
    }

    private class MySpringLoginBoxUiHandlers extends SpringLoginBoxUiHandlers {
        public MySpringLoginBoxUiHandlers() {
            super(getView().getLoginBox());
        }

        @Override
        protected void onLoginSucceded() {
            LoginAuthenticatedEvent.fire(getEventBus(), getView().getLoginBox().getUserName());
            placeManager.revealPlaceFromString(placeManager.getCurrentParameter(TARGET_PARAM, NameTokens.projectsListPage));
        }
    }
}
