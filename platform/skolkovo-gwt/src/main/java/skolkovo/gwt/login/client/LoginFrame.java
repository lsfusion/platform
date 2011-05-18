package skolkovo.gwt.login.client;

import com.google.gwt.user.client.Window;
import skolkovo.gwt.base.client.BaseFrame;
import skolkovo.gwt.login.client.ui.LoginMainWidget;

public class LoginFrame extends BaseFrame {
    private static LoginFrameMessages messages = LoginFrameMessages.Instance.get();

    public void onModuleLoad() {
        Window.setTitle(messages.title());
        setAsRootPane(new LoginMainWidget());
    }
}
