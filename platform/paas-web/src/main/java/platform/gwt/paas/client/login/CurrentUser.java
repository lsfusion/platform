package platform.gwt.paas.client.login;

public class CurrentUser {

    private String login;

    private boolean loggedIn = true;

    // private boolean authenticated = true;
    // private String email;
    // private String nickname;
    // private String realName;
    // private String locale;

    public CurrentUser(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}