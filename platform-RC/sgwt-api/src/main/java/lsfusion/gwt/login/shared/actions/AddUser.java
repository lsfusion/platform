package lsfusion.gwt.login.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class AddUser implements Action<StringResult> {
    public String username;
    public String email;
    public String password;
    public String firstName;
    public String lastName;
    public String captchaText;
    public String captchaSalt;

    public AddUser() {}

    public AddUser(String username, String email, String password, String firstName, String lastName, String captchaText, String captchaSalt) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.captchaText = captchaText;
        this.captchaSalt = captchaSalt;
    }
}
