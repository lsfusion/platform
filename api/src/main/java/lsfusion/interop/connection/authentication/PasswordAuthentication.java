package lsfusion.interop.connection.authentication;

public class PasswordAuthentication extends Authentication {

    private final String password;

    public PasswordAuthentication(String login, String password) {
        super(login);
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }
}
