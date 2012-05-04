package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class AddUserAction extends UnsecuredActionImpl<AddUserResult> {
    public String username;
    public String email;
    public String password;
    public String firstName;
    public String lastName;
    public String captchaText;
    public String captchaSalt;

    public AddUserAction(){}

    public AddUserAction(String username, String email, String password, String firstName, String lastName, String captchaText, String captchaSalt) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.captchaText = captchaText;
        this.captchaSalt = captchaSalt;
    }
}
