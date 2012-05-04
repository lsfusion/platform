package platform.gwt.paas.client.pages.login;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.sgwtbase.client.ui.CenterLayout;
import platform.gwt.sgwtbase.client.ui.login.LoginBox;
import platform.gwt.sgwtbase.client.ui.register.RegisterBox;

public class LoginPageView extends ViewImpl implements LoginPagePresenter.MyView {
    private VLayout mainPane;

    CenterLayout loginMain;
    private LoginBox loginBox;
    private RegisterBox registerBox;

    @Inject
    public LoginPageView() {
        loginBox = new LoginBox();
        loginBox.setShowEdges(true);

        registerBox = new RegisterBox();
        registerBox.setShowEdges(true);

        configureLayout();
    }

    private void configureLayout() {
        loginMain = new CenterLayout(loginBox);
        final CenterLayout registerMain = new CenterLayout(registerBox);

//        loginBox.setUserName("admin");
//        loginBox.setPassword("fusion");

        mainPane = new VLayout();
        mainPane.setWidth100();
        mainPane.setHeight100();
        mainPane.addMember(loginMain);
        mainPane.addMember(registerMain);
        mainPane.setVisibleMember(loginMain);

        loginBox.getRegisterButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                mainPane.setVisibleMember(registerMain);
            }
        });

        registerBox.getCancelButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                mainPane.setVisibleMember(loginMain);
            }
        });
    }

    public LoginBox getLoginBox() {
        return loginBox;
    }
    
    public RegisterBox getRegisterBox() {
        return registerBox;
    }

    @Override
    public Widget asWidget() {
        return mainPane;
    }

    @Override
    public void resetAndFocus() {
        mainPane.setVisibleMember(loginMain);
        loginBox.resetAndFocus();
        registerBox.reset();
    }
}
