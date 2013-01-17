package platform.interop.remote;

import java.io.Serializable;
import java.util.List;

public class UserInfo implements Serializable {
    public String username;
    public String password;
    public static String salt = "1234567890salt";
    public List<String> roles;

    public UserInfo(String username, String password, List<String> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }
}
