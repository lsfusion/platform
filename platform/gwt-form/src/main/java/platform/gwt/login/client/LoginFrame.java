package platform.gwt.login.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.ui.CenterLayout;
import platform.gwt.base.client.ui.ToolStripPanel;
import platform.gwt.base.client.ui.login.LoginBox;
import platform.gwt.base.client.ui.login.SpringLoginBoxUiHandlers;
import platform.gwt.base.shared.MessageException;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.login.shared.actions.RemindPassword;
import platform.gwt.utils.GwtUtils;

public class LoginFrame extends VLayout implements EntryPoint {
    private static final LoginFrameMessages messages = LoginFrameMessages.Instance.get();

    private final static StandardDispatchAsync loginService = new StandardDispatchAsync(new DefaultExceptionHandler());

    private LoginBox loginBox = new LoginBox();

    public void onModuleLoad() {
        Window.setTitle(messages.title());

        setWidth100();
        setHeight100();

        VLayout centerComponent = new VLayout();
        centerComponent.setAutoHeight();
        centerComponent.setAutoWidth();

        String userName = getUserName();
        if (userName == null) {
            loginBox = new LoginBox();
            loginBox.setUIHandlers(new MySpringLoginBoxUiHandlers());

            centerComponent.addMember(loginBox);
        } else {
            Label lbInfo = new Label(messages.loggedInMessage(userName));
            lbInfo.setAlign(Alignment.CENTER);

            centerComponent.setWidth(300);
            centerComponent.addMember(lbInfo);
        }

        CenterLayout main = new CenterLayout(centerComponent);

        addMember(new ToolStripPanel("logo_toolbar.png", messages.title(), userName != null));
        addMember(main);

        draw();

        GwtUtils.removeLoaderFromHostedPage();
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
