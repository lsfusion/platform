package lsfusion.interop.remote;

import java.io.Serializable;
import java.util.List;

public class UserInfo implements Serializable {
    public static String salt = "sdkvswhw34839h";

    public String username;
    public String password;
    public List<String> roles;

    public UserInfo(String username, String password, List<String> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }
}
