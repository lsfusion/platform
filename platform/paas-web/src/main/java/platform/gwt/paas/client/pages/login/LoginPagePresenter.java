package platform.gwt.paas.client.pages.login;

import com.google.web.bindery.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.dispatch.shared.DispatchAsync;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealRootContentEvent;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.paas.shared.exceptions.MessageException;
import platform.gwt.paas.client.common.ErrorHandlingCallback;
import platform.gwt.paas.shared.actions.AddUserResult;
import platform.gwt.paas.shared.actions.AddUserAction;
import platform.gwt.paas.shared.actions.RemindPasswordAction;
import platform.gwt.paas.shared.actions.VoidResult;
import platform.gwt.sgwtbase.client.ui.login.LoginBox;
import platform.gwt.sgwtbase.client.ui.login.SpringLoginBoxUiHandlers;
import platform.gwt.paas.client.NameTokens;
import platform.gwt.paas.client.PaasPlaceManager;
import platform.gwt.paas.client.login.LoginAuthenticatedEvent;
import platform.gwt.sgwtbase.client.ui.register.RegisterBox;
import platform.gwt.sgwtbase.client.ui.register.RegisterBoxUiHandlers;

public class LoginPagePresenter extends Presenter<LoginPagePresenter.MyView, LoginPagePresenter.MyProxy> {
    private final PaasPlaceManager placeManager;

    @Inject
    private DispatchAsync dispatcher;

    @ProxyStandard
    @NameToken(NameTokens.loginPage)
    @NoGatekeeper
    public interface MyProxy extends Proxy<LoginPagePresenter>, Place {
    }

    public interface MyView extends View {
        LoginBox getLoginBox();
        RegisterBox getRegisterBox(); 

        void resetAndFocus();
    }

    @Inject
    public LoginPagePresenter(EventBus eventBus, MyView view, MyProxy proxy, PaasPlaceManager placeManager) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;

        getView().getLoginBox().setUIHandlers(new MySpringLoginBoxUiHandlers());
        
        getView().getRegisterBox().setUiHandlers(new PaasRegisterBoxUiHandler());
    }

    @Override
    protected void onReset() {
        super.onReset();
        getView().resetAndFocus();
    }

    @Override
    protected void revealInParent() {
        RevealRootContentEvent.fire(this, this);
    }

    private class MySpringLoginBoxUiHandlers extends SpringLoginBoxUiHandlers {
        public MySpringLoginBoxUiHandlers() {
            super(getView().getLoginBox());
        }

        @Override
        protected void onLoginSucceded() {
            LoginAuthenticatedEvent.fire(getEventBus(), getView().getLoginBox().getUserName());
            placeManager.revealPlaceFromString(placeManager.getCurrentParameter(GwtClientUtils.TARGET_PARAM, NameTokens.projectsListPage));
        }

        @Override
        protected void onRemind() {
            dispatcher.execute(new RemindPasswordAction(getView().getLoginBox().getEmail()), new ErrorHandlingCallback<VoidResult>() {
                @Override
                public void failure(Throwable caught) {
                    if (caught instanceof MessageException) {
                        showError(caught.getMessage());
                    } else {
                        remindError();
                    }
                    onRemindFailed();
                }

                @Override
                public void success(VoidResult result) {
                    onRemindSucceded();
                }
            });
        }
    }

    private class PaasRegisterBoxUiHandler implements RegisterBoxUiHandlers {
        @Override
        public void register() {
            final RegisterBox rBox = getView().getRegisterBox();
            dispatcher.execute(new AddUserAction(rBox.getUsername(), rBox.getEmail(), rBox.getPassword(),
                    rBox.getFirstName(), rBox.getLastName(), rBox.getCaptchaText(), rBox.getCaptchaSalt()), new ErrorHandlingCallback<AddUserResult>() {
                @Override
                public void success(AddUserResult result) {
                    if (result.result == null) {
                        LoginBox lBox = getView().getLoginBox();
                        lBox.setUserName(rBox.getUsername());
                        lBox.setPassword(rBox.getPassword());
                        lBox.login();
                    } else {
                        rBox.showError(result.result);
                    }
                }
            });
        }
    }
}
