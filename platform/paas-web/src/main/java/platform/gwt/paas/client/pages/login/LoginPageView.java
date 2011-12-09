package platform.gwt.paas.client.pages.login;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.sgwtbase.client.ui.CenterLayout;
import platform.gwt.sgwtbase.client.ui.login.LoginBox;

public class LoginPageView extends ViewImpl implements LoginPagePresenter.MyView {
    private VLayout mainPane;

    private LoginBox loginBox;

    @Inject
    public LoginPageView() {
        loginBox = new LoginBox(false);
        loginBox.setShowEdges(true);

        configureLayout();
    }

    private void configureLayout() {
        CenterLayout main = new CenterLayout(loginBox);

//        loginBox.setUserName("admin");
//        loginBox.setPassword("fusion");

        mainPane = new VLayout();
        mainPane.setWidth100();
        mainPane.setHeight100();
        mainPane.addMember(main);
    }

    public LoginBox getLoginBox() {
        return loginBox;
    }

    @Override
    public Widget asWidget() {
        return mainPane;
    }

    @Override
    public void resetAndFocus() {
        loginBox.resetAndFocus();
    }
}
