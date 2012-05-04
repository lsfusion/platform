package platform.gwt.login.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import net.customware.gwt.dispatch.shared.general.StringResult;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.login.shared.actions.AddUser;
import platform.gwt.sgwtbase.client.ui.CenterLayout;
import platform.gwt.sgwtbase.client.ui.register.RegisterBox;
import platform.gwt.sgwtbase.client.ui.ToolStripPanel;
import platform.gwt.sgwtbase.client.ui.login.LoginBox;
import platform.gwt.sgwtbase.client.ui.login.SpringLoginBoxUiHandlers;
import platform.gwt.base.shared.MessageException;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.login.shared.actions.RemindPassword;
import platform.gwt.sgwtbase.client.ui.register.RegisterBoxUiHandlers;

public class LoginFrame extends VLayout implements EntryPoint {
    private static final LoginFrameMessages messages = LoginFrameMessages.Instance.get();

    private final static StandardDispatchAsync loginService = new StandardDispatchAsync(new DefaultExceptionHandler());

    private LoginBox loginBox = new LoginBox();
    private RegisterBox registerBox = new RegisterBox();
    private VLayout centerComponent;
    private ToolStripPanel toolStrip;

    public void onModuleLoad() {
        Window.setTitle(messages.title());

        setWidth100();
        setHeight100();

        centerComponent = new VLayout();
        centerComponent.setAutoHeight();
        centerComponent.setAutoWidth();

        String userName = getUserName();
        toolStrip = new ToolStripPanel("logo_toolbar.png", messages.title(), userName != null);
        if (userName == null) {
            loginBox = new LoginBox(true, false);
            loginBox.setUIHandlers(new MySpringLoginBoxUiHandlers());

            registerBox.setUiHandlers(new RegisterBoxUiHandler());

            centerComponent.addMember(loginBox);
            centerComponent.addMember(registerBox);
            centerComponent.setVisibleMember(loginBox);
        } else {
            Label lbInfo = new Label(messages.loggedInMessage(userName));
            lbInfo.setAlign(Alignment.CENTER);

            centerComponent.setWidth(300);
            centerComponent.addMember(lbInfo);
        }

        final CenterLayout main = new CenterLayout(centerComponent);

        loginBox.getRegisterButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                centerComponent.setVisibleMember(registerBox);
                toolStrip.setTitle(messages.registerTitle());
                Window.setTitle(messages.registerTitle());
            }
        });

        registerBox.getCancelButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                centerComponent.setVisibleMember(loginBox);
                toolStrip.setTitle(messages.title());
                Window.setTitle(messages.title());
            }
        });

        addMember(toolStrip);
        addMember(main);

        draw();

        GwtClientUtils.removeLoaderFromHostedPage();
    }

    public String getUserName() {
        try {
            Dictionary dict = Dictionary.getDictionary("parameters");
            if (dict != null) {
                return dict.get("userName");
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private class RegisterBoxUiHandler implements RegisterBoxUiHandlers {
        @Override
        public void register() {
            loginService.execute(new AddUser(registerBox.getUsername(), registerBox.getEmail(), registerBox.getPassword(),
                    registerBox.getFirstName(), registerBox.getLastName(), registerBox.getCaptchaText(), registerBox.getCaptchaSalt()),
                    new ErrorAsyncCallback<StringResult>() {
                @Override
                public void success(StringResult result) {
                    if (result.get() == null) {
                        loginBox.setUserName(registerBox.getUsername());
                        loginBox.setPassword(registerBox.getPassword());
                        loginBox.login();
                    } else {
                        registerBox.showError(result.get());
                    }
                }
            });
        }
    }

    private class MySpringLoginBoxUiHandlers extends SpringLoginBoxUiHandlers {
        public MySpringLoginBoxUiHandlers() {
            super(loginBox);
        }

        @Override
        protected void onRemind() {
            loginService.execute(new RemindPassword(loginBox.getEmail()), new AsyncCallback<VoidResult>() {
                @Override
                public void onFailure(Throwable t) {
                    if (t instanceof MessageException) {
                        showError(t.getMessage());
                    } else {
                        showError(messages.remindError());
                    }
                    onRemindFailed();
                }

                @Override
                public void onSuccess(VoidResult result) {
                    onRemindSucceded();
                }
            });
        }
    }
}
